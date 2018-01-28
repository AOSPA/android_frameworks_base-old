package com.android.server.theme;

import android.content.Context;
import andorid.content.Intent;
import android.content.IntentSender;
import android.content.IIntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.android.apksig.ApkSigner;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

public class ThemeHelper {
    static final String TAG = ThemeHelper.class.getSimpleName();
    static final String THEME_LOCATION = Environment.getDataDirectory() + "/system/theme/";
    static final String THEME_PACKAGE_PREFIX = "com.paranoid.theme";
    static final String KEY_PW = "overlay";
    static final boolean DEBUG = true;

    private final Context mContext;

    public ThemeHelper(Context context) {
        mContext = context;
    }

    /**
     * Builds and signs an OMS overlay
     * @param packageName the packagename of the app to build
     * @param accentColor the new accent color
     * @return the {@link java.io.File} object of the new apk
     */
    public File buildApk(String packageName, String accentColor) {
        String location = THEME_LOCATION + packageName + "/";
        String apk = location + packageName;

        // 1. Start with compiling the unsigned apk
        String command = "aapt -M " + location + "AndroidManifest.xml"
                + " -S " + location + "res/"
                + " -I /system/framework/framework-res.apk"
                + " -F " + apk + "-raw.apk"
                + " -f" + " --auto-add-overlay";
        if (DEBUG) {
            Log.d(TAG, "aapt command= " + command);
        }

        if (!runCommand(command)) {
            if (DEBUG) {
                Log.d(TAG, "failed to execute command " + command);
            }

            return null;
        }

        // 2. Zipalign the apk
        command = "zipalign 4" + apk + "-raw.apk "
                + apk + "-unsigned.apk";
        if (!runCommand(command)) {
            if (DEBUG) {
                Log.d(TAG, "failed to execute command " + command);
            }

            return null;
        }

        // 3. Sign the apk
        File key = new File(THEME_LOCATION + "key");
        char[] keyPass = KEY_PW.toCharArray();

        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(new FileInputStream(key, pass));

        PrivateKey privateKey = (PrivateKey) store.getKey("key", keyPass);

        List<X509Certificate> certs = new ArrayList();
        certs.add((X509Certificate) store.getCertificateChain("key")[0]);

        ApkSigner.SignerConfig config = new ApkSigner.SignerConfig
                .Builder(KEY_PW, privateKey, certs).build();

        List<ApkSigner.SignerConfig> configs = new ArrayList();
        configs.add(config);

        ApkSigner.Builder signer = new ApkSigner.Builder(configs);
        File input = new File(apk + "-unsigned.apk");
        File dest = new File(apk + ".apk");
        signer
                .setV1SigningEnabled(false)
                .setV2SigningEnabled(true)
                .setInputApk(input)
                .setOutputApk(dest)
                .setMinSdkVersion(Build.VERSION.SDK_INT)
                .build()
                .sign();

        // 4. Remove leftover apks
        File raw = new File(apk + "-raw.apk");
        File unsigned = new File(apk + "-unsigned.apk");

        if (!raw.delete()) {
            Log.e(TAG, "failed to delete file " + raw);
        }

        if (!unsigned.delete()) {
            Log.e(TAG, "failed to delete file " + unsigned);
        }

        // 5. Return the compiled apk
        dest.setReadable(true, false);
        return dest;
    }

    /**
     * Executes commands on the command line
     * @param command the command to execute
     * @return whether the command has executed successfully
     */
    private boolean runCommand(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            return br.readLine() == null;
        } catch (Exception e) {
            Log.e(TAG, "exception ocurred while executing command " + command, e);
            return false;
        }
    }

    /**
     * Install the package
     * @param Uri the uri of the apk
     * @return whether the package has been installed successfully
     */
    public boolean install(Uri uri) throws RemoteException {
        try {
            PackageInstaller installer = mContext.getPackageManager().getPackageInstaller();
            SessionParams params = new SessionParams(SessionParams.MODE_FULL_INSTALL);
            params.installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
            int sessionId = installer.createSession(params);
            PackageInstaller.Session session = installer.openSession(sessionId);

            try (InputStream is = ctx.getContentResolver().openInputStream(uri);
                OutputStream os = session.openWrite(TAG, 0, -1);) {
                int c;
                byte[] buffer = new byte[1024];
                while ((c = is.read(buffer)) != -1) {
                    os.write(buffer, 0, c);
                }
            }

            LocalIntentReceiver receiver = new LocalIntentReceiver();
            session.commit(receiver.getIntentSender());

            Intent result = receiver.getResult();
            int status = result.getIntExtra(PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE);
            return status == PackageInstaller.STATUS_SUCCESS;
        } finally {
            IoUtils.closeQuietly(session);
        }
    }

    private static class LocalIntentReceiver {
        private final SynchronousQueue<Intent> mResult = new SynchronousQueue<>();

        private IIntentSender.Stub mLocalSender = new IIntentSender.Stub() {
            @Override
            public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken,
                    IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                try {
                    mResult.offer(intent, 5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        public IntentSender getIntentSender() {
            return new IntentSender((IIntentSender) mLocalSender);
        }

        public Intent getResult() {
            try {
                return mResult.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
