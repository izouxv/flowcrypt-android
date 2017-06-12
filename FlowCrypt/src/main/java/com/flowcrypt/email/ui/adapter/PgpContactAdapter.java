package com.flowcrypt.email.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.flowcrypt.email.R;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.test.PgpContact;
import com.hootsuite.nachos.NachoTextView;

/**
 * This class describe a logic of create and show {@link PgpContact} objects in the
 * {@link NachoTextView}.
 *
 * @author DenBond7
 *         Date: 17.05.2017
 *         Time: 17:44
 *         E-mail: DenBond7@gmail.com
 */

public class PgpContactAdapter extends CursorAdapter {

    public PgpContactAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.pgp_contact_item, parent, false);
    }

    @Override
    public CharSequence convertToString(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(ContactsDaoSource.COL_EMAIL));
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView textViewName = (TextView) view.findViewById(R.id.textViewName);
        TextView textViewEmail = (TextView) view.findViewById(R.id.textViewEmail);
        TextView textViewOnlyEmail = (TextView) view.findViewById(R.id.textViewOnlyEmail);

        String name = cursor.getString(cursor.getColumnIndex(ContactsDaoSource.COL_NAME));
        String email = cursor.getString(cursor.getColumnIndex(ContactsDaoSource.COL_EMAIL));

        if (TextUtils.isEmpty(name)) {
            textViewName.setVisibility(View.GONE);
            textViewEmail.setVisibility(View.GONE);
            textViewOnlyEmail.setVisibility(View.VISIBLE);
            textViewOnlyEmail.setText(email);
        } else {
            textViewName.setVisibility(View.VISIBLE);
            textViewEmail.setVisibility(View.VISIBLE);
            textViewOnlyEmail.setVisibility(View.GONE);

            textViewEmail.setText(email);
            textViewName.setText(name);
        }
    }
}