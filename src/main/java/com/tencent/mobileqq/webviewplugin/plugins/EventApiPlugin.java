package com.tencent.mobileqq.webviewplugin.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.tencent.mobileqq.webviewplugin.Util;
import com.tencent.mobileqq.webviewplugin.WebViewPlugin;
import com.tencent.mobileqq.webviewplugin.util.LogUtil;
import com.tencent.smtt.sdk.WebView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 此插件需要在AndroidManifest.xml增加以下内容(其中$YOUR PERMISSION$自定)
 * <pre>
 * &lt;uses-permission android:name="$YOUR PERMISSION$" /&gt;
 * &lt;permission android:name="$YOUR PERMISSION$" android:protectionLevel="signature" /&gt;
 *
 * &lt;application ...&gt;
 *     &lt;meta-data android:name="ak_webview_sdk_broadcast_permission" android:value="$YOUR PERMISSION$" /&gt;
 * </pre>
 * author:pel
 */
//这个event事件不支持跨进程通信,所以会存在跨进程间事件没有收到
public class EventApiPlugin extends WebViewPlugin {

    private static final String ACTION_WEBVIEW_DISPATCH_EVENT = "com.qzone.qqjssdk.action.ACTION_WEBVIEW_DISPATCH_EVENT";
    private static final String KEY_BROADCAST = "broadcast";
    private static final String KEY_UNIQUE = "unique";
    private static final String KEY_EVENT = "event";
    private static final String KEY_DATA = "data";
    private static final String KEY_DOMAINS = "domains";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_ECHO = "echo";
    private static final String KEY_URL = "url";
    private static final String KEY_OPTIONS = "options";
    private String mUniqueMark;
    private boolean mReceiverRegistered = false;//是否注册了全局的监听器
    private static BroadcastReceiver sBroadcastReceiver = null;
    private static boolean sGlobalReceiverRegistered = false;
    private static HashSet<WeakReference<EventApiPlugin>> sRegisteredModules = null;
    private WeakReference<EventApiPlugin> mRef;
    private Map<String, String> mEvents;
    private static final String TAG = "EventApiPlugin";

