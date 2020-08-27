package com.tencent.mobileqq.webviewplugin.plugins;

import android.content.Intent;
import android.text.TextUtils;
import com.tencent.mobileqq.webviewplugin.WebViewPlugin;
import com.tencent.mobileqq.webviewplugin.util.PackageUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by pelli on 2015/2/11.
 */
public class AppApiPlugin extends WebViewPlugin {

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        boolean isHandleMethod = false;

        if ("isAppInstalled".equals(method) && args.length == 1) {
            isHandleMethod = doIsAppInstalled(args);
        } else if ("launchApp".equals(method) && args.length == 1) {
            isHandleMethod = doLaunchAppMethod(args);
        } else if ("checkAppInstalled".equals(method) && args.length == 1) {
            isHandleMethod = doCheckAppInstalledMethod(args);
        } else if ("checkAppInstalledBatch".equals(method) && args.length == 1) {
            isHandleMethod = doCheckAppInstalledBatchMethod(args);
        } else if ("isAppInstalledBatch".equals(method) && args.length == 1) {
            isHandleMethod = doIsAppInstalledBatchMethod(args);
        }

        return isHandleMethod;
    }

    private boolean doIsAppInstalled(String[] args) {
        try {
            JSONObject json = new JSONObject(args[0]);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                try {
                    JSONObject res = new JSONObject();
                    res.put("result", PackageUtil
                            .isAppInstalled(mRuntime.context, json.getString("name").trim()));
                    callJs(callback, getResult(res));
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                callJs(callback, "" + false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }


    private boolean doLaunchAppMethod(String[] args) {
        try {
            JSONObject json = new JSONObject(args[0]);
            String name = json.getString("name");
            Intent intent = mRuntime.context.getPackageManager()
                    .getLaunchIntentForPackage(name.trim());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mRuntime.context.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    private boolean doCheckAppInstalledMethod(String[] args) {
        try {
            JSONObject json = new JSONObject(args[0]);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                try {
                    JSONObject res = new JSONObject();
                    res.put("result", PackageUtil
                            .checkAppInstalled(mRuntime.context, json.getString("name").trim()));
                    callJs(callback, getResult(res));
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean doCheckAppInstalledBatchMethod(String[] args) {
        try {
            JSONObject json = new JSONObject(args[0]);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                try {
                    JSONObject res = new JSONObject();
                    res.put("result", PackageUtil.checkAppInstalledBatch(mRuntime.context,
                            json.getString("name").trim()));
                    callJs(callback, getResult(res));
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean doIsAppInstalledBatchMethod(String[] args) {
        try {
            JSONObject json = new JSONObject(args[0]);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                try {
                    JSONObject res = new JSONObject();
                    res.put("result", PackageUtil
                            .isAppInstalledBatch(mRuntime.context, json.getString("name").trim()));
                    callJs(callback, getResult(res));
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}
