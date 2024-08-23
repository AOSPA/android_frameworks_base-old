/*
 * SPDX-FileCopyrightText: 2024 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.util;

public interface IKeyboxProvider {

    boolean hasKeybox();

    String getEcPrivateKey();

    String getRsaPrivateKey();

    String[] getEcCertificateChain();

    String[] getRsaCertificateChain();
}
