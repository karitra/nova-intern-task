package com.hwbs.intertask.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.hwbs.intertask.client.proxies.CacheStateProxy;
import com.hwbs.intertask.client.proxies.NameRecordProxy;
import com.hwbs.intertask.client.proxies.ViewParametersProxy;
import com.hwbs.intertask.client.ui.LazyLoadDataGrid;
import com.hwbs.intertask.shared.ProgressState;

//import java.text.MessageFormat;

/**
 * User:      kaa
 * Timestamp: 12/19/13 10:50 PM
 */
public class TabbedView {

    enum ColumnId {
        FirstColumn,
        SecondColumn
    }


    //interface TabbedViewUiBinder extends UiBinder<HTMLPanel, TabbedView> {}
    interface TabbedViewUiBinder extends UiBinder<LayoutPanel, TabbedView> {}

    private static TabbedViewUiBinder ourUiBinder = GWT.create(TabbedViewUiBinder.class);

    private static final String DEFAULT_BT_CAPTION   = "Execute 1M";
    

    // Should be greater or near second in real app!
    private static final int DEFAULT_POLL_TIMEOUT_MS = 500;
    private static final int CACHE_POLL_INTERVAL_MS  = 250;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MARGIN_WIDGETS_COUNT = 2;
    private static final int CELLS_TO_PRESERVE    = 2;


    //
    // RequestFuck^^^actory support
    //
    private final SimpleEventBus eb = new SimpleEventBus();
    private final NamesServiceRequestFactory reqFac = GWT.create(NamesServiceRequestFactory.class);
    {
        reqFac.initialize(eb);
    }

    // Logger logger = Logger.getLogger("dbg");

    /**
     * Create a remote service proxy to talk to the server-side Greeting service.
     */
    private final GreetingServiceAsync rpcService = GWT.create(GreetingService.class);
    private final RpcExecutorStub      rpcStub = new RpcExecutorStub(rpcService, false);

    private AsyncCachedDataProvider dataProvider;

    //
    // TODO: add localization to the project!
    //
    private final Messages messages     = GWT.create(Messages.class);
    private final ClientMessages numFmt = GWT.create(ClientMessages.class);

    @UiField LazyLoadDataGrid<NameRecordProxy> namesGrid;
    @UiField Button            genBt;
    @UiField TabLayoutPanel tabPanel;


    //@UiField
    Button       genDummyBt;

    Duration dura;

    private LayoutPanel root;

    //
    //  Chunks size for records. Preset on server and retrieved on application startup or 'Generate' button press
    //
    private int pageSize   = DEFAULT_PAGE_SIZE;

    //
    //  Remember 'cursor' placement in table of already loaded records
    //
    private int loadOffset          = 0;
    private boolean lazyLoadEnabled = true;

    private boolean loadingPage = false;


    enum ScrollMovingDirection {
        Undefined,
        Forward,
        Backward
    }


    ScrollMovingDirection scrollDir = ScrollMovingDirection.Undefined;

    private int scrollPosDelta = 0;
    private int oldScrollPos   = 0;

    private int cellHeight     = 0;

