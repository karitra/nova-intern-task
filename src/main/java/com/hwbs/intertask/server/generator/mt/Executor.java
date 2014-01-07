package com.hwbs.intertask.server.generator.mt;

import com.hwbs.intertask.server.generator.TaskResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User:      kaa
 * Timestamp: 12/21/13 6:29 PM
 */
public class Executor {
    //
    // Logging context
    //
    Logger logger = Logger.getLogger("nviewer");


    private ExecutorService pool;
    private final int itemsNum;
    private final int subTaskSize;

    public static final int CORE_SCALE_FACTOR = 1;

    public Executor(int itemsNum, int subTaskSize, boolean sequential) {
        this(CORE_SCALE_FACTOR, itemsNum, subTaskSize, sequential);
    }


    public Executor(int itemsNum, int subTaskSize) {
        // multi-threaded by default
        this(CORE_SCALE_FACTOR, itemsNum, subTaskSize);
    }

    public Executor(int threadPerCore, int itemsNum, int subTaskSize) {
        // multi-threaded by default
        this(threadPerCore, itemsNum, subTaskSize, false);
    }

    private Executor(int threadPerCore, int itemsNum, int subTaskSize, boolean sequence) {
        //
        // sanity check
        //
        if (itemsNum < 0) {
            itemsNum = 0;
        }

        if (subTaskSize < 0 || subTaskSize > itemsNum ) {
            subTaskSize = itemsNum;
        }

        this.itemsNum    = itemsNum;
        this.subTaskSize = subTaskSize;

        // System.err.printf("items: %d, subtask_size: %d\n", itemsNum, subTaskSize);

        if (!sequence) {
            if (threadPerCore <= 0)
                threadPerCore  = 1;

            //
            // according the doc and common sense returned number should be  >= 1
            //
            int cores = Runtime.getRuntime().availableProcessors();

            //logger.log(Level.INFO, "number of threads in pool: " + threadPerCore * cores );
            pool = Executors.newFixedThreadPool(threadPerCore * cores);
        } else {
            pool = Executors.newSingleThreadExecutor();
            logger.log(Level.INFO, "single threaded pool created" );
        }
    }

    protected ExecutorService getPool() {
        return pool;
    }

    /**
     * Don't forget to halt the pool when generator no more needed
     */
    public void halt() {
        // TODO: more robust (clever) halt process
        pool.shutdown();
    }


    public TaskResult generate( TasksAbstractFactory gf, TaskResult gr) {

        int tasks  = itemsNum / subTaskSize;
        int quonty = itemsNum % subTaskSize;

        // Note: should be at task result
        //NameGenTaskResult gr = new NameGenTaskResult( tasks + 1, ac);

        // logger.log(Level.INFO, "tasks number: " + tasks );

        int j = 0, k = 0;
        for(int i = 0; i < tasks; ++i) {
            j = i * subTaskSize;
            k = j + subTaskSize;
            //logger.log(Level.INFO, "submitting task in range: (" + j + "," + k + ")" );
            gr.pushFuture(pool.submit( gf.makeSubTask(j, k) ));
        }

        // last one ( if needed )
        if (quonty != 0) {
            j  = k;
            k  = j + quonty;
            //logger.log(Level.INFO, "submitting task in range: (" + j + "," + k + ")" );
            gr.pushFuture(pool.submit( gf.makeSubTask(j, k) ));
        }

        return gr;
    }

    public int expectedSubTasksNum() {
        return itemsNum / subTaskSize + 1;
    }
}
