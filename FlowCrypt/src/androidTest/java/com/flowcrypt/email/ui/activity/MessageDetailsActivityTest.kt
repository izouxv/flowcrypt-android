/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.text.format.DateFormat
import android.text.format.Formatter
import android.view.View
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.GeneralMessageDetails
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.model.node.DecryptErrorMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.PublicKeyMsgBlock
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.matchers.CustomMatchers.Companion.isToastDisplayed
import com.flowcrypt.email.matchers.CustomMatchers.Companion.withDrawable
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddAttachmentToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.PrivateKeysManager
import com.flowcrypt.email.util.TestGeneralUtil
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

/**
 * @author Denis Bondarenko
 * Date: 3/13/19
 * Time: 4:32 PM
 * E-mail: DenBond7@gmail.com
 */
class MessageDetailsActivityTest : BaseTest() {
  override val activityTestRule: ActivityTestRule<*>? = IntentsTestRule(MessageDetailsActivity::class.java,
      false, false)
  private val simpleAttachmentRule =
      AddAttachmentToDatabaseRule(TestGeneralUtil.getObjectFromJson("messages/attachments/simple_att.json",
          AttachmentInfo::class.java)!!)

  private val encryptedAttachmentRule =
      AddAttachmentToDatabaseRule(TestGeneralUtil.getObjectFromJson("messages/attachments/encrypted_att.json",
          AttachmentInfo::class.java)!!)

  private val pubKeyAttachmentRule =
      AddAttachmentToDatabaseRule(TestGeneralUtil.getObjectFromJson("messages/attachments/pub_key.json",
          AttachmentInfo::class.java)!!)

  @get:Rule
  var ruleChain: TestRule = RuleChain
      .outerRule(ClearAppSettingsRule())
      .around(AddAccountToDatabaseRule())
      .around(AddPrivateKeyToDatabaseRule())
      .around(simpleAttachmentRule)
      .around(encryptedAttachmentRule)
      .around(pubKeyAttachmentRule)
      .around(activityTestRule)

  private val dateFormat: java.text.DateFormat = DateFormat.getTimeFormat(getTargetContext())
  private val localFolder: LocalFolder = LocalFolder(
      fullName = "INBOX",
      folderAlias = "INBOX",
      msgCount = 1,
      attributes = listOf("\\HasNoChildren"))

  @After
  fun unregisterDecryptionIdling() {
    val activity = activityTestRule?.activity ?: return
    if (activity is MessageDetailsActivity) {
      IdlingRegistry.getInstance().unregister(activity.idlingForDecryption)
    }
  }

  @Test
  fun testReplyButton() {
    testStandardMsgPlaneText()
    onView(withId(R.id.layoutReplyButton))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  fun testReplyAllButton() {
    testStandardMsgPlaneText()
    onView(withId(R.id.layoutReplyAllButton))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  fun testFwdButton() {
    testStandardMsgPlaneText()
    onView(withId(R.id.layoutFwdButton))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  fun testStandardMsgPlaneText() {
    val incomingMsgInfo = TestGeneralUtil.getObjectFromJson("messages/info/standard_msg_info_plane_text.json",
        IncomingMessageInfo::class.java)
    baseCheck(incomingMsgInfo)
  }

  @Test
  fun testStandardMsgPlaneTextWithOneAttachment() {
    val incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/standard_msg_info_plane_text_with_one_att.json",
        IncomingMessageInfo::class.java)
    baseCheckWithAtt(incomingMsgInfo, simpleAttachmentRule)
  }

  @Test
  fun testEncryptedMsgPlaneText() {
    val incomingMsgInfo = TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text.json",
        IncomingMessageInfo::class.java)
    baseCheck(incomingMsgInfo)
  }

  @Test
  fun testMissingKeyErrorImportKey() {
    val incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_missing_key.json",
        IncomingMessageInfo::class.java)

    testMissingKey(incomingMsgInfo)

    intending(hasComponent(ComponentName(getTargetContext(), ImportPrivateKeyActivity::class.java)))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

    PrivateKeysManager.saveKeyFromAssetsToDatabase("node/default@denbond7.com_secondKey_prv_strong.json",
        TestConstants.DEFAULT_STRONG_PASSWORD, KeyDetails.Type.EMAIL)

    onView(withId(R.id.buttonImportPrivateKey))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())

