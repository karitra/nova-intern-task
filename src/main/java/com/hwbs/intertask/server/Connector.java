package com.hwbs.intertask.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * User:      kaa
 * Timestamp: 12/22/13 9:43 PM
 *
 * TODO: implement connections pooling using apache commons-dbcp
 *
 */
public class Connector {

    private static final int INITIAL_CAPACITY = 1024;

    private String dburl;
    private String user;
    private String pwd;

    private Map<Connection,Boolean> map;

    //@SuppressWarnings("unchecked")
    public Connector(String driver, String dburl, String user, String pwd) throws ClassNotFoundException {

        Class.forName(driver);

        this.dburl = dburl;
        this.user  = user;
        this.pwd   = pwd;

        map = new HashMap<Connection,Boolean>(INITIAL_CAPACITY);
    }

    public synchronized Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection( dburl, user, pwd);
        map.put(c,Boolean.FALSE);
        return c;
    }


    public synchronized void close(Connection c) throws SQLException {
        Boolean inUse = map.get(c);
        if (null != inUse) {
            // TODO: mark as unused in case of capacity allows it;
            map.remove(c);
        }

        c.close();
    }
}
