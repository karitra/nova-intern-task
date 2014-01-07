package com.hwbs.intertask.shared;

/**
 * User:      kaa
 * Timestamp: 12/26/13 12:36 AM
 */
public class ViewParameters {
    private int items_cnt, page_size;
    //private boolean isCacheValid;

    public ViewParameters() {

    }

    public ViewParameters(int items, int page) {
        this.items_cnt    = items;
        this.page_size    = page;
        //this.isCacheValid = isValid;
    }

    public int getPageSize() {
        return page_size;
    }

    public int getTotalItems() {
        return items_cnt;
    }

}
