package com.tencent.mobileqq.webviewplugin.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.text.ClipboardManager;
import android.text.TextUtils;
import com.tencent.mobileqq.webviewplugin.CustomWebView;
import com.tencent.mobileqq.webviewplugin.CustomWebViewClient;
import com.tencent.mobileqq.webviewplugin.Util;
import com.tencent.mobileqq.webviewplugin.WebViewPlugin;
import com.tencent.mobileqq.webviewplugin.util.FileUtil;
import com.tencent.mobileqq.webviewplugin.util.IoUtils;
import com.tencent.mobileqq.webviewplugin.util.LogUtil;
import com.tencent.mobileqq.webviewplugin.util.ThreadManager;
import com.tencent.mobileqq.webviewplugin.util.TripleDes;
import com.tencent.smtt.sdk.WebView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by pelli on 2015/2/6.
 */
public class DataApiPlugin extends WebViewPlugin {

    static final int ERROR_WRONG_JSON = -2;
    static final int ERROR_EMPTY_PARAMS = -3;
    static final int ERROR_EMPTY_URL = -4;
    static final int ERROR_NO_PERMISSION_TO_DOMAIN = -5;
    static final int ERROR_EMPTY_PATH = -6;
    static final int ERROR_EMPTY_KEY = -7;
    static final int ERROR_EMPTY_DATA = -8;
    static final int ERROR_NO_SPACE_OR_NO_SDCARD = -9;
    static final int ERROR_WRONG_IMAGE_DATA = -10;
    static final int ERROR_DATA_NOT_EXIST = -11;
    static final int ERROR_TOO_MANY_DATA = -12;
    static final int ERROR_URL_EMPTY = -13;
    static final int ERROR_DATA_ERROR = -14;
    static final int ERROR_PATH_ERROR = -15;
    static final int ERROR_KEY_ERROR = -16;

    static final String DATA_PATH =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/Tencent/h5data/";
    static final long MAX_H5_DATA_SIZE = 50 * 1024 * 1024;
    static final String KEY_H5DATA = "H5Data";

    private long sH5DataUsage = 0;


