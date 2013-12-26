package com.hwbs.intertask.server.generator;

import com.hwbs.intertask.shared.Feedback;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User:      kaa
 * Timestamp: 12/22/13 12:17 AM
 */
public class TaskResult {
    private ArrayList<Future<?>> futures;
    protected boolean         canGetResult;
    private   static final int DEFAULT_RESULTS_CAPACITY = 1024 * 128;

    public TaskResult(int capacityHint) {
        futures      = new ArrayList<>( capacityHint );
    }

    public TaskResult() {
        this(DEFAULT_RESULTS_CAPACITY);
    }

    public void pushFuture(Future<?> f) {
        futures.add(f);
    }

    public void waitAll( Feedback feedback) {
        int completed = 0;
        for( Future<?> f : futures) {
            try {
                f.get();
                completed++;

                if (null != feedback) {
                    feedback.caclPercents(completed, futures.size());
                }

            } catch (ExecutionException e) {
                // continue
            } catch (InterruptedException e) {
                // continue
            }
        }

        canGetResult = true;
    }

    public void waitAll() {
        waitAll(null);
    }
}
