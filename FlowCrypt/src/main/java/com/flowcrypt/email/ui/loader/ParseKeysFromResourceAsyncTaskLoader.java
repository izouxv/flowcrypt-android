/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.KeyImportModel;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.util.ArrayList;

import androidx.loader.content.AsyncTaskLoader;

/**
 * This loader parses keys from the given resource (string or file).
 *
 * @author Denis Bondarenko
 * Date: 13.08.2018
 * Time: 13:00
 * E-mail: DenBond7@gmail.com
 */

public class ParseKeysFromResourceAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
  /**
   * Max size of a key is 256k.
   */
  private static final int MAX_SIZE_IN_BYTES = 256 * 1024;
  private KeyImportModel keyImportModel;
  private boolean isCheckSizeEnabled;

  public ParseKeysFromResourceAsyncTaskLoader(Context context, KeyImportModel model, boolean isCheckSizeEnabled) {
    super(context);
    this.keyImportModel = model;
    this.isCheckSizeEnabled = isCheckSizeEnabled;
    onContentChanged();
  }

  @Override
  public void onStartLoading() {
    if (takeContentChanged()) {
      forceLoad();
    }
  }

  @Override
  public LoaderResult loadInBackground() {
    ArrayList<KeyDetails> keyDetailsList = new ArrayList<>();
    try {
      if (keyImportModel != null) {
        String armoredKey = null;
        switch (keyImportModel.getType()) {
          case FILE:
            if (isCheckSizeEnabled && isKeyTooBig(keyImportModel.getFileUri())) {
              return new LoaderResult(null, new IllegalArgumentException("The file is too big"));
            }

            if (keyImportModel.getFileUri() == null) {
              throw new NullPointerException("Uri is null!");
            }

            armoredKey = GeneralUtil.readFileFromUriToString(getContext(), keyImportModel.getFileUri());
            break;

          case CLIPBOARD:
          case EMAIL:
            armoredKey = keyImportModel.getKeyString();
            break;
        }

        if (!TextUtils.isEmpty(armoredKey)) {
          Js js = new Js(getContext(), null);

          MessageBlock[] messageBlocks = js.crypto_armor_detect_blocks(armoredKey);

          for (MessageBlock messageBlock : messageBlocks) {
            keyDetailsList.addAll(parseKeys(js, messageBlock));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
      return new LoaderResult(null, e);
    }

    return new LoaderResult(keyDetailsList, null);
  }

  @Override
  public void onStopLoading() {
    cancelLoad();
  }

  private ArrayList<KeyDetails> parseKeys(Js js, MessageBlock messageBlock) {
    ArrayList<KeyDetails> keyDetailsList = new ArrayList<>();

    if (keyImportModel.isPrivateKey()) {
      if (MessageBlock.TYPE_PGP_PRIVATE_KEY.equals(messageBlock.getType())) {
        String normalizedKey = js.crypto_key_normalize(messageBlock.getContent());
        PgpKey pgpKey = js.crypto_key_read(normalizedKey);
        boolean isExist = EmailUtil.containsKey(keyDetailsList, normalizedKey);
        if (js.is_valid_key(normalizedKey, true) && !isExist) {
          KeyDetails keyDetails = new KeyDetails(normalizedKey, keyImportModel.getType());
          keyDetails.setPgpContact(pgpKey.getPrimaryUserId());
          keyDetailsList.add(keyDetails);
        }
      }
    } else {
      if (MessageBlock.TYPE_PGP_PUBLIC_KEY.equals(messageBlock.getType())) {
        String normalizedKey = js.crypto_key_normalize(messageBlock.getContent());
        PgpKey pgpKey = js.crypto_key_read(normalizedKey);
        boolean isExist = EmailUtil.containsKey(keyDetailsList, normalizedKey);
        if (js.is_valid_key(normalizedKey, false) && !isExist) {
          KeyDetails keyDetails = new KeyDetails(null, normalizedKey, keyImportModel.getType(), false);
          keyDetails.setPgpContact(pgpKey.getPrimaryUserId());
          keyDetailsList.add(keyDetails);
        }
      }
    }

    return keyDetailsList;
  }

  /**
   * Check that the key size not bigger then {@link #MAX_SIZE_IN_BYTES}.
   *
   * @param fileUri The {@link Uri} of the selected file.
   * @return true if the key size not bigger then {@link #MAX_SIZE_IN_BYTES}, otherwise false
   */
  private boolean isKeyTooBig(Uri fileUri) {
    long fileSize = GeneralUtil.getFileSizeFromUri(getContext(), fileUri);
    return fileSize > MAX_SIZE_IN_BYTES;
  }
}
