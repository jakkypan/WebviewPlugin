package com.tencent.mobileqq.webviewplugin;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityManager;
import com.tencent.smtt.sdk.WebView;
import com.tencent.weishi.lib.logger.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CustomWebView extends WebView {

    public static final String TAG_WEBVIEW_LOAD = "webviewLoad";
    public static final String TAG = CustomWebView.class.getSimpleName();
    ScrollInterface mt;
    protected WebViewPluginEngine mPluginEngine;
    boolean isPaused = true;
    boolean isDestroyed = false;
    private boolean attachedToWindow = false;
    Context mContext;
    private boolean isFirstLoad = true;
    public static long clickStartTime = -1;
    public static long startLoadUrlTime = -1;

    public CustomWebView(Context context) {
        super(context);
        init(context);
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        clickStartTime = System.currentTimeMillis();
        mContext = context;
        try {
            removeJavascriptInterface("searchBoxJavaBridge_");
            removeJavascriptInterface("accessibility");
            removeJavascriptInterface("accessibilityTraversal");
        } catch (Exception e) {
            Logger.e(TAG, "CustomWebView init:" + e);
        }
        //this.getSettings().setUserAgentString(this.getSettings().getUserAgentString() + "{SDK_VERSION}");
        this.getSettings()
                .setUserAgentString(this.getSettings().getUserAgentString() + " QQJSSDK/1.3 ");

    }

    public void setPluginEngine(WebViewPluginEngine pluginEngine) {
        mPluginEngine = pluginEngine;
    }

    @Override
    public void postUrl(String url, byte[] postData) {
        if (!TextUtils.isEmpty(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
            startLoadUrlTime = System.currentTimeMillis();
        }
        super.postUrl(url, postData);
    }

    @Override
    public void loadUrl(String url) {
        if (!TextUtils.isEmpty(url) && (url.startsWith("http://") || url.startsWith("https://"))) {
            startLoadUrlTime = System.currentTimeMillis();
        }
        if (isDestroyed) {
            return;
        }
        if (isFirstLoad) {
            isFirstLoad = false;
            Map<String, Object> info = new HashMap<String, Object>();
            info.put(WebViewPlugin.KEY_URL, url);
            if (mPluginEngine != null && mPluginEngine.handleBeforeLoad(info)) {
                return;
            }
            url = (String) info.get(WebViewPlugin.KEY_URL);
        }
        if (mPluginEngine == null) {
            super.loadUrl(url);
            //requestFocus();
        } else if (mPluginEngine.canHandleJsRequest(url)) {
        } else if (mPluginEngine.handleRequest(url)) {
        } else {
            super.loadUrl(url);
            //requestFocus();
        }
    }

    public void loadUrlOriginal(String url) {
        if (isDestroyed) {
            return;
        }
        if (isFirstLoad) {
            isFirstLoad = false;
            Map<String, Object> info = new HashMap<String, Object>();
            info.put(WebViewPlugin.KEY_URL, url);
            if (mPluginEngine != null && mPluginEngine.handleBeforeLoad(info)) {
                return;
            }
            url = (String) info.get(WebViewPlugin.KEY_URL);
        }
        super.loadUrl(url);
    }

    @Override
    public void loadData(String data, String mimeType, String encoding) {
        if (isFirstLoad) {
            isFirstLoad = false;
            Map<String, Object> info = new HashMap<String, Object>();
            info.put(WebViewPlugin.KEY_URL, data);
            if (mPluginEngine != null && mPluginEngine.handleBeforeLoad(info)) {
                return;
            }
            data = (String) info.get(WebViewPlugin.KEY_URL);
        }
        super.loadData(data, mimeType, encoding);
    }

    @Override
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding,
            String historyUrl) {
        if (isFirstLoad) {
            isFirstLoad = false;
            Map<String, Object> info = new HashMap<String, Object>();
            info.put(WebViewPlugin.KEY_URL, baseUrl);
            if (mPluginEngine != null && mPluginEngine.handleBeforeLoad(info)) {
                return;
            }
            baseUrl = (String) info.get(WebViewPlugin.KEY_URL);
        }
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        // Log.e("hhah",""+l+" "+t+" "+oldl+" "+oldt);
        if (mt != null) {
            mt.onSChanged(l, t, oldl, oldt);
        }
    }

    public void setOnCustomScroolChangeListener(ScrollInterface t) {
        this.mt = t;
    }

    public interface ScrollInterface {

        public void onSChanged(int l, int t, int oldl, int oldt);
    }


    /*/**
     * 检查更新完回调 主线程执行

    public void checkOfflineUpBack(int code) {
        if (mPluginEngine != null) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("code", code);
            mPluginEngine.handleEvent(getUrl(),
                    WebViewPlugin.EVENT_OFFLINE_UPDATE, m);
        }
    }
     */

    @Override
    public void onPause() {
        isPaused = true;
        super.onPause();
    }

    @Override
    public void onResume() {
        isPaused = false;
        super.onResume();
    }


    /**
     * {@link com.tencent.smtt.sdk.WebView#destroy()} 要保证webview core空闲之后并且webview detached之后最后调用,
     * 所以这里做了延时.
     */
    @Override
    public void destroy() {
        isDestroyed = true;
        if (!attachedToWindow) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    destroyWebView();
                }
            }, 1000);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attachedToWindow = false;
        if (isDestroyed) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    destroyWebView();
                }
            }, 1000);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attachedToWindow = true;
    }

    public void callJs(String func, String... args) {
        Util.callJs(this, func, args);
    }

    /**
     * destroy掉WebView的时候，需要做一些hack的工作，但这边有两处调用destroy，所以统一一下 问题应该是出在现在destroy是延迟1s的（为了解决webcore线程卡死的bug），而这时在开启TalkBack的情况下，
     * 重新打开WebView，就会引起shutdown掉TTS的bug，需要通过反射进去，将这个Exception捕获掉 add by melody
     */
    private void destroyWebView() {
        try {
            // 判断是否启用AccessibilityInjector，也就是是否使用了TalkBack
            AccessibilityManager manager = (AccessibilityManager) getContext()
                    .getSystemService(Context.ACCESSIBILITY_SERVICE);
            // 启用了AccessibilityInjector的话，再进行hack操作
            if (manager.isEnabled() && Build.VERSION.SDK_INT < 19) {
                /*
                 * 解决java.lang.IllegalArgumentException: Service not registered:
                 * android.speech.tts.TextToSpeech$Connection的hack
                 */
                Field isX5CoreField = WebView.class.getDeclaredField("isX5Core");
                isX5CoreField.setAccessible(true);
                boolean isX5Core = (Boolean) isX5CoreField.get(CustomWebView.this);
                if (!isX5Core) {
                    // 获取系统WebView
                    Field mSysWebViewField = WebView.class.getDeclaredField("mSysWebView");
                    mSysWebViewField.setAccessible(true);
                    Object mSysWebView = mSysWebViewField.get(CustomWebView.this);
                    // 获取WebViewClassic(mProvider)
                    Field mProviderField = android.webkit.WebView.class
                            .getDeclaredField("mProvider");
                    mProviderField.setAccessible(true);
                    Object mProvider = mProviderField.get(mSysWebView);
                    // 获取mAccessibilityInjector
                    Field mAccessibilityInjectorField = Class
                            .forName("android.webkit.WebViewClassic")
                            .getDeclaredField("mAccessibilityInjector");
                    mAccessibilityInjectorField.setAccessible(true);
                    Object mAccessibilityInjector = mAccessibilityInjectorField.get(mProvider);
                    // 获取mTextToSpeech
                    Field mTextToSpeechField = Class.forName("android.webkit.AccessibilityInjector")
                            .getDeclaredField("mTextToSpeech");
                    mTextToSpeechField.setAccessible(true);
                    Object mTextToSpeech = mTextToSpeechField.get(mAccessibilityInjector);
                    // 具体hack过程，强制shutdown掉TextToSpeech
                    if (mTextToSpeech != null) {
                        Object textToSpeech = mTextToSpeech;
                        // 将原始的mTextToSpeech置空
                        mTextToSpeechField.set(mAccessibilityInjector, null);
                        // 调用shutdown
                        Method shutdown = Class
                                .forName("android.webkit.AccessibilityInjector.TextToSpeechWrapper")
                                .getDeclaredMethod("shutdown");
                        shutdown.setAccessible(true);
                        shutdown.invoke(textToSpeech);
                    }
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CustomWebView.super.destroy();
    }

}
