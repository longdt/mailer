package vn.com.vndirect.pool;

import java.util.concurrent.TimeUnit;

/**
 * @author Daniel
 */
public class ObjectPool<T> {

    protected final PoolConfig config;
    protected final ObjectFactory<T> factory;
    protected final ObjectPoolPartition<T>[] partitions;
    private Scavenger scavenger;
    private volatile boolean shuttingDown;

    public ObjectPool(PoolConfig poolConfig, ObjectFactory<T> objectFactory) {
        this.config = poolConfig;
        this.factory = objectFactory;
        this.partitions = new ObjectPoolPartition[config.getPartitionSize()];
        try {
            for (int i = 0; i < config.getPartitionSize(); i++) {
                partitions[i] = new ObjectPoolPartition<T>(this, i, config, objectFactory);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (config.getScavengeIntervalMilliseconds() > 0) {
            this.scavenger = new Scavenger();
            this.scavenger.start();
        }
    }

    public Poolable<T> borrowObject() {
        return borrowObject(0);
    }

    public Poolable<T> borrowObject(long milisecondWait) {
        for (int i = 0; i < 3; i++) { // try at most three times
            Poolable<T> result = getObject(milisecondWait);
            if (result == null) {
                return null;
            } else if (factory.validate(result.getObject())) {
                return result;
            } else {
                this.partitions[result.getPartition()].decreaseObject(result);
            }
        }
        return null;
    }

    private Poolable<T> getObject(long milisecondWait) {
        if (shuttingDown) {
            throw new IllegalStateException("Your pool is shutting down");
        }
        int partition = (int) (Thread.currentThread().getId() % this.config.getPartitionSize());
        ObjectPoolPartition<T> subPool = this.partitions[partition];
        Poolable<T> freeObject = subPool.getObjectQueue().poll();
        if (freeObject == null) {
            // increase objects and return one, it will return null if reach max size
            subPool.increaseObjects(1);
            try {
                if (milisecondWait <= 0) {
                    freeObject = subPool.getObjectQueue().poll();
                } else {
                    freeObject = subPool.getObjectQueue().poll(milisecondWait, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e); // will never happen
            }
        }
        if (freeObject != null) {
            freeObject.setLastAccessTs(System.currentTimeMillis());
        }
        return freeObject;
    }

    public void returnObject(Poolable<T> obj) {
        ObjectPoolPartition<T> subPool = this.partitions[obj.getPartition()];
        try {
            subPool.getObjectQueue().put(obj);
            if (Log.isDebug())
                Log.debug("return object: queue size:", subPool.getObjectQueue().size(),
                        ", partition id:", obj.getPartition());
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // impossible for now, unless there is a bug, e,g. borrow once but return twice.
        }
    }

    public int getSize() {
        int size = 0;
        for (ObjectPoolPartition<T> subPool : partitions) {
            size += subPool.getTotalCount();
        }
        return size;
    }

    public synchronized int shutdown() throws InterruptedException {
        shuttingDown = true;
        int removed = 0;
        if (scavenger != null) {
            scavenger.interrupt();
            scavenger.join();
        }
        for (ObjectPoolPartition<T> partition : partitions) {
            removed += partition.shutdown();
        }
        return removed;
    }

    private class Scavenger extends Thread {

        @Override
        public void run() {
            int partition = 0;
            while (!ObjectPool.this.shuttingDown) {
                try {
                    Thread.sleep(config.getScavengeIntervalMilliseconds());
                    partition = ++partition % config.getPartitionSize();
                    Log.debug("scavenge sub pool ", partition);
                    partitions[partition].scavenge();
                } catch (InterruptedException ignored) {
                }
            }
        }

    }
}
