package com.hwbs.intertask.server;

import com.hwbs.intertask.shared.CacheState;
import com.hwbs.intertask.shared.CommonConfig;
import com.hwbs.intertask.shared.NameRecord;
import com.hwbs.intertask.shared.ViewParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * User:      kaa
 * Timestamp: 12/24/13 12:23 AM
 *
 * TODO: implement 'dirty' cache
 *
 */
public class DataModel {

    private NameRecord[] allCached,
            orderedBy1st,
            orderedBy2nd;

    private volatile boolean
            baseCacheValid,
            fstCacheValid,
            scdCacheValid;

    private static DataModel self;

    private ViewParameters parameters   = new ViewParameters(CommonConfig.TOTAL_RECORDS, CommonConfig.PAGE_SIZE_LIMIT);

    private long cacheUpdateStartTimeMS = 0;
    private static boolean loading      = true;

    private long databaseRecordsNum     = 0;

    private DataModel() {

    }

    private DataModel(NameRecord[] allCached,
              NameRecord[] orderedBy1st,
              NameRecord[] orderedBy2nd) {
        updateModel(allCached, orderedBy1st, orderedBy2nd);
    }


    /*
    public Slice getBaseSlice(int offest, int items) {
        return new Slice( allCached, offest, items);
    }

    public Slice getOrdered1StSlice(int offest, int items) {
        return new Slice( orderedBy2nd, offest, items);
    }

    public Slice getBaseOrdered2ndSlice(int offest, int items) {
        return new Slice( orderedBy2nd, offest, items);
    }
    */


    public static synchronized DataModel getInstance() {
        if (null == self ) {
            self = new DataModel();
        }

        return self;
    }

    //
    // For RequestFactory service
    //
    public synchronized List<NameRecord> getRecordsUnordered(int from, int num) {
        return getModelList( allCached, from, num, baseCacheValid, false );
    }

    public synchronized List<NameRecord> getRecords1stOrdered(int from, int num) {
        return getModelList(orderedBy1st, from, num,  fstCacheValid, false );
    }

    public synchronized List<NameRecord> getRecords2ndOrdered(int from, int num) {
        return getModelList(orderedBy2nd, from, num, scdCacheValid, false );
    }

    public synchronized List<NameRecord> getRecords1stOrderedDesc(int from, int num) {

        return getModelList(orderedBy1st, from, num, fstCacheValid, true);
    }

    public synchronized List<NameRecord> getRecords2ndOrderedDesc(int from, int num) {

        return getModelList(orderedBy2nd, from, num, scdCacheValid, true);
    }

    //
    // For RequestFactory service (by page number)
    //
    public synchronized List<NameRecord> getRecordsUnordered(int pageNum) {
        return getModelList( allCached, pageNum, baseCacheValid, false );
    }

    public synchronized List<NameRecord> getRecords1stOrdered(int pageNum) {
        return getModelList(orderedBy1st, pageNum,  fstCacheValid, false );
    }

    public synchronized List<NameRecord> getRecords2ndOrdered(int pageNum) {
        return getModelList(orderedBy2nd, pageNum,  scdCacheValid, false );
    }

    public synchronized List<NameRecord> getRecords1stOrderedDesc(int pageNum) {

        return getModelList(orderedBy1st, pageNum, fstCacheValid, true);
    }

    public synchronized List<NameRecord> getRecords2ndOrderedDesc(int pageNum) {

        return getModelList(orderedBy2nd, pageNum, scdCacheValid, true);
    }

    private synchronized List<NameRecord> getModelList(NameRecord[] model, int pageNum, boolean valid, boolean mirrorder) {
        return  getModelList( model,
                CommonConfig.PAGE_SIZE_LIMIT * pageNum,
                CommonConfig.PAGE_SIZE_LIMIT,
                valid, mirrorder );
    }

