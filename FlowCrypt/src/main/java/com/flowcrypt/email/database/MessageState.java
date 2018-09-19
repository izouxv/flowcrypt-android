/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database;

/**
 * This class describes the message states.
 *
 * @author Denis Bondarenko
 * Date: 16.09.2018
 * Time: 15:11
 * E-mail: DenBond7@gmail.com
 */
public enum MessageState {
    NONE(-1),
    QUEUED(1),
    SENDING(2);

    private int value;

    MessageState(int value) {
        this.value = value;
    }

    public static MessageState generate(int code) {
        for (MessageState messageState : MessageState.values()) {
            if (messageState.getValue() == code) {
                return messageState;
            }
        }

        return null;
    }

    public int getValue() {
        return value;
    }
}
