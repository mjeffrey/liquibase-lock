package be.sysa.liquibaselock;

import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.LockException;
import liquibase.lockservice.StandardLockService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractLiquibaseSessionBasedLockService extends StandardLockService {
    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean acquireLock() throws LockException {
        if (sessionLockSupported(database)) {
            return acquireSessionLock();
        } else {
            return super.acquireLock();
        }
    }

    @Override
    public void releaseLock() throws LockException {
        if (sessionLockSupported(database)) {
            releaseSessionLock();
        } else {
            super.releaseLock();
        }
    }
    protected abstract boolean sessionLockSupported(Database database);
    protected abstract void lock(String lockId) throws DatabaseException;
    protected abstract void unlock(String lockId)  throws DatabaseException;

    private boolean acquireSessionLock() throws LockException {

        try {
            lock(getLockId());
            return true;
        } catch (DatabaseException e) {
            log.error("Exception while acquiring lock", e);
            throw new LockException(e);
        }
    }

    private void releaseSessionLock() throws LockException {
        try {
            unlock(getLockId());
        } catch (DatabaseException e) {
            log.error("Exception while releasing lock. Lock will be removed when the database session disconnects", e);
            throw new LockException(e);
        }
    }

    protected String getLockId(){
        return ("liquibase-" + database.getConnection().getConnectionUserName()).toLowerCase();
    }

}
