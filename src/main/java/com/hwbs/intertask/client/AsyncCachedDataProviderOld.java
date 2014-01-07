package com.hwbs.intertask.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.hwbs.intertask.client.proxies.NameRecordProxy;
import com.hwbs.intertask.shared.PagesRange;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
* User:      kaa
* Timestamp: 12/28/13 1:48 PM
 *
 * Note: marked as deprecated, don't use it!
 *
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
 *
 *
*/
@Deprecated
class AsyncCachedDataProviderOld extends AsyncDataProvider<NameRecordProxy> {

    private static int MAX_GAP_ALLOWED     = 1024;
    private static int DEFAULT_PAGES_COUNT = 1024;

    enum SortingMode {
        Unordered,
        SortByFirst,
        SortByFirstDesc,
        SortBySecond,
        SortBySecondDesc
    }

    enum ListAdd {
        Unknown,
        AddToTail,
        AddToFront,
        AddBoth // unused in current implementation
    }


    private DataGrid<?> namesGrid;
    private NamesServiceRequestFactory requestFactory;

    private ArrayList<List<NameRecordProxy>> lazyLoadCache = new ArrayList<List<NameRecordProxy>>(DEFAULT_PAGES_COUNT);
    private int cacheStartOffset = 0;
    private int pageSize = 0;


    public AsyncCachedDataProviderOld(DataGrid<?> namesGrid, NamesServiceRequestFactory requestFactory) {
        this(namesGrid, 0, requestFactory);
    }


    public AsyncCachedDataProviderOld(DataGrid<?> namesGrid, int pageSize, NamesServiceRequestFactory requestFactory) {
        this.namesGrid      = namesGrid;
        this.requestFactory = requestFactory;
        this.pageSize       = pageSize;
    }


    @Override
    protected void onRangeChanged(HasData<NameRecordProxy> display) {

        final Range range = display.getVisibleRange();

        GWT.log("<--- Cache request ----------->");
        GWT.log("range requested: " + range );

        SortingMode sm = getSortingMode();
        GWT.log("sorting order: " + sm);

        //        int requestedPage = getRequestPageNumber(range);
        final PagesRange requestedPages = getRequestPages(range);

        GWT.log("requesting pages range: " + requestedPages);

//        if (requestedPages.getPagesNumber() > lazyLoadCache.size()) {
//            GWT.log("inconsistent page # " + requestedPages + ", should be " + lazyLoadCache.size() );
//            return;
//        }


        //
        // Try cache first
        //
        List<NameRecordProxy> li = getCached(requestedPages);
        if (null != li) {
            GWT.log("cache hit, returning number of items: " + li.size());
            updateRowData( range.getStart(), li);
            return;
        }

        //
        // Note: This implementation only, request just one page
        //
        final int requestedPageFinal = requestedPages.getStartPage();


        GWT.log("ready to request page range " + requestedPageFinal );

        Request<List<NameRecordProxy>> request = RequesterFabrique.getRequest(
                sm,
                requestFactory, requestedPageFinal );


        GWT.log("firing request");
        request.fire( new Receiver<List<NameRecordProxy>>() {

            @Override
            public void onSuccess(List<NameRecordProxy> response) {
                GWT.log( "get data list of size " + response.size() );

                // Data size can be less then page size.
                // It means we are at last page.
                if (response.size() != 0) {
                    if (response.size() != pageSize) {
                        GWT.log("result size is less then page size");
                    }

                    //lazyLoadCache.add( requestedPageFinal * pageSize, response );
                    lazyLoadCache.add( response );
                    updateDisplay(requestedPages);
                }
            }

            @Override
            public void onFailure(ServerFailure e) {
                GWT.log( "failed to get result: " + e.getMessage() );
            }
        } );

        //updateRowData(0, dataList);
    }

    private void updateDisplay( PagesRange pr) {
        List<NameRecordProxy> li = getCached(pr);
        updateRowData(pr.getStartPage() * pageSize, li );
    }

    private PagesRange getRequestPages(Range range) {
        if (pageSize == 0)
            return new PagesRange(0, 0);

        int startingPage = range.getStart() / pageSize;

        return new PagesRange( startingPage,
                (range.getStart() + range.getLength() - startingPage * pageSize) / pageSize );
    }

    private int getRequestPageNumber(Range range) {
//        int itemsInCache = (lazyLoadCache.size()-1) * pageSize;
//
//        //
//        // Last element can contain less then pageSize elements as it can be end of the whole table
//        //
//        if (lazyLoadCache.size() > 0) {
//            itemsInCache += lazyLoadCache.get(lazyLoadCache.size()-1).size();
//        }
//

        //
        // In more sophisticated application we should prepare the list of pages number to request and handle it
        // to the server to get pages list
        //
        // For now just get nex page not in cache
        //
        return lazyLoadCache.size();
    }


    private List<NameRecordProxy> getCached(PagesRange range) {
        List<NameRecordProxy> li;

        if (lazyLoadCache.size() == 0 ||
            range.getStartPage() >= lazyLoadCache.size())
            return null;

        li = new LinkedList<NameRecordProxy>();
        for(int i = 0; i < range.getPagesNumber(); ++i) {
            //
            // TODO: lists collection with concatiation support
            //
            li.addAll(lazyLoadCache.get( range.getStartPage() + i));
        }

        return li;
    }


    private List getCached(int index) {
        if (index >= lazyLoadCache.size())
            return null;

        return lazyLoadCache.get(index);
    }


