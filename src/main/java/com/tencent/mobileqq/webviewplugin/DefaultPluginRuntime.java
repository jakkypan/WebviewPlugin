package com.tencent.mobileqq.webviewplugin;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import com.tencent.mobileqq.webviewplugin.swift.WebUiBaseInterface;
import com.tencent.smtt.sdk.WebView;
import java.lang.ref.WeakReference;

public class DefaultPluginRuntime {

    private WeakReference<WebView> mWebView;
    private WeakReference<Activity> mActivity;
    private WeakReference<Fragment> mFragment;
    private WeakReference<WebUiBaseInterface> mIWebUiUtils;
    public Context context;
    /**
     * 是否直播页面使用的webview
     */
    private boolean isLiveWebView = false;

    public DefaultPluginRuntime(WebView webView, @NonNull Activity activity,
            WebUiBaseInterface iWebUIUtils) {
        mWebView = new WeakReference<WebView>(webView);
        mActivity = new WeakReference<Activity>(activity);
        context = activity.getApplicationContext();
        mIWebUiUtils = new WeakReference<>(iWebUIUtils);
    }


    public DefaultPluginRuntime(WebView webView, @NonNull Fragment fragment, @NonNull Context context,
            WebUiBaseInterface iWebUIUtils) {
        mWebView = new WeakReference<WebView>(webView);
        if (fragment.getActivity() != null && !fragment.getActivity().isFinishing()) {
            mActivity = new WeakReference<Activity>(fragment.getActivity());
        }
        this.context = context;
        mIWebUiUtils = new WeakReference<>(iWebUIUtils);
        mFragment = new WeakReference<>(fragment);
    }

    public DefaultPluginRuntime(WebView webView, @NonNull Context ctx,
            WebUiBaseInterface iWebUIUtils) {
        mWebView = new WeakReference<WebView>(webView);
        if ((ctx instanceof Activity) && !((Activity) ctx).isFinishing()) {
            mActivity = new WeakReference<Activity>(((Activity) ctx));
        }

        context = ctx.getApplicationContext();
        mIWebUiUtils = new WeakReference<>(iWebUIUtils);
    }

    public WebView getWebView() {
        return mWebView.get();
    }

    public Activity getActivity() {
        return mActivity.get();
    }

    public Fragment getFragment() {
        return mFragment.get();
    }

    public <T extends WebUiBaseInterface> T getWebUiBaseInterface() {

        return (T) mIWebUiUtils.get();
    }

    /**
     * should override in sub class
     *
     * @return nonempty string
     */
    public String getAccount() {
        return "0";
    }

    public boolean isLiveWebView() {
        return isLiveWebView;
    }

    public void setLiveWebView(boolean liveWebView) {
        isLiveWebView = liveWebView;
    }
}

