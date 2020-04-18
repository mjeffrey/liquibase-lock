# Proof of Concept for liquibase session locks

The StandardLockService in Liquibase works by creating a single record table and setting a column 'LOCKED' to true and committing.
When the migrations are complete the column is set to false.

Problems can arise if the column is not set back to false (for example the application is killed).
This can happen in large deployments in a CI/CD scenario where the migrations are applied automatically and if there are startup 
issues the containers are killed (we have experienced this many times especially when infrastructure changes causing the application to fail and flap).

If you are unlucky one of the kills happens while the locked column is true. Then the only option is a manual reset. 

Some databases provide general locking mechanism where the lock will persist beyond a transaction commit or rollback and only released 
when requested OR when the session dies.

Oracle has dbms_lock (supported since at least Oracle 8)
PostgreSQL has pg_advisory_lock https://www.postgresql.org/docs/12/explicit-locking.html#ADVISORY-LOCKS (also supported back to version 9)
 

To use allow liquibase Service Locator to find and use the Lock Services we place the class in the liquibase.ext package
with a high priority.

The example here is setup for Oracle.
