package com.hwbs.intertask.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.hwbs.intertask.client.ui.LazyLoadDataGrid;
import com.hwbs.intertask.shared.ProgressState;

import java.util.List;

//import java.text.MessageFormat;

/**
 * User:      kaa
 * Timestamp: 12/19/13 10:50 PM
 */
public class TabbedView {

    enum SortingMode {
        Unordered,
        SortByFirst,
        SortByFirstDesc,
        SortBySecond,
        SortBySecondDesc
    }

    //interface TabbedViewUiBinder extends UiBinder<HTMLPanel, TabbedView> {}
    interface TabbedViewUiBinder extends UiBinder<LayoutPanel, TabbedView> {}

    private static TabbedViewUiBinder ourUiBinder = GWT.create(TabbedViewUiBinder.class);

    private static final String DEFAULT_BT_CAPTION   = "Execute 1M";
    

    // Should be greater or near second in real app!
    private static final int DEFAULT_POLL_TIMEOUT_MS = 500;
    private static final int CACHE_POLL_INTERVAL_MS  = 1000;

    private static final int DEFAULT_PAGE_SIZE = 20;

    //
    // RequestFuck^^^actory support
    //
    private final SimpleEventBus eb = new SimpleEventBus();
    private final NamesServiceRequestFactory reqFac = GWT.create(NamesServiceRequestFactory.class);
    {
        reqFac.initialize(eb);
    }



    // Logger logger = Logger.getLogger("dbg");
    {
        // TODO: disable logger
    }


    /**
     * Create a remote service proxy to talk to the server-side Greeting service.
     */
    private final GreetingServiceAsync rpcService = GWT.create(GreetingService.class);
    private final RpcExecutorStub      rpcStub = new RpcExecutorStub(rpcService, false);

    private AsyncDataProvider<NameRecordProxy> dataProvider;


    //
    // TODO: add localization to the project!
    //
    private final Messages messages     = GWT.create(Messages.class);
    private final ClientMessages numFmt = GWT.create(ClientMessages.class);

    // @UiField CellTable<NameRecordProxy> namesTable;
    @UiField LazyLoadDataGrid<NameRecordProxy> namesGrid;
    @UiField Button            genBt;
    @UiField TabLayoutPanel tabPanel;
    //@UiField ScrollPanel   scrollPanel;
    //@UiField NativeVerticalScrollbar scroll;
    //@UiField DataGrid<NameRecordProxy> namesGrid;


    //@UiField
    Button       genDummyBt;

    //@UiField TabPanel tabPanel;

    Duration dura;

    //private HTMLPanel root;
    private LayoutPanel root;

    private int page_size = DEFAULT_PAGE_SIZE;

    private static final int MARGIN_WIDGETS_COUNT = 3;
    private static final int CELLS_TO_PRESERVE    = 2;

    enum ScrollMovingDirection {
        Undefinded,
        Forward,
        Backward
    }

    ScrollMovingDirection scrollDir = ScrollMovingDirection.Undefinded;

    private int scrollPosDelta = 0;
    private int oldScrollPos   = 0;

    private int cellHeight = 0;


    final TextColumn<NameRecordProxy> firstNameColumn = new TextColumn<NameRecordProxy>() {
        @Override
        public String getValue(NameRecordProxy object) {
            //GWT.log("my name is " + object.getFirstName());
            if (null == object)
                return null;

            return object.getFirstName();
        }
    };

    final TextColumn<NameRecordProxy> secondNameColumn = new TextColumn<NameRecordProxy>() {
        @Override
        public String getValue(NameRecordProxy object) {

            if (null == object)
                return null;

            return object.getSecondName();
        }
    };

    private enum ScrollingState {
        Init,
        FirstPage,
        LastPage,
        Moving,
        StartOfTable,
        EndOfTable,
        NextPageRequest,
        PrevPageRequest,
        NextSegment,
        PrevSegment,
        SwitchBack
    }

