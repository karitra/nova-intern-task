package com.hwbs.intertask.shared;

/**
 * User:      kaa
 * Timestamp: 12/22/13 5:57 PM
 */
public class Feedback {
    private float percents;

    public float getPercents() {
        return percents;
    }

    public void setPercents(float percents) {
        this.percents = percents;
    }

    public float caclPercents(int curret, int total) {
        float p = 100.f * curret / total;
        setPercents(p);
        return p;
    }

    public void reset() {
        setPercents(0f);
    }
}
