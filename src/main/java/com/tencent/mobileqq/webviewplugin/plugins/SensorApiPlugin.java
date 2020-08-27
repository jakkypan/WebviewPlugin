package com.tencent.mobileqq.webviewplugin.plugins;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import com.tencent.mobileqq.webviewplugin.Util;
import com.tencent.mobileqq.webviewplugin.WebViewPlugin;
import com.tencent.mobileqq.webviewplugin.util.LogUtil;
import com.tencent.weishi.lib.logger.Logger;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ippan on 2015/2/5.
 */
public class SensorApiPlugin extends WebViewPlugin {

    private static long sLastLocationTime = 0;
    private static double sLastLongitude = 0;
    private static double sLastLatitude = 0;

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if ("vibrate".equals(method) && args.length == 1) {
            vibrate(args[0]);
        } else if ("startAccelerometer".equals(method) && args.length == 1) {
            startAccelerometer(Util.getCallbackName(args[0]));
        } else if ("stopAccelerometer".equals(method) && TextUtils.equals(args[0], NULL_PARAM)) {
            stopAccelerometer();
        } else if ("startCompass".equals(method) && args.length == 1) {
            startCompass(args[0]);
        } else if ("stopCompass".equals(method) && args.length == 0) {
            stopCompass();
        } else if ("getLocation".equals(method) && args.length == 1) {
            getLocation(args[0]);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        stopAccelerometer();
        stopCompass();
    }

    protected static final int HANDLER_LISTEN = 0x123;

