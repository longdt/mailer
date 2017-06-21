package vn.com.vndirect.pool;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by naruto on 6/21/17.
 */
public class ConcurrentPool<T> implements ObjectPool<T> {
    protected final PoolConfig config;
    protected final ObjectFactory<T> factory;
    private volatile boolean shuttingDown;
    private final ConcurrentLinkedDeque<Poolable<T>> objectDeque;
    private ScheduledExecutorService scheduler;

    public ConcurrentPool(PoolConfig config, ObjectFactory<T> factory) {
        this.config = config;
        this.factory = factory;
        this.objectDeque = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < config.getMinSize(); i++) {
            objectDeque.offerFirst(new Poolable<>(factory.create(), this));
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
        long validationInterval = config.getScavengeIntervalMilliseconds();
        scheduler.scheduleWithFixedDelay(() -> {
            int size = objectDeque.size();
            if (size > config.getMaxSize()) {
                Poolable<T> obj = objectDeque.pollLast();
                if (obj != null) {
                    factory.destroy(obj.getObject());
                }
            }
            int checkSize = Math.min(size, config.getMinSize());
            int maxIdleMs = config.getMaxIdleMilliseconds();
            Poolable<T> obj;
            while (!shuttingDown && checkSize > 0 && (obj = objectDeque.pollLast()) != null) {
                if (obj.getLastAccessTs() + maxIdleMs > System.currentTimeMillis()) {
                    objectDeque.offerLast(obj);
                    break;
                } else if (factory.refresh(obj.getObject())) {
                    returnObject(obj);
                } else {
                    factory.destroy(obj.getObject());
                }
                --checkSize;
            }
        }, validationInterval, validationInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public Poolable<T> borrowObject() {
        Poolable<T> obj = objectDeque.pollFirst();
        if (obj == null) {
            return new Poolable<>(factory.create(), this);
        }
        return obj;
    }

    @Override
    public void returnObject(Poolable<T> obj) {
        objectDeque.offerFirst(obj);
        obj.setLastAccessTs(System.currentTimeMillis());
    }

    @Override
    public int getSize() {
        return objectDeque.size();
    }

    @Override
    public synchronized int shutdown() throws InterruptedException {
        if (shuttingDown) {
            return 0;
        }
        shuttingDown = true;
        scheduler.shutdown();
        int counter = 0;
        Poolable<T> obj;
        while ((obj = objectDeque.pollLast()) != null) {
            factory.destroy(obj.getObject());
            ++counter;
        }
        return counter;
    }
}
