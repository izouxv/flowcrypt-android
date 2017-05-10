package com.flowcrypt.email.api.email.model;

import android.os.Parcel;

import com.flowcrypt.email.test.PgpContact;

/**
 * Simple POJO class which describe an outgoing message model.
 *
 * @author DenBond7
 *         Date: 09.05.2017
 *         Time: 11:20
 *         E-mail: DenBond7@gmail.com
 */

public class OutgoingMessageInfo extends MessageInfo {
    public static final Creator<OutgoingMessageInfo> CREATOR = new Creator<OutgoingMessageInfo>() {
        @Override
        public OutgoingMessageInfo createFromParcel(Parcel source) {
            return new OutgoingMessageInfo(source);
        }

        @Override
        public OutgoingMessageInfo[] newArray(int size) {
            return new OutgoingMessageInfo[size];
        }
    };
    private PgpContact[] toPgpContacts;
    private PgpContact fromPgpContact;

    public OutgoingMessageInfo() {
    }

    protected OutgoingMessageInfo(Parcel in) {
        super(in);
        this.toPgpContacts = in.createTypedArray(PgpContact.CREATOR);
        this.fromPgpContact = in.readParcelable(PgpContact.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedArray(this.toPgpContacts, flags);
        dest.writeParcelable(this.fromPgpContact, flags);
    }

    public PgpContact[] getToPgpContacts() {
        return toPgpContacts;
    }

    public void setToPgpContacts(PgpContact[] toPgpContacts) {
        this.toPgpContacts = toPgpContacts;
    }

    public PgpContact getFromPgpContact() {
        return fromPgpContact;
    }

    public void setFromPgpContact(PgpContact fromPgpContact) {
        this.fromPgpContact = fromPgpContact;
    }
}