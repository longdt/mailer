package vn.com.vndirect.pool;

/**
 * Created by naruto on 6/21/17.
 */
public interface ObjectPool<T> {
    Poolable<T> borrowObject();

    void returnObject(Poolable<T> obj);

    int getSize();

    int shutdown() throws InterruptedException;
}
