package com.hwbs.intertask.server.messages;

import com.hwbs.intertask.shared.NameRecord;

/**
 * User:      kaa
 * Timestamp: 12/23/13 12:54 AM
 */
public class ProcessMessage implements CommandMessage {

    NameRecord[] cache;

    public ProcessMessage(NameRecord[] cache) {
        this.cache = cache;
    }

    @Override
    public NameRecord[] getPayload() {
        return cache;
    }

    @Override
    public boolean shouldHalt() {
        return false;
    }

}
