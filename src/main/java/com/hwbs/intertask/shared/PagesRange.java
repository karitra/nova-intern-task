package com.hwbs.intertask.shared;

/**
* User:      kaa
* Timestamp: 12/28/13 8:33 PM
*/
public class PagesRange {

    private int startPage, pagesNumber;

    public PagesRange(int startPage, int pageNumber) {
        this.startPage  = startPage;
        this.pagesNumber = pageNumber;
    }

    public int getStartPage()   { return   startPage; }
    public int getPagesNumber() { return pagesNumber; }

    public boolean empty()      { return startPage == 0 && pagesNumber == 0; }

    public String toString()    {
        StringBuilder sb = new StringBuilder(32);
        return sb.append('(')
                .append(startPage)
                .append(", ")
                .append(pagesNumber)
                .append(')').toString();
    }
}
