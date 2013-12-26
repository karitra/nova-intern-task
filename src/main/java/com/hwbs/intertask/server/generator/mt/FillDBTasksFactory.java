package com.hwbs.intertask.server.generator.mt;

import com.hwbs.intertask.server.Connector;
import com.hwbs.intertask.shared.NameRecord;

import java.util.LinkedList;

/**
 * User:      kaa
 * Timestamp: 12/22/13 9:14 PM
 */
public class FillDBTasksFactory implements TasksAbstractFactory {

    private final NameRecord[] cache;
    private final Connector conn;

    private LinkedList<DBTask> list = new LinkedList<>();

    public FillDBTasksFactory(Connector conn, NameRecord[] cache) {
        this.cache = cache;
        this.conn  = conn;
    }

    @Override
    public Runnable makeSubTask(int j, int k) {
        DBTask t = new DBTask(conn, cache, j, k);
        list.add(t);
        return t;
    }

    public void cancelAll() {
        for(DBTask t : list) {
            t.cancel();
        }

        list.clear();
    }
}
