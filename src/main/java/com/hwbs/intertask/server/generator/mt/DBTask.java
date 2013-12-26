package com.hwbs.intertask.server.generator.mt;

import com.google.gwt.core.client.GWT;
import com.hwbs.intertask.server.Connector;
import com.hwbs.intertask.shared.NameRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User:      kaa
 * Timestamp: 12/22/13 9:06 PM
 */
public class DBTask implements Runnable {

    //
    // Logging context
    //
    Logger logger = Logger.getLogger("nviewer");

    private static final int BATCH_INSERT_SIZE = 5000;
    // private static final int COMMIT_INTERVAL   = 10000;

    private final Connector connector;
    private final NameRecord[] cache;
    private boolean stop;

    private int j,k;

    public DBTask(Connector conn, NameRecord[] cache, int j, int k) {
        this.connector  = conn;
        this.cache      = cache;

        this.j = j;
        this.k = k;
    }


    @Override
    public void run() {

        boolean committed      = false;
        boolean batchCompleted = false;

        Connection conn = null;
        try {

            logger.log(Level.INFO, "starting inserting records from range: (" + j + "," + k +")");

            System.err.println("starting inserting records from range: (" + j + "," + k +")");

            conn = connector.connect();

            boolean oldAutoCommit = conn.getAutoCommit();

            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            stmt.execute("SET FILES LOG FALSE");
            stmt.execute("SET FILES NIO SIZE 8192");
            stmt.execute("SET FILES CACHE ROWS " + (BATCH_INSERT_SIZE + 128) );

            stmt.execute("CHECKPOINT");
            stmt.close();


            PreparedStatement insertStmt = conn.prepareStatement(
                    "insert into names(first_name, second_name) values (?,?)");



            int iteration = 1;

            for (int i = j; i < k && !stop; ++i, ++iteration) {

                committed      = false;
                batchCompleted = false;

                insertStmt.setString(1, String.valueOf(cache[i].firstName(),  0, cache[i].getFirstEnd()  ));
                insertStmt.setString(2, String.valueOf(cache[i].secondName(), 0, cache[i].getSecondEnd() ));
                insertStmt.addBatch();

                //System.err.println("iter: " + iteration);
                if (iteration % BATCH_INSERT_SIZE == 0) {
                    //System.err.println("execBatch(" + iteration + ") [" + j + "," + k + "]");
                    //logger.log(Level.WARNING,"execBatch(" + iteration + ") [" + j + "," + k + "]");
                    GWT.log("execBatch(" + iteration + ") [" + j + "," + k + "]");

                    insertStmt.executeBatch();
                    conn.commit();

                    committed      = true;
                    batchCompleted = true;
                }

                /*
                if (iteration % COMMIT_INTERVAL == 0) {
                    logger.log(Level.INFO,"commitBatch(" + iteration + ") [" + j + "," + k + "]");
                    System.err.println("commit(" + iteration + ") [" + j + "," + k + "]");
                    if (!batchCompleted) {
                        insertStmt.executeBatch();
                        batchCompleted = true;
                    }

                    conn.commit();
                    committed = true;
                }
                */
            }

            if (!batchCompleted) {
                insertStmt.executeBatch();
                // batchCompleted = true;
            }

            insertStmt.close();

            if (!committed) {
                conn.commit();
                committed = true;
            }

            conn.setAutoCommit(oldAutoCommit);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "exception while inserting records: " + e.getLocalizedMessage() );
            e.printStackTrace();

            try {
                if (!committed && null != conn )
                    conn.rollback();
            } catch (SQLException e1) {
                logger.log(Level.WARNING, "rollback failed: " + e.getLocalizedMessage() );
            }

        } finally {
            logger.log(Level.INFO, "done inserting: (" + j + "," + k +")");

            if (null != conn) {

                try {
                    conn.createStatement().execute("SET FILES LOG FALSE");
                    conn.createStatement().execute("CHECKPOINT");
                    connector.close(conn);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "failed to close db connection: " + e.getLocalizedMessage() );
                }
            }
        }

    }

    public void cancel() {
        stop = true;
    }
}
