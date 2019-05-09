/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.MenuItem;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.node.Node;
import com.flowcrypt.email.service.BaseService;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.LogsUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.idling.NodeIdlingResource;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.loader.content.Loader;

/**
 * This is a base activity. This class describes a base logic for all activities.
 *
 * @author DenBond7
 * Date: 30.04.2017.
 * Time: 22:21.
 * E-mail: DenBond7@gmail.com
 */
public abstract class BaseActivity extends AppCompatActivity implements BaseService.OnServiceCallback {
  protected final String tag;
  protected NodeIdlingResource nodeIdlingResource;

  private Snackbar snackbar;
  private Toolbar toolbar;
  private AppBarLayout appBarLayout;

  public BaseActivity() {
    tag = getClass().getSimpleName();
  }

  /**
   * This method can used to change "HomeAsUpEnabled" behavior.
   *
   * @return true if we want to show "HomeAsUpEnabled", false otherwise.
   */
  public abstract boolean isDisplayHomeAsUpEnabled();

  /**
   * Get the content view resources id. This method must return an resources id of a layout
   * if we want to show some UI.
   *
   * @return The content view resources id.
   */
  public abstract int getContentViewResourceId();

  /**
   * Get root view which will be used for show Snackbar.
   */
  public abstract View getRootView();

  @Override
  public void onReplyReceived(int requestCode, int resultCode, Object obj) {

  }

  @Override
  public void onProgressReplyReceived(int requestCode, int resultCode, Object obj) {

  }

