package com.tencent.mobileqq.webviewplugin.util;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.webkit.URLUtil;
import com.tencent.mobileqq.webviewplugin.annotation.Public;
import com.tencent.mobileqq.webviewplugin.util.PlatformUtil.VersionCodes;
import java.util.HashMap;


/**
 * Tencent. Author: raezlu Date: 12-10-30 Time: 下午7:22
 */
@Public
public class NetworkUtils {

    private final static String TAG = "NetworkUtil";

    // ------------------ common -------------------
    @Public
    public static boolean isNetworkAvailable(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        // 这里必须用isConnected,不能用avaliable，因为有网络的情况isAvailable也可能是false
        return info != null && info.isConnected();
    }

    @Public
    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = getActiveNetworkInfo(context);
        return activeNetworkInfo != null
                && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    @Public
    public static boolean isMobileConnected(Context context) {
        if (context == null) {
            return false;
        }
        NetworkInfo activeNetworkInfo = getActiveNetworkInfo(context);
        return activeNetworkInfo != null
                && activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
    }

    @Public
    public static NetworkInfo getActiveNetworkInfo(Context context) {
        try {
            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            return connMgr.getActiveNetworkInfo();
        } catch (Throwable e) {
            LogUtil.e(TAG, "fail to get active network info", e);
            return null;
        }
    }

    @Public
    public static boolean isNetworkUrl(String url) {
        //avoid spawn objects in URLUtil.isNetworkUrl
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return true;
        } else if (URLUtil.isFileUrl(url)) {
            return false;
        } else {
            return URLUtil.isNetworkUrl(url);
        }
    }

    // ------------------ apn & proxy -------------------
    public static class NetworkProxy implements Cloneable {

        public final String host;
        public final int port;

        NetworkProxy(String host, int port) {
            this.host = host;
            this.port = port;
        }

        final NetworkProxy copy() {
            try {
                return (NetworkProxy) clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof NetworkProxy) {
                NetworkProxy proxy = (NetworkProxy) obj;
                return TextUtils.equals(this.host, proxy.host) && this.port == proxy.port;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    public final static String APN_NAME_WIFI = "wifi";

    private final static Uri PREFERRED_APN_URI
            = Uri.parse("content://telephony/carriers/preferapn");

    private final static HashMap<String, NetworkProxy> sAPNProxies
            = new HashMap<String, NetworkProxy>();

    static {
        sAPNProxies.put("cmwap", new NetworkProxy("10.0.0.172", 80));
        sAPNProxies.put("3gwap", new NetworkProxy("10.0.0.172", 80));
        sAPNProxies.put("uniwap", new NetworkProxy("10.0.0.172", 80));
        sAPNProxies.put("ctwap", new NetworkProxy("10.0.0.200", 80));
    }

    public static NetworkProxy getProxy(Context context, boolean apnProxy) {
        return !apnProxy ? getProxy(context) : getProxyByApn(context);
    }

    public static NetworkProxy getProxy(Context context) {
        if (!isMobileConnected(context)) {
            return null;
        }
        String proxyHost = getProxyHost(context);
        int proxyPort = getProxyPort(context);
        if (!isEmpty(proxyHost) && proxyPort >= 0) {
            return new NetworkProxy(proxyHost, proxyPort);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static String getProxyHost(Context context) {
        String host = null;
        if (PlatformUtil.version() < VersionCodes.HONEYCOMB) {
            host = Proxy.getDefaultHost();
        } else {
            host = System.getProperty("http.proxyHost");
        }
        return host;
    }

    @SuppressWarnings("deprecation")
    private static int getProxyPort(Context context) {
        int port = -1;
        if (PlatformUtil.version() < VersionCodes.HONEYCOMB) {
            port = Proxy.getDefaultPort();
        } else {
            String portStr = System.getProperty("http.proxyPort");
            if (!isEmpty(portStr)) {
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        if (port < 0 || port > 65535) {
            // ensure valid port.
            port = -1;
        }
        return port;
    }

    public static NetworkProxy getProxyByApn(Context context) {
        if (!isMobileConnected(context)) {
            return null;
        }
        String apn = getApn(context);
        NetworkProxy proxy = sAPNProxies.get(apn);
        return proxy == null ? null : proxy.copy();
    }

    public static String getApn(Context context) {
        NetworkInfo activeNetInfo = getActiveNetworkInfo(context);
        if (activeNetInfo == null) {
            // no active network.
            return null;
        }

        String apn = null;
        if (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            apn = APN_NAME_WIFI;

        } else if (activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            if (PlatformUtil.version() < VersionCodes.JELLY_BEAN_MR1) {
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver()
                            .query(PREFERRED_APN_URI, null, null, null, null);
                    while (cursor != null && cursor.moveToNext()) {
                        apn = cursor.getString(cursor.getColumnIndex("apn"));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            if (TextUtils.isEmpty(apn)) {
                apn = activeNetInfo.getExtraInfo();
            }
        }
        if (apn != null) {
            // convert apn to lower case.
            apn = apn.toLowerCase();
        }

        return apn;
    }

    // ---------------- dns ------------------
    public final static class DNS {

        public String primary;
        public String secondary;

        DNS() {
        }

        @Override
        public String toString() {
            return primary + "," + secondary;
        }
    }

    public static DNS getDns(Context context) {
        DNS dns = new DNS();
        if (context != null) {
            if (isWifiConnected(context)) {
                WifiManager wifiManager = (WifiManager) context
                        .getSystemService(Context.WIFI_SERVICE);
                DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                if (dhcpInfo != null) {
                    dns.primary = int32ToIpStr(dhcpInfo.dns1);
                    dns.secondary = int32ToIpStr(dhcpInfo.dns2);
                }
            }
        }
        if (dns.primary == null && dns.secondary == null) {
            // retrieve dns with property.
            dns.primary = PropertyUtils.get(PropertyUtils.PROPERTY_DNS_PRIMARY, null);
            dns.secondary = PropertyUtils.get(PropertyUtils.PROPERTY_DNS_SECNDARY, null);
        }
        return dns;
    }

    private static String int32ToIpStr(int ip) {
        StringBuffer buffer = new StringBuffer();

        buffer.append(ip & 0xFF).append(".");
        buffer.append((ip >> 8) & 0xFF).append(".");
        buffer.append((ip >> 16) & 0xFF).append(".");
        buffer.append((ip >> 24) & 0xFF);

        return buffer.toString();
    }

    // ---------------- utils ------------------
    private static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    // -----------------------------------------
    private NetworkUtils() {
        // static use.
    }
}
