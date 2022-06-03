/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.flowcrypt.email.R
import com.flowcrypt.email.TestConstants
import com.flowcrypt.email.matchers.CustomMatchers
import com.flowcrypt.email.rules.AddPrivateKeyToDatabaseRule
import com.flowcrypt.email.rules.ClearAppSettingsRule
import com.flowcrypt.email.rules.LazyActivityScenarioRule
import com.flowcrypt.email.rules.RetryRule
import com.flowcrypt.email.rules.ScreenshotTestRule
import com.flowcrypt.email.ui.activity.CreateMessageActivity
import com.flowcrypt.email.ui.base.BaseComposeScreenTest
import com.flowcrypt.email.ui.widget.CustomChipSpanChipCreator
import com.flowcrypt.email.util.TestGeneralUtil
import com.flowcrypt.email.util.UIUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * @author Denis Bondarenko
 *         Date: 5/11/22
 *         Time: 11:30 AM
 *         E-mail: DenBond7@gmail.com
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeScreenImportRecipientPubKeyFlowTest : BaseComposeScreenTest() {
  override val activeActivityRule: LazyActivityScenarioRule<CreateMessageActivity>? = null
  override val activityScenarioRule = activityScenarioRule<CreateMessageActivity>(intent = intent)
  override val activityScenario: ActivityScenario<*>?
    get() = activityScenarioRule.scenario

  private val addPrivateKeyToDatabaseRule =
    AddPrivateKeyToDatabaseRule("pgp/default@flowcrypt.test_secondKey_prv_strong_revoked.asc")

  private val publicKey = TestGeneralUtil.readFileFromAssetsAsString(
    "pgp/not_attested_user@flowcrypt.test-pub.asc"
  )

  @get:Rule
  var ruleChain: TestRule = RuleChain
    .outerRule(RetryRule.DEFAULT)
    .around(ClearAppSettingsRule())
    .around(addAccountToDatabaseRule)
    .around(addPrivateKeyToDatabaseRule)
    .around(activityScenarioRule)
    .around(ScreenshotTestRule())

  @Test
  fun testImportRecipientPubKeyFromFile() {
    fillDataAndMoveToImportPublicKeyScreen()

    val fileWithPublicKey: File = TestGeneralUtil.createFileAndFillWithContent(
      TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER + "_pub.asc",
      publicKey
    )

    val resultData = TestGeneralUtil.genIntentWithPersistedReadPermissionForFile(fileWithPublicKey)
    intendingActivityResultContractsGetContent(resultData = resultData)

    onView(withId(R.id.buttonLoadFromFile))
      .check(matches(isDisplayed()))
      .perform(click())

    checkThatRecipientHasPublicKey()

    TestGeneralUtil.deleteFiles(listOf(fileWithPublicKey))
  }

  @Test
  fun testImportRecipientPubKeyFromClipboard() {
    fillDataAndMoveToImportPublicKeyScreen()
    addTextToClipboard("public key", publicKey)

    onView(withId(R.id.buttonLoadFromClipboard))
      .check(matches(isDisplayed()))
      .perform(click())

    checkThatRecipientHasPublicKey()
  }

  private fun fillDataAndMoveToImportPublicKeyScreen() {
    fillInAllFields(TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER)

    onView(withId(R.id.menuActionSend))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withText(R.string.import_their_public_key))
      .check(matches(isDisplayed()))
      .perform(click())

    onView(withText(R.string.import_public_key))
      .check(matches(isDisplayed()))
  }

  private fun checkThatRecipientHasPublicKey() {
    onView(withId(R.id.editTextRecipientTo))
      .check(
        matches(
          CustomMatchers.withChipsBackgroundColor(
            chipText = TestConstants.RECIPIENT_WITHOUT_PUBLIC_KEY_ON_ATTESTER,
            backgroundColor = UIUtil.getColor(
              context = getTargetContext(),
              colorResourcesId = CustomChipSpanChipCreator.CHIP_COLOR_RES_ID_HAS_USABLE_PUB_KEY
            )
          )
        )
      )
  }
}
