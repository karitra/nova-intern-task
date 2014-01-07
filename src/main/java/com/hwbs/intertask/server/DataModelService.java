package com.hwbs.intertask.server;

import com.hwbs.intertask.shared.CacheState;
import com.hwbs.intertask.shared.CommonConfig;
import com.hwbs.intertask.shared.NameRecord;
import com.hwbs.intertask.shared.ViewParameters;

import java.util.List;

/**
 * User:      kaa
 * Timestamp: 12/24/13 5:51 PM
 *
 * For representation DataModel singleton as service
 *
 */
public class DataModelService {

    public static List<NameRecord> getRecordsUnordered(int from, int num) {
        return DataModel.getInstance().getRecordsUnordered(from, num);
    }

    public static List<NameRecord> getRecords1stOrdered(int from, int num) {
        return DataModel.getInstance().getRecords1stOrdered(from, num);
    }

    public static List<NameRecord> getRecords2ndOrdered(int from, int num) {
        return DataModel.getInstance().getRecords2ndOrdered(from, num);
    }

    public static List<NameRecord> getRecords1stOrderedDesc(int from, int num) {
        return DataModel.getInstance().getRecords1stOrderedDesc(from, num);
    }

    public static List<NameRecord> getRecords2ndOrderedDesc(int from, int num) {
        return DataModel.getInstance().getRecords2ndOrderedDesc(from, num);
    }


    public static List<NameRecord> getRecordsUnordered(int pageNum) {
        return DataModel.getInstance().getRecordsUnordered(pageNum);
    }

    public static List<NameRecord> getRecords1stOrdered(int pageNum) {
        return DataModel.getInstance().getRecords1stOrdered(pageNum);
    }

    public static List<NameRecord> getRecords2ndOrdered(int pageNum) {
        return DataModel.getInstance().getRecords2ndOrdered(pageNum);
    }

    public static List<NameRecord> getRecords1stOrderedDesc(int pageNum) {
        return DataModel.getInstance().getRecords1stOrderedDesc(pageNum);
    }

    public static List<NameRecord> getRecords2ndOrderedDesc(int pageNum) {
        return DataModel.getInstance().getRecords2ndOrderedDesc(pageNum);
    }

    public static Boolean isMemStoreReady() {
        return DataModel.getInstance().isMemStoreReady();
    }

    public static Boolean isPrefetchedStoreReady() {
        return DataModel.getInstance().isPrefetchedStroreReady();
    }

    public static long getPageSize() {
        return CommonConfig.PAGE_SIZE_LIMIT;
    }

    public static long getItemsCount() {
        return DataModel.getInstance().getItemsCount();
    }

    public static ViewParameters getViewParameters() {
        return DataModel.getInstance().getViewParameters();
    }

    public static CacheState getCacheState() {
        return DataModel.getInstance().getCacheState();
    }


    //
    //  Note: for test and debug, can remove in production
    //
    public static Integer getInt() {
        return 1310;
    }

}
