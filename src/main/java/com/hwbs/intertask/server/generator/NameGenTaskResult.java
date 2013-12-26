package com.hwbs.intertask.server.generator;

import com.hwbs.intertask.server.generator.mt.NamesGenExecutor;
import com.hwbs.intertask.shared.NameRecord;

public class NameGenTaskResult extends TaskResult {
    private NameRecord[]           cache;

    public NameGenTaskResult(int tasksHint, NamesGenExecutor.ActiveCache ac) {
        super(tasksHint);
        this.cache = ac.get();
    }

    public NameGenTaskResult(NamesGenExecutor.ActiveCache ac) {
        super();
        this.cache = ac.get();
    }

    public NameRecord[] getRusult() throws IllegalStateException {
        if (canGetResult)
            return cache;

        throw new IllegalStateException("should wait for completion before retrieving");
    }

}

