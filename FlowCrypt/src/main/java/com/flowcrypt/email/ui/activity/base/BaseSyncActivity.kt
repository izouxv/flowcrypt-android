/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Messenger
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.shutdown
import com.flowcrypt.email.jetpack.workmanager.sync.ArchiveMsgsSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.ChangeMsgsReadStateSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesPermanentlySyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteMessagesSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.EmptyTrashSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.MovingToInboxSyncTask
import com.flowcrypt.email.jetpack.workmanager.sync.UpdateLabelsSyncTask
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.BaseNodeActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil

/**
 * This class describes a bind to the email sync service logic.
 *
 * @author DenBond7
 * Date: 16.06.2017
 * Time: 11:30
 * E-mail: DenBond7@gmail.com
 */
//todo-denbond7 need to refactor this class, too many duplicate code
abstract class BaseSyncActivity : BaseNodeActivity() {
  // Messengers for communicating with the service.
  protected var syncMessenger: Messenger? = null
  protected val syncReplyMessenger: Messenger = Messenger(ReplyHandler(this))

  val syncServiceCountingIdlingResource: CountingIdlingResource = CountingIdlingResource(GeneralUtil.genIdlingResourcesName(this::class.java), GeneralUtil.isDebugBuild())

  /**
   * Flag indicating whether we have called bind on the [EmailSyncService].
   */
  @JvmField
  var isSyncServiceBound: Boolean = false

  private val syncConn = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      LogsUtil.d(tag, "Activity connected to " + name.className)
      syncMessenger = Messenger(service)
      isSyncServiceBound = true

      registerReplyMessenger(EmailSyncService.MESSAGE_ADD_REPLY_MESSENGER, syncMessenger!!, syncReplyMessenger)
      onSyncServiceConnected()
    }

    override fun onServiceDisconnected(name: ComponentName) {
      LogsUtil.d(tag, "Activity disconnected from " + name.className)
      syncMessenger = null
      isSyncServiceBound = false
    }
  }

  /**
   * Check is a sync enable.
   *
   * @return true - if sync enable, false - otherwise.
   */
  abstract val isSyncEnabled: Boolean

  abstract fun onSyncServiceConnected()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (isSyncEnabled) {
      bindService(EmailSyncService::class.java, syncConn)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    disconnectFromSyncService()
    syncServiceCountingIdlingResource.shutdown()
  }

  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    syncServiceCountingIdlingResource.decrementSafely(requestCode.toString())
  }

  override fun onProgressReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {

  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    syncServiceCountingIdlingResource.decrementSafely(requestCode.toString())
  }

  override fun onCanceled(requestCode: Int, resultCode: Int, obj: Any?) {
    syncServiceCountingIdlingResource.decrementSafely(requestCode.toString())
  }

  protected fun disconnectFromSyncService() {
    if (isSyncEnabled && isSyncServiceBound) {
      if (syncMessenger != null) {
        unregisterReplyMessenger(EmailSyncService.MESSAGE_REMOVE_REPLY_MESSENGER, syncMessenger!!, syncReplyMessenger)
      }

      unbindService(EmailSyncService::class.java, syncConn)
      isSyncServiceBound = false
    }
  }

  /**
   * Run update a folders list.
   */
  fun updateLabels() {
    UpdateLabelsSyncTask.enqueue(this)
  }

  /**
   * Delete marked messages
   */
  fun deleteMsgs(deletePermanently: Boolean = false) {
    if (deletePermanently) {
      DeleteMessagesPermanentlySyncTask.enqueue(this)
    } else {
      DeleteMessagesSyncTask.enqueue(this)
    }
  }

  /**
   * Empty trash
   */
  fun emptyTrash() {
    EmptyTrashSyncTask.enqueue(this)
  }

  /**
   * Archive marked messages
   */
  fun archiveMsgs() {
    ArchiveMsgsSyncTask.enqueue(this)
  }

  /**
   * Change messages read state.
   */
  fun changeMsgsReadState() {
    ChangeMsgsReadStateSyncTask.enqueue(this)
  }

  /**
   * Move messages back to inbox
   *
   */
  fun moveMsgsToINBOX() {
    MovingToInboxSyncTask.enqueue(this)
  }
}
