package com.hwbs.intertask.server;

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

    public static final int TOTAL_RECORDS   = 1000000;
    public static final int PAGE_SIZE_LIMIT = 32;

    private NameRecord[] allCached,
            orderedBy1st,
            orderedBy2nd,
            orderedBy1stDesc,
            orderedBy2ndDesc;

    private volatile boolean
            baseCacheValid,
            fstCacheValid,
            scdCacheValid,
            fstDescCacheValid,
            scdDescCacheValid;

    private static DataModel self;

    private ViewParameters parameters = new ViewParameters(TOTAL_RECORDS, PAGE_SIZE_LIMIT);

    private DataModel() {

    }

    private DataModel(NameRecord[] allCached,
              NameRecord[] orderedBy1st,
              NameRecord[] orderedBy2nd) {
        updateModel(allCached, orderedBy1st, orderedBy2nd);
    }

    private DataModel(NameRecord[] allCached,
                      NameRecord[] orderedBy1st,
                      NameRecord[] orderedBy2nd,
                      NameRecord[] orderedBy1stDesc,
                      NameRecord[] orderedBy2ndDesc) {
        updateModel(allCached, orderedBy1st, orderedBy2nd, orderedBy1stDesc, orderedBy2ndDesc);
    }

    public Slice getBaseSlice(int offest, int items) {
        return new Slice( allCached, offest, items);
    }

    public Slice getOrdered1StSlice(int offest, int items) {
        return new Slice( orderedBy2nd, offest, items);
    }

    public Slice getBaseOrdered2ndSlice(int offest, int items) {
        return new Slice( orderedBy2nd, offest, items);
    }


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
        return getModelList( allCached, from, num, baseCacheValid );
    }

    public synchronized List<NameRecord> getRecords1stOrdered(int from, int num) {
        return getModelList(orderedBy1st, from, num,  fstCacheValid );
    }

    public synchronized List<NameRecord> getRecords2ndOrdered(int from, int num) {
        return getModelList(orderedBy2nd, from, num, scdCacheValid );
    }

    public synchronized List<NameRecord> getRecords1stOrderedDesc(int from, int num) {
        return getModelList(orderedBy1stDesc, from, num, fstDescCacheValid);
    }

    public synchronized List<NameRecord> getRecords2ndOrderedDesc(int from, int num) {
        return getModelList(orderedBy2ndDesc, from, num, scdDescCacheValid);
    }

    private synchronized List<NameRecord> getModelList(NameRecord[] model, int from, int num, boolean valid) {

        if (!valid)
            return new ArrayList<NameRecord>(0);


        List<NameRecord> li = new ArrayList<NameRecord>(num > PAGE_SIZE_LIMIT ? PAGE_SIZE_LIMIT : num);

        if (from >= model.length)
            return li;

        if (from + num > model.length)
            num = model.length - from;

        for(int i = 0; i < num && from + i < model.length; i++) {
            li.add( model[from + i] );
        }

        return li;
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

    public void updateModel(NameRecord[] allCached,
                            NameRecord[] orderedBy1st,
                            NameRecord[] orderedBy2nd,
                            NameRecord[] orderedBy1stDesc,
                            NameRecord[] orderedBy2ndDesc ) {

        assert allCached.length         == orderedBy1st.length     &&
                orderedBy1st.length     == orderedBy2nd.length     &&
                orderedBy2nd.length     == orderedBy1stDesc.length &&
                orderedBy1stDesc.length == orderedBy2ndDesc.length;

        setUnorderedModel(allCached);
        setOrderedBy1stModel(orderedBy1st);
        setOrderedBy2ndModel(orderedBy2nd);
        setOrderedBy1stModelDesc(orderedBy1stDesc);
        setOrderedBy2ndModelDesc(orderedBy2ndDesc);
    }



    public void setUnorderedModel(NameRecord[] model) {
        this.allCached = model;
        baseCacheValid = true;
    }

    public void setOrderedBy1stModel(NameRecord[] model) {
        this.orderedBy1st = model;
        fstCacheValid = true;
    }

    public void setOrderedBy2ndModel(NameRecord[] model) {
        this.orderedBy2nd = model;
        scdCacheValid = true;
    }

    public  void setOrderedBy1stModelDesc(NameRecord[] model) {
        this.orderedBy1stDesc = model;
        fstDescCacheValid = true;
    }

    public void setOrderedBy2ndModelDesc(NameRecord[] model) {
        this.orderedBy2ndDesc = model;
        scdDescCacheValid = true;
    }

    public synchronized void invalidateAllCaches() {
        baseCacheValid = fstCacheValid = scdCacheValid = fstDescCacheValid = scdDescCacheValid = false;
    }

    public NameRecord[] getUnorderedModel() {
        return allCached;
    }

    public NameRecord[] getOrderedBy1st() {
        return orderedBy1st;
    }

    public NameRecord[] getOrderedBy2nd() {
        return orderedBy2nd;
    }


    public synchronized boolean isMemStoreReady() {
        return baseCacheValid &&
                fstCacheValid && scdCacheValid &&
                fstDescCacheValid && scdDescCacheValid;
    }

    public boolean isPrefetchedStroreReady() {
        return false;
    }

    public ViewParameters getViewParameters() {
        return parameters;
    }

}
