/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.GrantPermissionRuleChooser
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * @author Denys Bondarenko
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeScreenReplyFlowTest : BaseTest() {
  private val addAccountToDatabaseRule = AddAccountToDatabaseRule().apply {
    execute()
  }
  private val msgInfo: IncomingMessageInfo?
    get() = getMsgInfo(
      path = "messages/info/standard_msg_reply_to_header.json",
      mimeMsgPath = "messages/mime/standard_msg_reply_to_header.txt",
      accountEntity = addAccountToDatabaseRule.accountEntityWithDecryptedInfo
    )

  override val activeActivityRule = activityScenarioRule<CreateMessageActivity>(
    Intent(
      getTargetContext(),
      CreateMessageActivity::class.java
    ).apply {
      putExtras(
        CreateMessageFragmentArgs(
          incomingMessageInfo = msgInfo,
          encryptedByDefault = false,
          messageType = MessageType.REPLY
        ).toBundle()
      )
    })

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(GrantPermissionRuleChooser.grant(android.Manifest.permission.POST_NOTIFICATIONS))
    .around(addAccountToDatabaseRule)
    .around(AddPrivateKeyToDatabaseRule())
    .around(activeActivityRule)
    .around(ScreenshotTestRule())

  @Test
  fun testReplyToHeader() {
    Thread.sleep(1000)
    onView(withId(R.id.recyclerViewChipsTo))
      .perform(
        scrollTo<RecyclerView.ViewHolder>(
          withText(msgInfo?.getReplyTo()?.first()?.address)
        )
      )
  }
}
