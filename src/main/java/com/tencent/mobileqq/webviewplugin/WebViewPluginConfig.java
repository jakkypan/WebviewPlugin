package com.tencent.mobileqq.webviewplugin;

import com.tencent.mobileqq.webviewplugin.plugins.AppApiPlugin;
import com.tencent.mobileqq.webviewplugin.plugins.EventApiPlugin;
import com.tencent.mobileqq.webviewplugin.plugins.MediaApiPlugin;
import java.util.HashMap;
import java.util.Map;

public class WebViewPluginConfig {

    public final static PluginInfo[] list;
    public final static Map<String, PluginInfo> map = new HashMap<String, PluginInfo>(); //新的插件列表

    static {

        //release

        list = new PluginInfo[]{
                new PluginInfo(MediaApiPlugin.class, "media", "mqq.media.* API", "1.0"),
                new PluginInfo(AppApiPlugin.class, "app", "mqq.app.* API", "1.0"),
                new PluginInfo(EventApiPlugin.class, "event", "mqq.event.* API", "1.0")
        };


        /*
        //debug & 第三方release
        list = new PluginInfo[]{
                new PluginInfo(MediaApiPlugin.class, "media", "mqq.media.* API", "1.0"),
                new PluginInfo(AppApiPlugin.class, "app", "mqq.app.* API", "1.0"),
                new PluginInfo(EventApiPlugin.class, "event", "mqq.event.* API", "1.0"),
                new PluginInfo(DataApiPlugin.class, "data", "mqq.data.* API", "1.0"),
                new PluginInfo(DevicePlugin.class, "device", "mqq.device.* API", "1.0"),
                new PluginInfo(SensorApiPlugin.class, "sensor", "mqq.sensor.* API", "1.0")
        };
        */
        for (int i = 0, len = list.length; i < len; ++i) {
            PluginInfo p = list[i];
            p.index = i + 1;
            if (p.async && p.namespace != null && p.namespace.length() > 0) {
                map.put(p.namespace, p);
            }
        }
    }

}
