package com.tencent.mobileqq.webviewplugin;


import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import com.tencent.mobileqq.webviewplugin.util.LogUtil;
import com.tencent.smtt.sdk.WebView;
import java.util.Map;
import org.json.JSONObject;

public class WebViewPlugin {

    protected static final String TAG = "QQJSSDK." + WebViewPlugin.class.getSimpleName() + ".";

    public static final int PLUGIN_START_REQUEST_CODE = 10000;
    public static final String KEY_CALLBACK = "callback";

    public static final int EVENT_LOAD_START = 0;          // 开始load页面 回调
    public static final int EVENT_LOAD_FINISH = 1;      // load页面结束回调
    public static final int EVENT_LOAD_ERROR = 2;       // 加载页面失败回调
    public static final int EVENT_ACTIVITY_CREATE = 3;
    public static final int EVENT_ACTIVITY_DESTROY = 4;
    public static final int EVENT_ACTIVITY_RESUME = 5;
    public static final int EVENT_ACTIVITY_PAUSE = 6;
    /**
     * activity finish 之前通知，有些业务要在finish之前做些事情
     */
    public static final int EVENT_BEFORE_ACTIVITY_FINISH = 7;
    /**
     * activity finish 之后的通知，让业务决定一些退出动画
     */
    public static final int EVENT_AFTER_ACTIVITY_FINISH = 8;
    public static final int EVENT_ACTIVITY_ONRESULT = 9;
    //离线包下载完成
    public static final int EVENT_OFFLINE_UPDATE = 10;
    public static final int EVENT_LOAD_RESOURCE = 11;
    public static final int EVENT_GO_BACK = 12;
    public static final int EVENT_FORWARD = 13;
    public static final int EVENT_SWITCH_URL = 14;


    protected static final int RET_CODE_SUCCESS = 0;
    protected static final int RET_CODE_FAIL = -1;
    protected static final int RET_CODE_CANCEL = -2;


    /**
     * 表情-动态表情-通知界面重新制作
     */
    public static final int EVENT_DEMOJI_ACTIVITYRESULT_REMAKE = 15;
    /**
     * 主题变更
     */
    public static final int EVENT_THEME_POSTCHANGED = 17;
    /**
     * 挂件主页onnewintent特殊处理，不能走QQbrowser
     */
    public static final int EVENT_AVATAR_PENDANT_HOME_ONNEWINTENT = 20;

    public static final int EVENT_BEFORE_LOAD = 21;

    public static final int EVENT_TITLE_BAR_TOUCH = 22;
    public static final int EVENT_MENU_BUTTON_CLICK = 23;

    public static final int EVENT_FRAGMENT_RESUME = 24;
    public static final int EVENT_FRAGMENT_PAUSE = 25;
    public static final int EVENT_FRAGMENT_DESTROY = 26;
    public static final int EVENT_FRAGMENT_VISIBLE = 27;
    public static final int EVENT_FRAGMENT_INVISIBLE = 28;

    //设置当前页面的商业化类型
    public static final int EVENT_SET_COMMERCIAL_TYPE = 29;

    public static final String KEY_ERROR_CODE = "errorCode";
    public static final String KEY_TARGET = "target";
    public static final String KEY_URL = "url";
    public static final String KEY_URL_LIST = "url_list";
    public static final int TARGET_LEFT_VIEW = 1; //左上角返回按钮
    public static final int TARGET_NAV_BACK = 2; //导航栏返回按钮
    public static final int TARGET_SYS_BACK = 3; //虚拟(物理)返回按钮
    protected boolean isDestroy = false;

    private JSONObject mResult; //子类回调数据模版
    protected static final String NULL_PARAM = "{}";

    protected String namespace = "";

    public void setNameSapce(String namespace) {
        this.namespace = namespace;
    }

    public String getNameSpace() {
        return namespace != null ? namespace : "";
    }

