package liquibase.ext;

import liquibase.database.Database;
import liquibase.database.core.OracleDatabase;
import liquibase.exception.DatabaseException;
import liquibase.executor.ExecutorService;
import liquibase.statement.core.RawCallStatement;
import lombok.extern.slf4j.Slf4j;

/**
 * The user executing the liquibase changesets must granted execute on
 * DBMS_LOCK (directly or via a role)
 */
@Slf4j
public class OracleSessionBasedLockService extends AbstractLiquibaseSessionBasedLockService {

    @Override
    protected void lock(String lockId) throws DatabaseException {
        log.info("Acquiring session based exclusive lock {}", lockId);
        RawCallStatement statement = new RawCallStatement(lockStatement("REQUEST", lockId));
        ExecutorService.getInstance().getExecutor(database).execute(statement);
        log.info("Acquired lock {}, proceeding with liquibase", lockId);
    }

    @Override
    protected void unlock(String lockId) throws DatabaseException {
        RawCallStatement statement = new RawCallStatement(lockStatement("RELEASE", lockId));
        ExecutorService.getInstance().getExecutor(database).execute(statement);
        log.info("Liquibase complete. Released lock {}", lockId);
    }


    @Override
    protected boolean sessionLockSupported(Database database) {
        return database instanceof OracleDatabase;
    }

    static final String templateString =
            "declare\n" +
            "  lockHandle VARCHAR2 (128);\n" +
            "  dummy INTEGER;\n" +
            "begin\n" +
            "   DBMS_LOCK.ALLOCATE_UNIQUE( '%s', lockHandle );\n" +
            "   dummy := DBMS_LOCK.%s( lockHandle );\n" +
            "end;";

    private String lockStatement( String procedureName, String lockName ){
        return String.format( templateString, lockName, procedureName);
    }

}
