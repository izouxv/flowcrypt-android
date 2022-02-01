/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.core.graphics.drawable.toBitmap
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

/**
 * @author Denis Bondarenko
 *         Date: 1/14/22
 *         Time: 11:55 AM
 *         E-mail: DenBond7@gmail.com
 */
class TextViewDrawableMatcher(
  @DrawableRes private val resourceId: Int,
  @DrawablePosition private val drawablePosition: Int
) : TypeSafeMatcher<View>() {
  override fun describeTo(description: Description) {
    description.appendText(
      "TextView with compound drawable at position $drawablePosition" +
          " same as drawable with id $resourceId"
    )
  }

  override fun matchesSafely(view: View): Boolean {
    if (view !is TextView) {
      return false
    }

    val expectedBitmap = view.context.getDrawable(resourceId)?.toBitmap()
    return view.compoundDrawables[drawablePosition].toBitmap().sameAs(expectedBitmap)
  }

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(
    DrawablePosition.LEFT,
    DrawablePosition.TOP,
    DrawablePosition.RIGHT,
    DrawablePosition.BOTTOM
  )
  annotation class DrawablePosition {
    companion object {
      const val LEFT = 0
      const val TOP = 1
      const val RIGHT = 2
      const val BOTTOM = 3
    }
  }
}