package com.hwbs.intertask.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.hwbs.intertask.client.GreetingService;
import com.hwbs.intertask.server.generator.NameGenTaskResult;
import com.hwbs.intertask.server.generator.TaskResult;
import com.hwbs.intertask.server.generator.mt.Executor;
import com.hwbs.intertask.server.generator.mt.FillDBTasksFactory;
import com.hwbs.intertask.server.generator.mt.NamesGenExecutor;
import com.hwbs.intertask.server.messages.ProcessMessage;
import com.hwbs.intertask.shared.Feedback;
import com.hwbs.intertask.shared.NameRecord;
import com.hwbs.intertask.shared.ProgressState;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements
    GreetingService {

    //
    // Logging context
    //
    Logger logger; // = Logger.getLogger("nviewer");

    //
    // TODO: should go to application wide config
    //
    private static final int BATCH_INSERT_SIZE = 20000;
    private static final int BATCH_BIG_STEP    = 4 * BATCH_INSERT_SIZE;

    private static final int WAITE_TO_SHUTDOWN_MS = 4 * 1000;

    private static final String DB_PATH = "db/names.db";

    private SimpleExecutor genWrk;
    private QueuedExecutor dbWrk;
    private Executor dbexec;
    private Thread          genThread;
    private Thread           dbThread;


    private NameRecord[]             cache;
    {
        cache = fillCacheSingle();
    }



    /*
    private NameRecord[] cacheOrderedBy1st;
    private NameRecord[] cacheOrderedBy2nd;
    private NameRecord[] cacheOrderedBy1stDesc;
    private NameRecord[] cacheOrderedBy2ndDesc;
    */

    DataModel model;

    //
    // TODO: implement with connection pooling (use Connector class as staring point)
    //
    private Connection conn;
    private Connector connector;

    //
    // Simple threads pool for various utility tasks, use AsyncTaskSentry to wait for tasks  completion
    //
    private ExecutorService pion;

    // Name not truly represent the entity, as it can wait for other types on tasks
    private AsyncTasksSentry sortingDoneSentry = new AsyncTasksSentry();

    //
    // TODO: remove this garbage
    //
    /*
  @Deprecated
  public String greetServer(String input) throws IllegalArgumentException {
    // Verify that the input is valid.
    if (!FieldVerifier.isValidName(input)) {
      // If the input is not valid, throw an IllegalArgumentException back to
      // the client.
      throw new IllegalArgumentException(
          "Name must be at least 4 characters long");
    }

    String serverInfo = getServletContext().getServerInfo();
    String userAgent = getThreadLocalRequest().getHeader("User-Agent");

    // Escape data from the client to avoid cross-site script vulnerabilities.
    input = escapeHtml(input);
    userAgent = escapeHtml(userAgent);

    return "Hello, " + input + "!<br><br>I am running " + serverInfo
        + ".<br><br>It looks like you are using:<br>" + userAgent;
  }
  */

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        int cores = Runtime.getRuntime().availableProcessors();
        pion = Executors.newFixedThreadPool(cores * 2);

        model = DataModel.getInstance();

        logger = Logger.getLogger("nviewer");

        //NamesGenExecutor namesGenerator = new NamesGenExecutor();
        //GenTask genTask = new GenTask(namesGenerator);
        //GenTask genTask = new GenTask();

        genWrk    = new SimpleExecutor( new GenTask(getCache()) );
        genThread = new Thread(genWrk);
        genThread.start();


        ServletContext context = getServletContext();
        String path   = context.getRealPath("/");

        if (path.charAt(path.length()-1) != File.separatorChar) {
            path += File.separator;
        }

        String dbpath = path + DB_PATH;

        // System.err.println("DB path: " + dbpath);

        logger.log(Level.INFO, "database path: " + dbpath);

        initDB(dbpath);

        dbWrk    = new QueuedExecutor( new DBSimpleTask() );
        dbThread = new Thread(dbWrk);
        dbThread.start();

        logger.log(Level.INFO, "service started");
    }



    @Override
    public void destroy() {
        super.destroy();

        try {
            //
            // TODO: Make halt logic clear (incapsulate in one transaction)
            //
            genWrk.halt();
            genThread.join(WAITE_TO_SHUTDOWN_MS);

            dbWrk.halt();
            dbThread.join();
        } catch(InterruptedException e) {
            // pass
        }

        detachDB();
        logger.log(Level.INFO, "service destroyed");
    }

    //
    // TODO: do something reasonable when database is locked on startup, for example return lazy adapter,
    // to try to connect next time when data access will be required.
    //
    private void initDB(String path) {
        if (null == path || path.length() == 0) {
            // Note: can be created in servlet container working directory
            path = DB_PATH;
        }

        try {

          connector = new Connector( "org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:"+path, "SA", "");
          conn      = connector.connect();

          // TODO: remove from final version!
          //conn.createStatement().execute("drop table names");

          try {
           createDBCache(conn);
          } catch (SQLException e) {
              logger.log(Level.WARNING, "can't create  database:" + e.getLocalizedMessage());
          }

          // do something else with db...

        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "failed to load HSQLDB driver: " + e.getLocalizedMessage());
        } catch (SQLException e) {
            logger.log(Level.WARNING, "sql error" + e.getLocalizedMessage());
            //e.printStackTrace();
        }

        startLoader();
    }

    private boolean shouldHaltDBLoad = false;

    //
    // Note: connector should be created at this point
    //
    private void startLoader() {

        Future<?> loadTask = pion.submit(new Runnable() {
            @Override
            public void run() {

                Connection conn = null;
                try {

                    logger.log(Level.WARNING, "starting database startup loading...");

                    final long start = System.nanoTime();

                    conn = connector.connect();
                    NameRecord[] dbRecs = getCache(); //new NameRecord[DataModel.TOTAL_RECORDS];

                    DataModel.getInstance().invalidateAllCaches();

                    Statement stmt = conn.createStatement();
                    ResultSet res = stmt.executeQuery("select count(*) from names");
                    int count = 0;
                    if (null != res && res.next()) {
                        count = res.getInt(1);
                    }
                    stmt.close();

                    System.err.println("ready to load: " + count + " record" + (count > 1 ? "s" : "" ));

                    if (count == 0) {
                        // no records yet, done
                        logger.log(Level.WARNING, "database is empty, should we force generate?");
                        return;
                    }

                    String query = "select first_name, second_name from names";
                    if (count > DataModel.TOTAL_RECORDS) {
                        query += "limit " + DataModel.TOTAL_RECORDS;
                    }

                    stmt = conn.createStatement();
                    res = stmt.executeQuery(query);

                    int i = 0;
                    while (res.next() && !shouldHaltDBLoad) {

                        dbRecs[i++].setNames(res.getString(1),res.getString(2));

                        //
                        // [DEBUG]
                        //
                        //if (i % 100000 == 0) {
                        //    logger.log(Level.WARNING, "item1: " + res.getString(1));
                        //    logger.log(Level.WARNING, "item2: " + res.getString(2));
                        //    logger.log(Level.WARNING, "db read. passed " + i + " items...");
                        //}
                    }

                    stmt.close();
                    logger.log(Level.WARNING, "done reading db");

                    if (shouldHaltDBLoad) {
                        return;
                    }

                    // Complete database init
                    if (i < DataModel.TOTAL_RECORDS) {

                        logger.log(Level.WARNING, "database isn't full, fill some records");

                        for (int j = i; j < DataModel.TOTAL_RECORDS && !shouldHaltDBLoad; ++j) {
                            dbRecs[j] = new NameRecord(res.getString(1), res.getString(2));
                        }
                    }

                    if (shouldHaltDBLoad) {
                        return;
                    }

                    //
                    // Record read at this point, sort them
                    //
                    startSorting(dbRecs);


                    //
                    // DEBUG: completion hook,  remove it!
                    //
                    new Thread( new Runnable() {
                        @Override
                        public void run() {
                            sortingDoneSentry.waitAll();
                            logger.log(Level.WARNING, "database loading and sorting took: " +
                                    (System.nanoTime() - start) / 1000000 + "ms");
                            System.err.println( "database loading and sorting took: " +
                                    (System.nanoTime() - start) / 1000000  + "ms");

                        }
                    }).start();

                } catch (SQLException e) {

                } finally {

                    shouldHaltDBLoad = false;

                    if (null != conn) {

                        try {
                            connector.close(conn);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });

        sortingDoneSentry.add( loadTask );
    }

    private void detachDB() {
        try {
            connector.close(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void createDBCache(Connection c) throws SQLException {
        c.createStatement().executeUpdate(
                "create cached table names (first_name char(10), second_name char(10))" );
        logger.log(Level.WARNING, "database created");
    }

    private void dropDBCache(Connection c) throws SQLException {
        c.createStatement().execute("drop table names");
        logger.log(Level.WARNING, "database deleted");

    }


    private void checkpoint(Connection c) throws SQLException {
        c.createStatement().execute("checkpoint");
    }


    private NameRecord[] fillCacheSingle() {

        NameRecord[] csh = new NameRecord[DataModel.TOTAL_RECORDS];
        for(int i = 0; i < DataModel.TOTAL_RECORDS; ++i) {
            csh[i] = new NameRecord();
        }

        return csh;
    }

    private NameRecord[] getCache() {
        return cache;
    }

    private void deleteDBCache() {
        Connection conn = null;
        try {
            conn = connector.connect();
            conn.createStatement().execute("delete from names");
            logger.log(Level.WARNING, "database deleted");
            //conn.createStatement().execute("checkpoint");
        } catch (SQLException e) {
            logger.log(Level.WARNING, "failed to remove database: " + e.getLocalizedMessage() );
        } finally {
            if (null != conn) {
                try {
                    connector.close(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void recreateDBCache() {
        Connection c = null;
        try {
            c = connector.connect();

            dropDBCache(c);
            createDBCache(c);
            checkpoint(c);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "failed to remove database: " + e.getLocalizedMessage() );
        } finally {
            if (null != c) {

                try {
                    connector.close(c);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void waitSortingCompetion() {
        sortingDoneSentry.waitAll();
    }

    private class DBSimpleTask implements SimpleTask {

        private final Executor gen;
        private FillDBTasksFactory fac;
        private TaskResult result;
        private Object lock = new Object();

        DBSimpleTask() {
            //
            // Creating single threaded Executor
            //
            this.gen = new Executor(NamesGenExecutor.NAMES_COUNT, NamesGenExecutor.NAMES_COUNT, true);
        }

        @Override
        public void doit(Feedback feedback) {

            logger.log(Level.WARNING, "starting database massive parallel data insertion");
            long start = System.nanoTime();

            //
            // Halt and wait for completion of previous 'run'
            //
            if (null != fac) {
                fac.cancelAll();
                if (null != result) {
                    logger.log(Level.INFO, "waiting for completion of previous step");
                    result.waitAll();
                }
            }

            //
            // Clear out database
            //
            // Note: we are single threaded here. so it should be ok not to synchronize here
            //
            //deleteDBCache();
            recreateDBCache();

            //
            // Sanity check: cache not ready yet by some reason, erronrecreateDBeous state, just boiling out
            //
            if (getCache() == null)
                return;

            fac = new FillDBTasksFactory(connector, getCache() );
            TaskResult tr = new TaskResult(gen.expectedSubTasksNum());
            gen.generate(fac, tr);
            tr.waitAll();

            logger.log(Level.WARNING, "massive parallel data insertion done: " + (System.nanoTime() - start) / 1000000.0 + " ms");

            // at this point all records must be inserted!
        }

        @Override
        public void halt() {
            gen.halt();
        }

        @Override
        public void addParam(Object arg) {
            //if (arg instanceof NameRecord[]) {
            //    cache = (NameRecord[]) arg;
            //}
        }
    }

    //
    // Note: updates DataStore cache on completion and sets main unordered store!
    //
    private void startSorting(final NameRecord[] csh) {

        DataModel.getInstance().setUnorderedModel(csh);

        Future<?> sortRes1 = pion.submit(new Runnable() {
            @Override
            public void run() {
                NameRecord[] cacheOrderedBy1st = csh.clone();
                Arrays.sort(cacheOrderedBy1st, new NameRecord.FirstNameComparator());
                DataModel.getInstance().setOrderedBy1stModel( cacheOrderedBy1st );
                logger.log(Level.WARNING, "sort done [by 1st name]");
            }
        });

        Future<?> sortRes2 = pion.submit( new Runnable() {
            @Override
            public void run() {
                NameRecord[] cacheOrderedBy2nd = csh.clone();
                Arrays.sort(cacheOrderedBy2nd, new NameRecord.SecondNameComparator() );
                DataModel.getInstance().setOrderedBy2ndModel(cacheOrderedBy2nd);
                logger.log(Level.WARNING, "sort done [by 2nd name]");
            }
        });

        Future<?> sortRes1Desc = pion.submit( new Runnable() {
            @Override
            public void run() {
                NameRecord[] cacheOrderedBy1stDesc = csh.clone();
                Arrays.sort( cacheOrderedBy1stDesc, new NameRecord.FirstNameComparatorDesc());
                DataModel.getInstance().setOrderedBy1stModelDesc(cacheOrderedBy1stDesc);
                logger.log(Level.WARNING, "sort done [by 1st name desc]");
            }
        });

        Future<?> sortRes2Desc = pion.submit( new Runnable() {
            @Override
            public void run() {
                NameRecord[] cacheOrderedBy2ndDesc = csh.clone();
                Arrays.sort(cacheOrderedBy2ndDesc, new NameRecord.SecondNameComparatorDesc() );
                DataModel.getInstance().setOrderedBy2ndModelDesc(cacheOrderedBy2ndDesc);
                logger.log(Level.WARNING, "sort done [by 2nd name desc]");
            }
        });


        sortingDoneSentry.add(sortRes1);
        sortingDoneSentry.add(sortRes2);
        sortingDoneSentry.add(sortRes1Desc);
        sortingDoneSentry.add(sortRes2Desc);
    }


    private class GenTask  implements SimpleTask {


        private final NamesGenExecutor gen;

        GenTask(NamesGenExecutor gen) {
            this.gen      = gen;
        }

        GenTask(NameRecord[] csh) {
            this.gen = new NamesGenExecutor(csh);
        }

        @Override
        public void doit(Feedback feedback) {
            logger.log(Level.INFO, "Wait for previous sorting to be done..." );

            shouldHaltDBLoad = true;
            sortingDoneSentry.waitAll();

            DataModel.getInstance().invalidateAllCaches();

            logger.log(Level.INFO, "start generating..." );
            long start = System.nanoTime();
            NameGenTaskResult r = new NameGenTaskResult( gen.expectedSubTasksNum(), gen.getActiveCache() );
            gen.generate(r);

            r.waitAll(feedback);
            NameRecord[] csh = r.getRusult();

            logger.log(Level.WARNING, "done generating: " +
                    (System.nanoTime() - start) / 1000000.0f + " ms elapsed" );

            logger.log(Level.WARNING, "ignite writing db..." );
            start = System.nanoTime();


            Runtime.getRuntime().gc();

            dbWrk.postCommand( new ProcessMessage(csh) );

            logger.log(Level.WARNING, "ignite sorting");

            start = System.nanoTime();
            startSorting(csh);

            final long q = start;
            new Thread( new Runnable() {
                @Override
                public void run() {

                    sortingDoneSentry.waitAll();
                    logger.log(Level.WARNING, "Sorting done, in (approximitly): " +
                            (System.nanoTime() - q) / 1000000 + "ms" );
                    Runtime.getRuntime().gc();
                }
            } ).start();


            //dumpHead(cacheOrderedBy1st, 7);
            //dumpHead(cacheOrderedBy2nd, 7);
            //dumpHead(cacheOrderedBy1stDesc, 7);
            //dumpHead(cacheOrderedBy2ndDesc, 7);

            //dummyWait(5);

            logger.log( Level.WARNING, "done ignition of db writing: " +
                    (System.nanoTime() - start) / 1000000.0f + " ms elapsed" );

        }

        @Override
        public void halt() {
            gen.halt();
        }

        @Override
        public void addParam(Object arg) {

        }

        public void dumpHead(NameRecord[] c,int lim) {
            for(int i = 0; i < lim; i++) {
                System.out.printf("[%3d] name: %s\n", i, c[i]);
            }
        }

        public void dummyWait(long seconds) {
            try {
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "sleep interrupted");
            }
        }

    }


    //
    // Service methods
    //
    public boolean generate() throws IllegalStateException {
        return genWrk.submit();
    }

    public ProgressState progress() throws IllegalStateException {
        return genWrk.progress();
    }

    public boolean generateDummy()       throws IllegalStateException {
        try {
            Thread.sleep( 4 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {

        }

        return true;
    }

    int dummySteps = 0;
    private static final int MAX_DUMMY_STEPS = 5;

    public ProgressState progressDummy() throws IllegalStateException {

        if (++dummySteps % MAX_DUMMY_STEPS == 0) {
            dummySteps = 0;
            return ProgressState.make(100.0f, true);
        }

        return ProgressState.make(100.f * dummySteps / MAX_DUMMY_STEPS, false );
    }

    /**
   * NOTE: Generated code!
   *
   * Escape an html string. Escaping data received from the client helps to
   * prevent cross-site script vulnerabilities.
   *
   * @param html the html string to escape
   * @return the escaped string
   */
  private String escapeHtml(String html) {
    if (html == null) {
      return null;
    }
    return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(
        ">", "&gt;");
  }
}
