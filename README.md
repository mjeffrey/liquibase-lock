# Proof of Concept for liquibase session locks

This example is based on a SpringBoot application created from here https://start.spring.io/
The POC is configured for Oracle and "Session Based" locking.
Postgres is easy to setup as well.

## Locking in Liquibase
The StandardLockService in Liquibase works by creating a single record table (default DATABASECHANGELOGLOCK) and 
setting a column 'LOCKED' to true and committing.
When the migrations are complete the column is set to false.

## Some issues
In a CI/CD environment it is convenient to just allow the application to apply the changes at startup.
However this can cause problems if the lock column is not set back to false (for example the application is killed).
If you are unlucky one of the kills happens while the locked column is true. Then the only option is a manual reset of the column. 

This can happen in large deployments where the migrations are applied automatically and if there are startup 
issues the containers are killed (we have experienced this many times especially when infrastructure changes causing the application to fail and flap).
And during development it will occur when the startup is aborted during liquibase processing.

A liquibase lock also prevents application auto scaling up until the lock is removed since the application will wait for the lock even though there are no changes to apply. 

## Possible Solutions
This POC shows a possible solution using session based locking (available in some databases).
Note To allow liquibase Service Locator to find and use these LockServices we place the class in the `liquibase.ext` package
with a high priority.

##  Solution 1 Session based locking
Some databases provide general locking mechanism where the lock will persist beyond a transaction commit or rollback and only released 
when requested OR when the session dies.

* [Postgres](https://www.postgresql.org/docs/12/explicit-locking.html#ADVISORY-LOCKS) (also supported back to version 9)
* [Oracle](https://docs.oracle.com/cd/B19306_01/appdev.102/b14258/d_lock.htm#ARPLS021) (since at least version 8) 

##  Solution 2 Locking in a separate connection
With normal database object locks (table locks, row locks etc) the database automatically releases them once the transaction commits or rollback.
Many databases autocommit on DDL operations and so all object locks are released.
 
The LockService could create a separate connection to issue a write lock on a table (e.g. DATABASECHANGELOGLOCK) without cahnging any data.
To release  the lock the connection would simply rollback. 
This has the advantage that it would work for most databases where liquibase locking is relevant (In-memory H2 databases don;t really need liquibase locking).

## Other Possible Enhancements
If liquibase checked if there were any changes to apply before taking the lock then auto scaling would not be broken (even if the lock was still there).
It may also improve startup performance in the common case where there are no DB changes. 