    @Deprecated
    protected void onRangeChangedExt(HasData<NameRecordProxy> display) {

        final Range range = display.getVisibleRange();

        GWT.log("<--- Cache request ----------->");
        GWT.log("range requested: " + range );

        SortingMode sm = getSortingMode();
        GWT.log("sorting order: " + sm);

        //
        // Requested by client to load those semgent
        //
        int segmentStartOffset = range.getStart();
        int segmentLen   = range.getLength();

        //
        // Try cache first
        //
        List<NameRecordProxy> li = null; // getCached(segmentStartOffset, segmentLen);
        if (null != li) {
            GWT.log("cache hit, returning number of items: " + li.size());
            updateRowData(segmentStartOffset, li);
            return;
        }

        //
        // Not in cache, fall through to the backend.
        //
        // 1. Check request bounds
        //
        int cacheEndOffset   = cacheStartOffset + lazyLoadCache.size() * pageSize;
        int segmentEndOffset = segmentStartOffset + segmentLen;

        ListAdd addSide      = ListAdd.Unknown;

        //
        // We may have 3 variants (in common, not in current application):
        //

        // 1) Head intersect or gap
        //
        // cache:             <---|---|--->
        // request:         <--->
        //
        // cache:             <---|---|--->
        // request:     <--->
        //
        if (segmentStartOffset < cacheStartOffset) {
            GWT.log("Cache relation: head gap or intersect");
            segmentEndOffset = cacheEndOffset;
            segmentStartOffset = cacheStartOffset - segmentStartOffset > MAX_GAP_ALLOWED ?
                    cacheStartOffset - MAX_GAP_ALLOWED :
                    segmentStartOffset;

            addSide = ListAdd.AddToFront;
        }

        //
        // 2) Tail gap or intersect
        // cache:             <---|---|--->
        // request:                          <---|--->
        //
        // cache:             <---|---|--->
        // request:                   <---|--->
        //
        if (segmentEndOffset > cacheEndOffset) {
            GWT.log("Cache relation: tail gap or intersect");

            segmentStartOffset = cacheEndOffset;

            segmentEndOffset = segmentEndOffset - cacheEndOffset > MAX_GAP_ALLOWED ?
                    cacheEndOffset + MAX_GAP_ALLOWED :
                    segmentStartOffset;

            addSide = ListAdd.AddToTail;
        }

        //
        // 3) Superset
        //
        // cache:             <---|---|--->
        // request:       <---|---|---|---|--->
        //
        if (segmentStartOffset < cacheStartOffset &&
                segmentEndOffset > cacheEndOffset) {
            GWT.log("Cache relation: superset [NOT IMPLEMENTED]");

            addSide = ListAdd.AddBoth;

            //
            // TODO: Requested chunk is superset of cache do two calls in this case (+head +tail)
            //
            //throw new Exception("TODO: superset cache set, not used in this application");
            return;
        }

        //
        // Make request boundary page-aligned
        //
        //int pageSize = pageSize;

        assert pageSize != 0;

        segmentStartOffset = segmentStartOffset / pageSize * pageSize;
        segmentEndOffset   = (segmentEndOffset / pageSize + 1) * pageSize;

        GWT.log("ready to request: " + segmentStartOffset + ", " + segmentEndOffset);


        Request<List<NameRecordProxy>> request = RequesterFabrique.getRequest(
                sm,
                requestFactory,
                segmentStartOffset, segmentEndOffset - segmentStartOffset);


        final ListAdd addDirection = addSide;

        GWT.log("firing request");
        request.fire( new Receiver<List<NameRecordProxy>>() {



            @Override
            public void onSuccess(List<NameRecordProxy> response) {
                GWT.log( "get data list of size " + response.size() );
                if (response.size() != 0) {

                    //updateRowData(range.getStart(), response);
                    //updateRowData(namesGrid.loadOffset, response);
                }

            }

            @Override
            public void onFailure(ServerFailure e) {
                GWT.log( "failed to get result: " + e.getMessage() );
            }
        } );

        //updateRowData(0, dataList);
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

    private interface Appender {
        void append(List<NameRecordProxy> list);
    }

    private class AppenderVoid implements Appender {
        @Override
        public void append(List<NameRecordProxy> list) {
            GWT.log("only a stub");
        }
    }

    private class AppenderFront implements Appender {

        @Override
        public void append(List<NameRecordProxy> list) {
            GWT.log("append to front");
            int pagesToInsert = list.size() / pageSize;

            if ( list.size() % pageSize != 0 )
                pagesToInsert++;

            List<NameRecordProxy> li = new ArrayList<NameRecordProxy>( pagesToInsert +
                    lazyLoadCache.size() );

            //
            // Add pages
            //
            List<NameRecordProxy> head = null;
            for(int i = 0; i < list.size(); ++i) {

                if (i % pageSize == 0) {
                    if (null != head) {
                        // submit
                        li.addAll(head);
                    }

                    head = new LinkedList<NameRecordProxy>();
                }

                head.add(list.remove(0));
            }

            //
            // Add reminder if any
            //
            if (null != head && head.size() != 0) {
                li.addAll(head);
            }

        }
    }

    private class AppenderBack implements Appender {

        @Override
        public void append(List<NameRecordProxy> list) {
            GWT.log("append to back");
        }

    }

    private class AppenderBoth implements Appender {

        @Override
        public void append(List<NameRecordProxy> list) {
            GWT.log("not implemented");
        }
    }


    public void resetCache(int pageSize) {
        this.pageSize = pageSize;

        // Should we really use it
//        for(List<NameRecordProxy> li : lazyLoadCache) {
//            li.clear();
//        }

        GWT.log("set pageSize in data provider to " + pageSize);

        this.lazyLoadCache.clear();
        this.lazyLoadCache = new ArrayList<List<NameRecordProxy>>(DEFAULT_PAGES_COUNT);
    }

}
