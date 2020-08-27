package com.tencent.mobileqq.webviewplugin;


public class PluginInfo {

    public Class<? extends WebViewPlugin> classType;
    public String desc;
    public String version;
    public String namespace;
    public int index;
    public boolean async;

    public PluginInfo(Class<? extends WebViewPlugin> classType, String desc, String version) {
        this(classType, null, desc, version, false);
    }

    public PluginInfo(Class<? extends WebViewPlugin> classType, String namespace, String desc,
            String version) {
        this(classType, namespace, desc, version, true);
    }

    public PluginInfo(Class<? extends WebViewPlugin> classType, String namespace, String desc,
            String version, boolean async) {
        this.classType = classType;
        this.desc = desc;
        this.version = version;
        this.namespace = namespace;
        this.async = async;
    }
}
