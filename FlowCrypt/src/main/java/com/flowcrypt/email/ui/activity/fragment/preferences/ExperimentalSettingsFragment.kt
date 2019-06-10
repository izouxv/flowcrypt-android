/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.preferences

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.DevSettingsActivity
import com.flowcrypt.email.ui.activity.fragment.base.BasePreferenceFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author DenBond7
 * Date: 29.09.2017.
 * Time: 22:46.
 * E-mail: DenBond7@gmail.com
 */
class ExperimentalSettingsFragment : BasePreferenceFragment() {
  override fun onCreatePreferences(bundle: Bundle?, s: String?) {
    addPreferencesFromResource(R.xml.preferences_experimental_settings)

    if (GeneralUtil.isDebugBuild()) {
      val preference = Preference(context!!)
      preference.setTitle(R.string.action_dev_settings)
      preference.isIconSpaceReserved = false
      preference.intent = Intent(context, DevSettingsActivity::class.java)

      preferenceScreen.addPreference(preference)
    }
  }
}
