package com.tencent.mobileqq.webviewplugin.util;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.tencent.mobileqq.webviewplugin.annotation.Public;
import java.io.File;
import java.io.IOException;


/**
 * Tencent. Author: raezlu Date: 13-11-19 Time: 涓嬪崍9:55
 */
@Public
public final class StorageUtils {

    private static final String TAG = "StorageUtils";
    private final static int STATE_UNKNOWN = -1;
    private final static int STATE_MOUNTED = 0;
    private final static int STATE_MOUNTED_READ_ONLY = 1;
    private final static int STATE_OTHERS = 2;

    private static int sMonitoredExternalState = STATE_UNKNOWN;

    private static volatile boolean sReceiverRegistered = false;
    private final static Singleton<BroadcastReceiver, Void> sReceiver = new Singleton<BroadcastReceiver, Void>() {
        @Override
        protected BroadcastReceiver create(Void param) {
            return new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    onStorageStateChanged();
                }
            };
        }
    };

    private StorageUtils() {
        // static usage.
    }

    public static void initiate(Context context) {
        registerReceiverIfNeeded(context);
    }

    /**
     * Whether external storage is readable.
     */
    @Public
    public static boolean isExternalReadable() {
        return isExternalMounted(null) || isExternalMountedReadOnly(null);
    }

    /**
     * Whether external storage is readable, this is a faster api.
     *
     * @param context application context.
     */
    @Public
    public static boolean isExternalReadable(Context context) {
        return isExternalMounted(context) || isExternalMountedReadOnly(context);
    }

    /**
     * Whether external storage is writeable.
     */
    @Public
    public static boolean isExternalWriteable() {
        return isExternalMounted(null);
    }

    /**
     * Whether external storage is writeable, this is a faster api.
     *
     * @param context application context.
     */
    @Public
    public static boolean isExternalWriteable(Context context) {
        return isExternalMounted(context);
    }

    private static boolean isExternalMounted(Context context) {
        if (sMonitoredExternalState != STATE_UNKNOWN) {
            return sMonitoredExternalState == STATE_MOUNTED;
        }
        int state = retrieveExternalStorageState();
        if (registerReceiverIfNeeded(context)) {
            // update state when register succeed.
            sMonitoredExternalState = state;
        }
        return state == STATE_MOUNTED;
    }

    private static boolean isExternalMountedReadOnly(Context context) {
        if (sMonitoredExternalState != STATE_UNKNOWN) {
            return sMonitoredExternalState == STATE_MOUNTED_READ_ONLY;
        }
        int state = retrieveExternalStorageState();
        if (registerReceiverIfNeeded(context)) {
            // update state when register succeed.
            sMonitoredExternalState = state;
        }
        return state == STATE_MOUNTED_READ_ONLY;
    }

    static void onStorageStateChanged() {
        sMonitoredExternalState = retrieveExternalStorageState();
    }

    private static boolean registerReceiverIfNeeded(Context context) {
        if (!sReceiverRegistered) {
            synchronized (sReceiver) {
                if (!sReceiverRegistered) {
                    if (context == null) {
                        return false;
                    }
                    sReceiverRegistered = true;
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
                    filter.addAction(Intent.ACTION_MEDIA_EJECT);
                    filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
                    filter.addAction(Intent.ACTION_MEDIA_REMOVED);
                    filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
                    filter.addDataScheme("file");
                    context.getApplicationContext().registerReceiver(sReceiver.get(null), filter);
                }

            }
        }
        return true;
    }

    private static int retrieveExternalStorageState() {
        String externalState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(externalState)) {
            return STATE_MOUNTED;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalState)) {
            return STATE_MOUNTED_READ_ONLY;
        } else {
            return STATE_OTHERS;
        }
    }

    // ------------------------------ dir related -------------------------------
    private final static Object sCacheDirLock = new Object();

    static class InnerEnvironment {

        private static final String TAG = "InnerEnvironment";

        private static final String EXTEND_SUFFIX = "-ext";

        private static final File EXTERNAL_STORAGE_ANDROID_DATA_DIRECTORY
                = new File(new File(Environment.getExternalStorageDirectory(),
                "Android"), "data");

        public static File getExternalStorageAndroidDataDir() {
            return EXTERNAL_STORAGE_ANDROID_DATA_DIRECTORY;
        }

        public static File getExternalStorageAppCacheDirectory(String packageName) {
            return new File(new File(EXTERNAL_STORAGE_ANDROID_DATA_DIRECTORY,
                    packageName), "cache");
        }

        public static File getExternalStorageAppFilesDirectory(String packageName) {
            return new File(new File(EXTERNAL_STORAGE_ANDROID_DATA_DIRECTORY,
                    packageName), "files");
        }

        @SuppressLint("NewApi")
        public static File getExternalCacheDir(Context context, boolean extend) {
            if (!extend && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                return context.getExternalCacheDir();
            }
            synchronized (InnerEnvironment.class) {
                File externalCacheDir = getExternalStorageAppCacheDirectory(
                        context.getPackageName() + (extend ? EXTEND_SUFFIX : ""));
                if (!externalCacheDir.exists()) {
                    try {
                        boolean createFlag = (new File(getExternalStorageAndroidDataDir(),
                                ".nomedia")).createNewFile();
                        if (!createFlag) {
                            LogUtil.e(TAG, "[getExternalCacheDir] create file failed");
                        }
                    } catch (IOException e) {
                        LogUtil.i(TAG, e.getMessage());
                    }
                    if (!externalCacheDir.mkdirs()) {
                        Log.w(TAG, "Unable to create external cache directory");
                        return null;
                    }
                }
                return externalCacheDir;
            }
        }

        @SuppressLint("NewApi")
        public static File getExternalFilesDir(Context context, String type, boolean extend) {
            if (!extend && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                return context.getExternalFilesDir(type);
            }
            synchronized (InnerEnvironment.class) {
                File externalFilesDir = getExternalStorageAppFilesDirectory(
                        context.getPackageName() + (extend ? EXTEND_SUFFIX : ""));
                if (!externalFilesDir.exists()) {
                    try {
                        boolean createFileFlag = (new File(getExternalStorageAndroidDataDir(),
                                ".nomedia")).createNewFile();
                        Log.d(TAG, "[getExternalFilesDir] create File:" + createFileFlag);
                    } catch (IOException e) {
                        Log.e(TAG, "[getExternalFilesDir] create File:" + e);
                    }
                    if (!externalFilesDir.mkdirs()) {
                        Log.w(TAG, "Unable to create external files directory");
                        return null;
                    }
                }
                if (type == null) {
                    return externalFilesDir;
                }
                File dir = new File(externalFilesDir, type);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.w(TAG, "Unable to create external media directory " + dir);
                        return null;
                    }
                }
                return dir;
            }
        }
    }

    /**
     * Get common cache dir(external if available, or internal) with corresponding name, which is
     * not persist.
     */
    public static String getCacheDir(Context context, String name) {
        return getCacheDir(context, name, false);
    }

    /**
     * Get common cache dir(external if available, or internal) with corresponding name.
     *
     * @param context context
     * @param name cache dir name.
     * @param persist whether this cache dir should be persist or not.
     * @return cache dir.
     */
    public static String getCacheDir(Context context, String name, boolean persist) {
        String dir = getExternalCacheDir(context, name, persist);
        return dir != null ? dir : getInternalCacheDir(context, name, persist);
    }

    /**
     * Get external cache dir with corresponding name, which is not persist.
     */
    public static String getExternalCacheDir(Context context, String name) {
        return getExternalCacheDir(context, name, false);
    }

    /**
     * Get external cache dir with corresponding name.
     *
     * @param persist whether this cache dir should be persist or not.
     */
    public static String getExternalCacheDir(Context context, String name, boolean persist) {
        String dir = null;
        try {
            dir = getExternalCacheDir(context, persist);
        } catch (Exception ex) {
            LogUtil.e("StorageUtil", "getExternalCacheDir exception occours:" + ex.getMessage());
        }

        if (dir == null) {
            return null;
        }
        if (isEmpty(name)) {
            return dir;
        }
        File file = new File(dir + File.separator + name);
        if (!file.exists() || !file.isDirectory()) {
            synchronized (sCacheDirLock) {
                if (!file.isDirectory()) {
                    boolean delFlag = file.delete();
                    if (!delFlag) {
                        LogUtil.e(TAG, "[getExternalCacheDir] delete file failed");
                    }
                    boolean mkFlag = file.mkdirs();
                    if (!mkFlag) {
                        LogUtil.e(TAG, "[getExternalCacheDir]  mkdirs file failed");
                    }
                } else if (!file.exists()) {
                    boolean mkFlag = file.mkdirs();
                    if (!mkFlag) {
                        LogUtil.e(TAG, "[getExternalCacheDir]  mkdirs file failed");
                    }
                }
            }
        }
        return file.getAbsolutePath();
    }

    public static String getExternalCacheDir(Context context, boolean persist) {
        if (!isExternalWriteable(context)) {
            return null;
        }
        File externalDir = !persist ? InnerEnvironment.getExternalCacheDir(context, false)
                : InnerEnvironment.getExternalFilesDir(context, "cache", false);
        return externalDir == null ? null : externalDir.getAbsolutePath();
    }

    /**
     * Get extend external cache dir with corresponding name, which is not persist.
     */
    public static String getExternalCacheDirExt(Context context, String name) {
        return getExternalCacheDirExt(context, name, false);
    }

    /**
     * Get extend external cache dir with corresponding name.
     *
     * @param persist whether this cache dir should be persist or not.
     */
    public static String getExternalCacheDirExt(Context context, String name, boolean persist) {
        String dir = getExternalCacheDirExt(context, persist);
        if (dir == null) {
            return null;
        }
        if (isEmpty(name)) {
            return dir;
        }
        File file = new File(dir + File.separator + name);
        if (!file.exists() || !file.isDirectory()) {
            synchronized (sCacheDirLock) {
                if (!file.isDirectory()) {
                    boolean deleteFlag = file.delete();
                    if (!deleteFlag) {
                        LogUtil.e(TAG, "[getExternalCacheDirExt] delete file failed!");
                    }
                    boolean mkFlag = file.mkdirs();
                    if (!mkFlag) {
                        LogUtil.e(TAG, "[getExternalCacheDirExt] mkdirs failed!");
                    }
                } else if (!file.exists()) {
                    boolean mkFlag = file.mkdirs();
                    if (!mkFlag) {
                        LogUtil.e(TAG, "[getExternalCacheDirExt] !file.exists mkdirs failed!");
                    }
                }
            }
        }
        return file.getAbsolutePath();
    }

    public static String getExternalCacheDirExt(Context context, boolean persist) {
        if (!isExternalWriteable(context)) {
            return null;
        }
        File externalDir = !persist ? InnerEnvironment.getExternalCacheDir(context, true)
                : InnerEnvironment.getExternalFilesDir(context, "cache", true);
        return externalDir == null ? null : externalDir.getAbsolutePath();
    }

    /**
     * Get internal cache dir with corresponding name, which is not persist.
     */
    public static String getInternalCacheDir(Context context, String name) {
        return getInternalCacheDir(context, name, false);
    }

    /**
     * Get internal cache dir with corresponding name.
     *
     * @param persist whether this cache dir should be persist or not.
     */
    public static String getInternalCacheDir(Context context, String name, boolean persist) {
        String dir = getInternalCacheDir(context, persist);
        if (isEmpty(name)) {
            return dir;
        }
        File file = new File(dir + File.separator + name);
        if (!file.exists() || !file.isDirectory()) {
            synchronized (sCacheDirLock) {
                if (!file.isDirectory()) {
                    boolean delFlag = file.delete();
                    if (!delFlag) {
                        LogUtil.e(TAG, "[getInternalCacheDir] delete file failed");
                    }

                    boolean mkFlag = file.mkdirs();
                    if (!mkFlag) {
                        LogUtil.e(TAG, "[getInternalCacheDir] mkFlag file failed");
                    }
                } else if (!file.exists()) {
                    boolean mkFlag = file.mkdirs();
                    if (!mkFlag) {
                        LogUtil.e(TAG, "[getInternalCacheDir] !file.exists() mkFlag file failed");
                    }
                }
            }
        }
        return file.getAbsolutePath();
    }

    public static String getInternalCacheDir(Context context, boolean persist) {
        return !persist ? context.getCacheDir().getAbsolutePath()
                : context.getFilesDir().getAbsolutePath() + File.separator + "cache";
    }

    public static String getInternalFileDir(Context context, boolean persist) {
        return !persist ? context.getCacheDir().getAbsolutePath()
                : context.getFilesDir().getAbsolutePath() + File.separator;
    }

    /**
     * Determine whether a path is internal.
     */
    public static boolean isInternal(String path) {
        String internalCacheDir = Environment.getDataDirectory().getAbsolutePath();
        return path != null && path.startsWith(internalCacheDir);
    }

    // ---------------- internal utils -----------------
    private static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
