package com.hwbs.intertask.server.generator.mt;

import com.hwbs.intertask.shared.NameRecord;

/**
 * User:      kaa
 * Timestamp: 12/23/13 8:45 PM
 */
public class ArrayCopyTask implements Runnable {

    private NameRecord[] src,dst;
    private int j,k;

    public ArrayCopyTask(NameRecord[] src, NameRecord[] dst, int j, int k) {
        this.src = src;
        this.dst = dst;
        this.j   = j;
        this.k   = k;
    }


    @Override
    public void run() {

        // System.err.printf("segment(%d,%d)\n", j, k);

        for(int i = j; i < k ; ++i) {
            dst[i] = src[i].clone();
        }
    }
}
