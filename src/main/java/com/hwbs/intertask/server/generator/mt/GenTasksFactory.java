package com.hwbs.intertask.server.generator.mt;

import com.hwbs.intertask.shared.NameRecord;

/**
 * User:      kaa
 * Timestamp: 12/22/13 2:19 PM
 */
public class GenTasksFactory implements TasksAbstractFactory {

    NameRecord[] records;
    public GenTasksFactory(NameRecord[] records) {
        this.records = records;
    }

    public Runnable makeSubTask( int j, int k) {
        return new RandGenTask(records, j, k);
    }
}
