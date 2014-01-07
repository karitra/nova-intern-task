package com.hwbs.intertask.client.ui;

import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.ScrollPanel;

/**
 * User:      kaa
 * Timestamp: 12/25/13 11:24 PM
 */
public class LazyLoadDataGrid<T> extends DataGrid<T> {



    private boolean loadingPage = false;
    private int basePageSize    = 0;

    public ScrollPanel getScrollPanel() {
        HeaderPanel header = (HeaderPanel) getWidget();
        return (ScrollPanel) header.getContentWidget();
    }


    public void setBasePageSize(int basePageSize) {
        this.basePageSize = basePageSize;
    }

    public int getBasePageSize() {
        return basePageSize;
    }


    /**
     * Represents the state of table: is asynchronous loading in progress.
     *
     * Mostly needed by scroll bar to avoid redundancy requests overhead, when scroll is in data
     * request poistion, but data already requested.
     *
     * @return
     */
    public boolean getLoadingPageState() {
        return loadingPage;
    }

    public void setLoadingPageState(boolean loadingPage) {
        this.loadingPage = loadingPage;
    }

}