  @Override
  public void onErrorHappened(int requestCode, int errorType, Exception e) {

  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    registerNodeIdlingResources();
    LogsUtil.d(tag, "onCreate");
    if (getContentViewResourceId() != 0) {
      setContentView(getContentViewResourceId());
      initScreenViews();
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    LogsUtil.d(tag, "onStart");
  }

  @Override
  public void onResume() {
    super.onResume();
    LogsUtil.d(tag, "onResume");
  }

  @Override
  public void onStop() {
    super.onStop();
    LogsUtil.d(tag, "onStop");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    LogsUtil.d(tag, "onDestroy");
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        if (isDisplayHomeAsUpEnabled()) {
          finish();
          return true;
        } else return super.onOptionsItemSelected(item);

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @VisibleForTesting
  public NodeIdlingResource getNodeIdlingResource() {
    return nodeIdlingResource;
  }

  public Toolbar getToolbar() {
    return toolbar;
  }

  public AppBarLayout getAppBarLayout() {
    return appBarLayout;
  }

  /**
   * Show information as Snackbar.
   *
   * @param view        The view to find a parent from.
   * @param messageText The text to show.  Can be formatted text.
   */
  public void showInfoSnackbar(View view, String messageText) {
    showInfoSnackbar(view, messageText, Snackbar.LENGTH_INDEFINITE);
  }

  /**
   * Show information as Snackbar.
   *
   * @param view        The view to find a parent from.
   * @param messageText The text to show.  Can be formatted text.
   * @param duration    How long to display the message.
   */
  public void showInfoSnackbar(View view, String messageText, int duration) {
    snackbar = Snackbar.make(view, messageText, duration).setAction(android.R.string.ok, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });
    snackbar.show();
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param messageText     The text to show.  Can be formatted text
   * @param buttonName      The text of the Snackbar button
   * @param onClickListener The Snackbar button click listener.
   */
  public void showSnackbar(View view, String messageText, String buttonName,
                           @NonNull View.OnClickListener onClickListener) {
    showSnackbar(view, messageText, buttonName, Snackbar.LENGTH_INDEFINITE, onClickListener);
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param messageText     The text to show.  Can be formatted text
   * @param buttonName      The text of the Snackbar button
   * @param duration        How long to display the message.
   * @param onClickListener The Snackbar button click listener.
   */
  public void showSnackbar(View view, String messageText, String buttonName, int duration,
                           @NonNull View.OnClickListener onClickListener) {
    snackbar = Snackbar.make(view, messageText, duration).setAction(buttonName, onClickListener);
    snackbar.show();
  }

  public Snackbar getSnackBar() {
    return snackbar;
  }

  public void dismissSnackBar() {
    if (snackbar != null) {
      snackbar.dismiss();
    }
  }

  public void handleLoaderResult(Loader loader, LoaderResult loaderResult) {
    if (loaderResult != null) {
      if (loaderResult.getResult() != null) {
        onSuccess(loader.getId(), loaderResult.getResult());
      } else if (loaderResult.getException() != null) {
        onError(loader.getId(), loaderResult.getException());
      } else {
        showInfoSnackbar(getRootView(), getString(R.string.unknown_error));
      }
    } else {
      showInfoSnackbar(getRootView(), getString(R.string.unknown_error));
    }
  }

  public void onError(int loaderId, Exception e) {

  }

  public void onSuccess(int loaderId, Object result) {

  }

  public String getReplyMessengerName() {
    return getClass().getSimpleName() + "_" + hashCode();
  }

  /**
   * Check is current {@link Activity} connected to some service.
   *
   * @return true if current activity connected to the service, otherwise false.
   */
  protected boolean checkServiceBound(boolean isBound) {
    if (!isBound) {
      if (GeneralUtil.isDebugBuild()) {
        LogsUtil.d(tag, "Activity not connected to the service");
      }
      return true;
    }
    return false;
  }

  protected void bindService(Class<?> cls, ServiceConnection conn) {
    bindService(new Intent(this, cls), conn, Context.BIND_AUTO_CREATE);
    LogsUtil.d(tag, "bind to " + cls.getSimpleName());
  }

  /**
   * Disconnect from a service
   */
  protected void unbindService(Class<?> cls, ServiceConnection conn) {
    unbindService(conn);
    LogsUtil.d(tag, "unbind from " + cls.getSimpleName());
  }

  /**
   * Register a reply {@link Messenger} to receive notifications from some service.
   *
   * @param what             A {@link Message#what}}
   * @param serviceMessenger A service {@link Messenger}
   * @param replyToMessenger A reply to {@link Messenger}
   */
  protected void registerReplyMessenger(int what, Messenger serviceMessenger, Messenger replyToMessenger) {
    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), -1, null);

    Message message = Message.obtain(null, what, action);
    message.replyTo = replyToMessenger;
    try {
      serviceMessenger.send(message);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Unregister a reply {@link Messenger} from some service.
   *
   * @param what             A {@link Message#what}}
   * @param serviceMessenger A service {@link Messenger}
   * @param replyToMessenger A reply to {@link Messenger}
   */
  protected void unregisterReplyMessenger(int what, Messenger serviceMessenger, Messenger replyToMessenger) {
    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), -1, null);

    Message message = Message.obtain(null, what, action);
    message.replyTo = replyToMessenger;
    try {
      serviceMessenger.send(message);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  public boolean isNodeReady() {
    if (Node.getInstance() == null || Node.getInstance().getLiveData() == null
        || Node.getInstance().getLiveData().getValue() == null) {
      return false;
    }

    return Node.getInstance().getLiveData().getValue();
  }

  protected void onNodeStateChanged(boolean isReady) {

  }

  private void registerNodeIdlingResources() {
    nodeIdlingResource = new NodeIdlingResource();
    Node.getInstance().getLiveData().observe(this, new Observer<Boolean>() {
      @Override
      public void onChanged(Boolean aBoolean) {
        nodeIdlingResource.setIdleState(aBoolean);
        onNodeStateChanged(aBoolean);
      }
    });
  }

  private void initScreenViews() {
    appBarLayout = findViewById(R.id.appBarLayout);
    setupToolbar();
  }

  private void setupToolbar() {
    toolbar = findViewById(R.id.toolbar);
    if (toolbar != null) {
      setSupportActionBar(toolbar);
    }

    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(isDisplayHomeAsUpEnabled());
    }
  }

  /**
   * The incoming handler realization. This handler will be used to communicate with a service and other Android
   * components.
   */
  protected static class ReplyHandler extends Handler {
    private final WeakReference<BaseService.OnServiceCallback> weakRef;

    ReplyHandler(BaseService.OnServiceCallback onServiceCallback) {
      this.weakRef = new WeakReference<>(onServiceCallback);
    }

    @Override
    public void handleMessage(Message message) {
      if (weakRef.get() != null) {
        BaseService.OnServiceCallback onServiceCallback = weakRef.get();
        switch (message.what) {
          case BaseService.REPLY_OK:
            onServiceCallback.onReplyReceived(message.arg1, message.arg2, message.obj);
            break;

          case BaseService.REPLY_ERROR:
            Exception exception = null;

            if (message.obj instanceof Exception) {
              exception = (Exception) message.obj;
            }

            onServiceCallback.onErrorHappened(message.arg1, message.arg2, exception);
            break;

          case BaseService.REPLY_ACTION_PROGRESS:
            onServiceCallback.onProgressReplyReceived(message.arg1, message.arg2, message.obj);
            break;
        }
      }
    }
  }
}