    private synchronized List<NameRecord> getModelList(NameRecord[] model, int from, int num, boolean valid, boolean mirrorder) {

//        System.err.println( "<---- Data request ----" );
//        System.err.println( "getModelList:     from " + from);
//        System.err.println( "getModelList:      num " + num );
//        System.err.println( "getModelList: mirrored " + mirrorder );
//        System.err.println( "getModelList:    valid " + valid );

        List<NameRecord> li = new ArrayList<NameRecord>(0);

        if (!valid)
            return li;

        if (from < 0)
            return li;

        if (mirrorder) { // descending direction
            if (from > model.length)
                return  li;

            from = model.length - from - 1;

            if (from + 1 - num < 0)
                num = from;

        } else { // ascending direction

            if (from >= model.length)
                return li;

            if (from + num > model.length)
                num = model.length - from;
        }

        // System.err.println( "getModelList: norm from " + from );
        li = new ArrayList<>(num > CommonConfig.PAGE_SIZE_LIMIT ? CommonConfig.PAGE_SIZE_LIMIT : num);

        Arithmetician a = mirrorder ? new Subster() : new Adder();

        for(int i = 0; i < num; i++)
            li.add( model[ a.op(from, i) ] );

        // System.err.println( "getModelList: li size " + li.size() );

        return li;
    }

    private interface Arithmetician {
        int op(int from, int i);
    }

    private class Adder implements Arithmetician {

        @Override
        public  int op(int from, int i) { return from + i; }
    }

    private class Subster implements Arithmetician {

        @Override
        public int op(int from, int i) { return from - i; }
    }

    public int getItemsCount() {
        return allCached.length;
    }


    public void updateModel(NameRecord[] allCached,
                     NameRecord[] orderedBy1st,
                     NameRecord[] orderedBy2nd) {

        assert allCached.length == orderedBy1st.length &&
                orderedBy1st.length == orderedBy2nd.length;

        setUnorderedModel(allCached);
        setOrderedBy1stModel(orderedBy1st);
        setOrderedBy2ndModel(orderedBy2nd);
    }



    public synchronized void setUnorderedModel(NameRecord[] model) {
        this.allCached = model;
        baseCacheValid = true;
        setLoadingState(false);
        setDatabaseRecordsNum(model.length);
    }

    public synchronized void setOrderedBy1stModel(NameRecord[] model) {
        this.orderedBy1st = model;
        fstCacheValid = true;
        setLoadingState(false);
        setDatabaseRecordsNum(model.length);
    }

    public synchronized void setOrderedBy2ndModel(NameRecord[] model) {
        this.orderedBy2nd = model;
        scdCacheValid = true;
        setLoadingState(false);
        setDatabaseRecordsNum(model.length);
    }

    public NameRecord[] getUnordered() {
        return allCached;
    }

    public NameRecord[] getOrderedBy1st() {
        return orderedBy1st;
    }

    public NameRecord[] getOrderedBy2nd() {
        return orderedBy2nd;
    }

    /*
    public  void setOrderedBy1stModelDesc(NameRecord[] model) {
        this.orderedBy1stDesc = model;
        fstDescCacheValid = true;
    }

    public void setOrderedBy2ndModelDesc(NameRecord[] model) {
        this.orderedBy2ndDesc = model;
        scdDescCacheValid = true;
    }
    */

    public synchronized void invalidateAllCaches() {
        baseCacheValid = fstCacheValid = scdCacheValid = false;
        cacheUpdateStartTimeMS = System.currentTimeMillis();
    }


    public synchronized boolean isMemStoreReady() {
        return ((baseCacheValid &&
                fstCacheValid  && scdCacheValid) && !loading);
    }

    public boolean isPrefetchedStroreReady() {
        return false;
    }

    public ViewParameters getViewParameters() {
        return parameters;
    }

    public long cacheInactiveTimeMS() {
        return System.currentTimeMillis() - cacheUpdateStartTimeMS;
    }

    public synchronized CacheState getCacheState() {
        //return new CacheState( isMemStoreReady(), loading, isEmpty(), cacheInactiveTimeMS() );
        return new CacheState( isMemStoreReady(), loading, isEmpty(), cacheInactiveTimeMS() );
        //return new CacheState( isMemStoreReady(), cacheInactiveTimeMS() );
    }

    //
    // Better name is: startup state
    //
    public synchronized void setLoadingState(boolean isLoading) {
         loading = isLoading;
    }

    public synchronized void setLoadingState(boolean isLoading, long recNum) {
        loading = isLoading;
        this.databaseRecordsNum = recNum;
    }


    public synchronized boolean isEmpty() {
        return  databaseRecordsNum == 0;
    }

    public synchronized void setDatabaseRecordsNum(long num) {
        //this.setLoadingState(false);
        this.databaseRecordsNum = num;
    }

//    public synchronized void setEmpty(boolean isLoading) {
//        this.empty = true;
//    }

}
