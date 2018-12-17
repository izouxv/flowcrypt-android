/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.model.ServiceInfo;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.model.MessageType;
import com.flowcrypt.email.service.PrepareOutgoingMessagesJobIntentService;
import com.flowcrypt.email.ui.activity.base.BaseBackStackSyncActivity;
import com.flowcrypt.email.ui.activity.fragment.base.CreateMessageFragment;
import com.flowcrypt.email.ui.activity.listeners.OnChangeMessageEncryptionTypeListener;
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.UIUtil;

import androidx.annotation.Nullable;

/**
 * This activity describes a logic of send encrypted or standard message.
 *
 * @author DenBond7
 * Date: 10.05.2017
 * Time: 11:43
 * E-mail: DenBond7@gmail.com
 */

public class CreateMessageActivity extends BaseBackStackSyncActivity implements
    CreateMessageFragment.OnMessageSendListener, OnChangeMessageEncryptionTypeListener {

  public static final String EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE =
      GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE", CreateMessageActivity.class);

  public static final String EXTRA_KEY_INCOMING_MESSAGE_INFO = GeneralUtil.generateUniqueExtraKey
      ("EXTRA_KEY_INCOMING_MESSAGE_INFO", CreateMessageActivity.class);

  public static final String EXTRA_KEY_SERVICE_INFO = GeneralUtil.generateUniqueExtraKey
      ("EXTRA_KEY_SERVICE_INFO", CreateMessageActivity.class);

  public static final String EXTRA_KEY_MESSAGE_TYPE =
      GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_MESSAGE_TYPE", CreateMessageActivity.class);

  private View nonEncryptedHintView;
  private View layoutContent;

  private MessageEncryptionType msgEncryptionType = MessageEncryptionType.ENCRYPTED;
  private ServiceInfo serviceInfo;

  public static Intent generateIntent(Context context, IncomingMessageInfo msgInfo,
                                      MessageEncryptionType msgEncryptionType) {
    return generateIntent(context, msgInfo, MessageType.NEW, msgEncryptionType);
  }

  public static Intent generateIntent(Context context, IncomingMessageInfo msgInfo,
                                      MessageType messageType, MessageEncryptionType msgEncryptionType) {
    return generateIntent(context, msgInfo, messageType, msgEncryptionType, null);
  }

  public static Intent generateIntent(Context context, IncomingMessageInfo msgInfo, MessageType messageType,
                                      MessageEncryptionType msgEncryptionType, ServiceInfo serviceInfo) {

    Intent intent = new Intent(context, CreateMessageActivity.class);
    intent.putExtra(EXTRA_KEY_INCOMING_MESSAGE_INFO, msgInfo);
    intent.putExtra(EXTRA_KEY_MESSAGE_TYPE, messageType);
    intent.putExtra(EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE, msgEncryptionType);
    intent.putExtra(EXTRA_KEY_SERVICE_INFO, serviceInfo);
    return intent;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    AccountDao account = new AccountDaoSource().getActiveAccountInformation(this);
    if (account == null) {
      Toast.makeText(this, R.string.setup_app, Toast.LENGTH_LONG).show();
      finish();
    }

    if (getIntent() != null) {
      serviceInfo = getIntent().getParcelableExtra(EXTRA_KEY_SERVICE_INFO);
      if (getIntent().hasExtra(EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE)) {
        msgEncryptionType = (MessageEncryptionType) getIntent().getSerializableExtra(EXTRA_KEY_MESSAGE_ENCRYPTION_TYPE);
      }
    }

    super.onCreate(savedInstanceState);

    layoutContent = findViewById(R.id.layoutContent);
    initNonEncryptedHintView();

    if (getIntent() != null) {
      onMsgEncryptionTypeChanged(msgEncryptionType);
      prepareActionBarTitle();
    }
  }

  @Override
  public View getRootView() {
    return layoutContent;
  }

  @Override
  public int getContentViewResourceId() {
    return R.layout.activity_create_message;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.activity_send_message, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    MenuItem menuActionSwitchType = menu.findItem(R.id.menuActionSwitchType);
    int titleRes = msgEncryptionType == MessageEncryptionType.STANDARD ? R.string.switch_to_secure_email : R.string
        .switch_to_standard_email;
    menuActionSwitchType.setTitle(titleRes);

    if (serviceInfo != null) {
      if (!serviceInfo.isMsgTypeSwitchable()) {
        menu.removeItem(R.id.menuActionSwitchType);
      }

      if (!serviceInfo.hasAbilityToAddNewAtt()) {
        menu.removeItem(R.id.menuActionAttachFile);
      }
    }

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuActionHelp:
        startActivity(new Intent(this, FeedbackActivity.class));
        return true;

      case R.id.menuActionSwitchType:
        switch (msgEncryptionType) {
          case ENCRYPTED:
            onMsgEncryptionTypeChanged(MessageEncryptionType.STANDARD);
            break;

          case STANDARD:
            onMsgEncryptionTypeChanged(MessageEncryptionType.ENCRYPTED);
            break;
        }
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void sendMsg(OutgoingMessageInfo outgoingMsgInfo) {
    PrepareOutgoingMessagesJobIntentService.enqueueWork(this, outgoingMsgInfo);
    Toast.makeText(this, GeneralUtil.isConnected(this) ? R.string.sending :
        R.string.no_connection_message_will_be_sent_later, Toast.LENGTH_SHORT).show();
    finish();
  }

  @Override
  public void onMsgEncryptionTypeChanged(MessageEncryptionType messageEncryptionType) {
    this.msgEncryptionType = messageEncryptionType;
    switch (messageEncryptionType) {
      case ENCRYPTED:
        getAppBarLayout().setBackgroundColor(UIUtil.getColor(this, R.color.colorPrimary));
        getAppBarLayout().removeView(nonEncryptedHintView);
        break;

      case STANDARD:
        getAppBarLayout().setBackgroundColor(UIUtil.getColor(this, R.color.red));
        getAppBarLayout().addView(nonEncryptedHintView);
        break;
    }

    invalidateOptionsMenu();
    notifyFragmentAboutChangeMsgEncryptionType(messageEncryptionType);
  }

  @Override
  public MessageEncryptionType getMsgEncryptionType() {
    return msgEncryptionType;
  }

  private void prepareActionBarTitle() {
    if (getSupportActionBar() != null) {
      if (getIntent().hasExtra(CreateMessageActivity.EXTRA_KEY_MESSAGE_TYPE)) {
        MessageType msgType = (MessageType) getIntent().getSerializableExtra(CreateMessageActivity
            .EXTRA_KEY_MESSAGE_TYPE);

        switch (msgType) {
          case NEW:
            getSupportActionBar().setTitle(R.string.compose);
            break;

          case REPLY:
            getSupportActionBar().setTitle(R.string.reply);
            break;

          case REPLY_ALL:
            getSupportActionBar().setTitle(R.string.reply_all);
            break;

          case FORWARD:
            getSupportActionBar().setTitle(R.string.forward);
            break;
        }
      } else {
        if (getIntent().getParcelableExtra(CreateMessageActivity.EXTRA_KEY_INCOMING_MESSAGE_INFO) != null) {
          getSupportActionBar().setTitle(R.string.reply);
        } else {
          getSupportActionBar().setTitle(R.string.compose);
        }
      }
    }
  }

  private void notifyFragmentAboutChangeMsgEncryptionType(MessageEncryptionType
                                                                  messageEncryptionType) {
    CreateMessageFragment fragment = (CreateMessageFragment) getSupportFragmentManager().findFragmentById(R.id
        .composeFragment);

    if (fragment != null) {
      fragment.onMsgEncryptionTypeChange(messageEncryptionType);
    }
  }

  private void initNonEncryptedHintView() {
    nonEncryptedHintView = getLayoutInflater().inflate(R.layout.under_toolbar_line_with_text, getAppBarLayout(), false);
    TextView textView = nonEncryptedHintView.findViewById(R.id.underToolbarTextTextView);
    textView.setText(R.string.this_message_will_not_be_encrypted);
  }
}