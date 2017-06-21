package vn.com.vndirect.pool;

import java.util.concurrent.TimeUnit;

/**
 * @author Daniel
 */
public class PartitionObjectPool<T> implements ObjectPool<T> {
    protected final PoolConfig config;
    protected final ObjectFactory<T> factory;
    protected final SubPool<T>[] partitions;
    protected final boolean setlat;
    private Scavenger scavenger;
    private volatile boolean shuttingDown;

    public PartitionObjectPool(PoolConfig poolConfig, ObjectFactory<T> objectFactory) {
        this.config = poolConfig;
        this.factory = objectFactory;
        this.partitions = new SubPool[config.getPartitionSize()];
        try {
            for (int i = 0; i < config.getPartitionSize(); i++) {
                partitions[i] = new SubPool<>(this, i, config, objectFactory);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        setlat = config.getScavengeIntervalMilliseconds() > 0;
        if (config.getScavengeIntervalMilliseconds() > 0) {
            this.scavenger = new Scavenger();
            this.scavenger.start();
        }
    }

    @Override
    public Poolable<T> borrowObject() {
        return borrowObject(0);
    }

    public Poolable<T> borrowObject(long milisecondWait) {
        PartitionPoolable<T> result = getObject(milisecondWait);
        if (result == null) {
            return null;
        } else if (factory.validate(result.getObject())) {
            return result;
        }
        this.partitions[result.getPartition()].decreaseObject(result);
        return null;
    }

    private PartitionPoolable<T> getObject(long milisecondWait) {
        if (shuttingDown) {
            throw new IllegalStateException("Your pool is shutting down");
        }
        int partition = (int) (Thread.currentThread().getId() % this.config.getPartitionSize());
        SubPool<T> subPool = this.partitions[partition];
        PartitionPoolable<T> freeObject = subPool.getObjectQueue().poll();
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
        if (setlat && freeObject != null) {
            freeObject.setLastAccessTs(System.currentTimeMillis());
        }
        return freeObject;
    }

    @Override
    public void returnObject(Poolable<T> obj) {
        PartitionPoolable partObj = (PartitionPoolable) obj;
        SubPool<T> subPool = this.partitions[partObj.getPartition()];
        subPool.getObjectQueue().offerFirst(partObj);
        if (Log.isDebug())
            Log.debug("return object: queue size:", subPool.getObjectQueue().size(),
                    ", partition id:", partObj.getPartition());
    }

    @Override
    public int getSize() {
        int size = 0;
        for (SubPool<T> subPool : partitions) {
            size += subPool.getTotalCount();
        }
        return size;
    }

    @Override
    public synchronized int shutdown() throws InterruptedException {
        shuttingDown = true;
        int removed = 0;
        if (scavenger != null) {
            scavenger.interrupt();
            scavenger.join();
        }
        for (SubPool<T> partition : partitions) {
            removed += partition.shutdown();
        }
        return removed;
    }

    private class Scavenger extends Thread {

        @Override
        public void run() {
            int partition = 0;
            while (!PartitionObjectPool.this.shuttingDown) {
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
