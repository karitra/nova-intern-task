package com.hwbs.intertask.server;

import com.hwbs.intertask.server.messages.CommandMessage;
import com.hwbs.intertask.server.messages.QuitMessage;
import com.hwbs.intertask.shared.NameRecord;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User:      kaa
 * Timestamp: 12/23/13 12:42 AM
 *
 * TODO: merge with parent as they share same functionality now
 */
public class QueuedExecutor extends SimpleExecutor {

    //
    // Logging context
    //
    Logger logger = Logger.getLogger("nviewer");

    private static final int WAIT_OFFER_SEC = 3;

    private static final int QUEUE_CAPCITY = 1;
    private final ArrayBlockingQueue<CommandMessage> queue =
            new ArrayBlockingQueue<CommandMessage>(QUEUE_CAPCITY);

    public QueuedExecutor(SimpleTask task) {
        super(task);
    }

    @Override
    public void run() {
        while(!shouldStop()) {
            try {
                CommandMessage m = queue.take();
                assert m != null;

                if (m.shouldHalt()) {
                    task().halt();
                    stopIt();
                    break;
                }

                //
                // At this point message is with payload
                //
                NameRecord[] data = m.getPayload();
                if (null == data) {
                    // strange but can proceed
                    logger.log(Level.WARNING, "got empty payload in queued executor" );
                    continue;
                }

                task().addParam(data);
                System.err.println("ready to populate db");
                long start = System.nanoTime();

                logger.log(Level.WARNING, "ready to populate database" );
                task().doit(feedback());

                System.err.println("done populating db: " + (System.nanoTime() - start) / 1000000.0  + " ms");

            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public synchronized void postCommand(CommandMessage m) {

        // logger.log(Level.WARNING, "posting the message..." );
        //System.err.println("postining the message");

        // throw away the head;
        CommandMessage hd = queue.poll();
        if (null != hd) {

            // check was it the quit message?
            // if so repost it and start halt sequence
            if (hd.shouldHalt()) {
                queue.offer(hd);
                return;
            }
        }

        try {
            while(!queue.offer(m, 4, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "queue 'offer' interrupted" );
        }

    }

    public synchronized void postQuit() {
        QuitMessage qm = new QuitMessage();
        while ( !queue.offer( qm ) );
    }

    public void halt() {
        postQuit();
    }

}