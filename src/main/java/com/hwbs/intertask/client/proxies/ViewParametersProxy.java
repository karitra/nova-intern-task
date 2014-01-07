package com.hwbs.intertask.client.proxies;

import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ValueProxy;
import com.hwbs.intertask.shared.ViewParameters;

/**
 * User:      kaa
 * Timestamp: 12/26/13 12:42 AM
 */
@ProxyFor(ViewParameters.class)
public interface ViewParametersProxy  extends ValueProxy {
    int getPageSize();
    int getTotalItems();
}
