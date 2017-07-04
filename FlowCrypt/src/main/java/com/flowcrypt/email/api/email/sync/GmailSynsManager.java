/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync;

import android.util.Log;

import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.sync.tasks.LoadMessageDetailsSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadMessagesToCacheSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.LoadNewMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.MoveMessagesSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.SendMessageSyncTask;
import com.flowcrypt.email.api.email.sync.tasks.SyncTask;
import com.flowcrypt.email.api.email.sync.tasks.UpdateLabelsSyncTask;
import com.google.android.gms.auth.GoogleAuthException;
import com.sun.mail.gimap.GmailSSLStore;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;

/**
 * This class describes a logic of work with {@link GmailSSLStore} for the single account. Via
 * this class we can retrieve a new information from the server and send a data to the server.
 * Here we open a new connection to the {@link GmailSSLStore} and keep it alive. This class does
 * all job to communicate with IMAP server.
 *
 * @author DenBond7
 *         Date: 14.06.2017
 *         Time: 10:31
 *         E-mail: DenBond7@gmail.com
 */

public class GmailSynsManager {
    private static final String TAG = GmailSynsManager.class.getSimpleName();

    private static final GmailSynsManager ourInstance = new GmailSynsManager();

    private BlockingQueue<SyncTask> syncTaskBlockingQueue;
    private ExecutorService executorService;
    private Future<?> syncTaskFuture;

    /**
     * This fields created as volatile because will be used in different threads.
     */
    private volatile SyncListener syncListener;
    private volatile Session session;
    private volatile GmailSSLStore gmailSSLStore;

    private GmailSynsManager() {
        this.syncTaskBlockingQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newSingleThreadExecutor();
        updateLabels(null, 0);
    }

    /**
     * Get the single instance of {@link GmailSynsManager}.
     *
     * @return The instance of {@link GmailSynsManager}.
     */
    public static GmailSynsManager getInstance() {
        return ourInstance;
    }

    /**
     * Start a synchronization.
     *
     * @param isNeedReset true if need a reconnect, false otherwise.
     */
    public void beginSync(boolean isNeedReset) {
        Log.d(TAG, "beginSync | isNeedReset = " + isNeedReset);
        if (isNeedReset) {
            cancelAllJobs();
            disconnect();
        }

        if (!isSyncThreadAlreadyWork()) {
            syncTaskFuture = executorService.submit(new SyncTaskRunnable());
        }
    }

    /**
     * Check a sync thread state.
     *
     * @return true if already work, otherwise false.
     */
    public boolean isSyncThreadAlreadyWork() {
        return syncTaskFuture != null && !syncTaskFuture.isCancelled() &&
                !syncTaskFuture.isDone();
    }

    /**
     * Stop a synchronization.
     */
    public void disconnect() {
        if (syncTaskFuture != null) {
            syncTaskFuture.cancel(true);
        }
    }

    /**
     * Clear the queue of sync tasks.
     */
    public void cancelAllJobs() {
        syncTaskBlockingQueue.clear();
    }

    /**
     * Set the {@link SyncListener} for current {@link GmailSynsManager}
     *
     * @param syncListener A new listener.
     */
    public void setSyncListener(SyncListener syncListener) {
        this.syncListener = syncListener;
    }

