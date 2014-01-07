package com.hwbs.intertask.client.ui;

import com.google.gwt.user.cellview.client.DataGrid;

/**
 * User:      kaa
 * Timestamp: 12/28/13 9:19 PM
 */
public class Experemental<T> extends DataGrid<T> {

    //    NativeVerticalScrollbar scroll;
//
//    class NavigationScrollPanel extends CustomScrollPanel {
//
//        NavigationScrollPanel(Widget w) {
//            super(w);
//            //setCustomScrollBar(null);
//            removeVerticalScrollbar();
//        }
//
//    }

    //
    // Reparent experements
    //
//    public void setCustomScrollBar(NativeVerticalScrollbar vb) {
//        //if (true)
//        //    return;
//
//        this.scroll = vb;
//
//        scroll.setScrollHeight(10000);
//        scroll.addScrollHandler( new ScrollHandler() {
//            @Override
//            public void onScroll(ScrollEvent event) {
//                GWT.log("new scroll: " + scroll.getVerticalScrollPosition());
//            }
//        });
//
//
//        HeaderPanel body = (HeaderPanel) getWidget();
//
//        ScrollPanel sp = getScrollPanel();
//        Widget dataPanel = sp.getWidget();
//
//        NavigationScrollPanel newPanel = new NavigationScrollPanel(dataPanel);
//        body.setContentWidget(newPanel);
//
//        //body.add(content);
//        //body.remove(sp);
//
////        ScrollPanel sp = getScrollPanel();
////        Widget content = sp.getWidget();
////        HeaderPanel body = (HeaderPanel) getWidget();
////
////        Widget c = body.getContentWidget();
////        GWT.log("content: " + c);
////        GWT.log("scrollp: " + sp);
////
////        NavigationScrollPanel newPanel = new NavigationScrollPanel(content);
////
////        sp.removeFromParent();
////        GWT.log("content: " + body);
////        body.add(newPanel);
////        GWT.log("nconten: " + newPanel);
////
////        /*
////         * CustomScrollPanel applies the inline block style to the container
////         * element, but we want the container to fill the available width.
////        */
////        content.getElement().getParentElement().getStyle().setDisplay(com.google.gwt.dom.client.Style.Display.BLOCK);BLOCK
//    }


//    @Override
//    public void redraw() {
//        super.redraw();
//        if (null != scroll) {
//            int hdrHeight = getHdrHeight();
//            int height = scroll.getNativeScrollbarHeight();
//            int newHeight = height - hdrHeight;
//            int tb = getTableHeadElement().getParentElement().getClientHeight();
//
//            GWT.log("scl height = " + height    );
//            GWT.log("hdr height = " + hdrHeight );
//            GWT.log("new height = " + newHeight );
//            GWT.log("tb  height = " + tb );
//
//            //scroll.setHeight(newHeight + "px");
//        }
//
//    }
}

