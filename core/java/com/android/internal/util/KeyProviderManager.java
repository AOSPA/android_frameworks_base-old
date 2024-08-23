/*
 * SPDX-FileCopyrightText: 2024 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util;

import android.content.Context;
import android.util.Log;

import com.android.internal.R;

public class KeyProviderManager {

    private static final String TAG = "KeyProviderManager";

    private static IKeyboxProvider provider = null;

    public static IKeyboxProvider getProvider() {
        if (provider == null) {
            provider = new DefaultKeyboxProvider();
        }
        return provider;
    }

    public static void setProvider(IKeyboxProvider newProvider) {
        provider = newProvider;
    }

    public static void resetProvider() {
        provider = null;
    }

    public static boolean isKeyboxAvailable() {
        return getProvider().hasKeybox();
    }

    private static class DefaultKeyboxProvider implements IKeyboxProvider {

        private String ecPrivateKey;
        private String ecCert_1;
        private String ecCert_2;
        private String ecCert_3;

        private String rsaPrivateKey;
        private String rsaCert_1;
        private String rsaCert_2;
        private String rsaCert_3;

        public DefaultKeyboxProvider() {
            Context context = android.app.AppGlobals.getInitialApplication().getApplicationContext();
            String[] keybox = context.getResources().getStringArray(R.array.config_certifiedKeybox);

            for (String entry : keybox) {
                final String[] fieldAndProp = entry.split(":", 2);
                if (fieldAndProp.length != 2)
                    continue;
                if ("EC.PRIV".equals(fieldAndProp[0]))
                    ecPrivateKey = fieldAndProp[1];
                else if ("EC.CERT_1".equals(fieldAndProp[0]))
                    ecCert_1 = fieldAndProp[1];
                else if ("EC.CERT_2".equals(fieldAndProp[0]))
                    ecCert_2 = fieldAndProp[1];
                else if ("EC.CERT_3".equals(fieldAndProp[0]))
                    ecCert_3 = fieldAndProp[1];
                else if ("RSA.PRIV".equals(fieldAndProp[0]))
                    rsaPrivateKey = fieldAndProp[1];
                else if ("RSA.CERT_1".equals(fieldAndProp[0]))
                    rsaCert_1 = fieldAndProp[1];
                else if ("RSA.CERT_2".equals(fieldAndProp[0]))
                    rsaCert_2 = fieldAndProp[1];
                else if ("RSA.CERT_3".equals(fieldAndProp[0]))
                    rsaCert_3 = fieldAndProp[1];
            }
        }

        @Override
        public boolean hasKeybox() {
            return areAllNonNull(ecPrivateKey, ecCert_1, ecCert_2, ecCert_3, rsaPrivateKey, rsaCert_1, rsaCert_2, rsaCert_3);
        }

        @Override
        public String getEcPrivateKey() {
            return ecPrivateKey;
        }

        @Override
        public String getRsaPrivateKey() {
            return rsaPrivateKey;
        }

        @Override
        public String[] getEcCertificateChain() {
            return new String[] { ecCert_1, ecCert_2, ecCert_3 };
        }

        @Override
        public String[] getRsaCertificateChain() {
            return new String[] { rsaCert_1, rsaCert_2, rsaCert_3};
        }

        private boolean areAllNonNull(Object... objects) {
            for (Object obj : objects)
                if (obj == null)
                    return false;
            return true;
        }
    }
}
