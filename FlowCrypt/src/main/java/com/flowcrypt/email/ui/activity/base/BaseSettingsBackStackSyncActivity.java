/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.flowcrypt.email.R;
import com.flowcrypt.email.service.EmailSyncService;
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity;

/**
 * A base settings activity which uses back stack and {@link EmailSyncService}
 *
 * @author Denis Bondarenko
 * Date: 07.08.2018
 * Time: 15:15
 * E-mail: DenBond7@gmail.com
 */
public abstract class BaseSettingsBackStackSyncActivity extends BaseBackStackSyncActivity {

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_settings, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menuActionHelp:
        startActivity(new Intent(this, FeedbackActivity.class));
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }
}