    @Override
    protected void onCreate() {
        super.onCreate();
        mUniqueMark = getUniqueMark();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.i(TAG, "[onDestroy]");
        if (mReceiverRegistered) {//为了提高效率,这里如果没有注册广播就不去移除
            sRegisteredModules.remove(mRef);
            if (sRegisteredModules.size() == 0) {
                sGlobalReceiverRegistered = false;
                mRuntime.getActivity().getApplication().unregisterReceiver(sBroadcastReceiver);
                LogUtil.i(TAG, "[onDestroy] unregisterReceiver");
            }
        }
    }

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if (KEY_EVENT.equals(pkgName)) {
            if ("init".equals(method)) {
                // 触发插件初始化, 不做其它事情
                return true;
            } else if ("dispatchEvent".equals(method) && args.length == 1) {
                doDispatchEventMethod(args);
            } else if ("addEventListener".equals(method) && args.length == 1) {
                doAddEventListenerMethod(args);
            } else if ("removeEventListener".equals(method) && args.length == 1) {
                doRemoveEventListener(args);
            }
            return true;
        }
        return false;
    }

    private void doDispatchEventMethod(String[] args) {
        try {
            WebView webview = mRuntime.getWebView();
            if (webview == null) {
                return;
            }
            JSONObject json = new JSONObject(args[0]);
            String event = json.optString(KEY_EVENT);
            if (TextUtils.isEmpty(event)) {
                LogUtil.w(TAG, "param event is requested");
                return;
            }
            JSONObject data = json.optJSONObject(KEY_DATA);
            JSONObject options = json.optJSONObject(KEY_OPTIONS);
            boolean echo = true;
            boolean broadcast = true;
            ArrayList<String> domains = new ArrayList<String>();
            String currentUrl = webview.getUrl();
            if (options != null) {
                echo = options.optBoolean(KEY_ECHO, true);
                broadcast = options.optBoolean(KEY_BROADCAST, true);
                JSONArray d = options.optJSONArray(KEY_DOMAINS);
                if (d != null) {
                    for (int i = 0, len = d.length(); i < len; ++i) {
                        String domain = d.optString(i);
                        if (!TextUtils.isEmpty(domain)) {
                            domains.add(domain);
                        }
                    }
                }
            }
            JSONObject source = new JSONObject();
            source.put(KEY_URL, currentUrl);
            if (domains.size() == 0 && currentUrl != null) {
                Uri uri = Uri.parse(currentUrl);
                if (uri.isHierarchical()) {
                    domains.add(uri.getHost());
                }
            }
            Intent intent = new Intent(ACTION_WEBVIEW_DISPATCH_EVENT);
            intent.putExtra(KEY_BROADCAST, broadcast);
            intent.putExtra(KEY_UNIQUE, mUniqueMark);
            intent.putExtra(KEY_EVENT, event);
            if (data != null) {
                intent.putExtra(KEY_DATA, data.toString());
            }
            intent.putStringArrayListExtra(KEY_DOMAINS, domains);
            intent.putExtra(KEY_SOURCE, source.toString());
            mRuntime.context.sendBroadcast(intent, getPermission(mRuntime.context));
            if (echo) {
                dispatchJsEvent(event, data, source);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void doAddEventListenerMethod(String[] args) {
        JSONObject jsonObject = getParams(args[0]);
        if (jsonObject == null) {
            return;
        }
        String eventName = jsonObject.optString(KEY_EVENT);
        if (TextUtils.isEmpty(eventName)) {
            LogUtil.e(TAG, "[addEventListener] event is empty !!!!");
            return;
        }
        String callBack = jsonObject.optString(KEY_CALLBACK);
        if (TextUtils.isEmpty(callBack)) {
            LogUtil.e(TAG, "[addEventListener] callback is empty!");
            return;
        }
        ensureBroadcastReceiver();
        if (mEvents == null) {
            mEvents = new HashMap<>();
        }
        mEvents.put(eventName, callBack);
    }

    private void doRemoveEventListener(String[] args) {
        JSONObject jsonObject = getParams(args[0]);
        if (jsonObject == null) {
            return;
        }
        String eventName = jsonObject.optString(KEY_EVENT);
        if (TextUtils.isEmpty(eventName)) {
            LogUtil.e(TAG, "event is empty !!!!");
            return;
        }
        if (mEvents != null) {
            mEvents.remove(eventName);
        }
    }

    private static void createGlobalBroadcastReceiver() {
        if (sBroadcastReceiver == null) {
            sBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    LogUtil.i(TAG, "onReceive");
                    for (WeakReference<EventApiPlugin> ref : sRegisteredModules) {
                        EventApiPlugin module = ref.get();
                        if (module != null) {
                            module.onReceive(context, intent);
                        }
                    }
                }
            };
        }
        if (sRegisteredModules == null) {
            sRegisteredModules = new HashSet<>();
        }
    }

    private void ensureBroadcastReceiver() {
        if (mReceiverRegistered) {
            return;
        }
        createGlobalBroadcastReceiver();

        if (!sGlobalReceiverRegistered) {
            sGlobalReceiverRegistered = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(EventApiPlugin.ACTION_WEBVIEW_DISPATCH_EVENT);
            mRuntime.getActivity().getApplication().registerReceiver(sBroadcastReceiver, filter);
        }

        mReceiverRegistered = true;
        mRef = new WeakReference<>(this);
        sRegisteredModules.add(mRef);
    }

    @Nullable
    private JSONObject getParams(@NonNull String jsonStr) {
        JSONObject json = null;
        try {
            json = new JSONObject(jsonStr);
            return json;
        } catch (JSONException e) {
            LogUtil.e(TAG, "[getParams] " + e);
        }
        return null;
    }

    protected void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        boolean broadcast = intent.getBooleanExtra(KEY_BROADCAST, true);
        if (!broadcast) {
            LogUtil.i(TAG, "[onReceive] is not broadcast");
            return;
        }

        String unique = intent.getStringExtra(KEY_UNIQUE);
        if (unique != null && unique.equals(mUniqueMark)) { //不接收自己发的
            LogUtil.i(TAG, "[onReceive] unique is equals");
            return;
        }

        String event = intent.getStringExtra(KEY_EVENT);
        if (TextUtils.isEmpty(event)) {
            LogUtil.e(TAG, "[onReceive] event is empty!!");
            return;
        }

        String dataStr = intent.getStringExtra(KEY_DATA);
        JSONObject data = null;
        if (dataStr != null) {
            try {
                data = new JSONObject(dataStr);
            } catch (JSONException e) {
                LogUtil.e(TAG, "[onReceive] json parse error! " + dataStr);
                return;
            }
        }

        ArrayList<String> domains = intent.getStringArrayListExtra(KEY_DOMAINS);
        if (domains == null) {
            return;
        }

        String sourceStr = intent.getStringExtra(KEY_SOURCE);
        JSONObject source = null;
        if (sourceStr != null) {
            try {
                source = new JSONObject(sourceStr);
            } catch (JSONException e) {
                return;
            }
        }

        WebView webview = mRuntime.getWebView();
        if (webview == null) {
            return;
        }

        String currentUrl = webview.getUrl();
        if (currentUrl == null) {
            return;
        }
        LogUtil.i(TAG, "module onReceive");

        Uri uri = Uri.parse(currentUrl);
        String host = uri.getHost();
        for (int i = 0, len = domains.size(); i < len; ++i) {
            if (Util.isDomainMatch(domains.get(i), host)) {
                dispatchJsEvent(event, data, source);
                callBackEvent(event, data, source);//通知
                break;
            }
        }
    }

    private void callBackEvent(String eventName, JSONObject data, JSONObject source) {
        if (mEvents != null && !TextUtils.isEmpty(eventName) && mEvents.containsKey(eventName)) {
            JSONObject result = new JSONObject();
            LogUtil.i(TAG, "module callBackEvent");
            try {
                result.put(EventApiPlugin.KEY_EVENT, eventName);
                result.put(EventApiPlugin.KEY_DATA, data);
                result.put(EventApiPlugin.KEY_SOURCE, source);
                callJs(mEvents.get(eventName), getResult(0, "回调数据成功", result));
            } catch (JSONException e) {
                LogUtil.e(TAG, "[callBackEvent] " + e);
                callJs(mEvents.get(eventName), getResult(-1, "json解析失败", result));
            }
        }
    }

    private String getUniqueMark() {
        if (mUniqueMark == null) {
            mUniqueMark =
                    System.currentTimeMillis() + String.valueOf((int) (Math.random() * 1000000));
        }
        return mUniqueMark;
    }

    /**
     * 向webveiw发广播
     *
     * @param event 和web侧约定的事件名
     * @param data 消息体, 可以为null
     * @param domains 能够接收此广播的域名, 支持通配符, 不能为null, 例如["*.qq.com, m.qzone.com"], ["*"]
     * @param referer 发送此广播的页面url, web侧用于检查来源, 可以为null
     */
    public static void sendWebBroadcast(Context context, String event, JSONObject data,
            ArrayList<String> domains, String referer) {
        Intent intent = new Intent(ACTION_WEBVIEW_DISPATCH_EVENT);
        intent.putExtra("event", event);
        if (data != null) {
            intent.putExtra("data", data.toString());
        }
        intent.putStringArrayListExtra("domains", domains);
        JSONObject source = new JSONObject();
        try {
            source.put("url", referer);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        intent.putExtra("source", source.toString());
        context.sendBroadcast(intent);
        LogUtil.i(TAG, "sendWebBroadcast");
    }

    private static String sPermission = null;

    private static String getPermission(Context context) {
        if (sPermission != null) {
            return sPermission;
        }
        String permission = null;
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            permission = appInfo.metaData.getString("ak_webview_sdk_broadcast_permission");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(permission)) {
            LogUtil.e("ak_webview_sdk",
                    "\"ak_webview_sdk_broadcast_permission\" meta data not found");
            return null;
        }
        return sPermission = permission;
    }

}
