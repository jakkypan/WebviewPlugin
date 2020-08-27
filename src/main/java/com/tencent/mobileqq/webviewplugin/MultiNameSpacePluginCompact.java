package com.tencent.mobileqq.webviewplugin;

/**
 * Created by jemstyli on 2017/12/28 为了解决一个插件有多个NameSpace对应问题，特意设计MultiNameSpacePlugin做区分
 */

public interface MultiNameSpacePluginCompact {

    String[] getMultiNameSpace();
}