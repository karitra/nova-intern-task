package com.hwbs.intertask.client.proxies;

import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ValueProxy;
import com.hwbs.intertask.shared.NameRecord;

/**
 * User:      kaa
 * Timestamp: 12/24/13 6:17 PM
 */
@ProxyFor(NameRecord.class)
public interface NameRecordProxy extends ValueProxy {
    public String getSecondName();
    public String getFirstName();
}
