/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.util.Log;

import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.js.tasks.DecryptRawMimeMessageJsTask;
import com.flowcrypt.email.js.tasks.JsTask;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This manager creates and manages {@link JsTask}s using a background thread.
 *
 * @author Denis Bondarenko
 * Date: 15.02.2018
 * Time: 13:04
 * E-mail: DenBond7@gmail.com
 */

public class JsInBackgroundManager {
  private static final int JS_THREADS_COUNT = 3;
  private static final String TAG = JsInBackgroundManager.class.getSimpleName();

  private ExecutorService executorService;
  private Future<?> futureFirst;
  private Future<?> futureSecond;
  private Future<?> futureThird;

  /**
   * This fields created as volatile because will be used in different threads.
   */
  private volatile JsListener jsListener;
  private volatile BlockingQueue<JsTask> queue;

  public JsInBackgroundManager() {
    this.queue = new LinkedBlockingQueue<>();
    this.executorService = Executors.newFixedThreadPool(JS_THREADS_COUNT);
  }

  /**
   * Init background threads.
   */
  public void init() {
    Log.d(TAG, "init");
    if (!isThreadAlreadyWorking(futureFirst)) {
      futureFirst = executorService.submit(new JsRunnable("FirstJsWorker"));
    }

    if (!isThreadAlreadyWorking(futureSecond)) {
      futureSecond = executorService.submit(new JsRunnable("SecondJsWorker"));
    }

    if (!isThreadAlreadyWorking(futureThird)) {
      futureThird = executorService.submit(new JsRunnable("ThirdJsWorker"));
    }
  }

  /**
   * Stop an active action.
   */
  public void stop() {
    Log.d(TAG, "stop");
    releaseResources();

    if (executorService != null) {
      executorService.shutdown();
    }
  }

  /**
   * Clear the tasks queue.
   */
  public void cancelAllTasks() {
    Log.d(TAG, "cancelAllTasks");
    if (queue != null) {
      queue.clear();
    }
  }

  /**
   * Set the {@link JsListener} for current {@link JsInBackgroundManager}
   *
   * @param jsListener A new listener.
   */
  public void setJsListener(JsListener jsListener) {
    this.jsListener = jsListener;
  }

  /**
   * Decrypt an incoming raw message.
   *
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for identify the current action.
   * @param rawMsg  The incoming raw message.
   */
  public void decryptMsg(String ownerKey, int requestCode, String rawMsg) {
    try {
      queue.put(new DecryptRawMimeMessageJsTask(ownerKey, requestCode, rawMsg));
    } catch (InterruptedException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Restart a current manager.
   */
  public void restart() {
    Log.d(TAG, "restart");
    releaseResources();
    init();
  }

  /**
   * Check a pull thread state.
   *
   * @return true if already work, otherwise false.
   */
  private boolean isThreadAlreadyWorking(Future<?> future) {
    return future != null && !future.isCancelled() && !future.isDone();
  }

  /**
   * Reset a current pull thread.
   */
  private void releaseResources() {
    Log.d(TAG, "releaseResources");
    cancelAllTasks();

    if (futureFirst != null) {
      futureFirst.cancel(true);
    }

    if (futureSecond != null) {
      futureSecond.cancel(true);
    }

    if (futureThird != null) {
      futureThird.cancel(true);
    }
  }

  /**
   * An implementation of the worker thread.
   */
  private class JsRunnable implements Runnable {
    private String tag;
    private Js js;
    private String workerName;

    JsRunnable(String workerName) {
      this.workerName = workerName;
      this.tag = JsRunnable.class.getSimpleName() + "|" + workerName;
    }

    @Override
    public void run() {
      Log.d(tag, " run!");
      Thread.currentThread().setName(workerName);

      try {
        Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      try {
        js = new Js(jsListener.getContext(), new SecurityStorageConnector(jsListener.getContext()));
        boolean isInterrupted = false;
        while (!isInterrupted) {
          try {
            Log.d(tag, "queue size = " + queue.size());
            JsTask jsTask = queue.take();

            if (jsTask != null) {
              runJsTask(jsTask);
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
            isInterrupted = true;
            Log.d(tag, "A task was interrupted!");
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      Log.d(tag, " stopped!");
    }

    /**
     * Run the incoming {@link JsTask}
     *
     * @param jsTask The incoming {@link JsTask}
     */
    void runJsTask(JsTask jsTask) {
      try {
        Log.d(tag, "Start a new task = " + jsTask.getClass().getSimpleName());
        jsTask.runAction(js, jsListener);
        Log.d(tag, "The task = " + jsTask.getClass().getSimpleName() + " completed");
      } catch (Exception e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
        jsTask.handleException(e, jsListener);
      }
    }
  }
}
