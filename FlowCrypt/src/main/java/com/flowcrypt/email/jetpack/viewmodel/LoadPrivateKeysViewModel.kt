/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.SearchBackupsUtil
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NodeException
import com.google.android.gms.auth.GoogleAuthException
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.mail.Folder
import javax.mail.MessagingException
import javax.mail.Session
import kotlin.collections.ArrayList

/**
 * This loader finds and returns a user backup of private keys from the mail.
 *
 * @author DenBond7
 * Date: 30.04.2017.
 * Time: 22:28.
 * E-mail: DenBond7@gmail.com
 */
class LoadPrivateKeysViewModel(application: Application) : BaseAndroidViewModel(application) {
  val privateKeysLiveData = MutableLiveData<Result<ArrayList<NodeKeyDetails>?>>()

  fun fetchAvailableKeys(accountEntity: AccountEntity?) {
    viewModelScope.launch {
      val context: Context = getApplication()
      privateKeysLiveData.postValue(Result.loading(progressMsg = context.getString(R.string.searching_backups)))
      if (accountEntity != null) {
        val result = fetchKeys(accountEntity)
        privateKeysLiveData.postValue(result)
      } else {
        privateKeysLiveData.postValue(Result.exception(NullPointerException("AccountEntity is null!")))
      }
    }
  }

  private suspend fun fetchKeys(accountEntity: AccountEntity): Result<ArrayList<NodeKeyDetails>> =
      withContext(Dispatchers.IO) {
        try {
          when (accountEntity.accountType) {
            AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
              GmailApiHelper.executeWithResult {
                Result.success(ArrayList(GmailApiHelper.getPrivateKeyBackups(getApplication(), accountEntity)))
              }
            }

            else -> Result.success(ArrayList(getPrivateKeyBackupsUsingJavaMailAPI(accountEntity)))
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
          Result.exception(e)
        }
      }

  /**
   * Get a list of [NodeKeyDetails] using the standard JavaMail API
   *
   * @param session A [Session] object.
   * @return A list of [NodeKeyDetails]
   * @throws MessagingException
   * @throws IOException
   * @throws GoogleAuthException
   */
  private suspend fun getPrivateKeyBackupsUsingJavaMailAPI(accountEntity: AccountEntity): Collection<NodeKeyDetails> =
      withContext(Dispatchers.IO) {
        val details = ArrayList<NodeKeyDetails>()
        OpenStoreHelper.openStore(getApplication(), accountEntity, OpenStoreHelper.getAccountSess(getApplication(), accountEntity)).use { store ->
          try {
            val context: Context = getApplication()
            val folders = store.defaultFolder.list("*")

            privateKeysLiveData.postValue(Result.loading(progressMsg = context.resources
                .getQuantityString(R.plurals.found_folder, folders.size, folders.size)))

            for ((index, folder) in folders.withIndex()) {
              val containsNoSelectAttr = EmailUtil.containsNoSelectAttr(folder as IMAPFolder)
              if (!containsNoSelectAttr) {
                folder.open(Folder.READ_ONLY)

                val foundMsgs = folder.search(SearchBackupsUtil.genSearchTerms(accountEntity.email))

                for (message in foundMsgs) {
                  val backup = EmailUtil.getKeyFromMimeMsg(message)

                  if (TextUtils.isEmpty(backup)) {
                    continue
                  }

                  try {
                    details.addAll(PgpKey.parseKeysC(backup.toByteArray()))
                  } catch (e: NodeException) {
                    e.printStackTrace()
                    ExceptionUtil.handleError(e)
                  }
                }

                folder.close(false)
              }

              privateKeysLiveData.postValue(Result.loading(progressMsg = context.getString(R.string.searching_in_folders, index, folders.size)))
            }
          } catch (e: Exception) {
            e.printStackTrace()
            throw e
          }
        }
        return@withContext details
      }
}
