package com.hwbs.intertask.client;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;
import com.hwbs.intertask.server.DataModelService;

import java.util.List;

/**
 * User:      kaa
 * Timestamp: 12/24/13 5:43 PM
 */
@Service(DataModelService.class)
public interface DataModelRequest extends RequestContext {

    Request<List<NameRecordProxy>> getRecordsUnordered(int from, int num);
    Request<List<NameRecordProxy>> getRecords1stOrdered(int from, int num);
    Request<List<NameRecordProxy>> getRecords2ndOrdered(int from, int num);
    Request<List<NameRecordProxy>> getRecords1stOrderedDesc(int from, int num);
    Request<List<NameRecordProxy>> getRecords2ndOrderedDesc(int from, int num);

    Request<ViewParametersProxy>   getViewParameters();

    // Should be marked as deprecated, remove in future version
    Request<Boolean> isMemStoreReady();
    Request<Boolean> isPrefetchedStoreReady();

    Request<Long> getPageSize();
    Request<Long> getItemsCount();

    // Debug request
    Request<Integer> getInt();
}
