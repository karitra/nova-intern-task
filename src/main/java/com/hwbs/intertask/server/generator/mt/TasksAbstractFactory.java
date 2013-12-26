package com.hwbs.intertask.server.generator.mt;

/**
 * User:      kaa
 * Timestamp: 12/22/13 7:24 PM
 */
public interface TasksAbstractFactory {
    Runnable makeSubTask( int j, int k);
}