    private boolean startScrollTransition = false;


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
                // GWT.log("tab selected: " + event.getSelectedItem() );
                namesGrid.redraw();
            }
        }) ;
    }

    public RequestFactory getRequestFactory() {
        return reqFac;
    }

    public LayoutPanel getSelf() {
          return root;
    }

    private void resetLoadedState() {
        pageSize = loadOffset = 0;
        namesGrid.setLoadingPageState(true);
    }


    //
    // Should be called from constructor only
    //
    private void initCellTable() {

        firstNameColumn.setSortable(true);
        secondNameColumn.setSortable(true);

        namesGrid.addColumn( firstNameColumn,  messages.tbHeaderFirstName()  );
        namesGrid.addColumn(secondNameColumn, messages.tbHeaderSecondName()  );

        namesGrid.setWidth("100%");
        namesGrid.setHeight("100%");

        namesGrid.setColumnWidth(firstNameColumn,  50.0, Style.Unit.PCT );
        namesGrid.setColumnWidth(secondNameColumn, 50.0, Style.Unit.PCT );

        namesGrid.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        dataProvider = new AsyncCachedDataProvider(namesGrid, reqFac);

        //dataProvider.addDataDisplay(namesTable);
        dataProvider.addDataDisplay(namesGrid);

        //
        // Add selection model which ingore user selection
        // TODO: do something funcy in case user really want to select something
        //
        ProvidesKey<NameRecordProxy> pk = new ProvidesKey<NameRecordProxy>() {
            @Override
            public Object getKey(NameRecordProxy item) {
                return item.getFirstName() + item.getSecondName();
            }
            };


        namesGrid.setSelectionModel( new SelectionModel.AbstractSelectionModel<NameRecordProxy>(pk) {
            @Override
            public boolean isSelected(NameRecordProxy object) {
                return false;
            }

            @Override
            public void setSelected(NameRecordProxy object, boolean selected) {
                // ingore...
            }
        });

        //
        // Staring out cache-ready poll
        //
        new Timer() {
            @Override
            public void run() {
                DataModelRequest req = reqFac.dataModelRequest();
                //Request<Boolean> cacheReady = req.isMemStoreReady();
                Request<CacheStateProxy> cacheReady = req.getCacheState();
                cacheReady.fire( new PollCacheReceiver(this) );
            }
        }.scheduleRepeating(CACHE_POLL_INTERVAL_MS);



//        ColumnSortEvent.AsyncHandler colSortHandler = new ColumnSortEvent.AsyncHandler(namesGrid) {
//            @Override
//            public void onColumnSort(ColumnSortEvent event) {
//                dataProvider.resetCache(pageSize, true);
//
//                super.onColumnSort(event);
//
//                //
//                // Note: will fire second range  request on 'setPageSize',
//                // by we can freely ignore one (any one of them),
//                // as pageSize is now reset to original state.
//                //
//                namesGrid.setPageSize(pageSize);
//
//
//                GWT.log( "Sorting event!" );
//            }
//        };

        ColumnSortEvent.AsyncHandler colSortHandler = new ColumnSortEvent.AsyncHandler(namesGrid);
        namesGrid.addColumnSortHandler(colSortHandler);

        //
        // Setup scroll
        //
        //namesGrid.setCustomScrollBar(scroll);

        //setupScrollExp1States();
        setupScrollHandler();
    }

    @Deprecated
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

                    scrollDir = ScrollMovingDirection.Undefined;
                    return;
                }


                if (scrollPosDelta > 0) {
                    scrollDir = ScrollMovingDirection.Forward;
                } else if (scrollPosDelta < 0) {
                    scrollDir = ScrollMovingDirection.Backward;
                } else {
                    scrollDir = ScrollMovingDirection.Undefined;
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

    TableRowElement te = null;
    int lastScrollPosition = 0;

    private void setupScrollHandler() {
        ScrollPanel p = namesGrid.getScrollPanel();
        assert null != p;

        HandlerRegistration handlerRegistration = p.addScrollHandler(new ScrollHandler() {
            @Override
            public void onScroll(ScrollEvent event) {

                if (namesGrid.getLoadingPageState())
                    return;

                //
                // Update row height
                //
                if (namesGrid.getVisibleItemCount() > 0) {
                    cellHeight = namesGrid.getRowElement(0).getClientHeight();
                    //GWT.log("cellHeight: " + cellHeight );
                }

                ScrollPanel sp = namesGrid.getScrollPanel();

                int pos = sp.getVerticalScrollPosition();
                int min = sp.getMinimumVerticalScrollPosition();
                int max = sp.getMaximumVerticalScrollPosition();

//                GWT.log("<-------");
//                GWT.log("  scroll event: pos " + pos);
//                GWT.log("  scroll event: min " + min);
//                GWT.log("  scroll event: max " + max);

                if (startScrollTransition) {
                    startScrollTransition = false;

                    GWT.log("  stop scrolling transition, last pos: " + lastScrollPosition);

                    //
                    // GWT bogus behaviour can move the scroll to the start of scroll panel
                    // in that case and if we are after page update, move page to last known
                    // good position
                    //
                    if (Math.abs(pos - lastScrollPosition) > cellHeight * (MARGIN_WIDGETS_COUNT + 2)) {
                        GWT.log("  scroll delta: " + Math.abs(pos - lastScrollPosition));
                        GWT.log("  setting scroll position: " + lastScrollPosition);
                        sp.setVerticalScrollPosition(lastScrollPosition);
                    }

                    return;
                }


                if (pos > max - MARGIN_WIDGETS_COUNT * cellHeight + 2 /* pixels */) {
                    GWT.log("  scroll event: pageSize " + pageSize);

                    namesGrid.setLoadingPageState(true);
                    namesGrid.setPageSize(namesGrid.getPageSize() + pageSize);

                    startScrollTransition = true;
                    lastScrollPosition = sp.getMaximumVerticalScrollPosition() - MARGIN_WIDGETS_COUNT * cellHeight;
                }


            }
        });
    }

    @Deprecated
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
                GWT.log("<----!!!!---");
                GWT.log("  scroll event: pos " + pos);
                GWT.log("  scroll event: min " + min);
                GWT.log("  scroll event: max " + max);
                //GWT.log("  scroll event: delta  " + scrollPosDelta  );

                if (scrollPosDelta > 0) {
                    scrollDir = ScrollMovingDirection.Forward;
                } else if (scrollPosDelta < 0) {
                    scrollDir = ScrollMovingDirection.Backward;
                } else {
                    scrollDir = ScrollMovingDirection.Undefined;
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

                        GWT.log( "updating scroll to pos: " + (max - cellHeight * MARGIN_WIDGETS_COUNT - 2) );
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

        //rpcService.generate(new GenerateCallback<Boolean>());

        rpcStub.setDummyMode(false);
        rpcStub.generate();

        //dataProvider.updateRowCount(0, true);
        resetLoadedState();

        lazyLoadEnabled = false;
    }

    //@UiHandler("genDummyBt")
    void handleDummyClick(ClickEvent e) {

        dura = new Duration();

        setGenBtRunning();

        //GWT.log("Pressing dummy button");
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

                self_timer.cancel();
                setGenBtDefault();

                new Timer() {
                    @Override
                    public void run() {
                        scrollProcessingDisabled = true;
                        DataModelRequest req = reqFac.dataModelRequest();

                        //Request<Boolean> cacheReady = req.isMemStoreReady();
                        Request<CacheStateProxy> cacheReady = req.getCacheState();
                        cacheReady.fire( new PollCacheReceiver(this) );
                    }
                }.scheduleRepeating(CACHE_POLL_INTERVAL_MS);

//                reqFac.dataModelRequest().getRecordsUnordered(0, pageSize).fire(
//                        new Receiver<List<NameRecordProxy>>() {
//                            @Override
//                            public void onSuccess(List<NameRecordProxy> data) {
//                                resetLoadedState();
//                                dataProvider.updateRowData( 0, data);
//                                lazyLoadEnabled = true;
//                        }
//
//                        @Override
//                            public void onFailure(ServerFailure e) {
//                            GWT.log("failed to get new data: " + e.getMessage());
//                        }
//                });

            } else {
                GWT.log("result not yet ready: " + r.getPercents());

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
                // Start the polling for completion results

                // to force page reload
                namesGrid.setPageSize(0);
                namesGrid.getScrollPanel().setVerticalScrollPosition(0);

                new Timer() {
                    @Override
                    public void run() {
                        DataModelRequest req = reqFac.dataModelRequest();
                        //Request<java.lang.Boolean> cacheReady = req.isMemStoreReady();
                        Request<CacheStateProxy> cacheReady = req.getCacheState();
                        cacheReady.fire( new PollCacheReceiver(this) );
                    }
                }.scheduleRepeating(CACHE_POLL_INTERVAL_MS);

            } else {
                // 'Execute' call processed successfully, but returns false
                // logical error: user somehow have pressed button again, but generating process still running
                // probably one of generate calls failed earlier
            }


        }
    }

    private class PollCacheReceiver extends Receiver<CacheStateProxy> {

        private final Timer t;

        PollCacheReceiver(Timer t) {
            this.t = t;
        }

        @Override
        public void onSuccess(CacheStateProxy response) {

            if (!response.isValid()) {

                if (!response.isLoading() && response.isEmpty()) {
                    GWT.log( "Cache is empty, get out of here: " + response.getTimeRegenerating() + " ms passed");

                    t.cancel();
                    setGenBtDefault();
                    return;
                }

                //
                // Continue polling if cache not valid
                //
                GWT.log( "Cache not ready yet: " + response.getTimeRegenerating() + " ms passed" );
                setGenBtRunning( response.getTimeRegenerating() / 1000.0f );
            } else { // !loading and (valid or empty)

                //
                // cache is ready or empty, stop polling
                //
                t.cancel();

                //
                //  Request backend view parameters
                //
                DataModelRequest req = reqFac.dataModelRequest();
                req.getViewParameters().fire(new Receiver<ViewParametersProxy>() {
                    @Override
                    public void onSuccess(ViewParametersProxy response) {

                        // resetLoadedState();

                        pageSize = response.getPageSize();
                        int items = response.getTotalItems();

                        GWT.log("pageSize: " + pageSize);
                        GWT.log("items:    " + items + " in database");

                        //namesGrid.setRowCount(items, true);
                        dataProvider.resetCache(pageSize);

                        namesGrid.setBasePageSize(pageSize);

                        namesGrid.setPageSize(pageSize);
                        namesGrid.setRowCount(items, true);

                        //dataProvider.updateRowCount( items, true);
                        setGenBtDefault();
                    }

                    @Override
                    public void onFailure(ServerFailure e) {
                        GWT.log("error in requesting view parameters: " + e.getMessage());
                        t.cancel();
                        setGenBtDefault();
                    }
                });
            }
        }


        @Override
        public void onFailure(ServerFailure e) {
            GWT.log("error in requesting server state: " + e.getMessage());
            t.cancel();
        }
    }



    private void setGenBtRunning() {
        setGenBtRunning(0f);
    }

    private void setGenBtRunning(float f) {

        genBt.setText( messages.generating( numFmt.numberFormater(f) ) );
        genBt.setEnabled(false);
    }

    private void setGenBtDefault() {
        genBt.setText(messages.genButton());
        genBt.setEnabled(true);
    }

}