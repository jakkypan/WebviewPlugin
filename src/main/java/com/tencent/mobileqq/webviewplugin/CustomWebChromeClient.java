package com.tencent.mobileqq.webviewplugin;

import android.text.TextUtils;
import com.tencent.mobileqq.webviewplugin.util.LogUtil;
import com.tencent.smtt.export.external.interfaces.ConsoleMessage;
import com.tencent.smtt.export.external.interfaces.JsPromptResult;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;

public class CustomWebChromeClient extends WebChromeClient {

    private static final String TAG =
            "QQJSSDK." + CustomWebChromeClient.class.getSimpleName() + ".";

    private WebViewPluginEngine mPluginEngine;

    public void setPluginEngine(WebViewPluginEngine pluginEngine) {
        mPluginEngine = pluginEngine;
    }

    public CustomWebChromeClient(WebViewPluginEngine pluginEngine) {
        mPluginEngine = pluginEngine;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        super.onConsoleMessage(consoleMessage);
        // ping 得通的话，使用console.log来处理
        String msg = consoleMessage.message();
        if (TextUtils.equals("pingJsbridge://", msg)) {
            if (mPluginEngine != null && mPluginEngine.getRuntime() != null) {
                WebView webView = mPluginEngine.getRuntime().getWebView();
                if (webView != null) {
                    String script = "javascript:window.{ACTION}_AVAILABLE=true;"
                            .replace("{ACTION}", "CONSOLE");
                    webView.loadUrl(script);
                    LogUtil.d(TAG + "pingJsbridge", " !!!!! console ok !!!!! ");
                }
                return true;
            }
        }

        LogUtil.d(TAG + "onConsoleMessage", " by onConsoleMessage : " + msg);
        if (mPluginEngine == null) {
            LogUtil.d(TAG + "onConsoleMessage", "mPluginEngine is null");
        } else if (mPluginEngine.canHandleJsRequest(msg)) {
            return true;
        } else if (mPluginEngine.handleRequest(msg)) {
            return true;
        }

        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
            JsPromptResult result) {
        if (TextUtils.equals("pingJsbridge://", defaultValue)) {
            if (mPluginEngine != null && mPluginEngine.getRuntime() != null) {
                WebView webView = mPluginEngine.getRuntime().getWebView();
                if (webView != null) {
                    String script = "javascript:window.{ACTION}_AVAILABLE=true;"
                            .replace("{ACTION}", "PROMPT");
                    webView.loadUrl(script);
                    LogUtil.d(TAG + "pingJsbridge", " !!!!! prompt ok !!!!! ");
                }
                result.confirm();
                return true;
            }
        }

        LogUtil.d(TAG + "onJsPrompt", " by onJsPrompt : " + defaultValue);
        if (mPluginEngine == null) {
            LogUtil.d(TAG + "onJsPrompt", "mPluginEngine is null");
        } else if (mPluginEngine.canHandleJsRequest(defaultValue)) {
            result.confirm();
            return true;
        } else if (mPluginEngine.handleRequest(defaultValue)) {
            result.confirm();
            return true;
        }

        return super.onJsPrompt(view, url, message, defaultValue, result);
    }
}
