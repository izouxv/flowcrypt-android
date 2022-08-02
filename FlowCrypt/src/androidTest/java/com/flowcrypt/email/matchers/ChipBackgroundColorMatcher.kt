/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import com.google.android.material.chip.Chip
import org.hamcrest.Description

/**
 * @author Denis Bondarenko
 *         Date: 4/22/21
 *         Time: 10:19 AM
 *         E-mail: DenBond7@gmail.com
 */
class ChipBackgroundColorMatcher(
  private val color: Int
) : BoundedMatcher<View, Chip>(Chip::class.java) {
  public override fun matchesSafely(chip: Chip): Boolean {
    return color == chip.chipBackgroundColor?.defaultColor
  }

  override fun describeTo(description: Description) {
    description.appendText("Chip details: color = $color")
  }
}