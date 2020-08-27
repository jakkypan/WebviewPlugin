package com.tencent.mobileqq.webviewplugin;

import android.content.Intent;

/**
 * Created by pelli on 14-2-18.
 */
public interface WebViewPluginContainer {

    int pluginStartActivityForResult(WebViewPlugin plugin, Intent intent, byte requestCode);
}
