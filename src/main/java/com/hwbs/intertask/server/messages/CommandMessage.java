package com.hwbs.intertask.server.messages;

import com.hwbs.intertask.shared.NameRecord;

/**
 * User:      kaa
 * Timestamp: 12/23/13 12:43 AM
 */
public interface CommandMessage {
    NameRecord[] getPayload();
    boolean shouldHalt();
}