    protected JSONObject getResult(int code, String msg, JSONObject data) {
        if (mResult == null) {
            mResult = new JSONObject();
        }
        try {
            //{code:code,msg:msg,data:data}
            mResult.put("code", code);
            mResult.put("msg", msg);
            mResult.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mResult;
    }

    protected JSONObject getResult(JSONObject data) {
        if (mResult == null) {
            mResult = new JSONObject();
        }
        try {
            //{\"code\" : 0, \"msg\" : \"\", \"data\" : {}}
            mResult.put("code", 0);
            mResult.put("msg", "");
            mResult.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mResult;
    }


    public DefaultPluginRuntime mRuntime;

    final void initRuntime(DefaultPluginRuntime runtime) {
        mRuntime = runtime;
    }

    /**
     * 在webview中执行js <br/> 无参数回调需要使用 callJs(func, "") <br/> 优先使用 mqq.execGlobalCallback(func,
     * ...args) 回调, 没有则执行 func(...args)
     */
    public void callJs(String func, String... args) {
        if (isDestroy) {
            LogUtil.d(TAG + "callJs", "WebViewPlugin：plugin has destory");
            return;
        }
        WebView webView = mRuntime.getWebView();
        LogUtil.i(TAG + "callJs", "WebViewPlugin：webview ready to call js...func=" + func);
        Util.callJs(webView, func, args);
    }

    /**
     * ！！！！！优先使用本接口 在webview中执行js <br/> 无参数回调需要使用 callJs(func, data) <br/>
     */
    public void callJs(String func, JSONObject data) {
        if (isDestroy) {
            LogUtil.d(TAG + "callJs", "WebViewPlugin： plugin has destory");
            return;
        }
        WebView webView = mRuntime.getWebView();
        LogUtil.i(TAG + "callJs", "WebViewPlugin：webview ready to call js...func=" + func);
        Util.callJs(webView, func, data);
    }

    public void dispatchJsEvent(String eventName, JSONObject data, JSONObject source) {
        dispatchJsEvent(eventName, data.toString(), source.toString());
    }

    public void dispatchJsEvent(String eventName, String data, String source) {
        if (isDestroy) {
            return;
        }
        WebView webView = mRuntime.getWebView();
        String script = new StringBuilder()
                .append("window.mqq && mqq.execEventCallback && mqq.execEventCallback(")
                .append(Util.toJsString(eventName))
                .append(",")
                .append(data)
                .append(",")
                .append(source)
                .append(");")
                .toString();
        Util.callJs(webView, script);
    }

    protected void onCreate() {
    }

    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        return false;
    }

    protected boolean handleSchemaRequest(String url, String scheme) {
        return false;
    }

    protected boolean handleEvent(String url, int type, Map<String, Object> info) {
        return false;
    }

    protected Object handleEvent(String url, int type) {
        return null;
    }

    protected void onDestroy() {
        isDestroy = true;
    }

    /**
     * @param requestCode 注意这里的byte是128到255时候的判断不能简单的==，因为255会变成-1
     */
    public void onActivityResult(Intent intent, byte requestCode, int resultCode) {
        //dosomething, should override in subclass
    }

    /**
     * @param requestCode 0-255,不要用负数
     */
    public void startActivityForResult(Intent intent, byte requestCode) {
        if (mRuntime == null) {
            return;
        }
        Activity activity = mRuntime.getActivity();
        Fragment fragment = mRuntime.getFragment();
        if (activity instanceof WebViewPluginContainer) {
            ((WebViewPluginContainer) activity).pluginStartActivityForResult(
                    this, intent, requestCode);
        } else if (fragment != null && fragment instanceof WebViewPluginContainer) {
            ((WebViewPluginContainer) fragment).pluginStartActivityForResult(
                    this, intent, requestCode);
        } else {
            throw new RuntimeException(
                    "the container Activity must implements WebViewPluginContainer");
        }
    }

    /**
     * Default implement for {@link WebViewPluginContainer#pluginStartActivityForResult}, should
     * override {@link Activity#onActivityResult} to dispatch the "result", and tranform the
     * "requestCode" follow the same rule
     *
     * @see WebViewPlugin#defaultPluginOnActivityResult
     */
    public static int defaultPluginStartActivityForResult(Activity activity, WebViewPlugin plugin,
            Intent intent, byte requestCode, WebViewPluginEngine engine) {
        int actualCode = -1;
        if (engine != null && plugin != null) {
            int index = engine.getPluginIndex(plugin);
            actualCode = index * 1000 + Util.byte2UnsignedInt(requestCode)
                    + PLUGIN_START_REQUEST_CODE;//这里不处理的话,255会变成-1,需要做个 | 转换成没符号的整数型
            activity.startActivityForResult(intent, actualCode);
        }
        return actualCode;
    }

    /**
     * For example
     * <pre>
     * &#64;Override
     * protected void onActivityResult(int requestCode, int resultCode, Intent data) {
     *     super.onActivityResult(requestCode, resultCode, data);
     *     if (WebViewPlugin.defaultPluginOnActivityResult(mPluginEngine, requestCode, resultCode, data)) {
     *         return;
     *     }
     * }
     * </pre>
     *
     * @see WebViewPlugin#defaultPluginStartActivityForResult
     */
    public static boolean defaultPluginOnActivityResult(WebViewPluginEngine engine, int requestCode,
            int resultCode, Intent data) {
        if (requestCode > PLUGIN_START_REQUEST_CODE) { // 插件requestCode段,
            // 派发给相应插件处理
            int index = (requestCode - PLUGIN_START_REQUEST_CODE) / 1000;
            byte code = (byte) ((requestCode - PLUGIN_START_REQUEST_CODE) % 1000);

            if (engine != null) {
                WebViewPlugin plugin = engine.getPluginByIndex(index);

                if (plugin != null) {
                    plugin.onActivityResult(data, code, resultCode);
                    return true;
                }
            }
        }
        return false;
    }

}
