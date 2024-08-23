/*
 * SPDX-FileCopyrightText: 2024 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util;

public class KeyProviderManager {

    private static IKeyboxProvider provider = null;

    public static IKeyboxProvider getProvider() {
        if (provider == null) {
            return new DefaultKeyboxProvider();
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
        @Override
        public String getEcPrivateKey() {
            return "";
        }

        @Override
        public String getRsaPrivateKey() {
            return "";
        }

        @Override
        public String[] getEcCertificateChain() {
            return new String[]{"", "", ""};
        }

        @Override
        public String[] getRsaCertificateChain() {
            return new String[]{"", "", ""};
        }
    }
}
