package com.hwbs.intertask.shared;

/**
 * User:      kaa
 * Timestamp: 1/7/14 1:57 PM
 */
public class CacheState {

    private boolean isValid;
    private long timePassedMS;
    private boolean loading;
    private boolean empty;

    public CacheState() {}

    public CacheState(boolean isValid, boolean loading, boolean empty, long timePassedMS) {
        this.isValid      = isValid;
        this.loading      = loading;
        this.empty        = empty;
        this.timePassedMS = timePassedMS;
    }

    public boolean isValid() {
        return isValid;
    }

    public long getTimeRegenerating() {
        return timePassedMS;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isEmpty() {
        return empty;
    }

}
