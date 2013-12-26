package com.hwbs.intertask.server.messages;

import com.hwbs.intertask.shared.NameRecord;

/**
 * User:      kaa
 * Timestamp: 12/23/13 12:56 AM
 */
public class QuitMessage implements CommandMessage {

    @Override
    public NameRecord[] getPayload() {
        throw new IllegalStateException();
    }

    @Override
    public boolean shouldHalt() {
        return true;
    }

}
