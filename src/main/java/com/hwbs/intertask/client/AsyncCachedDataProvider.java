package com.hwbs.intertask.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.hwbs.intertask.client.proxies.NameRecordProxy;
import com.hwbs.intertask.client.ui.LazyLoadDataGrid;

import java.util.LinkedList;
import java.util.List;

/**
* User:      kaa
* Timestamp: 12/28/13 1:48 PM
 *
 * Client side records cache
 *
 * <i>Note:</i> we have assumption that the cache never has gaps at the middle or beginning
 * and always expand at the and. But this realisation can handle such 'gaps' if it will
 * be needed in the future releases. Optimized to work with page sized 'gaps' (chunks).
 *
 * No incomplete pages allowed, if incomplete requested it will be filled up to page size
 * (if possible)
 *
 * <b>Warning:</b> not optimal for huge queries.
*/
class AsyncCachedDataProvider extends AsyncDataProvider<NameRecordProxy> {

    private static int MAX_GAP_ALLOWED     = 1024;
    private static int DEFAULT_PAGES_COUNT = 1024;

    enum SortingMode {
        Unordered,
        SortByFirst,
        SortByFirstDesc,
        SortBySecond,
        SortBySecondDesc
    }

    private SortingMode currentMode = SortingMode.Unordered;

    private LazyLoadDataGrid<?> namesGrid;
    private NamesServiceRequestFactory requestFactory;

    private List<NameRecordProxy> cachedList = new LinkedList<NameRecordProxy>();

    public AsyncCachedDataProvider(LazyLoadDataGrid namesGrid, NamesServiceRequestFactory requestFactory) {
        this.namesGrid      = namesGrid;
        this.requestFactory = requestFactory;
    }

    private SortingMode sortingMode = SortingMode.Unordered;

    //
    // Cache reset, next page size of data grid widget will be updated,
    // so we must ignore one request - for sorting event and process,
    // next for page size update
    //
    private boolean sortingState = false;
    private int currentPageSize  = 0;
    private int semaphore        = 0;

    @Override
    protected void onRangeChanged(HasData<NameRecordProxy> display) {

        final Range range = display.getVisibleRange();

        GWT.log("<--- Cache request ----------->");
        GWT.log("range requested: " + range );

        SortingMode sm = getSortingMode();
        GWT.log("sorting order: " + sm);

//        if (sortingState) {
//            // If sorting by column was fired we must ignore one request
//            // with wrong page size
//            sortingState = false;
//            GWT.log( "ignoring first request (after sorting command) for range: " + range );
//            return;
//        }

//        if (semaphore > 0) {
//            semaphore--;
//            return;
//        }


        if (sm != sortingMode) {
            GWT.log("sorting mode changed, was: " + sortingMode );
            semaphore = 2;
            sortingMode = sm;
            resetCache(true);
            namesGrid.setLoadingPageState(true);
            //namesGrid.getScrollPanel().scrollToTop();
            if (namesGrid.getBasePageSize() != namesGrid.getPageSize()) {
                namesGrid.setPageSize(namesGrid.getBasePageSize());
                return;
            }
        }



        int from = cachedList.size();
        int num  = range.getStart() + range.getLength() - from;

        GWT.log("ready to request range: " + from + ", " + num );

        Request<List<NameRecordProxy>> request = RequesterFabrique.getRequest(
                sm,
                requestFactory, from, num);

        GWT.log("firing request");
        request.fire( new Receiver<List<NameRecordProxy>>() {

            @Override
            public void onSuccess( List<NameRecordProxy> response) {
                GWT.log( "get data list of size " + response.size() );

                //
                // Data size can be less then page size.
                // It means we are at last page.
                //
                if (response.size() != 0) {
                    cachedList.addAll(response);
                    updateRowData(0, cachedList);
                }

                //
                // In any case reset the table loading state to activate the scrollbar movement custom
                // processing
                //
                namesGrid.setLoadingPageState(false);
            }

            @Override
            public void onFailure(ServerFailure e) {
                GWT.log( "failed to get result: " + e.getMessage() );
            }

        } );

    }

    public void resetCache(boolean sortingState) {
        resetCache(0, sortingState);
    }

    public void resetCache() {
        resetCache(0, false);
    }

    public void resetCache(int pageSize) {
        resetCache( pageSize, false);
    }


    public void resetCache(int pageSize, boolean igoreSortingState) {
        currentPageSize = pageSize;
        this.cachedList.clear();
        sortingState = igoreSortingState;
    }


    private SortingMode getSortingMode() {
        SortingMode sm = SortingMode.Unordered;

        ColumnSortList sortList = namesGrid.getColumnSortList();

        if (sortList.size() > 0) {
            if (sortList.get(0).getColumn() == namesGrid.getColumn( TabbedView.ColumnId.FirstColumn.ordinal())) {
                sm = sortList.get(0).isAscending() ?
                        SortingMode.SortByFirst :
                        SortingMode.SortByFirstDesc;
            } else if (sortList.get(0).getColumn() == namesGrid.getColumn( TabbedView.ColumnId.SecondColumn.ordinal())) {
                sm = sortList.get(0).isAscending() ?
                        SortingMode.SortBySecond :
                        SortingMode.SortBySecondDesc;
            }
        }

        return sm;
    }


    private static class RequesterFabrique {

        private static Request<List<NameRecordProxy>> getRequest(SortingMode m, NamesServiceRequestFactory factory, int from, int len) {
            DataModelRequest req = factory.dataModelRequest();
            switch (m) {
                case Unordered:        return req.getRecordsUnordered( from, len);
                case SortByFirst:      return req.getRecords1stOrdered( from, len);
                case SortByFirstDesc:  return req.getRecords1stOrderedDesc( from,len);
                case SortBySecond:     return req.getRecords2ndOrdered( from, len);
                case SortBySecondDesc: return req.getRecords2ndOrderedDesc( from,len);
            }

            return req.getRecordsUnordered(from, len);
        }

        private static Request<List<NameRecordProxy>> getRequest(SortingMode m, NamesServiceRequestFactory factory,
                                                                 int pageNum) {
            DataModelRequest req = factory.dataModelRequest();
            switch (m) {
                case Unordered:        return req.getRecordsUnordered( pageNum);
                case SortByFirst:      return req.getRecords1stOrdered( pageNum);
                case SortByFirstDesc:  return req.getRecords1stOrderedDesc( pageNum);
                case SortBySecond:     return req.getRecords2ndOrdered( pageNum);
                case SortBySecondDesc: return req.getRecords2ndOrderedDesc( pageNum);
            }

            return req.getRecordsUnordered( pageNum);
        }

    }

}
