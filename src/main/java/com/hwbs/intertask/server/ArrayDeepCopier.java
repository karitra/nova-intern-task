package com.hwbs.intertask.server;

import com.hwbs.intertask.server.generator.TaskResult;
import com.hwbs.intertask.server.generator.mt.ArrayCopyFactory;
import com.hwbs.intertask.server.generator.mt.Executor;
import com.hwbs.intertask.shared.NameRecord;

/**
* User:      kaa
* Timestamp: 12/23/13 9:13 PM
*/
class ArrayDeepCopier {

    static NameRecord[] clone(NameRecord[] src) {
        NameRecord[] dst = new NameRecord[src.length];

        Executor e = new Executor(src.length, 100000);

        TaskResult tr = new TaskResult();
        ArrayCopyFactory af = new ArrayCopyFactory(src,dst);
        e.generate(af, tr);

        tr.waitAll();
        return dst;
    }
}