    val incomingMsgInfoFixed =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_missing_key_fixed.json",
        IncomingMessageInfo::class.java)
    onView(withText(incomingMsgInfoFixed?.msgBlocks?.get(0)?.content))
        .check(matches(isDisplayed()))

    PrivateKeysManager.deleteKey("node/default@denbond7.com_secondKey_prv_strong.json")
  }

  @Test
  fun testMissingPubKey() {
    val incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_error_one_pub_key.json",
        IncomingMessageInfo::class.java)

    testMissingKey(incomingMsgInfo)
  }

  @Test
  fun testBadlyFormattedMsg() {
    val incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_error_badly_formatted.json",
        IncomingMessageInfo::class.java)

    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.generalMsgDetails

    launchActivity(details)
    matchHeader(details)

    val block = incomingMsgInfo.msgBlocks?.get(0) as DecryptErrorMsgBlock
    val decryptError = block.error
    val formatErrorMsg = (getResString(R.string.decrypt_error_message_badly_formatted,
        getResString(R.string.app_name)) + "\n\n" + decryptError?.details?.type + ":" + decryptError?.details?.message)

    onView(withId(R.id.textViewErrorMessage))
        .check(matches(withText(formatErrorMsg)))

    testSwitch(block.content ?: "")
    matchReplyButtons(details)
  }

  @Test
  fun testMissingKeyErrorChooseSinglePubKey() {
    val incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_missing_key.json",
        IncomingMessageInfo::class.java)

    testMissingKey(incomingMsgInfo)

    onView(withId(R.id.buttonSendOwnPublicKey))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewMessage)).check(
        matches(withText(getResString(R.string.tell_sender_to_update_their_settings))))
    onView(withId(R.id.buttonOk))
        .check(matches(isDisplayed()))
        .perform(click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  fun testMissingKeyErrorChooseFromFewPubKeys() {
    val incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_missing_key.json",
        IncomingMessageInfo::class.java)

    testMissingKey(incomingMsgInfo)

    PrivateKeysManager.saveKeyFromAssetsToDatabase("node/default@denbond7.com_secondKey_prv_strong.json",
        TestConstants.DEFAULT_STRONG_PASSWORD, KeyDetails.Type.EMAIL)
    onView(withId(R.id.buttonSendOwnPublicKey))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())

    val msg = (getResString(R.string.tell_sender_to_update_their_settings) + "\n\n" + getResString(R.string.select_key))

    onView(withId(R.id.textViewMessage))
        .check(matches(withText(msg)))
    onView(withId(R.id.buttonOk))
        .check(matches(isDisplayed()))
        .perform(click())
    onView(withText(getResString(R.string.please_select_key)))
        .inRoot(isToastDisplayed())
        .check(matches(isDisplayed()))
    onData(anything())
        .inAdapterView(withId(R.id.listViewKeys))
        .atPosition(1)
        .perform(click())
    onView(withId(R.id.buttonOk))
        .check(matches(isDisplayed()))
        .perform(click())
    intended(hasComponent(CreateMessageActivity::class.java.name))
  }

  @Test
  fun testEncryptedMsgPlaneTextWithOneAttachment() {
    val incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_one_att.json",
        IncomingMessageInfo::class.java)

    baseCheckWithAtt(incomingMsgInfo, encryptedAttachmentRule)
  }

  @Test
  fun testEncryptedMsgPlaneTextWithPubKey() {
    val incomingMsgInfo =
        TestGeneralUtil.getObjectFromJson("messages/info/encrypted_msg_info_plane_text_with_pub_key.json",
        IncomingMessageInfo::class.java)

    baseCheckWithAtt(incomingMsgInfo, pubKeyAttachmentRule)

    val nodeKeyDetails = PrivateKeysManager.getNodeKeyDetailsFromAssets("node/denbond7@denbond7.com_pub.json")
    val pgpContact = nodeKeyDetails.primaryPgpContact

    onView(withId(R.id.textViewKeyOwnerTemplate)).check(matches(withText(
        getResString(R.string.template_message_part_public_key_owner, pgpContact.email))))

    onView(withId(R.id.textViewKeyWordsTemplate)).check(matches(withText(
        getHtmlString(getResString(R.string.template_message_part_public_key_key_words,
            nodeKeyDetails.keywords ?: "")))))

    onView(withId(R.id.textViewFingerprintTemplate)).check(matches(withText(
        getHtmlString(getResString(R.string.template_message_part_public_key_fingerprint,
            GeneralUtil.doSectionsInText(" ", nodeKeyDetails.fingerprint, 4)!!)))))

    val block = incomingMsgInfo?.msgBlocks?.get(1) as PublicKeyMsgBlock

    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(not<View>(isDisplayed())))
    onView(withId(R.id.switchShowPublicKey))
        .check(matches(not<View>(isChecked())))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(isDisplayed()))
    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(withText(block.content)))
    onView(withId(R.id.switchShowPublicKey))
        .check(matches(isChecked()))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewPgpPublicKey))
        .check(matches(not<View>(isDisplayed())))

    onView(withId(R.id.buttonKeyAction))
        .check(matches(isDisplayed()))
        .perform(scrollTo(), click())
    onView(withId(R.id.buttonKeyAction))
        .check(matches(not<View>(isDisplayed())))
  }

  private fun testMissingKey(incomingMsgInfo: IncomingMessageInfo?) {
    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.generalMsgDetails

    launchActivity(details)
    matchHeader(details)

    val block = incomingMsgInfo.msgBlocks?.get(0) as DecryptErrorMsgBlock
    val errorMsg = getResString(R.string.decrypt_error_current_key_cannot_open_message)

    onView(withId(R.id.textViewErrorMessage))
        .check(matches(withText(errorMsg)))

    testSwitch(block.content ?: "")
    matchReplyButtons(details)
  }

  private fun testSwitch(content: String) {
    onView(withId(R.id.textViewOrigPgpMsg))
        .check(matches(not<View>(isDisplayed())))
    onView(withId(R.id.switchShowOrigMsg))
        .check(matches(not<View>(isChecked())))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewOrigPgpMsg))
        .check(matches(isDisplayed()))
    onView(withId(R.id.textViewOrigPgpMsg))
        .check(matches(withText(content)))
    onView(withId(R.id.switchShowOrigMsg))
        .check(matches(isChecked()))
        .perform(scrollTo(), click())
    onView(withId(R.id.textViewOrigPgpMsg))
        .check(matches(not<View>(isDisplayed())))
  }

  private fun baseCheck(incomingMsgInfo: IncomingMessageInfo?) {
    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.generalMsgDetails
    launchActivity(details)
    matchHeader(details)
    onView(withText(incomingMsgInfo.msgBlocks?.get(0)?.content))
        .check(matches(isDisplayed()))
    matchReplyButtons(details)
  }

  private fun baseCheckWithAtt(incomingMsgInfo: IncomingMessageInfo?, rule: AddAttachmentToDatabaseRule) {
    assertThat(incomingMsgInfo, notNullValue())

    val details = incomingMsgInfo!!.generalMsgDetails
    launchActivity(details)
    matchHeader(details)
    onView(withText(incomingMsgInfo.msgBlocks?.get(0)?.content))
        .check(matches(isDisplayed()))
    onView(withId(R.id.layoutAtt))
        .check(matches(isDisplayed()))
    matchAtt(rule.attInfo)
    matchReplyButtons(details)
  }

  private fun matchHeader(details: GeneralMessageDetails) {
    onView(withId(R.id.textViewSenderAddress))
        .check(matches(withText(EmailUtil.getFirstAddressString(details.from))))
    onView(withId(R.id.textViewDate))
        .check(matches(withText(dateFormat.format(details.receivedDate))))
    onView(withId(R.id.textViewSubject))
        .check(matches(withText(details.subject)))
  }

  private fun matchAtt(att: AttachmentInfo) {
    onView(withId(R.id.textViewAttchmentName))
        .check(matches(withText(att.name)))
    onView(withId(R.id.textViewAttSize))
        .check(matches(withText(Formatter.formatFileSize(getContext(), att.encodedSize))))
  }

  private fun matchReplyButtons(details: GeneralMessageDetails) {
    onView(withId(R.id.imageButtonReplyAll))
        .check(matches(isDisplayed()))
    onView(withId(R.id.layoutReplyButton))
        .check(matches(isDisplayed()))
    onView(withId(R.id.layoutReplyAllButton))
        .check(matches(isDisplayed()))
    onView(withId(R.id.layoutFwdButton))
        .check(matches(isDisplayed()))

    if (details.isEncrypted) {
      onView(withId(R.id.textViewReply))
          .check(matches(withText(getResString(R.string.reply_encrypted))))
      onView(withId(R.id.textViewReplyAll))
          .check(matches(withText(getResString(R.string.reply_all_encrypted))))
      onView(withId(R.id.textViewFwd))
          .check(matches(withText(getResString(R.string.forward_encrypted))))

      onView(withId(R.id.imageViewReply))
          .check(matches(withDrawable(R.mipmap.ic_reply_green)))
      onView(withId(R.id.imageViewReplyAll))
          .check(matches(withDrawable(R.mipmap.ic_reply_all_green)))
      onView(withId(R.id.imageViewFwd))
          .check(matches(withDrawable(R.mipmap.ic_forward_green)))
    } else {
      onView(withId(R.id.textViewReply))
          .check(matches(withText(getResString(R.string.reply))))
      onView(withId(R.id.textViewReplyAll))
          .check(matches(withText(getResString(R.string.reply_all))))
      onView(withId(R.id.textViewFwd))
          .check(matches(withText(getResString(R.string.forward))))

      onView(withId(R.id.imageViewReply))
          .check(matches(withDrawable(R.mipmap.ic_reply_red)))
      onView(withId(R.id.imageViewReplyAll))
          .check(matches(withDrawable(R.mipmap.ic_reply_all_red)))
      onView(withId(R.id.imageViewFwd))
          .check(matches(withDrawable(R.mipmap.ic_forward_red)))
    }
  }

  private fun launchActivity(details: GeneralMessageDetails) {
    activityTestRule?.launchActivity(MessageDetailsActivity.getIntent(getTargetContext(), localFolder, details))
    IdlingRegistry.getInstance().register((activityTestRule?.activity as BaseActivity).nodeIdlingResource)
    IdlingRegistry.getInstance().register((activityTestRule.activity as MessageDetailsActivity).idlingForDecryption)
  }
}