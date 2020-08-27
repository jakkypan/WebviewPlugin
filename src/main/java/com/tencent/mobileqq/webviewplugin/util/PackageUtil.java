package com.tencent.mobileqq.webviewplugin.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import com.tencent.base.Global;
import com.tencent.version.VersionHelper;

/**
 * @author rongodzhang
 */
public class PackageUtil {

    private static final String CURRENT_UIN = "current_uin";
    private static String TAG = "PackageUtil";

    /**
     * 判断应用是否安装
     *
     * @param pkgName 包名
     */
    public static boolean isAppInstalled(Context context, String pkgName) {
        if (TextUtils.isEmpty(pkgName)) { // zivon add, 如果传入的包名为空, 则视同没有安装
            return false;
        }
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(pkgName.trim(), 0);
            if (pi == null) {
                return false;
            }

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 判断应用是否安装.返回版本号
     *
     * @param pkgName 包名
     */
    public static String checkAppInstalled(Context context, String pkgName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(pkgName.trim(), 0);
            if (pi == null) {
                return "0";
            }
            if (TextUtils.equals(pkgName, Global.getPackageName())) {
                return VersionHelper.getInstance().getVersionName();
            }
            return pi.versionName;

        } catch (Exception e) {
            return "0";
        }
    }


    /**
     * 批量判断应用是否安装 返回版本号 js接口测试传入字符串数组无效，改用|分隔，返回0表示没安装
     */
    public static String checkAppInstalledBatch(Context context, String arrayStr) {
        if (arrayStr == null) {
            return "0";
        }
        PackageManager pm = context.getPackageManager();
        String[] array = arrayStr.split("\\|");
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                buffer.append("|");
            }
            try {
                PackageInfo pi = pm.getPackageInfo(array[i].trim(), 0);
                if (pi == null) {
                    buffer.append(0);
                } else {
                    buffer.append(pi.versionName);
                }

            } catch (Exception e) {
                buffer.append(0);
            }
        }
        return buffer.toString();
    }


    /**
     * 批量判断应用是否安装 js接口测试传入字符串数组无效，改用|分隔，返回0表示没安装
     */
    public static String isAppInstalledBatch(Context context, String arrayStr) {
        if (arrayStr == null) {
            return "0";
        }
        PackageManager pm = context.getPackageManager();
        String[] array = arrayStr.split("\\|");
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                buffer.append("|");
            }
            try {
                PackageInfo pi = pm.getPackageInfo(array[i].trim(), 0);
                if (pi == null) {
                    buffer.append(0);
                } else {
                    buffer.append(1);
                }

            } catch (Exception e) {
                buffer.append(0);
            }
        }
        return buffer.toString();
    }

    /**
     * 启动app
     */
    public static boolean startAppWithPkgName(Context context, String pkgName, String uin) {
        try {
            LogUtil.d(TAG, "<--startAppWithPkgName pkgName=" + pkgName + ",openid=" + uin);
            //启动app上报 by pricezhang
            //try{
            //    QQAppInterface app;
            //    app = ((BaseActivity)context).app;
            //    StartAppObserverHandler startAppHandler = (StartAppObserverHandler)app.getBusinessHandler(QQAppInterface.STARTAPPOBSERVER_HANDLER);
            //  startAppHandler.SendStartedAppInfo(pkgName.trim());
            //}catch(Exception e){
            //  QLog.d(StartAppObserverHandler.TAG, QLog.CLR, "<-- AppStartedObserver Failed!");
            //}

            Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName.trim());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (uin != null && uin.length() > 4) {
                intent.putExtra(CURRENT_UIN, uin);
            }
            intent.putExtra("platformId", "qq_m");
            context.startActivity(intent);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 批量判断应用是否安装 返回版本号 js接口测试传入字符串数组无效，改用|分隔，返回0表示没安装 jlin
     */
    public static String getAppsVerionCodeBatch(Context context, String arrayStr) {
        if (arrayStr == null) {
            return "0";
        }
        PackageManager pm = context.getPackageManager();
        String[] array = arrayStr.split("\\|");
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                buffer.append("|");
            }
            try {
                PackageInfo pi = pm.getPackageInfo(array[i].trim(), 0);
                if (pi == null) {
                    buffer.append(0); // 表示未安装的
                } else {
                    buffer.append(pi.versionCode);
                }

            } catch (Exception e) {
                buffer.append(0);
            }
        }
        return buffer.toString();
    }
}
