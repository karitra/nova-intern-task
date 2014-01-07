package com.hwbs.intertask.client.proxies;

import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ValueProxy;
import com.hwbs.intertask.shared.CacheState;

/**
 * User:      kaa
 * Timestamp: 1/7/14 2:06 PM
 */
@ProxyFor(CacheState.class)
public interface CacheStateProxy extends ValueProxy {
    public boolean isValid();
    public long getTimeRegenerating();
    public boolean isLoading();
}
