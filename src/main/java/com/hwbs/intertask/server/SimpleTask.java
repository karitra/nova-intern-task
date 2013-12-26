package com.hwbs.intertask.server;

import com.hwbs.intertask.shared.Feedback;

/**
 * User:      kaa
 * Timestamp: 12/22/13 6:41 PM
 */
public interface SimpleTask {
    void doit(Feedback feedback);
    void halt();

    //
    // Note: should be not synchronized with 'doit' as it intended to run in one thread with it
    // sequentially
    //
    void addParam(Object arg);
}
