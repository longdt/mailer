package vn.com.vndirect.pool;


/**
 * @author Daniel
 * AutoCloseable.close() is not idemponent, so don't close it multiple times!
 */
public class Poolable<T> implements AutoCloseable {

    private final T object;
    private ObjectPool<T> pool;
    private volatile long lastAccessTs;

    public Poolable(T t, ObjectPool<T> pool) {
        this.object = t;
        this.pool = pool;
        this.lastAccessTs = System.currentTimeMillis();
    }

    public T getObject() {
        return object;
    }

    public ObjectPool<T> getPool() {
        return pool;
    }

    public void returnObject() {
        pool.returnObject(this);
    }

    public long getLastAccessTs() {
        return lastAccessTs;
    }

    public void setLastAccessTs(long lastAccessTs) {
        this.lastAccessTs = lastAccessTs;
    }

    /**
     * This method is not idemponent, don't call it twice, which will return the object twice to the pool and cause severe problems.
     */
    @Override
    public void close() {
        this.returnObject();
    }
}
