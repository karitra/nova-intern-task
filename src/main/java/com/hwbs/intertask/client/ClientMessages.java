package com.hwbs.intertask.client;

/**
 * User:      kaa
 * Timestamp: 12/20/13 7:21 PM
 *
 * Workaround for gwt-i18n issue: generated formatter always making String args
 *
 */
public interface ClientMessages extends com.google.gwt.i18n.client.Messages {

    @Key("numberFormat")
    @DefaultMessage("{0,number,###.#}")
    String numberFormater(float f);
}