    /**
     * Run update a folders list.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     */
    public void updateLabels(String ownerKey, int requestCode) {
        try {
            syncTaskBlockingQueue.put(new UpdateLabelsSyncTask(ownerKey, requestCode));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add load a messages information task. This method create a new
     * {@link LoadMessagesSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param folderName  A server folder name.
     * @param start       The position of the start.
     * @param end         The position of the end.
     */
    public void loadMessages(String ownerKey, int requestCode, String folderName, int start, int
            end) {
        try {
            syncTaskBlockingQueue.put(new LoadMessagesSyncTask(ownerKey, requestCode, folderName,
                    start, end));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add load a messages information task. This method create a new
     * {@link LoadMessagesSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param folderName  A server folder name.
     * @param uid         The {@link com.sun.mail.imap.protocol.UID} of {@link Message ).
     */
    public void loadMessageDetails(String ownerKey, int requestCode, String folderName, int uid) {
        try {
            syncTaskBlockingQueue.put(new LoadMessageDetailsSyncTask(ownerKey, requestCode,
                    folderName, uid));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add the task of load information of the next messages. This method create a new
     * {@link LoadMessagesToCacheSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey                     The name of the reply to {@link android.os.Messenger}.
     * @param requestCode                  The unique request code for the reply to
     *                                     {@link android.os.Messenger}.
     * @param folderName                   A server folder name.
     * @param countOfAlreadyLoadedMessages The count of already cached messages in the folder.
     */
    public void loadNextMessages(String ownerKey, int requestCode, String folderName, int
            countOfAlreadyLoadedMessages) {
        try {
            syncTaskBlockingQueue.put(new LoadMessagesToCacheSyncTask(ownerKey, requestCode,
                    folderName, countOfAlreadyLoadedMessages));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add load a new messages information task. This method create a new
     * {@link LoadNewMessagesSyncTask} object and added it to the current synchronization
     * BlockingQueue.
     *
     * @param ownerKey       The name of the reply to {@link android.os.Messenger}.
     * @param requestCode    The unique request code for the reply to {@link android.os.Messenger}.
     * @param folderName     A server folder name.
     * @param lastUIDInCache The UID of the last message of the current folder in the local cache.
     */
    public void loadNewMessagesManually(String ownerKey, int requestCode, String folderName, int
            lastUIDInCache) {
        try {
            syncTaskBlockingQueue.put(new LoadNewMessagesSyncTask(ownerKey, requestCode,
                    folderName, lastUIDInCache));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Move the message to an another folder.
     *
     * @param ownerKey              The name of the reply to {@link android.os.Messenger}.
     * @param requestCode           The unique request code for identify the current action.
     * @param sourceFolderName      The source folder name.
     * @param destinationFolderName The destination folder name.
     * @param uid                   The {@link com.sun.mail.imap.protocol.UID} of {@link javax.mail
     *                              .Message ).
     */
    public void moveMessage(String ownerKey, int requestCode, String sourceFolderName, String
            destinationFolderName, int uid) {
        try {
            syncTaskBlockingQueue.put(new MoveMessagesSyncTask(ownerKey, requestCode,
                    sourceFolderName, destinationFolderName, new long[]{uid}));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Move the message to an another folder.
     *
     * @param ownerKey            The name of the reply to {@link android.os.Messenger}.
     * @param requestCode         The unique request code for identify the current action.
     * @param rawEncryptedMessage The raw encrypted message.
     */
    public void sendEncryptedMessage(String ownerKey, int requestCode, String rawEncryptedMessage) {
        try {
            syncTaskBlockingQueue.put(new SendMessageSyncTask(ownerKey, requestCode,
                    rawEncryptedMessage));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private class SyncTaskRunnable implements Runnable {
        private final String TAG = SyncTaskRunnable.class.getSimpleName();

        @Override
        public void run() {
            Log.d(TAG, "SyncTaskRunnable run");
            Thread.currentThread().setName(SyncTaskRunnable.class.getCanonicalName());
            while (!Thread.interrupted()) {
                try {
                    Log.d(TAG, "SyncTaskBlockingQueue size = " + syncTaskBlockingQueue.size());
                    SyncTask syncTask = syncTaskBlockingQueue.take();

                    if (syncTask != null) {
                        try {
                            if (!isConnected()) {
                                Log.d(TAG, "Not connected. Start a reconnection ...");
                                openConnectionToGmailStore();
                                Log.d(TAG, "Reconnection done");
                            }

                            Log.d(TAG, "Start a new task = " + syncTask.getClass().getSimpleName());
                            if (syncTask.isUseSMTP()) {
                                syncTask.run(session, getEmail(), getValidToken(), syncListener);
                            } else {
                                syncTask.run(gmailSSLStore, syncListener);
                            }
                            Log.d(TAG, "The task = " + syncTask.getClass().getSimpleName()
                                    + " completed");
                        } catch (Exception e) {
                            e.printStackTrace();
                            syncTask.handleException(e, syncListener);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "SyncTaskRunnable stop");
        }

        private void openConnectionToGmailStore() throws IOException,
                GoogleAuthException, MessagingException {
            session = OpenStoreHelper.getGmailSession();
            gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(session, getValidToken(),
                    getEmail());
        }

        /**
         * Check available connection to the gmail store.
         * Must be called from non-main thread.
         *
         * @return trus if connected, false otherwise.
         */
        private boolean isConnected() {
            return gmailSSLStore != null && gmailSSLStore.isConnected();
        }

        /**
         * Check available connection to the gmail store. If connection does not exists try to
         * reconnect.
         * Must be called from non-main thread.
         *
         * @return true if connection available, false otherwise.
         */
        private void checkConnection() throws GoogleAuthException, IOException, MessagingException {
            if (!isConnected()) {
                openConnectionToGmailStore();
            }
        }

        private String getValidToken() throws IOException, GoogleAuthException {
            if (syncListener != null) {
                return syncListener.getValidToken();
            } else
                throw new IllegalArgumentException("You must specify"
                        + SyncListener.class.getSimpleName() + " to use this method");
        }

        private String getEmail() throws IOException, GoogleAuthException {
            if (syncListener != null) {
                return syncListener.getEmail();
            } else
                throw new IllegalArgumentException("You must specify"
                        + SyncListener.class.getSimpleName() + " to use this method");
        }
    }
}