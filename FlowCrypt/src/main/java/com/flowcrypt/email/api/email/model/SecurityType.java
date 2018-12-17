/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.JavaEmailConstants;

import java.util.ArrayList;

import androidx.annotation.NonNull;

/**
 * This class describes settings for some security type.
 *
 * @author Denis Bondarenko
 * Date: 13.09.2017
 * Time: 14:35
 * E-mail: DenBond7@gmail.com
 */

public class SecurityType implements Parcelable {

  public static final Creator<SecurityType> CREATOR = new Creator<SecurityType>() {
    @Override
    public SecurityType createFromParcel(Parcel source) {
      return new SecurityType(source);
    }

    @Override
    public SecurityType[] newArray(int size) {
      return new SecurityType[size];
    }
  };
  private String name;
  private int defImapPort;
  private int defSmtpPort;
  private Option option;

  public SecurityType(String name, Option option, int defImapPort, int smtpPort) {
    this.name = name;
    this.option = option;
    this.defImapPort = defImapPort;
    this.defSmtpPort = smtpPort;
  }

  public SecurityType(Parcel in) {
    this.name = in.readString();
    this.defImapPort = in.readInt();
    this.defSmtpPort = in.readInt();
    int tmpOpt = in.readInt();
    this.option = tmpOpt == -1 ? null : Option.values()[tmpOpt];
  }

  /**
   * Generate a list which contains all available {@link SecurityType}.
   *
   * @return The list of all available {@link SecurityType}.
   */
  @NonNull
  public static ArrayList<SecurityType> generateSecurityTypes(Context context) {
    ArrayList<SecurityType> securityTypes = new ArrayList<>();
    securityTypes.add(new SecurityType(context.getString(R.string.none), SecurityType.Option.NONE,
        JavaEmailConstants.DEFAULT_IMAP_PORT, JavaEmailConstants.DEFAULT_SMTP_PORT));
    securityTypes.add(new SecurityType(context.getString(R.string.ssl_tls), SecurityType.Option.SSL_TLS,
        JavaEmailConstants.SSL_IMAP_PORT, JavaEmailConstants.SSL_SMTP_PORT));
    securityTypes.add(new SecurityType(context.getString(R.string.startls), SecurityType.Option.STARTLS,
        JavaEmailConstants.DEFAULT_IMAP_PORT, JavaEmailConstants.STARTTLS_SMTP_PORT));
    return securityTypes;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.name);
    dest.writeInt(this.defImapPort);
    dest.writeInt(this.defSmtpPort);
    dest.writeInt(this.option == null ? -1 : this.option.ordinal());
  }

  public String getName() {
    return name;
  }

  public int getDefImapPort() {
    return defImapPort;
  }

  public int getDefaultSmtpPort() {
    return defSmtpPort;
  }

  public Option getOpt() {
    return option;
  }

  public enum Option {
    NONE, SSL_TLS, STARTLS
  }
}