package com.tencent.mobileqq.webviewplugin;

import android.text.TextUtils;
import com.tencent.mobileqq.webviewplugin.util.LogUtil;
import com.tencent.smtt.sdk.WebView;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class WebViewPluginEngine {

    private static final String TAG = "QQJSSDK." + WebViewPluginEngine.class.getSimpleName() + ".";
    protected List<WebViewPlugin> pluginList;
    protected Map<String, WebViewPlugin> pluginHashMap;
    private DefaultPluginRuntime mRuntime;
    private String mAppid;

    public WebViewPluginEngine(DefaultPluginRuntime runtime) {
        pluginList = new ArrayList<WebViewPlugin>();
        pluginHashMap = new HashMap<String, WebViewPlugin>();
        mRuntime = runtime;
        for (int i = 0; i < WebViewPluginConfig.list.length; ++i) {
            PluginInfo pluginInfo = WebViewPluginConfig.list[i];
            if (!pluginInfo.async) {
                pluginList.add(createPlugin(pluginInfo));
            }
        }
    }

    public WebViewPluginEngine(PluginInfo[] list, DefaultPluginRuntime runtime) {
        pluginList = new ArrayList<WebViewPlugin>();
        pluginHashMap = new HashMap<String, WebViewPlugin>();
        mRuntime = runtime;
        for (int i = 0; i < list.length; ++i) {
            PluginInfo pluginInfo = list[i];
            if (!pluginInfo.async) {
                pluginList.add(createPlugin(pluginInfo));
            }
        }
    }

    /**
     * 预加载SDK插件
     *
     * @param appid 应用APPID，假如第三方APP想使用分享能力，必须用这个构造器
     */
    public WebViewPluginEngine(PluginInfo[] list, DefaultPluginRuntime runtime, String appid) {
        pluginList = new ArrayList<WebViewPlugin>();
        pluginHashMap = new HashMap<String, WebViewPlugin>();
        mRuntime = runtime;
        mAppid = appid;
        for (int i = 0; i < list.length; ++i) {
            PluginInfo pluginInfo = list[i];
            if (!pluginInfo.async) {
                pluginList.add(createPlugin(pluginInfo));
            }
        }

    }

    /**
     * 预加载SDK插件
     */
    public void preInitPlugin(String[] list) {
        if (list == null || list.length == 0) {
            return;
        }
        for (int i = 0; i < list.length; ++i) {
            PluginInfo pluginInfo = WebViewPluginConfig.map.get(list[i]);
            if (pluginInfo != null) {
                WebViewPlugin plugin = createPlugin(pluginInfo);
                if (plugin != null) {
                    pluginList.add(plugin);
                }
            }
        }
    }

    /**
     * 第三方APP批量插入插件
     */
    public void insertPlugin(PluginInfo[] list) {

        if (list == null || list.length == 0) {
            return;
        }

        for (int i = 0; i < list.length; ++i) {
            WebViewPlugin plugin = createPlugin(list[i]);
            if (plugin == null) {
                return;
            }
            pluginList.add(plugin);
            if (plugin instanceof MultiNameSpacePluginCompact) {
                String[] namespaces = ((MultiNameSpacePluginCompact) plugin).getMultiNameSpace();
                if (namespaces != null && namespaces.length > 0) {
                    for (String namespace : namespaces) {
                        if (pluginHashMap.containsKey(namespace)) {
                            LogUtil.d(TAG + " insertPlugin",
                                    "insertPlugin:namespace " + namespace + " already exists!");
                        } else {
                            if (!TextUtils.isEmpty(namespace)) {
                                pluginHashMap.put(namespace, plugin);
                            }
                        }
                    }
                }
            } else {
                if (pluginHashMap.containsKey(list[i].namespace)) {
                    LogUtil.d(TAG + " insertPlugin",
                            "insertPlugin:namespace " + list[i].namespace + " already exists!");
                } else {
                    pluginHashMap.put(list[i].namespace, plugin);
                }
            }
        }
    }

    private WebViewPlugin createPlugin(PluginInfo pluginInfo) {
        try {
            WebViewPlugin plugin = (WebViewPlugin) Util.getConstructor(
                    pluginInfo.classType).newInstance();
            initPlugin(plugin);
            return plugin;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void initPlugin(WebViewPlugin plugin) {
        plugin.initRuntime(mRuntime);
        plugin.onCreate();
    }

    /**
     * @param pluginList must be not null
     * @throws java.lang.NullPointerException if pluginList is null
     */
    public WebViewPluginEngine(DefaultPluginRuntime runtime, List<WebViewPlugin> pluginList) {
        pluginHashMap = new HashMap<String, WebViewPlugin>();
        this.pluginList = pluginList;
        mRuntime = runtime;
        for (int i = 0; i < pluginList.size(); ++i) {
            initPlugin(pluginList.get(i));
        }
    }

    public boolean handleRequest(String url) {
        WebView webview = mRuntime.getWebView();
        if (TextUtils.isEmpty(url) || webview == null) {
            LogUtil.d(TAG, " TextUtils.isEmpty(url) || webview == null ");
            return false;
        }
        int index = url.indexOf(":");
        String scheme = index > 0 ? url.substring(0, index) : "";
        for (int i = 0; i < pluginList.size(); ++i) {
            WebViewPlugin plugin = pluginList.get(i);
            if (plugin.handleSchemaRequest(url, scheme)) {
                return true;
            }
        }
        LogUtil.d(TAG, " no plugin handler this request ");
        return false;
    }

    public boolean handleEvent(String url, int type, Map<String, Object> info) {
        if (pluginList == null) {
            return false;
        }
        for (int i = 0; i < pluginList.size(); ++i) {
            WebViewPlugin plugin = pluginList.get(i);
            if (plugin == null) {
                continue;
            }
            if (plugin.handleEvent(url, type, info)) {
                return true;
            }
        }
    /*for (Map.Entry<String, WebViewPlugin> entry : pluginHashMap.entrySet()) {
        WebViewPlugin plugin = entry.getValue();
        if (plugin.handleEvent(url, type, info)) {
            return true;
        }
    }*/
        return false;
    }

    public Object handleEvent(String url, int type) {
        Object o;
        for (int i = 0; i < pluginList.size(); ++i) {
            WebViewPlugin plugin = pluginList.get(i);
            if ((o = plugin.handleEvent(url, type)) != null) {
                return o;
            }
        }
    /*for (Map.Entry<String, WebViewPlugin> entry : pluginHashMap.entrySet()) {
        WebViewPlugin plugin = entry.getValue();
        if ((o = plugin.handleEvent(url, type)) != null) {
            return o;
        }
    }*/
        return null;
    }

    public boolean handleError(String url, int type, int errorCode) {
        Map<String, Object> info = new HashMap<String, Object>();
        info.put(WebViewPlugin.KEY_ERROR_CODE, errorCode);
        for (int i = 0; i < pluginList.size(); ++i) {
            WebViewPlugin plugin = pluginList.get(i);
            if (plugin.handleEvent(url, type, info)) {
                return true;
            }
        }
        return false;
    }

    public int getPluginIndex(WebViewPlugin plugin) {
        if (plugin != null && pluginList != null) {
            for (int i = 0, len = pluginList.size(); i < len; ++i) {
                if (pluginList.get(i) == plugin) {
                    return i;
                }
            }
        }
        return -1;
    }

    public WebViewPlugin getPluginByIndex(int index) {
        if (pluginList == null || index < 0 || index >= pluginList.size()) {
            return null;
        }

        return pluginList.get(index);
    }

    public WebViewPlugin getPluginByClass(Class<?> clazz) {
        for (WebViewPlugin plugin : pluginList) {
            if (plugin.getClass() == clazz) {
                return plugin;
            }
        }
        return null;
    }

    public void onDestroy() {
        if (pluginList == null) {
            return;
        }
        for (WebViewPlugin plugin : pluginList) {
            plugin.onDestroy();
        }
        pluginList.clear();
        pluginHashMap.clear();
    }

    private boolean handleJsRequest(WebViewPlugin plugin, String url, String objectName,
            String methodName, String[] args) {
        try {
            if (plugin.handleJsRequest(url, objectName, methodName,
                    args)) {
                LogUtil.d(TAG + "handleJsRequest", " 插件处理完 ");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public boolean canHandleJsRequest(String url) {
        WebView webview = mRuntime.getWebView();
        if (webview == null) {
            LogUtil.d(TAG + "canHandleJsRequest", "webview is null");
            return false;
        }
        if (url != null && url.startsWith("jsbridge://")) {
            String[] paths = (url + "/#").split("/");
            String[] args;
            if (paths.length < 5) {
                // 非法jsbridge请求，直接吃掉
                return true;
            }
            String objectName = paths[2];
            String methodName; // = paths[3];
            boolean isOldVersion = true;
            long sn = -1;
            if (paths.length == 5) { // new version
                isOldVersion = false;
                // jsbridge://namespace/method?p=test&p2=xxx&p3=yyy#sn
                String[] split = paths[3].split("#");
                if (split.length > 1) {
                    try {
                        sn = Integer.parseInt(split[1]);
                    } catch (NumberFormatException e) {
                        // 非法jsbridge请求，直接吃掉
                        return true;
                    }
                }
                split = split[0].split("\\?");
                if (split.length > 1) {
                    args = split[1].split("&");
                    for (int i = 0, l = args.length; i < l; ++i) {
                        int pos = args[i].indexOf('=');
                        if (pos != -1) {
                            args[i] = URLDecoder.decode(args[i].substring(pos + 1));
                        } else {
                            args[i] = "";
                        }
                    }
                } else {
                    args = new String[0];
                }
                methodName = split[0];
            } else {
                // jsbridge://objname/method/sn/arg0/arg1/arg2
                // 移除到只剩method及后续的arg
                methodName = paths[3];
                try {
                    sn = Long.parseLong(paths[4]);
                } catch (Exception e) {
                    // 非法jsbridge请求，直接吃掉
                    return true;
                }

                int copyLength = paths.length - 6;
                args = new String[copyLength];
                System.arraycopy(paths, 5, args, 0, copyLength);
                // decode url
                for (int i = 0, l = args.length; i < l; i++) {
                    args[i] = URLDecoder.decode(args[i]);
                }
            }

            AuthorizeConfig authCfg = AuthorizeConfig
                    .getInstance(mRuntime.getActivity().getApplicationContext());
            String currentUrl = webview.getUrl();
            if (!authCfg.hasCommandRight(currentUrl, objectName + "."
                    + methodName)) {
                LogUtil.d(TAG + "canHandleJsRequest", " !authCfg.hasCommandRight ");
                return false;
            }
            WebViewPlugin plugin = null;
            if (pluginHashMap.containsKey(objectName)) {
                plugin = pluginHashMap.get(objectName);
                LogUtil.d(TAG + "canHandleJsRequest", " 内存有懒加载的插件处理这个请求 ");
            } else if (WebViewPluginConfig.map.containsKey(objectName)) {
                PluginInfo pluginInfo = WebViewPluginConfig.map.get(objectName);
                plugin = createPlugin(pluginInfo);
                pluginHashMap.put(objectName, plugin);
                pluginList.add(plugin); // 懒加载创建的插件, 加入插件列表
                LogUtil.d(TAG + "canHandleJsRequest", " 内存有必要的插件处理这个请求 ");
            }
            if (plugin == null) {
                LogUtil.d(TAG + "canHandleJsRequest", " 暂时没有插件处理这个请求 遍历传入的插件列表");
                for (int i = 0; i < pluginList.size(); ++i) {
                    plugin = pluginList.get(i);
                    if (handleJsRequest(plugin, url, objectName, methodName, args)) {
                        LogUtil.d(TAG + "canHandleJsRequest", " 找到一个能处理这个请求 ");
                        pluginHashMap.put(objectName, plugin); // 命名空间匹配成功,放入map, 下次直接访问
                        return true;
                    }
                }
            } else {
                if (handleJsRequest(plugin, url, objectName,
                        methodName, args)) {

                    return true;
                }
            }
            // no such method
            if (!isOldVersion) {
                String callback = null;
                if (args.length > 0 && args[0].startsWith("{")) {
                    try {
                        JSONObject json = new JSONObject(args[0]);
                        callback = json.optString("callback");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                if (TextUtils.isEmpty(callback) && sn != -1) {
                    callback = Long.toString(sn);
                }
            } /*else{
                if (sn != -1) {
                    // webview.callJs("window.JsBridge&&JsBridge.callback(" + sn
                    // + ",{'r':1,'result':'no such method'})");
                    //
                }
            }*/
            return true;
        } else {
            LogUtil.d(TAG + "canHandleJsRequest", " URL invalid ");
        }
        return false;
    }

    public boolean handleBeforeLoad(Map<String, Object> info) {
        if (pluginList == null) {
            return false;
        }
        for (int i = 0; i < pluginList.size(); ++i) {
            WebViewPlugin plugin = pluginList.get(i);
            Object newUrl = plugin == null ? null : info.get(WebViewPlugin.KEY_URL);
            if (!(newUrl instanceof String)) {
                continue;
            }
            if (plugin.handleEvent((String) newUrl, WebViewPlugin.EVENT_BEFORE_LOAD, info)) {
                return true;
            }
        }
        return false;
    }

    public DefaultPluginRuntime getRuntime() {
        return mRuntime;
    }

}
