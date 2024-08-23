/*
 * SPDX-FileCopyrightText: 2024 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util;

import android.content.Context;
import android.util.Log;

import com.android.internal.R;

public class KeyProviderManager {

    private static IKeyboxProvider provider = null;
    private static String TAG = "KeyProviderManager";

    public static IKeyboxProvider getProvider() {
        if (provider == null) {
            DefaultKeyboxProvider newProvider = new DefaultKeyboxProvider();
            if (newProvider.hasKeybox)
                provider = newProvider;
        }
        return provider;
    }

    public static void setProvider(IKeyboxProvider newProvider) {
        provider = newProvider;
    }

    public static void resetProvider() {
        provider = null;
    }

    private static class DefaultKeyboxProvider implements IKeyboxProvider {

        private String mEcPrivateKey;
        private String mEcCert_1;
        private String mEcCert_2;
        private String mEcCert_3;

        private String mRsaPrivateKey;
        private String mRsaCert_1;
        private String mRsaCert_2;
        private String mRsaCert_3;

        public boolean hasKeybox = false;

        public DefaultKeyboxProvider() {
            Context context = android.app.AppGlobals.getInitialApplication().getApplicationContext();
            String[] keybox = context.getResources().getStringArray(R.array.config_keybox);

            for (String entry : keybox) {
                final String[] fieldAndProp = entry.split(":", 2);
                if (fieldAndProp.length != 2)
                    continue;
                if ("EC.PRIV".equals(fieldAndProp[0]))
                    mEcPrivateKey = fieldAndProp[1];
                else if ("EC.CERT_1".equals(fieldAndProp[0]))
                    mEcCert_1 = fieldAndProp[1];
                else if ("EC.CERT_2".equals(fieldAndProp[0]))
                    mEcCert_2 = fieldAndProp[1];
                else if ("EC.CERT_3".equals(fieldAndProp[0]))
                    mEcCert_3 = fieldAndProp[1];
                else if ("RSA.PRIV".equals(fieldAndProp[0]))
                    mRsaPrivateKey = fieldAndProp[1];
                else if ("RSA.CERT_1".equals(fieldAndProp[0]))
                    mRsaCert_1 = fieldAndProp[1];
                else if ("RSA.CERT_2".equals(fieldAndProp[0]))
                    mRsaCert_2 = fieldAndProp[1];
                else if ("RSA.CERT_3".equals(fieldAndProp[0]))
                    mRsaCert_3 = fieldAndProp[1];
            }

            if (areAllNonNull(mEcPrivateKey, mEcCert_1, mEcCert_2, mEcCert_3, mRsaPrivateKey, mRsaCert_1, mRsaCert_2, mRsaCert_3)) {
                hasKeybox = true;
            }
        }

        @Override
        public String getEcPrivateKey() {
            return mEcPrivateKey;
        }

        @Override
        public String getRsaPrivateKey() {
            return mRsaPrivateKey;
        }

        @Override
        public String[] getEcCertificateChain() {
            return new String[] { mEcCert_1, mEcCert_2, mEcCert_3 };
        }

        @Override
        public String[] getRsaCertificateChain() {
            return new String[] { mRsaCert_1, mRsaCert_2, mRsaCert_3};
        }
        
        private boolean areAllNonNull(Object... objects) {
            for (Object obj : objects)
                if (obj == null)
                    return false;
            return true;
        }
    }
}
