package com.hwbs.intertask.server.generator.mt;

import com.hwbs.intertask.shared.NameRecord;

/**
 * User:      kaa
 * Timestamp: 12/23/13 8:44 PM
 */
public class ArrayCopyFactory implements TasksAbstractFactory {

    private NameRecord[] dst, src;

    public ArrayCopyFactory(NameRecord[] src, NameRecord[] dst) {
        this.dst = dst;
        this.src = src;
    }


    @Override
    public Runnable makeSubTask(int j, int k) {
        return new ArrayCopyTask(src, dst, j, k);
    }
}
