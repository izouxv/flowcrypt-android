/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.flowcrypt.email.database.dao.KeysDao;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static android.support.test.InstrumentationRegistry.getContext;

/**
 * @author Denis Bondarenko
 *         Date: 18.01.2018
 *         Time: 13:02
 *         E-mail: DenBond7@gmail.com
 */

public class TestGeneralUtil {

    public static <T> T readObjectFromResources(String path, Class<T> aClass) {
        try {
            return new Gson().fromJson(
                    IOUtils.toString(aClass.getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8),
                    aClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String readFileFromAssetsAsString(Context context, String filePath) throws IOException {
        return IOUtils.toString(context.getAssets().open(filePath), "UTF-8");
    }

    public static void saveKeyToDatabase(String privetKey, KeyDetails.Type type) throws Exception {
        KeysDaoSource keysDaoSource = new KeysDaoSource();
        KeyDetails keyDetails = new KeyDetails(privetKey, type);
        KeyStoreCryptoManager keyStoreCryptoManager = new KeyStoreCryptoManager(InstrumentationRegistry
                .getTargetContext());
        String armoredPrivateKey = null;

        switch (keyDetails.getBornType()) {
            case FILE:
                armoredPrivateKey = GeneralUtil.readFileFromUriToString(InstrumentationRegistry.getTargetContext(),
                        keyDetails.getUri());
                break;
            case EMAIL:
            case CLIPBOARD:
                armoredPrivateKey = keyDetails.getValue();
                break;
        }
        Js js = new Js(InstrumentationRegistry.getTargetContext(), null);
        String normalizedArmoredKey = js.crypto_key_normalize(armoredPrivateKey);

        PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);
        keysDaoSource.addRow(getContext(),
                KeysDao.generateKeysDao(keyStoreCryptoManager, keyDetails, pgpKey, "android"));
    }

}