    @Override
    protected void onCreate() {
        if (sH5DataUsage == 0) {
            ThreadManager.executeOnNetWorkThread(new Runnable() {
                @Override
                public void run() {
                    long usage = getH5DataUsage();
                    synchronized (this) {
                        sH5DataUsage = usage;
                    }
                }
            });
        }
    }


    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if (args == null) {
            return false;
        }
        if (args.length == 1) {
            if ("setClipboard".equals(method)) {
                return (doSetClickboardMethod(args[0]));
            } else if ("getClipboard".equals(method)) {
                doGetClipboard(args[0]);
            } else if ("getPerformance".equals(method)) {
                getPerformance(args[0]);
            } else if ("getPageLoadStamp".equals(method)) {
                getPageLoadStamp(args[0]);
            } else if ("readH5Data".equals(method)) {
                doReadH5DataMethod(args);
            } else if ("writeH5Data".equals(method)) {
                doWriteH5DataMethod(args);
            }
        } else if (args.length == 2) {
            if ("deleteH5Data".equals(method)) {
                deleteH5Data(args[0], args[1]);
            } else if ("deleteH5DataByHost".equals(method)) {
                deleteH5DataByHost(args[0], args[1]);
            }
        }
        return true;
    }

    private void doReadH5DataMethod(String[] args) {
        String callBack = "";
        JSONObject json = null;
        try {
            json = new JSONObject(args[0]);
            callBack = json.optString(KEY_CALLBACK);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        readH5Data(args[0], callBack);
    }

    private void doWriteH5DataMethod(String[] args) {
        String callBack = "";
        JSONObject json = null;
        try {
            json = new JSONObject(args[0]);
            callBack = json.optString(KEY_CALLBACK);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        writeH5Data(args[0], callBack);
    }

    private void doGetClipboard(String arg) {
        try {
            JSONObject json = new JSONObject(arg);
            String callback = json.optString(KEY_CALLBACK);
            if (!TextUtils.isEmpty(callback)) {
                String text;
                @SuppressWarnings("deprecation")
                ClipboardManager cm = (ClipboardManager) mRuntime.context
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    text = cm.getText().toString();
                } else {
                    text = "";
                }
                callJs(callback, Util.toJsString(text));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean doSetClickboardMethod(String arg) {
        String callback = null;
        try {
            JSONObject json = new JSONObject(arg);
            String text = json.optString("text");
            callback = json.optString(KEY_CALLBACK);
            if (text == null) {
                text = "";
            }
            @SuppressWarnings("deprecation")
            ClipboardManager cm = (ClipboardManager) mRuntime.context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setText(text);
                if (!TextUtils.isEmpty(callback)) {
                    callJs(callback, "true");
                }
                return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(callback)) {
            callJs(callback, "false");
        }
        return false;
    }

    void getPageLoadStamp(final String args) {
        long onCreateTime = -1, startLoadUrlTime = -1;
        String url = "";
        JSONObject json = null;
        String callback = null;
        try {
            json = new JSONObject(args);
            callback = json.optString(KEY_CALLBACK);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (mRuntime != null) {
            CustomWebView webView = (CustomWebView) mRuntime.getWebView();
            if (webView == null) {
                return;
            }
            try {
                onCreateTime = CustomWebView.clickStartTime;
                startLoadUrlTime = CustomWebView.startLoadUrlTime;
                url = CustomWebViewClient.mUrl;
                JSONObject result = new JSONObject();
                result.put("ret", 0);
                result.put("onCreateTime", onCreateTime);
                result.put("startLoadUrlTime", startLoadUrlTime);
                result.put("url", url);
                if (!TextUtils.isEmpty(callback)) {
                    callJs(callback, getResult(result));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                callJs(callback, "{ret: -1}");
            }
        }
    }

    void getPerformance(final String args) {
        long clickStartTime = -1, pageStartTime = -1, pageFinishTime = -1;
        if (mRuntime != null) {
            CustomWebView webView = (CustomWebView) mRuntime.getWebView();
            if (webView == null) {
                return;
            }
            try {
                JSONObject json = new JSONObject(args);
                String callback = json.optString(KEY_CALLBACK);
                clickStartTime = CustomWebView.clickStartTime;
                pageStartTime = CustomWebViewClient.pageStartTime;
                pageFinishTime = CustomWebViewClient.pageFinishTime;
                final JSONObject data = new JSONObject();
                try {
                    data.put("clickStart", clickStartTime);
                    data.put("pageStart", pageStartTime);
                    data.put("pageFinish", pageFinishTime);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (!TextUtils.isEmpty(callback)) {
                    callJs(callback, getResult(data));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    void writeH5Data(final String jsonParams, final String callback) {
        final JSONObject response = new JSONObject();
        if (TextUtils.isEmpty(jsonParams)) {
            callJs(callback, getResult(ERROR_EMPTY_PARAMS, "", response));
            return;
        }

        if (jsonParams.length() > 500 * 1024) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    writeData(jsonParams, callback, response);
                }
            }).start();
        } else {
            writeData(jsonParams, callback, response);
        }
    }

    void writeData(final String jsonParams, final String callback, final JSONObject response) {
        try {
            WebView webView = mRuntime.getWebView();
            if (webView == null) {
                return;
            }

            JSONObject json = new JSONObject(jsonParams);
            String callid = getCallId(json);

            response.put("callid", callid);
            // 判断H5数据占用大小是否超过上限
            if (sH5DataUsage > MAX_H5_DATA_SIZE) {
                writeDataAgain(jsonParams, callback, response);
                return;
            }

            String path = json.optString("path");
            if (TextUtils.isEmpty(path)) {
                callJs(callback, getResult(ERROR_EMPTY_PATH, "", response));
                return;
            }

            String key = json.optString("key");
            if (TextUtils.isEmpty(key)) {
                callJs(callback, getResult(ERROR_EMPTY_KEY, "", response));
                return;
            }

            String data = json.optString("data");
            if (TextUtils.isEmpty(data)) {
                callJs(callback, getResult(ERROR_EMPTY_DATA, "", response));
                return;
            }

            String host = null;
            try {
                URL url = new URL(webView.getUrl());
                host = url.getHost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String requestHost = json.optString("host");
            if (!TextUtils.isEmpty(requestHost)) {
                if (isParentDomain(requestHost, host)) {
                    host = requestHost;
                } else {
                    callJs(callback, getResult(ERROR_NO_PERMISSION_TO_DOMAIN, "", response));
                    return;
                }
            }

            if (TextUtils.isEmpty(host)) {
                host = "defaulthost";
            }

            String hash = hash(data);
            if (TextUtils.isEmpty(hash)) {
                callJs(callback, getResult(ERROR_DATA_ERROR, "", response));//hash值为空
                return;
            }
            writeHash(host, path, key, hash);
            String encryptData = encrypt(data, hash);
            File uinDir = new File(DATA_PATH + "/" + Util.string2HexString(mRuntime.getAccount()));
            deleteAndMakeFile(uinDir);

            if (TextUtils.isEmpty(host)) {
                callJs(callback, getResult(ERROR_URL_EMPTY, "", response));
                return;
            }

            File hostDir = new File(uinDir, host);
            deleteAndMakeFile(hostDir);
            String pathHexStr = Util.string2HexString(path);
            if (TextUtils.isEmpty(pathHexStr)) {
                callJs(callback, getResult(ERROR_PATH_ERROR, "", response));
                return;
            }

            File pathDir = new File(hostDir, pathHexStr);
            deleteAndMakeFile(pathDir);

            String keyHexStr = Util.string2HexString(key);
            if (TextUtils.isEmpty(keyHexStr)) {
                callJs(callback, getResult(ERROR_KEY_ERROR, "", response));
                return;
            }
            writeDataFinally(callback, response, encryptData, pathDir, keyHexStr);
        } catch (JSONException e) {
            callJs(callback, getResult(ERROR_WRONG_JSON, "", response));
        }
    }

    @Nullable
    private String getCallId(JSONObject json) {
        String callid = json.optString("callid");
        if (!TextUtils.isEmpty(callid)) {
            callid = callid.replace("\\", "\\\\").replace("'", "\\'");
        }
        return callid;
    }

    private void writeDataFinally(String callback, JSONObject response, String encryptData,
            File pathDir, String keyHexStr) {
        FileWriter fileWriter = null;
        try {
            synchronized (this) {
                File file = new File(pathDir, keyHexStr);
                if (file.exists()) {
                    boolean flag = file.delete();
                    if (!flag) {
                        LogUtil.e(TAG, "[writeData] delete file fail!");
                    }
                }
                fileWriter = new FileWriter(file);
                fileWriter.write(encryptData);
                sH5DataUsage += file.length();
            }
            callJs(callback, getResult(response));
        } catch (IOException e) {
            callJs(callback, getResult(ERROR_NO_SPACE_OR_NO_SDCARD, "", response));
        } finally {
            IoUtils.close(fileWriter);
        }
    }

    private void deleteAndMakeFile(File uinDir) {
        if (!uinDir.exists()) {
            uinDir.mkdirs();
        } else if (uinDir.isFile()) {
            uinDir.delete();
            uinDir.mkdirs();
        }
    }

    private void writeDataAgain(final String jsonParams, final String callback,
            final JSONObject response) {
        // 重新获取占用大小, 确保空间大小没有差别
        ThreadManager.executeOnNetWorkThread(new Runnable() {
            @Override
            public void run() {
                long usage = getH5DataUsage();
                synchronized (this) {
                    sH5DataUsage = usage;
                }
                if (sH5DataUsage > MAX_H5_DATA_SIZE) {
                    callJs(callback, getResult(ERROR_TOO_MANY_DATA, "", response));
                    return;
                } else {
                    writeData(jsonParams, callback, response);
                }
            }
        });
    }

    void readH5Data(final String jsonParams, final String callback) {
        final JSONObject response = new JSONObject();
        if (TextUtils.isEmpty(jsonParams)) {
            callJs(callback, getResult(ERROR_EMPTY_PARAMS, "", response));
            return;
        }
        WebView webView = null;
        if ((webView = mRuntime.getWebView()) == null) {
            return;
        }
        final String url = webView.getUrl();
        new Thread(new Runnable() {
            @Override
            public void run() {
                readData(jsonParams, callback, response, url);
            }
        }).start();
    }

    void readData(final String jsonParams, final String callback, final JSONObject response,
            String urlStr) {
        try {

            JSONObject json = new JSONObject(jsonParams);
            String callid = getCallId(json);

            response.put("callid", callid);

            String path = json.optString("path");
            if (TextUtils.isEmpty(path)) {
                callJs(callback, getResult(ERROR_EMPTY_PATH, "", response));
                return;
            }

            String host = null;
            try {
                URL url = new URL(urlStr);
                host = url.getHost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String requestHost = json.optString("host");
            if (!TextUtils.isEmpty(requestHost)) {
                if (isParentDomain(requestHost, host)) {
                    host = requestHost;
                } else {
                    callJs(callback, getResult(ERROR_NO_PERMISSION_TO_DOMAIN, "", response));
                    return;
                }
            }

            if (TextUtils.isEmpty(host)) {
                host = "defaulthost";
            }

            String key = json.optString("key");
            if (TextUtils.isEmpty(key)) {
                callJs(callback, getResult(ERROR_EMPTY_KEY, "", response));
                return;
            }

            String hash = readHash(host, path, key);
            if (TextUtils.isEmpty(hash)) {
                callJs(callback, getResult(ERROR_DATA_NOT_EXIST, "", response));
                return;
            }

            File file = new File(DATA_PATH + "/" + Util.string2HexString(mRuntime.getAccount())
                    + "/" + host + "/" + Util.string2HexString(path) + "/"
                    + Util.string2HexString(key));
            if (!file.exists()) {
                callJs(callback, getResult(ERROR_DATA_NOT_EXIST, "", response));
                return;
            }

            InputStream is = null;
            String result = null;
            try {
                synchronized (this) {
                    is = new FileInputStream(file);
                    long fileLength = file.length();
                    byte[] byteData = new byte[(int) fileLength];
                    int readFlag = is.read(byteData);
                    if (readFlag == -1) {
                        LogUtil.e(TAG, "[readData] error!");
                    }
                    String outString = new String(byteData);
                    is.close();
                    result = decrypt(outString, hash);
                    if (result == null) {
                        callJs(callback, getResult(ERROR_DATA_NOT_EXIST, "", response));
                        return;
                    } else {
                        result = result.replace("\\", "\\\\").replace("'", "\\'");
                        response.put("data", result);
                    }
                }
                callJs(callback, getResult(response));
            } catch (IOException e) {
                callJs(callback, getResult(ERROR_NO_SPACE_OR_NO_SDCARD, "", response));
            } finally {
                IoUtils.close(is);
            }
        } catch (JSONException e) {
            callJs(callback, getResult(ERROR_WRONG_JSON, "", response));
        }
    }

    void deleteH5Data(final String jsonParams, final String callback) {
        WebView webView = mRuntime.getWebView();
        if (webView == null) {
            return;
        }

        JSONObject response = new JSONObject();
        if (TextUtils.isEmpty(jsonParams)) {
            callJs(callback, getResult(ERROR_EMPTY_PARAMS, "", response));
            return;
        }
        try {
            JSONObject json = new JSONObject(jsonParams);
            String callid = getCallId(json);

            response.put("callid", callid);

            String path = json.optString("path");
            if (TextUtils.isEmpty(path)) {
                callJs(callback, getResult(ERROR_EMPTY_PATH, "", response));
                return;
            }

            String key = json.optString("key");

            String host = null;
            try {
                URL url = new URL(webView.getUrl());
                host = url.getHost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String requestHost = json.optString("host");
            if (!TextUtils.isEmpty(requestHost)) {
                if (isParentDomain(requestHost, host)) {
                    host = requestHost;
                } else {
                    callJs(callback, getResult(ERROR_NO_PERMISSION_TO_DOMAIN, "", response));
                    return;
                }
            }

            if (TextUtils.isEmpty(host)) {
                host = "defaulthost";
            }

            if (TextUtils.isEmpty(key)) {
                deleteHash(host, path);
                FileUtil.deleteFile(new File(
                        DATA_PATH + "/" + Util.string2HexString(mRuntime.getAccount()) + "/" + host
                                + "/" + Util.string2HexString(path)));
            } else {
                deleteHash(host, path, key);
                FileUtil.deleteFile(new File(
                        DATA_PATH + "/" + Util.string2HexString(mRuntime.getAccount()) + "/" + host
                                + "/" + Util.string2HexString(path)
                                + "/" + Util.string2HexString(key)));
            }
            callJs(callback, getResult(response));
        } catch (JSONException e) {
            callJs(callback, getResult(ERROR_WRONG_JSON, "", response));
        }
    }

    void deleteH5DataByHost(final String jsonParams, final String callback) {
        WebView webView = mRuntime.getWebView();
        if (webView == null) {
            return;
        }

        JSONObject response = new JSONObject();
        if (TextUtils.isEmpty(jsonParams)) {
            callJs(callback, getResult(ERROR_EMPTY_PARAMS, "", response));
            return;
        }
        try {
            JSONObject json = new JSONObject(jsonParams);

            String callid = getCallId(json);

            response.put("callid", callid);

            String host = null;
            try {
                URL url = new URL(webView.getUrl());
                host = url.getHost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String requestHost = json.optString("host");
            if (!TextUtils.isEmpty(requestHost)) {
                if (isParentDomain(requestHost, host)) {
                    host = requestHost;
                } else {
                    callJs(callback, getResult(ERROR_NO_PERMISSION_TO_DOMAIN, "", response));
                    return;
                }
            }

            if (TextUtils.isEmpty(host)) {
                host = "defaulthost";
            }

            deleteHash(host);
            File hostDir = new File(
                    DATA_PATH + "/" + Util.string2HexString(mRuntime.getAccount()) + "/" + host);
            FileUtil.deleteFile(hostDir);
            callJs(callback, getResult(response));
        } catch (JSONException e) {
            callJs(callback, getResult(ERROR_WRONG_JSON, "", response));
        }
    }

    private static long getH5DataUsage() {
        long usage = 0;
        File uinDir = new File(DATA_PATH);
        if (!uinDir.exists()) {
            return 0;
        }
        ArrayList<File> fileList = new ArrayList<File>();
        fileList.add(uinDir);
        File file;
        while (!fileList.isEmpty()) {
            file = fileList.remove(0);
            if (file.isFile()) {
                usage += file.length();
            } else {
                File[] childFileList = file.listFiles();
                if (childFileList != null) {
                    for (File childFile : childFileList) {
                        fileList.add(childFile);
                    }
                }
            }
        }

        return usage;
    }

    private String encrypt(final String data, final String key) {
        return TripleDes.encode(data, key);
    }

    private String decrypt(final String encryptData, final String key) {
        return TripleDes.decode(encryptData, key);
    }


    private String hash(final String data) {
        try {
            MessageDigest alga = MessageDigest.getInstance("MD5");
            alga.update(data.getBytes(Charset.defaultCharset()));
            String sign = Util.bytes2HexStr(alga.digest());
            alga.reset();
            return sign;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "wronghash";
    }

    private void writeHash(final String host, final String path, final String key,
            final String hash) {
        SharedPreferences sp = getH5DataSharedPreferences();
        String rootStr = sp.getString(host, null);
        JSONObject root = null;
        if (rootStr != null) {
            try {
                root = new JSONObject(rootStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (root == null) {
            root = new JSONObject();
        }
        JSONObject pathTree = root.optJSONObject(path);
        if (pathTree == null) {
            pathTree = new JSONObject();
            try {
                root.put(path, pathTree);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            pathTree.put(key, hash);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SharedPreferences.Editor editor = sp.edit().putString(host, root.toString());
        if (Build.VERSION.SDK_INT >= 9) {
            editor.apply();
        } else {
            editor.apply();
        }
    }

    private String readHash(final String host, final String path, final String key) {
        SharedPreferences sp = getH5DataSharedPreferences();
        String rootStr = sp.getString(host, null);
        if (rootStr != null) {
            try {
                JSONObject root = new JSONObject(rootStr);
                JSONObject pathTree = root.optJSONObject(path);
                if (pathTree != null) {
                    String hash = pathTree.optString(key);
                    if (hash.length() > 0) {
                        return hash;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void deleteHash(final String host, final String path, final String key) {
        SharedPreferences sp = getH5DataSharedPreferences();
        String rootStr = sp.getString(host, null);
        if (rootStr != null) {
            try {
                JSONObject root = new JSONObject(rootStr);
                JSONObject pathTree = root.optJSONObject(path);
                if (pathTree != null) {
                    pathTree.remove(key);
                    SharedPreferences.Editor editor = sp.edit().putString(host, root.toString());
                    if (Build.VERSION.SDK_INT >= 9) {
                        editor.apply();
                    } else {
                        editor.apply();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteHash(final String host, final String path) {
        SharedPreferences sp = getH5DataSharedPreferences();
        String rootStr = sp.getString(host, null);
        if (rootStr != null) {
            try {
                JSONObject root = new JSONObject(rootStr);
                root.remove(path);
                SharedPreferences.Editor editor = sp.edit().putString(host, root.toString());
                if (Build.VERSION.SDK_INT >= 9) {
                    editor.apply();
                } else {
                    editor.apply();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteHash(final String host) {
        SharedPreferences sp = getH5DataSharedPreferences();
        SharedPreferences.Editor editor = sp.edit().remove(host);
        if (Build.VERSION.SDK_INT >= 9) {
            editor.apply();
        } else {
            editor.apply();
        }
    }

    private SharedPreferences h5sp;

    private SharedPreferences getH5DataSharedPreferences() {
        if (h5sp == null) {
            String spKey = KEY_H5DATA + '_' + mRuntime.getAccount();
            h5sp = mRuntime.context.getSharedPreferences(spKey, Context.MODE_PRIVATE);
        }
        return h5sp;
    }

    private static boolean isParentDomain(String parentDomain, String childDomain) {
        if (childDomain == null) {
            return false;
        }
        return (childDomain.equals(parentDomain) || (parentDomain.indexOf(".") > 0
                && childDomain.endsWith("." + parentDomain)));
    }

}
