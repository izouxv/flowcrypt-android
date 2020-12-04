/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 *         Date: 11/20/20
 *         Time: 11:28 AM
 *         E-mail: DenBond7@gmail.com
 */
object IMAPStoreManager {
  val activeConnections = HashMap<Long, IMAPStoreConnection>()

  fun init(context: Context) {
    val applicationContext = context.applicationContext
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

    val pureActiveAccountLiveData: LiveData<AccountEntity?> = roomDatabase.accountDao().getActiveAccountLD()
    val activeAccountLiveData: LiveData<AccountEntity?> = pureActiveAccountLiveData.switchMap { accountEntity ->
      liveData {
        emit(AccountViewModel.getAccountEntityWithDecryptedInfoSuspend(accountEntity))
      }
    }

    activeAccountLiveData.observeForever { activeAccount ->
      GlobalScope.launch(Dispatchers.IO) {
        //check if we have not-registered connections and close them
        activeConnections.forEach { connection ->
          if (roomDatabase.accountDao().getAccount(connection.value.accountEntity.email) == null) {
            try {
              connection.value.disconnect()
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }
        }

        activeAccount?.let {
          val key = it.id ?: -1
          //stop and create a new one
          val existedActiveAccountIMAPStoreConnection = activeConnections[key]
          try {
            existedActiveAccountIMAPStoreConnection?.disconnect()
          } catch (e: Exception) {
            e.printStackTrace()
          }
          activeConnections.remove(key, existedActiveAccountIMAPStoreConnection)

          val newActiveAccountIMAPStoreConnection = IMAPStoreConnection(applicationContext, it)
          activeConnections[key] = newActiveAccountIMAPStoreConnection
          try {
            newActiveAccountIMAPStoreConnection.connect()
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }
    }
  }
}