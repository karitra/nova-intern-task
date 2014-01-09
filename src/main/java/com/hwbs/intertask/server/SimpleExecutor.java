package com.hwbs.intertask.server;

import com.hwbs.intertask.shared.Feedback;
import com.hwbs.intertask.shared.ProgressState;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
* User:      kaa
* Timestamp: 12/22/13 6:44 PM
 *
 * TODO: merge this code with QueuedExecutor as it actually shares the the same
 * functionality now
*/
public class SimpleExecutor implements Runnable {

    public static final int SUBMIT_RETRIES_NUM = 20;
    public static final int MESSAGE_QUEUE_SIZE = 1024;

    public enum Command {
        Execute,
        Halt
    }

    private          boolean    stop;
    private volatile boolean running;

    private ArrayBlockingQueue<Command> msgQueue;
    private Object sentry;
    private Command cmd;
    private SimpleTask task;
    private final Feedback feedback;

    public SimpleExecutor(SimpleTask task) {
        this.task     = task;
        this.feedback = new Feedback();
        this.sentry   = new Object();

        this.msgQueue = new ArrayBlockingQueue<>(MESSAGE_QUEUE_SIZE);
    }

    @Override
    public void run() {
        while (!shouldStop()) {
            try {
                Command cmd = msgQueue.poll(100, TimeUnit.MILLISECONDS);

                if (null == cmd) {
                    continue;
                }

                running = true;

                switch (cmd) {
                    case Halt:
                        task.halt();
                        stopIt();
                        break;
                    case Execute:
                        feedback.reset();
                        task.doit(feedback);
                        break;
                }

            } catch (InterruptedException e) {
                stopIt();
            } finally {
                running = false;
            }
        }
    }

    @Deprecated
    public void runOld() {
        Command actualCommand;
        while (!shouldStop()) {
            try {
                synchronized (sentry) {
                    sentry.wait();
                    running = true;
                    actualCommand = cmd;
                }
            } catch (InterruptedException e) {
                break;
            }

            try {
                switch (actualCommand) {
                    case Halt:
                        task.halt();
                        stopIt();

                        break;
                    case Execute:


                        break;
                } // switch
            } finally {
                running = false;
            }

        }// while
    }

    public void setTask(SimpleTask st) {
        //synchronized (sentry) {
            task = st;
        //}
    }

    public boolean submit(Object arg) {
        task.addParam(arg);
        return submit();
    }

    public boolean submit() {
        if (running)
            return false;

        putCmd( Command.Execute);
        return true;
    }

    private void putCmd(Command cmd) {
        try {

            for(int retries = 0;
                !msgQueue.offer( cmd, 100, TimeUnit.MILLISECONDS ) && retries < SUBMIT_RETRIES_NUM;
                retries++);

        } catch(InterruptedException e) {
            // pass
        }
    }

    @Deprecated
    public boolean submitOld() {
        if (running)
            return false;

        synchronized (sentry) {
            cmd = Command.Execute;
            sentry.notify();
        }

        return true;
    }

    public void halt() {
        putCmd(Command.Halt);
    }

    @Deprecated
    public void haltOld() {
        synchronized (sentry) {
            stop = true;
            cmd  = Command.Halt;
            sentry.notify();
        }
    }

    public boolean isRunning() {
       return running;
    }

    public float percents() {
        return feedback.getPercents();
    }

    public ProgressState progress() {
        return ProgressState.make(percents(), running ?  false : true);
    }


    protected boolean shouldStop() {
        return stop;
    }

    protected SimpleTask task() {
        return task;
    }

    protected Feedback feedback() {
        return feedback;
    }

    protected void stopIt() {
        stop = true;
    }



}
