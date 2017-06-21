package vn.com.vndirect.pool;

/**
 * Created by naruto on 6/21/17.
 */
public class PartitionPoolable<T> extends Poolable<T> {
    private int partition;

    public PartitionPoolable(T t, ObjectPool<T> pool, int partition) {
        super(t, pool);
        this.partition = partition;
    }

    public int getPartition() {
        return partition;
    }
}
