/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.util.GeneralUtil

/**
 * This class describes the base settings for added email providers
 *
 * @author Denis Bondarenko
 *         Date: 7/24/20
 *         Time: 9:09 AM
 *         E-mail: DenBond7@gmail.com
 */
object ProvideEmailSettingsHelper {
  private const val IMAP_SERVER_OUTLOOK = "outlook.office365.com"
  private const val SMTP_SERVER_OUTLOOK = "smtp.office365.com"
  private const val PROVIDER_OUTLOOK = "outlook.com"

  /**
   * Get the base settings for the given account.
   */
  fun getBaseSettings(email: String, password: String): AuthCredentials? {
    if (!GeneralUtil.isEmailValid(email)) {
      return null
    }

    return when {
      PROVIDER_OUTLOOK.equals(EmailUtil.getDomain(email), true) -> getOutlookSettings(email, password)

      else -> {
        null
      }
    }
  }

  private fun getOutlookSettings(email: String, password: String): AuthCredentials {
    return AuthCredentials(
        email = email,
        username = email,
        password = password,
        imapServer = IMAP_SERVER_OUTLOOK,
        imapPort = JavaEmailConstants.SSL_IMAP_PORT,
        imapOpt = SecurityType.Option.SSL_TLS,
        smtpServer = SMTP_SERVER_OUTLOOK,
        smtpPort = JavaEmailConstants.STARTTLS_SMTP_PORT,
        smtpOpt = SecurityType.Option.STARTLS,
        hasCustomSignInForSmtp = true,
        smtpSigInUsername = email,
        smtpSignInPassword = password
    )
  }
}