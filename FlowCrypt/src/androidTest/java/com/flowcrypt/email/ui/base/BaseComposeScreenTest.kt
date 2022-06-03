/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.flowcrypt.email.R
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.rules.AddAccountToDatabaseRule
import com.flowcrypt.email.rules.LazyActivityScenarioRule
import com.flowcrypt.email.rules.lazyActivityScenarioRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.activity.fragment.CreateMessageFragmentArgs

/**
 * @author Denis Bondarenko
 *         Date: 6/11/21
 *         Time: 4:28 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseComposeScreenTest : BaseTest() {
  override val useIntents: Boolean = true
  override val activeActivityRule: LazyActivityScenarioRule<CreateMessageActivity>? =
    lazyActivityScenarioRule(launchActivity = false)
  override val activityScenario: ActivityScenario<*>?
    get() = activeActivityRule?.scenario

  protected open val addAccountToDatabaseRule = AddAccountToDatabaseRule()

  protected val intent: Intent =
    Intent(getTargetContext(), CreateMessageActivity::class.java).apply {
      putExtras(
        CreateMessageFragmentArgs(
          encryptedByDefault = true,
          messageType = MessageType.NEW
        ).toBundle()
      )
    }

  protected fun fillInAllFields(recipient: String) {
    onView(withId(R.id.layoutTo))
      .perform(scrollTo())
    onView(withId(R.id.editTextRecipientTo))
      .perform(typeText(recipient), closeSoftKeyboard())
    //need to leave focus from 'To' field. move the focus to the next view
    onView(withId(R.id.editTextEmailSubject))
      .perform(
        scrollTo(),
        click(),
        typeText("subject"),
        closeSoftKeyboard()
      )
    onView(withId(R.id.editTextEmailMessage))
      .perform(
        scrollTo(),
        typeText("message"),
        closeSoftKeyboard()
      )
  }
}