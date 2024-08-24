/*
 * Copyright (C) 2022 Paranoid Android
 *           (C) 2023 ArrowOS
 *           (C) 2023 The LibreMobileOS Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.app.ActivityTaskManager;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Binder;
import android.os.Process;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

import android.security.keystore.KeyProperties;
import android.system.keystore2.KeyEntryResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;
import com.android.internal.org.bouncycastle.asn1.ASN1Boolean;
import com.android.internal.org.bouncycastle.asn1.ASN1Encodable;
import com.android.internal.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.internal.org.bouncycastle.asn1.ASN1Enumerated;
import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.ASN1OctetString;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.internal.org.bouncycastle.asn1.DEROctetString;
import com.android.internal.org.bouncycastle.asn1.DERSequence;
import com.android.internal.org.bouncycastle.asn1.DERTaggedObject;
import com.android.internal.org.bouncycastle.asn1.x509.Extension;
import com.android.internal.org.bouncycastle.cert.X509CertificateHolder;
import com.android.internal.org.bouncycastle.cert.X509v3CertificateBuilder;
import com.android.internal.org.bouncycastle.operator.ContentSigner;
import com.android.internal.org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class PropImitationHooks {

    private static final String TAG = "PropImitationHooks";
    private static final boolean DEBUG = false;

    private static final String PACKAGE_ARCORE = "com.google.ar.core";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_GMS_UNSTABLE = PACKAGE_GMS + ".unstable";
    private static final String PACKAGE_NETFLIX = "com.netflix.mediaclient";
    private static final String PACKAGE_GPHOTOS = "com.google.android.apps.photos";

    private static final String PROP_SECURITY_PATCH = "persist.sys.pihooks.security_patch";
    private static final String PROP_FIRST_API_LEVEL = "persist.sys.pihooks.first_api_level";

    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity");

    private static final Set<String> sPixelFeatures = Set.of(
        "PIXEL_2017_PRELOAD",
        "PIXEL_2018_PRELOAD",
        "PIXEL_2019_MIDYEAR_PRELOAD",
        "PIXEL_2019_PRELOAD",
        "PIXEL_2020_EXPERIENCE",
        "PIXEL_2020_MIDYEAR_EXPERIENCE",
        "PIXEL_EXPERIENCE"
    );

    private static volatile String[] sCertifiedProps;
    private static volatile String sStockFp, sNetflixModel;

    private static volatile String sProcessName;
    private static volatile boolean sIsPixelDevice, sIsGms, sIsFinsky, sIsPhotos;

    private static final PrivateKey EC, RSA;
    private static final byte[] EC_CERTS;
    private static final byte[] RSA_CERTS;
    private static final ASN1ObjectIdentifier OID = new ASN1ObjectIdentifier("1.3.6.1.4.1.11129.2.1.17");
    private static final CertificateFactory certificateFactory;
    private static final X509CertificateHolder EC_holder, RSA_holder;
    private static volatile String algo;

    static {
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");

            EC = parsePrivateKey(Keybox.EC.PRIVATE_KEY, KeyProperties.KEY_ALGORITHM_EC);
            RSA = parsePrivateKey(Keybox.RSA.PRIVATE_KEY, KeyProperties.KEY_ALGORITHM_RSA);

            byte[] EC_cert1 = parseCert(Keybox.EC.CERTIFICATE_1);
            byte[] RSA_cert1 = parseCert(Keybox.RSA.CERTIFICATE_1);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            stream.write(EC_cert1);
            stream.write(parseCert(Keybox.EC.CERTIFICATE_2));
            stream.write(parseCert(Keybox.EC.CERTIFICATE_3));

            EC_CERTS = stream.toByteArray();

            stream.reset();

            stream.write(RSA_cert1);
            stream.write(parseCert(Keybox.RSA.CERTIFICATE_2));
            stream.write(parseCert(Keybox.RSA.CERTIFICATE_3));

            RSA_CERTS = stream.toByteArray();

            stream.close();

            EC_holder = new X509CertificateHolder(EC_cert1);
            RSA_holder = new X509CertificateHolder(RSA_cert1);

        } catch (Throwable t) {
            if (DEBUG) Log.e(TAG, Log.getStackTraceString(t));
            throw new RuntimeException(t);
        }
    }

    public static void setProps(Context context) {
        final String packageName = context.getPackageName();
        final String processName = Application.getProcessName();

        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(processName)) {
            Log.e(TAG, "Null package or process name");
            return;
        }

        final Resources res = context.getResources();
        if (res == null) {
            Log.e(TAG, "Null resources");
            return;
        }

        sCertifiedProps = res.getStringArray(R.array.config_certifiedBuildProperties);
        sStockFp = res.getString(R.string.config_stockFingerprint);
        sNetflixModel = res.getString(R.string.config_netflixSpoofModel);

        sProcessName = processName;
        sIsPixelDevice = Build.MANUFACTURER.equals("Google") && Build.MODEL.contains("Pixel");
        sIsGms = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_UNSTABLE);
        sIsFinsky = packageName.equals(PACKAGE_FINSKY);
        sIsPhotos = packageName.equals(PACKAGE_GPHOTOS);

        /* Set Certified Properties for GMSCore
         * Set Stock Fingerprint for ARCore
         * Set custom model for Netflix
         */
        if (sIsGms) {
            setCertifiedPropsForGms();
        } else if (!sStockFp.isEmpty() && packageName.equals(PACKAGE_ARCORE)) {
            dlog("Setting stock fingerprint for: " + packageName);
            setPropValue("FINGERPRINT", sStockFp);
        } else if (!sNetflixModel.isEmpty() && packageName.equals(PACKAGE_NETFLIX)) {
            dlog("Setting model to " + sNetflixModel + " for Netflix");
            setPropValue("MODEL", sNetflixModel);
        }
    }

    private static void setPropValue(String key, String value) {
        try {
            dlog("Setting prop " + key + " to " + value.toString());
            Class clazz = Build.class;
            if (key.startsWith("VERSION.")) {
                clazz = Build.VERSION.class;
                key = key.substring(8);
            }
            Field field = clazz.getDeclaredField(key);
            field.setAccessible(true);
            // Cast the value to int if it's an integer field, otherwise string.
            field.set(null, field.getType().equals(Integer.TYPE) ? Integer.parseInt(value) : value);
            field.setAccessible(false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void setCertifiedPropsForGms() {
        if (sCertifiedProps.length == 0) {
            dlog("Certified props are not set");
            return;
        }
        final boolean was = isGmsAddAccountActivityOnTop();
        final TaskStackListener taskStackListener = new TaskStackListener() {
            @Override
            public void onTaskStackChanged() {
                final boolean is = isGmsAddAccountActivityOnTop();
                if (is ^ was) {
                    dlog("GmsAddAccountActivityOnTop is:" + is + " was:" + was +
                            ", killing myself!"); // process will restart automatically later
                    Process.killProcess(Process.myPid());
                }
            }
        };
        if (!was) {
            dlog("Spoofing build for GMS");
            setCertifiedProps();
        } else {
            dlog("Skip spoofing build for GMS, because GmsAddAccountActivityOnTop");
        }
        try {
            ActivityTaskManager.getService().registerTaskStackListener(taskStackListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register task stack listener!", e);
        }
    }

    private static void setCertifiedProps() {
        for (String entry : sCertifiedProps) {
            // Each entry must be of the format FIELD:value
            final String[] fieldAndProp = entry.split(":", 2);
            if (fieldAndProp.length != 2) {
                Log.e(TAG, "Invalid entry in certified props: " + entry);
                continue;
            }
            setPropValue(fieldAndProp[0], fieldAndProp[1]);
        }
        setSystemProperty(PROP_SECURITY_PATCH, Build.VERSION.SECURITY_PATCH);
        setSystemProperty(PROP_FIRST_API_LEVEL,
                Integer.toString(Build.VERSION.DEVICE_INITIAL_SDK_INT));
    }

    private static void setSystemProperty(String name, String value) {
        try {
            SystemProperties.set(name, value);
            dlog("Set system prop " + name + "=" + value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set system prop " + name + "=" + value, e);
        }
    }

    private static boolean isGmsAddAccountActivityOnTop() {
        try {
            final ActivityTaskManager.RootTaskInfo focusedTask =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            return focusedTask != null && focusedTask.topActivity != null
                    && focusedTask.topActivity.equals(GMS_ADD_ACCOUNT_ACTIVITY);
        } catch (Exception e) {
            Log.e(TAG, "Unable to get top activity!", e);
        }
        return false;
    }

    public static boolean shouldBypassTaskPermission(Context context) {
        // GMS doesn't have MANAGE_ACTIVITY_TASKS permission
        final int callingUid = Binder.getCallingUid();
        final int gmsUid;
        try {
            gmsUid = context.getPackageManager().getApplicationInfo(PACKAGE_GMS, 0).uid;
            dlog("shouldBypassTaskPermission: gmsUid:" + gmsUid + " callingUid:" + callingUid);
        } catch (Exception e) {
            Log.e(TAG, "shouldBypassTaskPermission: unable to get gms uid", e);
            return false;
        }
        return gmsUid == callingUid;
    }

    public static boolean hasSystemFeature(String name, boolean has) {
        if (sIsPhotos && !sIsPixelDevice && has
                && sPixelFeatures.stream().anyMatch(name::contains)) {
            dlog("Blocked system feature " + name + " for Google Photos");
            has = false;
        }
        return has;
    }

    private static PrivateKey parsePrivateKey(String str, String algo) throws Throwable {
        byte[] bytes = Base64.getDecoder().decode(str);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance(algo).generatePrivate(spec);
    }

    private static byte[] parseCert(String str) {
        return Base64.getDecoder().decode(str);
    }

    private static byte[] getCertificateChain(String algo) throws Throwable {
        if (KeyProperties.KEY_ALGORITHM_EC.equals(algo)) {
            return EC_CERTS;
        } else if (KeyProperties.KEY_ALGORITHM_RSA.equals(algo)) {
            return RSA_CERTS;
        }
        throw new Exception();
    }

    private static byte[] modifyLeaf(byte[] bytes) throws Throwable {
        X509Certificate leaf = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(bytes));

        if (leaf.getExtensionValue(OID.getId()) == null) throw new Exception();

        X509CertificateHolder holder = new X509CertificateHolder(leaf.getEncoded());

        Extension ext = holder.getExtension(OID);

        ASN1Sequence sequence = ASN1Sequence.getInstance(ext.getExtnValue().getOctets());

        ASN1Encodable[] encodables = sequence.toArray();

        ASN1Sequence teeEnforced = (ASN1Sequence) encodables[7];

        ASN1EncodableVector vector = new ASN1EncodableVector();

        ASN1Sequence rootOfTrust = null;
        for (ASN1Encodable asn1Encodable : teeEnforced) {
            ASN1TaggedObject taggedObject = (ASN1TaggedObject) asn1Encodable;
            if (taggedObject.getTagNo() == 704) {
                rootOfTrust = (ASN1Sequence) taggedObject.getObject();
                continue;
            }
            vector.add(asn1Encodable);
        }

        if (rootOfTrust == null) throw new Exception();

        algo = leaf.getPublicKey().getAlgorithm();

        boolean isEC = KeyProperties.KEY_ALGORITHM_EC.equals(algo);

        X509CertificateHolder cert1 = isEC ? EC_holder : RSA_holder;
        PrivateKey privateKey = isEC ? EC : RSA;

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(cert1.getSubject(), holder.getSerialNumber(), holder.getNotBefore(), holder.getNotAfter(), holder.getSubject(), holder.getSubjectPublicKeyInfo());
        ContentSigner signer = new JcaContentSignerBuilder(leaf.getSigAlgName()).build(privateKey);

        byte[] verifiedBootKey = new byte[32];
        ThreadLocalRandom.current().nextBytes(verifiedBootKey);

        DEROctetString verifiedBootHash = (DEROctetString) rootOfTrust.getObjectAt(3);

        if (verifiedBootHash == null) {
            byte[] temp = new byte[32];
            ThreadLocalRandom.current().nextBytes(temp);
            verifiedBootHash = new DEROctetString(temp);
        }

        ASN1Encodable[] rootOfTrustEnc = {new DEROctetString(verifiedBootKey), ASN1Boolean.TRUE, new ASN1Enumerated(0), new DEROctetString(verifiedBootHash)};

        ASN1Sequence rootOfTrustSeq = new DERSequence(rootOfTrustEnc);

        ASN1TaggedObject rootOfTrustTagObj = new DERTaggedObject(704, rootOfTrustSeq);

        vector.add(rootOfTrustTagObj);

        ASN1Sequence hackEnforced = new DERSequence(vector);

        encodables[7] = hackEnforced;

        ASN1Sequence hackedSeq = new DERSequence(encodables);

        ASN1OctetString hackedSeqOctets = new DEROctetString(hackedSeq);

        Extension hackedExt = new Extension(OID, false, hackedSeqOctets);

        builder.addExtension(hackedExt);

        for (ASN1ObjectIdentifier extensionOID : holder.getExtensions().getExtensionOIDs()) {
            if (OID.getId().equals(extensionOID.getId())) continue;
            builder.addExtension(holder.getExtension(extensionOID));
        }

        return builder.build(signer).getEncoded();
    }

    public static KeyEntryResponse onGetKeyEntry(KeyEntryResponse response) {
        if (response == null) return null;
        //if (!SystemProperties.getBoolean(SPOOF_PIXEL_PI, true)) return response;

        if (response.metadata == null) return response;

        algo = null;

        try {
            byte[] newLeaf = modifyLeaf(response.metadata.certificate);
            response.metadata.certificateChain = getCertificateChain(algo);

            response.metadata.certificate = newLeaf;

        } catch (Throwable t) {
            if (DEBUG) Log.e(TAG, "onGetKeyEntry", t);
        }

        return response;
    }

    private static final class Keybox {
        public static final class EC {
            public static final String PRIVATE_KEY = "MHcCAQEEINaPB4DfsyTwEUAwMuPwhK846BaK8emigUEChxovfLVHoAoGCCqGSM49AwEHoUQDQgAEhxFbz8vLiN1Jz8q6V/KJO8AyLp5nFp6uEj/N02TL/v8MBZdSuBUnxY4W9ul9n75pgxlprjgFuDQo480/tS5cpg==";
            public static final String CERTIFICATE_1 = "MIIB8zCCAXmgAwIBAgIQW/JBBAUYnCKBrGrs4ymPLzAKBggqhkjOPQQDAjA5MQwwCgYDVQQMDANURUUxKTAnBgNVBAUTIGQyMDE1OTIwYmU2ZTViOGMzNDMwMWVhMGUzNjQxMTMxMB4XDTIxMDExMzIxMDc1M1oXDTMxMDExMTIxMDc1M1owOTEMMAoGA1UEDAwDVEVFMSkwJwYDVQQFEyAxZjQwOGI4ZDBiNzcxZDdlZTE3MWMwMDQ2ZmM3MjBlYzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABIcRW8/Ly4jdSc/KulfyiTvAMi6eZxaerhI/zdNky/7/DAWXUrgVJ8WOFvbpfZ++aYMZaa44Bbg0KOPNP7UuXKajYzBhMB0GA1UdDgQWBBTm4pjL0fLRqYB+FdhIQ+gu/wXGzDAfBgNVHSMEGDAWgBTJVDfuRoZUDVFx2lkEwCfvwtYTJTAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwICBDAKBggqhkjOPQQDAgNoADBlAjAbxmO4nIbw/GWUlULzzY/YEwBtDxiFRe+b/a+91iTd0IN16Kq5lWB2EFkq0gFbq7ECMQDYxGOFZji0Y3z2nnZzmndPrTUFt8nLMxXpmao+qY4Xkjv3MF68tuIc0NT/RPknoSM=";
            public static final String CERTIFICATE_2 = "MIIDlDCCAXygAwIBAgIRAJyZwgh0SNQJ6ffLHa3eDVAwDQYJKoZIhvcNAQELBQAwGzEZMBcGA1UEBRMQZjkyMDA5ZTg1M2I2YjA0NTAeFw0yMTAxMTMyMTA1NDNaFw0zMTAxMTEyMTA1NDNaMDkxDDAKBgNVBAwMA1RFRTEpMCcGA1UEBRMgZDIwMTU5MjBiZTZlNWI4YzM0MzAxZWEwZTM2NDExMzEwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAARrmcn5dlsy4BTpt07TYrhleI82EPodzRKYfWbGmiMLQkS+MximOf3/8lgh0koGa4kq79HDPl9E7LVcUDIOu/P3WkSuj0EpgjkJGjLD0BhDSDhC7NWd2GsDra7VXN1+sYajYzBhMB0GA1UdDgQWBBTJVDfuRoZUDVFx2lkEwCfvwtYTJTAfBgNVHSMEGDAWgBQ2YeEAfIgFCVGLRGxH/xpMyepPEjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwICBDANBgkqhkiG9w0BAQsFAAOCAgEAUMjBHDSbnJ4iFNzaI8qQqN6a1zi9fdN4GmsDFTZ6TBv6tOExsWQuM3zNgzCRmbGLaeszLnNoBXyw3ED/Y0m6/Pez1fbhu46Ad3LX5mhFD6bpenr8OzYeDCQYBRBheEC0fdbB00Vb5HxZYWSKxgWj1zrI4FsGBuUPZ90VtalfLZszaANPFvG2QTYXTPYFDd3gbBauhl2yf8SV0BEOu5ztVCkykpwhP0xgziIEs55EkX2YP+wqDWNISwCte7hlVii6ZsOP/wS8DpAhKgyPTVxUa5Xr0Yx3BkbqaBk1cFyPy+yHYmqjI2JIKm4kdGCh3osgUCZCvzH/GXzpFB4rgbvO6bhgHtdgf8/hJ+MH3RVn9OpUpiTLgfMAueye8ZvuGNmJbFvQcFeXun/fjqyy18JykfraUGP72RATAaUWiP78ZfsVksTJNwxcU7gMYSCQOZJSNxryVNpL6CeQ6IYVqxS/JbqI42jXl1TzWvGuLg0DllgdLdcM2jJqyUcKBvQn0665spPvRfHyMsjVmibkcgQpypFt+ngiOksq3Ykd7vPGO+18BL9yxzc9Wdh5DYZ/LDLqPm6fsqoenyDmrQ/Azejr9UtxybQ/bW2EjVDaE+rMn7nYO6CSDFq+EUxJ+B5wTqTsHhmeSKqfGOhN9uIZzlytaoTlfnsyp2oVcGGG4gMBrS0=";
            public static final String CERTIFICATE_3 = "MIIFHDCCAwSgAwIBAgIJANUP8luj8tazMA0GCSqGSIb3DQEBCwUAMBsxGTAXBgNVBAUTEGY5MjAwOWU4NTNiNmIwNDUwHhcNMTkxMTIyMjAzNzU4WhcNMzQxMTE4MjAzNzU4WjAbMRkwFwYDVQQFExBmOTIwMDllODUzYjZiMDQ1MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAr7bHgiuxpwHsK7Qui8xUFmOr75gvMsd/dTEDDJdSSxtf6An7xyqpRR90PL2abxM1dEqlXnf2tqw1Ne4Xwl5jlRfdnJLmN0pTy/4lj4/7tv0Sk3iiKkypnEUtR6WfMgH0QZfKHM1+di+y9TFRtv6y//0rb+T+W8a9nsNL/ggjnar86461qO0rOs2cXjp3kOG1FEJ5MVmFmBGtnrKpa73XpXyTqRxB/M0n1n/W9nGqC4FSYa04T6N5RIZGBN2z2MT5IKGbFlbC8UrW0DxW7AYImQQcHtGl/m00QLVWutHQoVJYnFPlXTcHYvASLu+RhhsbDmxMgJJ0mcDpvsC4PjvB+TxywElgS70vE0XmLD+OJtvsBslHZvPBKCOdT0MS+tgSOIfga+z1Z1g7+DVagf7quvmag8jfPioyKvxnK/EgsTUVi2ghzq8wm27ud/mIM7AY2qEORR8Go3TVB4HzWQgpZrt3i5MIlCaY504LzSRiigHCzAPlHws+W0rB5N+er5/2pJKnfBSDiCiFAVtCLOZ7gLiMm0jhO2B6tUXHI/+MRPjy02i59lINMRRev56GKtcd9qO/0kUJWdZTdA2XoS82ixPvZtXQpUpuL12ab+9EaDK8Z4RHJYYfCT3Q5vNAXaiWQ+8PTWm2QgBR/bkwSWc+NpUFgNPN9PvQi8WEg5UmAGMCAwEAAaNjMGEwHQYDVR0OBBYEFDZh4QB8iAUJUYtEbEf/GkzJ6k8SMB8GA1UdIwQYMBaAFDZh4QB8iAUJUYtEbEf/GkzJ6k8SMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgIEMA0GCSqGSIb3DQEBCwUAA4ICAQBOMaBc8oumXb2voc7XCWnuXKhBBK3e2KMGz39t7lA3XXRe2ZLLAkLM5y3J7tURkf5a1SutfdOyXAmeE6SRo83Uh6WszodmMkxK5GM4JGrnt4pBisu5igXEydaW7qq2CdC6DOGjG+mEkN8/TA6p3cnoL/sPyz6evdjLlSeJ8rFBH6xWyIZCbrcpYEJzXaUOEaxxXxgYz5/cTiVKN2M1G2okQBUIYSY6bjEL4aUN5cfo7ogP3UvliEo3Eo0YgwuzR2v0KR6C1cZqZJSTnghIC/vAD32KdNQ+c3N+vl2OTsUVMC1GiWkngNx1OO1+kXW+YTnnTUOtOIswUP/Vqd5SYgAImMAfY8U9/iIgkQj6T2W6FsScy94IN9fFhE1UtzmLoBIuUFsVXJMTz+Jucth+IqoWFua9v1R93/k98p41pjtFX+H8DslVgfP097vju4KDlqN64xV1grw3ZLl4CiOe/A91oeLm2UHOq6wn3esB4r2EIQKb6jTVGu5sYCcdWpXr0AUVqcABPdgL+H7qJguBw09ojm6xNIrw2OocrDKsudk/okr/AwqEyPKw9WnMlQgLIKw1rODG2NvU9oR3GVGdMkUBZutL8VuFkERQGt6vQ2OCw0sV47VMkuYbacK/xyZFiRcrPJPb41zgbQj9XAEyLKCHex0SdDrx+tWUDqG8At2JHA==";
        }
        public static final class RSA {
            public static final String PRIVATE_KEY = "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";
            public static final String CERTIFICATE_1 = "MIIE4DCCAsigAwIBAgIRANEddk05W1jqsOQlSRGYH2wwDQYJKoZIhvcNAQELBQAwOTEMMAoGA1UEDAwDVEVFMSkwJwYDVQQFEyAxZTAxNjc1MzMwOGEwMWMwMzYwNzBiOTkxNjI5NmEyNzAeFw0yMjA5MTcxNzE0MDVaFw0zMjA5MTQxNzE0MDVaMDkxDDAKBgNVBAwMA1RFRTEpMCcGA1UEBRMgMGM4Njg0YzY2ZDVjM2Y2M2MyZDI0OTRiNzJiODJkNTAwggGiMA0GCSqGSIb3DQEBAQUAA4IBjwAwggGKAoIBgQCjkMAv0HCcncLjtvAtc+CS5Wy/BH8MS5nOr7CwM+76o87fv71323nZoBKDjb9VIrNJirE3i+DogUvcSG0oHQ0nlrYYmcD4d0Ze9SGNkeh3xNDgUKKUa0kmoCbbCTFqQz1FiNnRORu/jMvwiVO4mnmLBOGHfK7dIJ4m945NjD/Bv1F8f6RLzOJpCuQeOYbnup+aYKQo4PD1g4ctGiSaPatzucdfG2Q+Hbkmnibal++CMdpVG/DRfeHZ5Rx9W2b4++FbrMSv/RR0wkho07zRVqE9dZ6oiUK7eNmgwfpo3OK1RYUK5OPp8Tt0Fz74WFIqJjbiY0VgTFZvqCIrIieXWXImWTKbUBSL50fdjvR7rvUb4d01ZDKlGa3BXJxRaGktFxevAh7b7613Uy953DDgXd+XO4c3JGzGcH2l+h7mphB2+RzqnLJPzfHD3IwTBLNgIH1zCbGZQnhe4k3UGOxcIQJOzZ+KaA/uIl7DTdV3pb/UD5v3KKGuWo9W88sX+OYgoNsCAwEAAaNjMGEwHQYDVR0OBBYEFMJS2luT0WMslf/fwYc3xGvW8z0qMB8GA1UdIwQYMBaAFNplJLLkhPHv/IrpvTUvfFjRarMGMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgIEMA0GCSqGSIb3DQEBCwUAA4ICAQBolN+VgyoV8hTOQdtzLuOvLuYLfYNZGcpQ4GtCPjWUUa3YXTJTrYfTpT3nP5Yr4JhVurCK8toGVvEHWdGi8Zxsjw9z/tlpqLKguoPculD28OhjZBZbOZ5X9QH/NKi9H/KyRB/m0kv53/gw0p2GZrqhXkAklxuhvsY3bhchp2I6rz/ie2CZQedp4A3jX6C6pS5HMbQi9Y2m8kNp0/DQy8oJa7uiom07iL/X7KWZTY1sbZi3g99qLZJEYzd6B8PufR3dR5TFNx75+uBacyOUdzhuWGk+XPjhrSvACpk9my3CcO8phWfrKDKTmISoZQzEY4UFN8VclU5cX5QmJKNvIZ9mPJ2yzwzEVsBjv2qu146iLuCgz64hqeXlS7++Qfs1YWgIhVS/r8Og1p2HgnbRt1lm1x6iqIF0pcQOnPqbDAMeuTHnwoiBJlPTwf8ix3Yy9w0/UTVqO3LjK+ALdy6CS3agpUmVLkxUhIxlb8QGJ3GmG1eHQn/SHyXpxIwKCKIKMOCo85WssJv80YGQI5rpKrjQ8Yzhlc8wq3PClkY6sYPMIbgqoymYET56VLoRryiLIAnpUsezMLulTE8Wu6csUd4DqbzK2W+ZVN9eXDunLQzwi2jmdxLyN5DtpobDEaXo7B1yhO64Mg1nxAF4Wc3rF0QnkJUbuG2Fp6N2fnxA0foYLw==";
            public static final String CERTIFICATE_2 = "MIIFQTCCAymgAwIBAgIQBCM0AVWvUSM8Njd0xc/g5zANBgkqhkiG9w0BAQsFADAbMRkwFwYDVQQFExBmOTIwMDllODUzYjZiMDQ1MB4XDTIyMDkxNzE3MTIyM1oXDTMyMDkxNDE3MTIyM1owOTEMMAoGA1UEDAwDVEVFMSkwJwYDVQQFEyAxZTAxNjc1MzMwOGEwMWMwMzYwNzBiOTkxNjI5NmEyNzCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBALBW8YfWz8PlqzhJdISHAq10CI1pBVnTACtmuOogoWrLSfxuPtMBkW0bk5u+ger0ZPO5qlXzWjHTP6dpg12DxU7MA8CBvcgWZ5yGP4yYdpQTcWcOpxQIKD7CYOemGQUKAXWO5oVn1lkqIYPcXgsDB30tgmNT+lvT0OZhRnv3t3I9E2L852cMEWIZnYHNbwUdRIMf5ZAkspFatzwskGOKutknX4FTGHhIikzB0xdWbCoZYxczL9u6RkLy/RytmfWmWSpUz1E+HsvZzdjnSZ1u6ouU2hmNisOwGxKJZed4OmKcqifd087sy12sLpPN6/khSkWbp3Pk45lg2kXoovieH01P4I8NEYhgLOToEJY93TtBhp9eATrfTpj5X+lWEu4vu1X7kw6XL6cMwlHIy0jrL8++2pUXircsnuYZwUNlG6umjFCIUSiTl9iZJqwjYy1SDrfggcSX6Dm+lurYMdSbo5UN30zsaharGVyUf99nqe5a6eEHtliiPdl+WtS/P08wfF2Rm0NBqJUR8cbe/vaFxqZyZ2Y/upY7LzTBQNftMiPoxrQPiLfCB4lrPlNUH+7bKjziL7mnREQq18SHU5Gt5nlQtJIotFMUE3rFjTXtOMfwyQ4PSG6WMQ4XycOYwj9N74LqNC5MVmv2pYuWYw33dl620BgrZZrsVk9XwTJqf7PpAgMBAAGjYzBhMB0GA1UdDgQWBBTaZSSy5ITx7/yK6b01L3xY0WqzBjAfBgNVHSMEGDAWgBQ2YeEAfIgFCVGLRGxH/xpMyepPEjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwICBDANBgkqhkiG9w0BAQsFAAOCAgEAV+9eU49qaS+KJfynRtZWFtHLuGSTzh+L+QE+5U9QY6NFB0HHhEP9HUGmCt02biK6couBP2XsisNtcRqpM3SxyunTztZjP8U+ucBaonxhxOViS6J9Zxg6n54lLSataraLE800jyi83iPar6kU3EUJkagEGc54t1b7E/UZWaEtKZ/uaOSkhd7SCGOsmduTaecjshTxqV8Qwj/c+DNGMqu2HhQnpxs7krcdDNOxxXP6E0xY2/iIUqEcf5ON24S9qYD8ZJWt46TLrTO4PJPOmj7WwX5jA4qbkzmugP+v6EJls6gflk2hynAXm4lAI8xFdO7YFCZ8L0SDSVw8SK9cEyYhZhXiZ7MBvSJ9ak5XvuMYTaEXFS5QhqD9+ObEBKG68n7s5ySPfz44QP+8iftWAYMMwD4cYxJElYHTYp91zlN3kJDbwnLoDLS7PZVqBkJkSvnAEM5ejRiaKCK7tB3WkYX6YRxUQ0lsaEGXy4/183sYrKTCmXeU1ccWH8liMb8n81hmQSN9YQtnQVNKcHCkfKt++GFKNlkl43gdUUcLJ73zNrAJnV36TuF1HMtFWrNOAzT53qfvHY8gBD5OJrA+ZxdX4n9g52iWWYxJEIXmLg4caIuz028KlGHpFCT9RIeNaEsEWS03yQF7ekotjqfumdY8B9W53sKqiPRsY3jsljrXiC4=";
            public static final String CERTIFICATE_3 = "MIIFHDCCAwSgAwIBAgIJAPHBcqaZ6vUdMA0GCSqGSIb3DQEBCwUAMBsxGTAXBgNVBAUTEGY5MjAwOWU4NTNiNmIwNDUwHhcNMjIwMzIwMTgwNzQ4WhcNNDIwMzE1MTgwNzQ4WjAbMRkwFwYDVQQFExBmOTIwMDllODUzYjZiMDQ1MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAr7bHgiuxpwHsK7Qui8xUFmOr75gvMsd/dTEDDJdSSxtf6An7xyqpRR90PL2abxM1dEqlXnf2tqw1Ne4Xwl5jlRfdnJLmN0pTy/4lj4/7tv0Sk3iiKkypnEUtR6WfMgH0QZfKHM1+di+y9TFRtv6y//0rb+T+W8a9nsNL/ggjnar86461qO0rOs2cXjp3kOG1FEJ5MVmFmBGtnrKpa73XpXyTqRxB/M0n1n/W9nGqC4FSYa04T6N5RIZGBN2z2MT5IKGbFlbC8UrW0DxW7AYImQQcHtGl/m00QLVWutHQoVJYnFPlXTcHYvASLu+RhhsbDmxMgJJ0mcDpvsC4PjvB+TxywElgS70vE0XmLD+OJtvsBslHZvPBKCOdT0MS+tgSOIfga+z1Z1g7+DVagf7quvmag8jfPioyKvxnK/EgsTUVi2ghzq8wm27ud/mIM7AY2qEORR8Go3TVB4HzWQgpZrt3i5MIlCaY504LzSRiigHCzAPlHws+W0rB5N+er5/2pJKnfBSDiCiFAVtCLOZ7gLiMm0jhO2B6tUXHI/+MRPjy02i59lINMRRev56GKtcd9qO/0kUJWdZTdA2XoS82ixPvZtXQpUpuL12ab+9EaDK8Z4RHJYYfCT3Q5vNAXaiWQ+8PTWm2QgBR/bkwSWc+NpUFgNPN9PvQi8WEg5UmAGMCAwEAAaNjMGEwHQYDVR0OBBYEFDZh4QB8iAUJUYtEbEf/GkzJ6k8SMB8GA1UdIwQYMBaAFDZh4QB8iAUJUYtEbEf/GkzJ6k8SMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgIEMA0GCSqGSIb3DQEBCwUAA4ICAQB8cMqTllHc8U+qCrOlg3H7174lmaCsbo/bJ0C17JEgMLb4kvrqsXZs01U3mB/qABg/1t5Pd5AORHARs1hhqGICW/nKMav574f9rZN4PC2ZlufGXb7sIdJpGiO9ctRhiLuYuly10JccUZGEHpHSYM2GtkgYbZba6lsCPYAAP83cyDV+1aOkTf1RCp/lM0PKvmxYN10RYsK631jrleGdcdkxoSK//mSQbgcWnmAEZrzHoF1/0gso1HZgIn0YLzVhLSA/iXCX4QT2h3J5z3znluKG1nv8NQdxei2DIIhASWfu804CA96cQKTTlaae2fweqXjdN1/v2nqOhngNyz1361mFmr4XmaKH/ItTwOe72NI9ZcwS1lVaCvsIkTDCEXdm9rCNPAY10iTunIHFXRh+7KPzlHGewCq/8TOohBRn0/NNfh7uRslOSZ/xKbN9tMBtw37Z8d2vvnXq/YWdsm1+JLVwn6yYD/yacNJBlwpddla8eaVMjsF6nBnIgQOf9zKSe06nSTqvgwUHosgOECZJZ1EuzbH4yswbt02tKtKEFhx+v+OTge/06V+jGsqTWLsfrOCNLuA8H++z+pUENmpqnnHovaI47gC+TNpkgYGkkBT6B/m/U01BuOBBTzhIlMEZq9qkDWuM2cA5kW5V3FJUcfHnw1IdYIg2Wxg7yHcQZemFQg==";
        }
    }

    public static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, "[" + sProcessName + "] " + msg);
    }
}
