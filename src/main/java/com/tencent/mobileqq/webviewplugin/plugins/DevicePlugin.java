package com.tencent.mobileqq.webviewplugin.plugins;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.tencent.mobileqq.webviewplugin.WebViewPlugin;
import com.tencent.mobileqq.webviewplugin.util.IoUtils;
import com.tencent.oscar.app.GlobalContext;
import com.tencent.oscar.base.utils.DeviceUtils;
import com.tencent.oscar.module.ipc.WSApiProxy;
import com.tencent.oscar.utils.GPSUtils;
import com.tencent.version.VersionHelper;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by pelli on 2015/2/11.
 */
public class DevicePlugin extends WebViewPlugin {

    private static final String TAG = "QQJSSDK." + DevicePlugin.class.getSimpleName() + ".";

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if ("getDeviceInfo".equals(method)) {
            doGetDeviceInfoMethod(args[0]);
            return true;
        } else if ("getNetworkInfo".equals(method)) {
            doGetNetworkInfoMethod(args[0]);
            return true;
        } else if ("getClientInfo".equals(method)) {
            doGetClientInfoMethod(args);
            return true;
        } else if ("getCPUInfo".equals(method)) {
            doGetCPUInfoMethod(args[0]);
            return true;
        } else if ("getMemInfo".equals(method)) {
            doGetMemInfoMethod(args[0]);
        }
        return true;
    }

    private void doGetMemInfoMethod(String arg) {
        try {
            JSONObject json = new JSONObject(arg);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                JSONObject json1 = new JSONObject();
                json1.put("idleMem", getIdleMem());
                json1.put("totalMem", getTotalMem());
                callJs(callback, getResult(json1));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void doGetCPUInfoMethod(String arg) {
        try {
            JSONObject json = new JSONObject(arg);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                JSONObject json1 = new JSONObject();
                json1.put("maxFreq", getMaxCpuFreq());
                json1.put("minFreq", getMinCpuFreq());
                json1.put("curFreq", getCurCpuFreq());
                json1.put("CPUName", getCpuName());
                callJs(callback, getResult(json1));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void doGetClientInfoMethod(String[] args) {
        PackageManager manager = this.mRuntime.context.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(this.mRuntime.context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (info != null) {
            try {
                JSONObject json = new JSONObject(args[0]);
                String callback = json.getString("callback");
                if (!TextUtils.isEmpty(callback)) {
                    JSONObject json1 = new JSONObject();
                    json1.put("version", VersionHelper.getInstance().getVersionName());
                    json1.put("build", 0);
                    callJs(callback, getResult(json1));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void doGetNetworkInfoMethod(String arg) {
        //@param {Number} callback.data.type 网络类型，和原本的getNetworkType接口一样
        //@param {String} callback.data.radio 细化的网络类型
        int type = 0;
        String typeStr = "NETWORK_TYPE_NO";
        ConnectivityManager cm = (ConnectivityManager) mRuntime.getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                switch (networkInfo.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                        type = 1;
                        typeStr = "NETWORK_TYPE_WIFI";
                        break;
                    case ConnectivityManager.TYPE_MOBILE:
                        int subType = networkInfo.getSubtype();
                        switch (subType) {
                            // 参考android.telephony.TelephonyManager#getNetworkClass
                            // 2G
                            case TelephonyManager.NETWORK_TYPE_GPRS:
                                type = 2;
                                typeStr = "NETWORK_TYPE_GPRS";
                                break;
                            case TelephonyManager.NETWORK_TYPE_EDGE:
                                type = 2;
                                typeStr = "NETWORK_TYPE_EDGE";
                                break;
                            case TelephonyManager.NETWORK_TYPE_CDMA:
                                type = 2;
                                typeStr = "NETWORK_TYPE_CDMA";
                                break;
                            case TelephonyManager.NETWORK_TYPE_1xRTT:
                                type = 2;
                                typeStr = "NETWORK_TYPE_1xRTT";
                                break;
                            case TelephonyManager.NETWORK_TYPE_IDEN:
                                type = 2;
                                typeStr = "NETWORK_TYPE_IDEN";
                                break;
                            // 3G
                            case TelephonyManager.NETWORK_TYPE_UMTS:
                                type = 3;
                                typeStr = "NETWORK_TYPE_UMTS";
                                break;
                            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                                type = 3;
                                typeStr = "NETWORK_TYPE_EVDO_0";
                                break;
                            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                                type = 3;
                                typeStr = "NETWORK_TYPE_EVDO_A";
                                break;
                            case TelephonyManager.NETWORK_TYPE_HSDPA:
                                type = 3;
                                typeStr = "NETWORK_TYPE_HSDPA";
                                break;
                            case TelephonyManager.NETWORK_TYPE_HSUPA:
                                type = 3;
                                typeStr = "NETWORK_TYPE_HSUPA";
                                break;
                            case TelephonyManager.NETWORK_TYPE_HSPA:
                                type = 3;
                                typeStr = "NETWORK_TYPE_HSPA";
                                break;
                            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                                type = 3;
                                typeStr = "NETWORK_TYPE_EVDO_B";
                                break;
                            case TelephonyManager.NETWORK_TYPE_EHRPD:
                                type = 3;
                                typeStr = "NETWORK_TYPE_EHRPD";
                                break;
                            case TelephonyManager.NETWORK_TYPE_HSPAP:
                                type = 3;
                                typeStr = "NETWORK_TYPE_HSPAP";
                                break;
                            // 4G
                            case TelephonyManager.NETWORK_TYPE_LTE:
                                type = 4;
                                typeStr = "NETWORK_TYPE_LTE";
                                break;
                            // unknown
                            default:
                                type = -1;
                                typeStr = "NETWORK_TYPE_UNKNOWN";
                                break;
                        }
                        break;
                    default:
                        type = -1;
                        typeStr = "NETWORK_TYPE_UNKNOWN";
                        break;
                }
            }
        }

        try {
            JSONObject json = new JSONObject(arg);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                JSONObject json1 = new JSONObject();
                json1.put("type", type);
                json1.put("radio", typeStr);
                callJs(callback, getResult(json1));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void doGetDeviceInfoMethod(String arg) {
        TelephonyManager tm = (TelephonyManager) mRuntime.getActivity()
                .getSystemService(Activity.TELEPHONY_SERVICE);
        try {
            JSONObject json = new JSONObject(arg);
            String callback = json.getString("callback");
            if (!TextUtils.isEmpty(callback)) {
                JSONObject json1 = new JSONObject();
                json1.put("model", android.os.Build.MODEL);
                json1.put("systemVersion", android.os.Build.VERSION.RELEASE);
                try {
                    json1.put("identifier", tm.getDeviceId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                json1.put("systemName", "android");
                json1.put("modelVersion", android.os.Build.MODEL);
                json1.put("manu", android.os.Build.MANUFACTURER);

                String qimei = WSApiProxy.isInit() ? WSApiProxy.g().getQIMEI() : "";
                json1.put("qimei", TextUtils.isEmpty(qimei) ? "N/A" : qimei);

                // 高端机型判断补充字段
                int deviceAPI = Build.VERSION.SDK_INT;
                int deviceMemory = (int) DeviceUtils.getTotalMem() / 1024;
                int deviceAppMemory = (int) DeviceUtils.getHeapMaxSizeInKb() / 1024;

                json1.put("isLowEndDevice", DeviceUtils.isLowEndDeviceByWNS() ? "1" : "0");
                json1.put("deviceAPI", deviceAPI);
                json1.put("deviceMemory", deviceMemory);
                json1.put("deviceAppMemory", deviceAppMemory);

                // 安全上报补充字段
                json1.put("mac", DeviceUtils.getMac());
                json1.put("modelName", Build.DEVICE);
                json1.put("imei", DeviceUtils.getImei(GlobalContext.getContext()));
                json1.put("imei2", DeviceUtils.getImei2(GlobalContext.getContext()));
                json1.put("systemVersion", DeviceUtils.getOSVersion());
                json1.put("appVersion", DeviceUtils.getAppVersion());
                json1.put("isRoot", DeviceUtils.isRoot());
                json1.put("android_id", DeviceUtils.getAndroidId());
                json1.put("android_cid", DeviceUtils.getAndroidCid());
                json1.put("qq_guid", DeviceUtils.getQQGuid());
                json1.put("imsi", DeviceUtils.getIMSI(GlobalContext.getContext()));

                Location location = GPSUtils.getInstance().getLastLocation();
                if (location != null) {
                    json1.put("longitude", String.valueOf(location.getLongitude()));
                    json1.put("latitude", String.valueOf(location.getLatitude()));
                }

                json1.put("screenHeight", DeviceUtils.getScreenHeight());
                json1.put("screenWidth", DeviceUtils.getScreenWidth());

                callJs(callback, getResult(json1));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 获取CPU最大频率（单位KHZ）
    public String getMaxCpuFreq() {
        String result = "";
        ProcessBuilder cmd;
        InputStream in = null;
        try {
            String[] args = {"/system/bin/cat",
                    "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"};
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            in = process.getInputStream();
            byte[] re = new byte[24];
            StringBuilder sb = new StringBuilder();
            while (in.read(re) != -1) {
                sb.append(new String(re));
            }
            result = sb.toString();
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            result = "N/A";
        } finally {
            IoUtils.close(in);
        }
        return result.trim();
    }

    // 获取CPU最小频率（单位KHZ）
    public String getMinCpuFreq() {
        String result = "";
        ProcessBuilder cmd;
        InputStream in = null;
        try {
            String[] args = {"/system/bin/cat",
                    "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq"};
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            in = process.getInputStream();
            byte[] re = new byte[24];
            StringBuilder sb = new StringBuilder();
            while (in.read(re) != -1) {
                sb.append(new String(re));
            }
            result = sb.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            result = "N/A";
        } finally {
            IoUtils.close(in);
        }
        return result.trim();
    }

    // 实时获取CPU当前频率（单位KHZ）
    public String getCurCpuFreq() {
        String result = "N/A";
        FileReader fr = null;
        try {
            fr = new FileReader(
                    "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
            BufferedReader br = new BufferedReader(fr);
            String text = br.readLine();
            if (text != null) {
                result = text.trim();
            }
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IoUtils.close(fr);
        }
        return result;
    }

    // 获取CPU名字
    public String getCpuName() {
        BufferedReader br = null;
        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            br = new BufferedReader(fr);
            String text = br.readLine();
            if (TextUtils.isEmpty(text)) {
                return null;
            }
            String[] array = text.split(":\\s+", 2);
            return array.length > 1 ? array[1] : "";
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IoUtils.close(br);
        }
        return null;
    }

    // 获得可用的内存
    public long getIdleMem() {
        ActivityManager am = (ActivityManager) mRuntime.context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem / 1024;
    }

    // 获得总内存
    public long getTotalMem() {
        long mTotal;
        String path = "/proc/meminfo";
        String content = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path), 8);
            String line;
            if ((line = br.readLine()) != null) {
                content = line;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (content != null) {
            // beginIndex
            int begin = content.indexOf(':');
            // endIndex
            int end = content.indexOf('k');
            // 截取字符串信息
            content = content.substring(begin + 1, end).trim();
            mTotal = Integer.parseInt(content);
            return mTotal;
        }
        return -1;
    }

}
