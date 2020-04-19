package liquibase.ext;

import liquibase.database.Database;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.DatabaseException;
import liquibase.executor.ExecutorService;
import liquibase.statement.core.RawCallStatement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostgresSessionBasedLockService extends AbstractLiquibaseSessionBasedLockService {

    @Override
    protected void lock(String lockId) throws DatabaseException {
        int lockNumber = asNumber(lockId);
        log.info("Acquiring session based exclusive lock {}", lockNumber);
        RawCallStatement sql = new RawCallStatement("select pg_advisory_lock(" + lockNumber + ")");
        ExecutorService.getInstance().getExecutor(database).execute(sql);
        log.info("Acquired lock {}, proceeding with liquibase", lockNumber);
    }

    @Override
    protected void unlock(String lockId) throws DatabaseException {
        int lockNumber = asNumber(lockId);
        RawCallStatement sql = new RawCallStatement("select pg_advisory_unlock(" + lockNumber + ")");
        ExecutorService.getInstance().getExecutor(database).execute(sql);
        log.info("Liquibase complete. Released lock {}", lockNumber);
    }

    @Override
    protected boolean sessionLockSupported(Database database) {
        return database instanceof PostgresDatabase;
    }

    protected int asNumber(String lockId) {
        return Math.abs(lockId.hashCode());
    }

}