    private ScrollingState scrollState = ScrollingState.Init;
    private boolean scrollProcessingDisabled = false;

    public TabbedView(String btCaption) {
        root = ourUiBinder.createAndBindUi(this);

        tabPanel.selectTab(0);
        genBt.setText(btCaption);

        //genDummyBt.setText("dummy test");

        initCellTable();

        //
        // Workaround for GWT Issue 6889:
        // DataGrid rows not visible in second tab of TabLayoutPanel
        //
        tabPanel.addSelectionHandler( new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                GWT.log("tab selected: " + event.getSelectedItem() );
                namesGrid.redraw();
            }
        }) ;
    }

    public LayoutPanel getSelf() {
          return root;
    }

    private SortingMode getSortingMode() {
        SortingMode sm = SortingMode.Unordered;

        ColumnSortList sortList = namesGrid.getColumnSortList();

        if (sortList.size() > 0) {
            if (sortList.get(0).getColumn() == firstNameColumn) {
                sm = sortList.get(0).isAscending() ?
                        SortingMode.SortByFirst :
                        SortingMode.SortByFirstDesc;
            } else if (sortList.get(0).getColumn() == secondNameColumn) {
                sm = sortList.get(0).isAscending() ?
                        SortingMode.SortBySecond :
                        SortingMode.SortBySecondDesc;
            }
        }

        return sm;
    }

    //
    // Should be called from constructor only
    //
    private void initCellTable() {

        firstNameColumn.setSortable(true);
        secondNameColumn.setSortable(true);

        //namesTable.addColumn( firstNameColumn,  messages.tbHeaderFirstName()  );
        //namesTable.addColumn( secondNameColumn, messages.tbHeaderSecondName() );

        namesGrid.addColumn( firstNameColumn,  messages.tbHeaderFirstName()  );
        namesGrid.addColumn(secondNameColumn, messages.tbHeaderSecondName()  );

        //namesTable.setRowCount( 100, true );
        //namesTable.setVisibleRange(0, 10);

        //namesGrid.setVisibleRange(0, 1000000 );

        // namesTable.setWidth("100%", true);
        namesGrid.setWidth("100%");
        namesGrid.setHeight("100%");

        //namesTable.setColumnWidth(firstNameColumn,  50.0, Style.Unit.PCT );
        //namesTable.setColumnWidth(secondNameColumn, 50.0, Style.Unit.PCT );

        namesGrid.setColumnWidth(firstNameColumn, 50.0, Style.Unit.PCT);
        namesGrid.setColumnWidth(secondNameColumn, 50.0, Style.Unit.PCT );

        dataProvider = new AsyncDataProvider<NameRecordProxy>() {
            @Override
            protected void onRangeChanged(HasData<NameRecordProxy> display) {
                final Range range = display.getVisibleRange();

                GWT.log("range requested: " + range.getStart() + " ~ " + range.getLength() );


                //final ColumnSortList sortList = namesTable.getColumnSortList();

                SortingMode sm = getSortingMode();
                GWT.log("sorting order: " + sm);


                Request<List<NameRecordProxy>> request = RequesterFabrique.getRequest(sm,
                        reqFac,
                        range.getStart(), range.getLength() );

                GWT.log("firing request");
                request.fire( new Receiver<List<NameRecordProxy>>() {
                    @Override
                    public void onSuccess(List<NameRecordProxy> response) {
                        GWT.log( "get data list of size " + response.size() );
                        if (response.size() != 0) {
                            updateRowData(range.getStart(), response);
                        }

                    }

                    @Override
                    public void onFailure(ServerFailure e) {
                        GWT.log( "failed to get result: " + e.getMessage() );
                    }
                } );

                //updateRowData(0, dataList);
            }
        };

        //dataProvider.addDataDisplay(namesTable);
        dataProvider.addDataDisplay(namesGrid);


        //
        // Staring out cache-ready poll
        //
        new Timer() {
            @Override
            public void run() {
                DataModelRequest req = reqFac.dataModelRequest();
                Request<Boolean> cacheReady = req.isMemStoreReady();
                cacheReady.fire( new PollCacheReceiver(this) );
            }
        }.scheduleRepeating(CACHE_POLL_INTERVAL_MS);


        //ColumnSortEvent.AsyncHandler colSortHandler = new ColumnSortEvent.AsyncHandler(namesTable);
        //namesTable.addColumnSortHandler(colSortHandler);

        ColumnSortEvent.AsyncHandler colSortGridHandler = new ColumnSortEvent.AsyncHandler(namesGrid);
        namesGrid.addColumnSortHandler(colSortGridHandler);

        // Setup scroll
        //namesGrid.setCustomScrollBar(scroll);

        setupScrollExp1States();
    }

    private void setupScrollOld2() {

        //p.addScrollHandler(
        new ScrollHandler() {
            @Override
            public void onScroll(ScrollEvent event) {

                ScrollPanel sp = namesGrid.getScrollPanel();

                int pos = sp.getVerticalScrollPosition();
                int min = sp.getMinimumVerticalScrollPosition();
                int max = sp.getMaximumVerticalScrollPosition();

                // update direction delta
                scrollPosDelta = pos - oldScrollPos;
                oldScrollPos   = pos;

                GWT.log("  scroll event: pos " + pos );
                GWT.log("  scroll event: min " + min );
                GWT.log("  scroll event: max " + max );
                GWT.log("  scroll event: delta  " + scrollPosDelta  );


                if (pos == 0 && namesGrid.getPageStart() != 0 ) { // if not at the start of the table
                    if (scrollDir == ScrollMovingDirection.Forward)
                        sp.setVerticalScrollPosition(min + MARGIN_WIDGETS_COUNT * cellHeight);

                    if (scrollDir == ScrollMovingDirection.Backward)
                        sp.setVerticalScrollPosition(max - MARGIN_WIDGETS_COUNT * cellHeight);

                    scrollDir = ScrollMovingDirection.Undefinded;
                    return;
                }


                if (scrollPosDelta > 0) {
                    scrollDir = ScrollMovingDirection.Forward;
                } else if (scrollPosDelta < 0) {
                    scrollDir = ScrollMovingDirection.Backward;
                } else {
                    scrollDir = ScrollMovingDirection.Undefinded;
                }

                GWT.log("scrollDir: " + scrollDir);

                //
                // Update row height
                //
                if (namesGrid.getVisibleItemCount() > 0) {
                    cellHeight = namesGrid.getRowElement(0).getClientHeight();
                    GWT.log("cellHeight: " + cellHeight );
                }

                if (pos > max - MARGIN_WIDGETS_COUNT * cellHeight - 2 /* few pixels */ &&
                        scrollDir == ScrollMovingDirection.Forward) {
                    // nav next page
                    GWT.log("next page request");
                    int navCursor = namesGrid.getPageStart() + namesGrid.getPageSize() - 2 * MARGIN_WIDGETS_COUNT - 2;
                    if (navCursor > namesGrid.getRowCount() - namesGrid.getPageSize() - MARGIN_WIDGETS_COUNT) {
                        navCursor = namesGrid.getRowCount() - namesGrid.getPageSize() - MARGIN_WIDGETS_COUNT;
                    }

                    namesGrid.setPageStart(navCursor);
                    return;
                }

                if (pos < min + MARGIN_WIDGETS_COUNT * cellHeight + 2 /* few pixels */ &&
                        scrollDir == ScrollMovingDirection.Backward) {
                    // prev page
                    GWT.log("prev page request");
                    int navCusror = namesGrid.getPageStart() - namesGrid.getPageSize() - MARGIN_WIDGETS_COUNT;
                    if (navCusror < 0) {
                        navCusror = 0;
                    }

                    namesGrid.setPageStart(navCusror);
                    return;
                }
            }
        };


    }


    private void setupScrollExp1States() {
        ScrollPanel p = namesGrid.getScrollPanel();
        assert null != p;

        p.addScrollHandler(new ScrollHandler() {
            @Override
            public void onScroll(ScrollEvent event) {

                if (scrollProcessingDisabled)
                    return;

                ScrollPanel sp = namesGrid.getScrollPanel();

                int pos = sp.getVerticalScrollPosition();
                int min = sp.getMinimumVerticalScrollPosition();
                int max = sp.getMaximumVerticalScrollPosition();

                // update direction delta
                scrollPosDelta = pos - oldScrollPos;
                oldScrollPos = pos;
                GWT.log("<-------");
                GWT.log("  scroll event: pos " + pos);
                GWT.log("  scroll event: min " + min);
                GWT.log("  scroll event: max " + max);
                //GWT.log("  scroll event: delta  " + scrollPosDelta  );

                if (scrollPosDelta > 0) {
                    scrollDir = ScrollMovingDirection.Forward;
                } else if (scrollPosDelta < 0) {
                    scrollDir = ScrollMovingDirection.Backward;
                } else {
                    scrollDir = ScrollMovingDirection.Undefinded;
                }

                //
                // Update row height
                //
                if (namesGrid.getVisibleItemCount() > 0) {
                    cellHeight = namesGrid.getRowElement(0).getClientHeight();
                    //GWT.log("cellHeight: " + cellHeight );
                }

                GWT.log("  scroll dir: " + scrollDir);
                GWT.log("  scroll state: " + scrollState);

                int reqOffset = 0;
                switch (scrollState) {
                    case FirstPage:
                        if (pos < min + cellHeight * MARGIN_WIDGETS_COUNT &&
                                namesGrid.getPageStart() == 0) {
                            return;
                        }
                    case Init:
                    case Moving:
                        if (pos > max - cellHeight * MARGIN_WIDGETS_COUNT + 2  /* pixels*/ &&
                                scrollDir == ScrollMovingDirection.Forward) {
                            scrollState = ScrollingState.NextPageRequest;

                            reqOffset = namesGrid.getPageStart() + namesGrid.getPageSize() -
                                    2 * MARGIN_WIDGETS_COUNT - CELLS_TO_PRESERVE;
                            if (reqOffset >= namesGrid.getRowCount())
                                reqOffset = namesGrid.getRowCount() - 1;

                            namesGrid.setPageStart(reqOffset);

                            return;
                        }

                        if (pos < min + cellHeight * MARGIN_WIDGETS_COUNT - 2/* pixels*/ &&
                                scrollDir == ScrollMovingDirection.Backward) {
                            scrollState = ScrollingState.PrevPageRequest;

                            reqOffset = namesGrid.getPageStart() - namesGrid.getPageSize() + 2 * MARGIN_WIDGETS_COUNT + CELLS_TO_PRESERVE;
                            if (reqOffset < 0)
                                reqOffset = 0;

                            namesGrid.setPageStart(reqOffset);
                            sp.setVerticalScrollPosition(max - cellHeight * MARGIN_WIDGETS_COUNT - 2);
                            return;
                        }

                        break;
                    case NextPageRequest:
                        sp.setVerticalScrollPosition(cellHeight * MARGIN_WIDGETS_COUNT + 2 /* pixels */);
                        scrollState = ScrollingState.Init;
                        break;
                    case PrevPageRequest:
                        if (namesGrid.getPageStart() == 0) {
                            // first page
                            //sp.setVerticalScrollPosition(0);
                            sp.setVerticalScrollPosition(max - cellHeight * MARGIN_WIDGETS_COUNT - 2);
                            scrollState = ScrollingState.FirstPage;
                            return;
                        }

//                        if (pos < min + cellHeight * MARGIN_WIDGETS_COUNT + 2 /* pixels*/) {
//                            sp.setVerticalScrollPosition(max - cellHeight * MARGIN_WIDGETS_COUNT - 4);
//                            return;
//                        }

                        GWT.log("updating scroll to pos: " + (max - cellHeight * MARGIN_WIDGETS_COUNT - 2) );
                        sp.setVerticalScrollPosition(max - cellHeight * MARGIN_WIDGETS_COUNT - 2);
                        scrollState = ScrollingState.Init;
                        break;
                }
            }

        });

    }

    @UiHandler("genBt")
    void handleClick(ClickEvent e) {

        dura = new Duration();

        setGenBtRunning();

        GWT.log("Pressing button");
        //rpcService.generate(new GenerateCallback<Boolean>());
        rpcStub.setDummyMode(false);
        rpcStub.generate();
        GWT.log("Done: pressing button!");

        dataProvider.updateRowCount(0, true);

        new Timer() {
            @Override
            public void run() {
                scrollProcessingDisabled = true;
                DataModelRequest req = reqFac.dataModelRequest();
                Request<Boolean> cacheReady = req.isMemStoreReady();
                cacheReady.fire( new PollCacheReceiver(this) );
            }
        }.scheduleRepeating(CACHE_POLL_INTERVAL_MS);
    }

    //@UiHandler("genDummyBt")
    void handleDummyClick(ClickEvent e) {

        dura = new Duration();

        setGenBtRunning();

        GWT.log("Pressing dummy button");
        rpcStub.setDummyMode(true);
        rpcStub.generate();
        //rpcService.generateDummy(new GenerateCallback<Boolean>());
        GWT.log("Done: pressing button!");
    }

    //
    // TODO: [debug mode] remove it!
    //
    private class RpcExecutorStub {
        private GreetingServiceAsync srv;
        private boolean dummyMode;

        RpcExecutorStub( GreetingServiceAsync s, boolean dummyMode ) {
            this.srv = s;
            this.dummyMode = dummyMode;
        }

        void generate() {
            if (dummyMode) {
                srv.generateDummy( new GenerateCallback<Boolean>() );
            } else {
                srv.generate( new GenerateCallback<Boolean>() );
            }
        }

        void progress(Timer t) {
            if (dummyMode) {
                // Note: don't remove ProgressState
                rpcService.progressDummy( new PollCallback<ProgressState>(t) );
            } else {
                // Note: don't remove ProgressState
                rpcService.progress(new PollCallback<ProgressState>(t));
            }
        }

        public void setDummyMode(boolean mode) {
            dummyMode = mode;
        }

    }

    /**
     * Poll server (default: twice per second) for names generating completion
     *
     * @param <T> must be subclass of ProgressState
     */
    private class PollCallback<T extends ProgressState> implements AsyncCallback<T> {

        final Timer self_timer;

        public PollCallback(Timer t) {
            self_timer = t;
        }

        @Override
        public void onFailure(Throwable caught) {
            setGenBtDefault();
            Window.alert(caught.getLocalizedMessage());
        }

        @Override
        public void onSuccess(T r) {
            //Window.alert("Poll: is done " + r.isDone());
            if (r.isDone()) {
                GWT.log("time taken: " + dura.elapsedMillis() + "ms");
                // logger.log(Level.INFO, "time taken: " + dura.elapsedMillis() + "ms");
                self_timer.cancel();
                setGenBtDefault();

                reqFac.dataModelRequest().getRecordsUnordered(0, page_size).fire(
                        new Receiver<List<NameRecordProxy>>() {
                            @Override
                            public void onSuccess(List<NameRecordProxy> data) {
                                dataProvider.updateRowData(0, data);

                                //if (scrollDir == ScrollMovingDirection.Forward) {
                                //    namesGrid.getScrollPanel().setVerticalScrollPosition(3 * cellHeight);
                                //}
                        }

                        @Override
                            public void onFailure(ServerFailure e) {
                            GWT.log("failed to get new data: " + e.getMessage());
                        }
                });


                //Window.alert("Poll: is done?" + r.isDone());
                // TODO: load cached pages for names load view
            } else {
                GWT.log("result not yet ready: " + r.getPercents());
                // logger.log(Level.INFO, "percents: " + r.getPercents());
                setGenBtRunning(r.getPercents());
            }
        }
    }

    /**
     * Ignite the generating sequence
     *
     * @param <Boolean>
     */
    private class GenerateCallback<Boolean> implements AsyncCallback<Boolean> {
        @Override
        public void onFailure(Throwable caught) {
            Window.alert("Failed! " + caught.getMessage());
            setGenBtDefault();
        }

        @Override
        public void onSuccess(Boolean result) {

            //Window.alert("Success: " + result);

            if (result.equals(java.lang.Boolean.TRUE)) {
                 final Timer t = new Timer() {
                    @Override
                    public void run() {
                        GWT.log("check-progress...");
                        //rpcService.progress(new PollCallback<ProgressState>(this) );
                        //rpcService.progressDummy( new PollCallback<ProgressState>(this) );
                        rpcStub.progress(this);
                        GWT.log("check-progress called, waiting for result...");
                    }
                 };

                // Start the polling for completion results
                t.scheduleRepeating(DEFAULT_POLL_TIMEOUT_MS);

            } else {
                // 'Execute' call processed successfully, but returns false
                // logical error: user somehow have pressed button again, but generating process still running
                // probably one of generate calls failed earlier
            }


        }
    }

    private class PollCacheReceiver extends Receiver<Boolean> {

        final Timer t;

        PollCacheReceiver(Timer t) {
            this.t = t;
        }

        @Override
        public void onSuccess(Boolean response) {
            if (response.booleanValue()) {

                // cache is ready
                t.cancel();

                DataModelRequest req = reqFac.dataModelRequest();
                req.getViewParameters().fire(new Receiver<ViewParametersProxy>() {
                    @Override
                    public void onSuccess(ViewParametersProxy response) {
                        page_size = response.getPageSize();

                        //namesGrid.setPageSize(page_size + MARGIN_WIDGETS_COUNT * 2);
                        namesGrid.setPageSize(page_size);
                        GWT.log("page_size: " + page_size);

                        int items = response.getTotalItems();

                        scrollProcessingDisabled = false;

                        namesGrid.setRowCount(items, true);
                        dataProvider.updateRowCount(items, true);
                        GWT.log("items: " + items + " of requested data");
                    }
                });

            } else {
                GWT.log("Cache not ready yet [" + response +  "]" );
                // not ready continue polling
            }
        }

        @Override
        public void onFailure(ServerFailure e) {
            GWT.log("error in requesting view parameters: " + e.getMessage());
        }
    }

    private static class RequesterFabrique {

         private static Request<List<NameRecordProxy>> getRequest(SortingMode m, NamesServiceRequestFactory factory, int from, int len) {
            DataModelRequest req = factory.dataModelRequest();
            switch (m) {
                case Unordered:        return req.getRecordsUnordered(from, len);
                case SortByFirst:      return req.getRecords1stOrdered(from, len);
                case SortByFirstDesc:  return req.getRecords1stOrderedDesc(from,len);
                case SortBySecond:     return req.getRecords2ndOrdered(from, len);
                case SortBySecondDesc: return req.getRecords2ndOrderedDesc(from,len);
            }

            return req.getRecordsUnordered(from, len);
        }

    }


    private void setGenBtRunning() {
        setGenBtRunning(0f);
    }

    private void setGenBtRunning(float f) {
        genBt.setText(messages.generating(numFmt.numberFormater(f)));
        genBt.setEnabled(false);
    }

    private void setGenBtDefault() {
        genBt.setText(messages.genButton());
        genBt.setEnabled(true);
    }

}