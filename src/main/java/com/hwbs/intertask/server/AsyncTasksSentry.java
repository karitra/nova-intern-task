package com.hwbs.intertask.server;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User:      kaa
 * Timestamp: 12/24/13 12:59 AM
 */
public class AsyncTasksSentry {

    ConcurrentLinkedQueue<Future<?>> waitList =
            new ConcurrentLinkedQueue<>();

    public synchronized void add(Future<?> f) {
        waitList.add(f);
    }

    public synchronized void waitAll() {
        for(Future<?> future : waitList) {
            try {
                future.get();
            } catch (ExecutionException e) {
                continue;
            } catch (InterruptedException e) {
                continue;
            }
        }

        waitList.clear();
    }

}
