package com.hwbs.intertask.server.generator.mt;

import com.hwbs.intertask.server.generator.NameGenTaskResult;
import com.hwbs.intertask.server.generator.TaskResult;
import com.hwbs.intertask.shared.NameRecord;

import java.util.logging.Logger;


public class NamesGenExecutor extends Executor {

    //
    // Number of item to process per task
    // Note: If reminder is less then this number it should be processed accordingly
    //
    public static final int SUBTASK_SET_SIZE  = 10000;
    // Number of names to generate
    public static final int NAMES_COUNT       = 1000000;


    //
    // Logging context
    //
    Logger logger = Logger.getLogger("nviewer");


    // Number of planes of namesCache arrays: used to make some speedup.
    // users can work with one copy while another being generated
    private static final int CACHE_SIZE  = 2;

    //
    // Switch planes not supported in this version due of strict memory restriction, so syncronization
    // must be used more aggressively
    //
    //private int activePlane;
    //private NameRecord[][] namesCache;

    //
    // TODO: refactor cache out
    //
    private NameRecord[] namesCache;

    public NamesGenExecutor(int threadPerCore, NameRecord[] cache) {
        super(threadPerCore, NAMES_COUNT, SUBTASK_SET_SIZE);
        namesCache = cache;
        //namesCache = fillCacheSingle();
    }

    public NamesGenExecutor(NameRecord[] cache) {
        this(CORE_SCALE_FACTOR, cache);
    }

    private NameRecord[][] fillCache() {

        NameRecord[][] cache = new NameRecord[CACHE_SIZE][NAMES_COUNT];

        for(int k = 0; k < CACHE_SIZE; k++) {
            cache[k] = new NameRecord[NAMES_COUNT];
            for(int i = 0; i < NAMES_COUNT; ++i) {
                cache[k][i] = new NameRecord();
            }
        }

        return cache;
    }

    public TaskResult generate() {
        return generate(new NameGenTaskResult( expectedSubTasksNum(), getActiveCache()));
    }
    
    public TaskResult generate( TaskResult tr) {
        return generate(new GenTasksFactory(getActiveRecords()), tr);
    }

    @Override
    public TaskResult generate(TasksAbstractFactory tf, TaskResult tr) {
        super.generate(tf, tr);
        switchPlane();
        return tr;
    }
    
    public static class ActiveCache {
        final NameRecord[] c;
        
        public ActiveCache(NameRecord[] c) {
            this.c = c;
        }
        
        public NameRecord[] get() {
            return c;
        }
    }
    
    public ActiveCache getActiveCache() {
        // return new ActiveCache(namesCache[activePlane]);
        return new ActiveCache(namesCache);
    }
    
    private NameRecord[] getActiveRecords() {
        //return namesCache[activePlane];
        return namesCache;
    }
    
    private void switchPlane() {
        // nop now:
        // activePlane ^= 1;
    }
}