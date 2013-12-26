package com.hwbs.intertask.shared;

import java.io.Serializable;

/**
 * User:      kaa
 * Timestamp: 12/20/13 12:40 PM
 */
public class ProgressState implements Serializable {


    private float percents;
    private boolean isDone;

    private ProgressState() {}

    public float getPercents() {
        return percents;
    }

    public void setPercents(float percents) {
        this.percents = percents;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public static ProgressState  make(float pc , boolean isDone) {
        ProgressState p = new ProgressState();
        p.setDone(isDone);
        p.setPercents(pc);
        return p;
    }
}