    protected Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == HANDLER_LISTEN) {
                updateMicStatus((String) msg.obj);
            }
        }
    };


    protected MediaRecorder mMediaRecorder;
    protected static final int SPACE = 100;// 间隔取样时间

    protected void updateMicStatus(String callback) {
        if (mMediaRecorder != null) {
            int max = mMediaRecorder.getMaxAmplitude();
            int db = (int) (20 * Math.log10(max));
            if (!TextUtils.isEmpty(callback)) {
                callJs(callback, "true", Integer.toString(db));
                Message msg2 = new Message();
                msg2.what = HANDLER_LISTEN;
                msg2.obj = callback;
                mHandler.sendMessageDelayed(msg2, SPACE);
            }
        }
    }


    /**
     * 手机震动
     *
     * @param milliseconds 震动持续时间
     */
    final void vibrate(String milliseconds) {
        if (TextUtils.isEmpty(milliseconds)) {
            return;
        }
        long m = 0;

        try {
            m = new JSONObject(milliseconds).optLong("time");
        } catch (Exception e) {
            LogUtil.d(TAG, "vibrate json parse error");
            e.printStackTrace();
        }

        if (m > 0) {
            Vibrator vib = (Vibrator) mRuntime.context.getSystemService(Service.VIBRATOR_SERVICE);
            if (vib == null) {
                return;
            }
            vib.vibrate(m);
        }
    }

    ////////////////////传感器相关 ///////////////////////////////
    protected static final byte SENSOR_TYPE_ACCELEROMETER = 0;
    protected static final byte SENSOR_TYPE_LIGHT = 1;
    protected static final byte SENSOR_TYPE_DIRECTION = 2;
    protected static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;
    protected SensorManager mSensorManager;
    protected QQSensorEventListener mAccelerometerListener;

    /**
     * 开始获取三个方向的重力加速度
     */
    final void startAccelerometer(final String callBack) {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) mRuntime.context
                    .getSystemService(Context.SENSOR_SERVICE);
        }
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (sensors.size() > 0) {
            Sensor sensor = sensors.get(0);
            if (mAccelerometerListener != null) {
                stopAccelerometer();
            }
            mAccelerometerListener = new QQSensorEventListener(SENSOR_TYPE_ACCELEROMETER, callBack);
            mSensorManager.registerListener(mAccelerometerListener, sensor, SENSOR_DELAY);
        } else {
            callJs(callBack, "false");
        }
    }

    final void stopAccelerometer() {
        if (mSensorManager != null && mAccelerometerListener != null) {
            mSensorManager.unregisterListener(mAccelerometerListener);
            mAccelerometerListener = null;
        }
    }


    /**
     * 传感器listener统一封装, 一切为了放法数 = =
     */
    class QQSensorEventListener implements SensorEventListener {

        protected byte mSensorType;
        protected String mCallBack;
        private JSONObject accData;

        public QQSensorEventListener(byte sensorType, String callBack) {
            mSensorType = sensorType;
            mCallBack = callBack;
            accData = new JSONObject();
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            //(window.mqq && mqq.execGlobalCallback).apply(window, [\"2\", {\"retcode\" : 0, \"msg\" : \"\", \"data\" : {}}]);
            switch (mSensorType) {
                case SENSOR_TYPE_ACCELEROMETER:
                    try {
                        accData.put("x", event.values[0]);
                        accData.put("y", event.values[1]);
                        accData.put("z", event.values[2]);
                        callJs(mCallBack, getResult(accData));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case SENSOR_TYPE_LIGHT:
                    float lux = event.values[0];
                    callJs(mCallBack, String.valueOf(true), String.valueOf(lux));
                    break;
                case SENSOR_TYPE_DIRECTION:
                    float x = event.values[0];
                    callJs(mCallBack, String.valueOf(true), String.valueOf(x));
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }


    protected QQSensorEventListener mDirectionListener;

    /**
     * 获取手机方向 返回值  float  东（90） 南（180） 西（270） 北（360）
     */
    final void startCompass(final String callBack) {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) mRuntime.context
                    .getSystemService(Context.SENSOR_SERVICE);
        }
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        if (sensors.size() > 0) {
            Sensor sensor = sensors.get(0);
            if (mDirectionListener != null) {
                stopCompass();
            }
            mDirectionListener = new QQSensorEventListener(SENSOR_TYPE_DIRECTION, callBack);
            mSensorManager.registerListener(mDirectionListener, sensor, SENSOR_DELAY);
        } else {
            callJs(callBack, "false");
        }
    }

    void stopCompass() {
        if (mSensorManager != null && mDirectionListener != null) {
            mSensorManager.unregisterListener(mDirectionListener);
            mDirectionListener = null;
        }
    }

    LocationManager mLocationManager;

    class StatusLocationListener implements LocationListener {

        boolean mFinished = false;
        String mCallback;

        public StatusLocationListener(String callback) {
            this.mCallback = callback;
        }

        public void onLocationChanged(Location location) {
            double lon = location.getLongitude();
            double lat = location.getLatitude();
            JSONObject res = new JSONObject();
            try {
                res.put("latitude", Double.toString(lat));
                res.put("longitude", Double.toString(lon));
                callJs(this.mCallback, getResult(0, "获取地理位置成功", res));
            } catch (JSONException e) {
                Logger.e(TAG, "获取地理位置失败");
                callJs(this.mCallback, getResult(0, "获取地理位置失败", res));
            }
            mLocationManager.removeUpdates(this);
            this.mFinished = true;
            synchronized (SensorApiPlugin.class) {
                sLastLongitude = lon;
                sLastLatitude = lat;
                sLastLocationTime = System.currentTimeMillis();
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    }

    void getLocation(String param) {
        try {
            JSONObject json = new JSONObject(param);
            final String callback = json.getString("callback");
            long allowCacheTime = json.optLong("allowCacheTime", 0L) * 1000L;
            long current = System.currentTimeMillis();
            if (current - sLastLocationTime < allowCacheTime) {
                double lat;
                double lon;
                synchronized (SensorApiPlugin.class) {
                    lon = sLastLongitude;
                    lat = sLastLatitude;
                }

                JSONObject res = new JSONObject();
                res.put("latitude", Double.toString(lat));
                res.put("longitude", Double.toString(lon));
                this.callJs(callback, this.getResult(res));
                return;
            }

            if (this.mLocationManager == null) {
                this.mLocationManager = (LocationManager) this.mRuntime.context
                        .getSystemService(Context.LOCATION_SERVICE);
            }

            final StatusLocationListener listener = new StatusLocationListener(callback);
            boolean hasProvider = false;
            if (this.mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                this.mLocationManager
                        .requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0.0F, listener);
                hasProvider = true;
            }

            if (this.mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                this.mLocationManager
                        .requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0.0F,
                                listener);
                hasProvider = true;
            }

            if (!hasProvider) {
                JSONObject res = new JSONObject();
                res.put("latitude", "0");
                res.put("longitude", "0");
                this.callJs(callback, this.getResult(-1, "", res));
            } else {
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        if (!listener.mFinished) {
                            mLocationManager.removeUpdates(listener);

                            try {
                                JSONObject res = new JSONObject();
                                res.put("latitude", "0");
                                res.put("longitude", "0");
                                callJs(callback, getResult(RET_CODE_FAIL, "", res));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }, 10000L);
            }
        } catch (JSONException e) {
            LogUtil.d(this.TAG, "getLocation json parse error");
            e.printStackTrace();
        }

    }
}
