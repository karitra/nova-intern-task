package com.hwbs.intertask.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;

/**
 * User:      kaa
 * Timestamp: 12/19/13 9:31 PM
 */
public class NViewer implements EntryPoint {

    private final Messages messages = GWT.create(Messages.class);

    @Override
    public void onModuleLoad() {
        TabbedView tb = new TabbedView(messages.genButton());

        Window.enableScrolling(false);
        Window.setMargin("10px");

        //RootPanel.get().add( tb.getSelf() );
        RootLayoutPanel.get().add(tb.getSelf());
        Document.get().setTitle(messages.title());
    }

}
