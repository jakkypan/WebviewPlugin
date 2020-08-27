package com.tencent.mobileqq.webviewplugin.plugins;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;
import com.tencent.mobileqq.webviewplugin.WebViewPlugin;
import com.tencent.oscar.utils.WeishiToastUtils;
import com.tencent.widget.dialog.DialogShowUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ippan on 2015/2/5.
 */
public class MediaApiPlugin extends WebViewPlugin implements DialogInterface.OnCancelListener {

    // TODO 此目录需提供一个统一位置进行设置及获取
    public static final String HTML_OFFLINE_HTML_TOOR_DIR = "tencent/MobileQQ/qbiz/html5/"; //SD卡html5离线根目录
    static final byte CODE_OPEN_CAMERA = 1;
    static final byte CODE_PHOTO_LIBRARY = 2;
    static final String CAMERA_PHOTO_PATH = "camera_photo_path";
    static final String KEY_GET_PICTURE_PARAM = "getPictureParam";
    static final String KEY_CALLBACK = "callback";

    static final String KEY_MATCH = "match";
    static final String KEY_DATA = "data";
    static final String KEY_IMAGE_ID = "imageID";
    static final String KEY_RET_CODE = "retCode";
    static final String KEY_MSG = "msg";
    static final String KEY_STATUS_CODE = "statusCode";

    private Thread mPicturesThread;
    ProgressDialog mLoadingDialog;

    @Override
    protected boolean handleJsRequest(String url, String pkgName, String method, String... args) {
        if ("preloadSound".equals(method) && args.length == 3) {
            preloadSound(args[0], args[1], args[2]);
        } else if ("playLocalSound".equals(method) && args.length == 2) {
            playSound(args[0], args[1]);
        } else if ("getPicture".equals(method) && args.length == 1) {
            if (getPicture(args[0])) {
                return true;
            }
        } else if ("getLocalImage".equals(method) && args.length == 1) {
            if (getLocalImage(args[0])) {
                return true;
            }
        }
        return true;
    }

    private boolean getLocalImage(String arg) {
        try {
            JSONObject json = new JSONObject(arg);
            String callback = json.optString(KEY_CALLBACK);
            String imageID = json.optString(KEY_IMAGE_ID);
            if (TextUtils.isEmpty(callback) || TextUtils.isEmpty(imageID)) {
                return true;
            }
            if (mLoadingDialog == null) {
                Activity activity = mRuntime.getActivity();
                mLoadingDialog = new ProgressDialog(activity);
                mLoadingDialog.setMessage("正在处理，请稍候…");
                mLoadingDialog.setOnCancelListener(this);
            }
            if (!mLoadingDialog.isShowing()) {
                DialogShowUtils.show(mLoadingDialog);
            }
            new PrepareSinglePictureAndCallbackThread(
                    callback,
                    json.optInt("outMaxWidth", 1280),
                    json.optInt("outMaxHeight", 1280),
                    json.optInt("inMinWidth", 1),
                    json.optInt("inMinHeight", 1),
                    imageID
            ).start();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean getPicture(String arg) {
        try {
            JSONObject json = new JSONObject(arg);
            String callback = json.optString(KEY_CALLBACK);
            if (TextUtils.isEmpty(callback)) {
                return true;
            }
            int source = json.optInt("source", 0);
            if (source == 0) { //from system album
                try {
                    Intent intent = new Intent()
                            .setType("image/*")
                            .setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, CODE_PHOTO_LIBRARY);
                    PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                            .putString(KEY_GET_PICTURE_PARAM, json.toString())
                            .apply();
                } catch (Exception e) {
                    callJs(callback, "2", "[]");
                    return true;
                }
            } else if (source == 1) { //from camera
                //启动相机之前先停止声音播放
                //MediaPlayerManager.getInstance(app).stop(true);
                String basePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/Tencent/photo/";
                File folder = new File(basePath);
                if (!folder.exists()) {
                    if (!folder.mkdirs()) {
                        WeishiToastUtils
                                .show(mRuntime.context, "无SD卡，请插入SD卡后再试", Toast.LENGTH_SHORT);
                        callJs(callback, "2", "[]");
                        return true;
                    }
                }
                // 兼容moto手机：拍照后不返回照片的处理方法，先将拍照图片的path存到setting
                String path = basePath + System.currentTimeMillis() + ".jpg";
                Uri uploadPhotoUri = Uri.fromFile(new File(path));
                // PreferenceManager.getDefaultSharedPreferences(context).edit().putString(AppConstants.Preferences.CAMERA_PHOTO_PATH, path).commit();
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uploadPhotoUri);
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 100);
                if (json.optBoolean("front", false)) {
                    intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
                }
                try {
                    startActivityForResult(intent, CODE_OPEN_CAMERA);
                    PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                            .putString(CAMERA_PHOTO_PATH, path)
                            .putString(KEY_GET_PICTURE_PARAM, json.toString())
                            .apply();
                } catch (Exception e) {
                    e.printStackTrace();
                    WeishiToastUtils.show(mRuntime.context, "相机启动失败", Toast.LENGTH_SHORT);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
        if (mPicturesThread != null) {
            mPicturesThread.interrupt();
        }
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    //播放声音用,MediaPlayer播放一个声音太慢
    protected SoundPool mSoundPool;
    //缓存下加载后的id,重复播放时不用重新加载
    protected HashMap<String, Integer> mLoadMap;

    /**
     * 预加载声音
     *
     * @param bid 离线包的id
     * @param url 音频的路径形式如：http://vip.qq.com/sound.mp3
     */
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.FROYO)
    public boolean preloadSound(String bid, final String url, final String callback) {
        if (TextUtils.isEmpty(url) || (mLoadMap != null && mLoadMap.containsKey(url))) {
            return false;
        }

        //设置看业务目录下有没有
        String sdcardPath =
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
        StringBuilder sb = new StringBuilder();
        sb.append(sdcardPath);
        sb.append(HTML_OFFLINE_HTML_TOOR_DIR);
        sb.append(bid);
        sb.append('/');

        //需要分离出文件路径
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        String prefix = "";
        if (scheme != null) {
            prefix = scheme + "://";
        }

        if (url.length() >= prefix.length()) {
            //在离线包内的路径
            String filePath = url.substring(prefix.length());
            sb.append(filePath);
        }

        File file = new File(sb.toString());
        if (!file.exists()) {
            return false;
        }

        if (mSoundPool == null) {
            mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        }

        if (mLoadMap == null) {
            mLoadMap = new HashMap<String, Integer>();
        }

        if (Build.VERSION.SDK_INT >= 8) {
            mSoundPool.setOnLoadCompleteListener(null);
        }
        final int loadedId = mSoundPool.load(file.getAbsolutePath(), 1);

        if (loadedId == 0) {
//            if (QLog.isColorLevel()) {
//                QLog.d("SensorApi", QLog.CLR, "load failure url=" + url);
//            }
            return false;
        }
        mLoadMap.put(url, loadedId);
        return true;
    }


    protected static final int HANDLER_LISTEN = 0x123;

    protected Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == HANDLER_LISTEN) {
                updateMicStatus((String) msg.obj);
            }

        }

        ;
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
     * 播放声音
     *
     * @param bid 离线包的id
     * @param url 音频的路径形式如：http://vip.qq.com/sound.mp3
     * @return 播放成功return true,播放失败，找不到文件return false;
     */
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.FROYO)
    public boolean playSound(String bid, final String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        if (mSoundPool == null) {
            mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        }

        if (mLoadMap == null) {
            mLoadMap = new HashMap<String, Integer>();
        }

        if (!mLoadMap.containsKey(url)) {
            if (!preloadSound(bid, url, null)) {
                return false;
            }

            if (Build.VERSION.SDK_INT >= 8) {
                //等待加载完后播放
                mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                    @Override
                    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                        int result = mSoundPool.play(sampleId, 1, 1, 0, 0, 1.0f);
                        if (result == 0) {
//                            if (QLog.isColorLevel()) {
//                                QLog.d("SensorApi", QLog.CLR, "play failure url=" + url);
//                            }
                            return;
                        }
                    }
                });
            } else {
                //API8以上才能设置回调函数，蛋疼，只能延时一段时间, 但具体要多长时间无法知道。。
                final int sampleId = mLoadMap.get(url);
                if (mHandler != null) {
                    mHandler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            if (mSoundPool != null) {
                                int result = mSoundPool.play(sampleId, 1, 1, 0, 0, 1.0f);
//                                if (result == 0) {
//                                    if (QLog.isColorLevel()) {
//                                        QLog.d("SensorApi", QLog.CLR, "play failure url=" + url);
//                                    }
//                                }
                            }
                        }
                    }, 200);
                }
            }
        } else {
            int loadedId = mLoadMap.get(url);
            int result = mSoundPool.play(loadedId, 1, 1, 0, 0, 1.0f);
            if (result == 0) {
//                if (QLog.isColorLevel()) {
//                    QLog.d("SensorApi", QLog.CLR, "play failure url=" + url);
//                }
                return false;
            }
        }

        return true;
    }

    @Override
    public void onActivityResult(Intent intent, byte requestCode, int resultCode) {
        super.onActivityResult(intent, requestCode, resultCode);
        if (requestCode == CODE_OPEN_CAMERA || requestCode == CODE_PHOTO_LIBRARY) {
            SharedPreferences pref = PreferenceManager
                    .getDefaultSharedPreferences(mRuntime.context);
            String path = pref.getString(CAMERA_PHOTO_PATH, "");
            String savedParam = pref.getString(KEY_GET_PICTURE_PARAM, "");
            PreferenceManager.getDefaultSharedPreferences(mRuntime.context).edit()
                    .remove(CAMERA_PHOTO_PATH)
                    .remove(KEY_GET_PICTURE_PARAM)
                    .apply();
            if (TextUtils.isEmpty(savedParam)) {
                return;
            }
            String callback;
            JSONObject json;
            try {
                json = new JSONObject(savedParam);
                callback = json.getString(KEY_CALLBACK);
                if (TextUtils.isEmpty(callback)) {
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            if (resultCode == Activity.RESULT_OK) {
                onResultOK(intent, requestCode, path, callback, json);
            } else {
                callJs(callback, "1", "[]");
            }
        }
    }

    private void onResultOK(Intent intent, byte requestCode, String path, String callback,
            JSONObject json) {
        String[] paths;
        if (requestCode == CODE_OPEN_CAMERA) {
            paths = new String[]{path};
        } else { // requestCode == CODE_PHOTO_LIBRARY
            Uri uri = intent != null ? intent.getData() : null;
            if (uri != null) {
                paths = new String[]{uri.toString()};
            } else {
                callJs(callback, "2", "[]");
                return;
            }
        }
        if (callJsNotUrlOnly(callback, json, paths)) {
            return;
        }
        if (mLoadingDialog == null) {
            Activity activity = mRuntime.getActivity();
            mLoadingDialog = new ProgressDialog(activity);
            mLoadingDialog.setMessage("正在处理，请稍候…");
            mLoadingDialog.setOnCancelListener(this);
        }
        startPicturesThread(callback, json, paths);
    }

    private boolean callJsNotUrlOnly(String callback, JSONObject json, String[] paths) {
        if (json.optBoolean("urlOnly", false)) {
            try {
                JSONArray images = new JSONArray();
                for (String p : paths) {
                    JSONObject obj = new JSONObject();
                    obj.put(KEY_DATA, "");
                    obj.put(KEY_IMAGE_ID, p);
                    obj.put(KEY_MATCH, 0);
                    images.put(obj);
                }
                callJs(callback, "0", images.toString());
            } catch (JSONException e) {
                callJs(callback, "2", "[]");
            }
            return true;
        }
        return false;
    }

    private void startPicturesThread(String callback, JSONObject json, String[] paths) {
        if (mPicturesThread != null) {
            mPicturesThread.interrupt();
        }
        if (!mLoadingDialog.isShowing()) {
            DialogShowUtils.show(mLoadingDialog);
        }
        String method = json.optString("method");
        mPicturesThread = new PreparePicturesAndCallbackThread(
                callback,
                json.optInt("outMaxWidth", 1280),
                json.optInt("outMaxHeight", 1280),
                json.optInt("inMinWidth", 1),
                json.optInt("inMinHeight", 1),
                paths
        );
        mPicturesThread.start();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (mPicturesThread != null) {
            mPicturesThread.interrupt();
        }
    }

    private class PreparePicturesAndCallbackThread extends Thread {

        String mCallback;
        int mOutMaxWidth;
        int mOutMaxHeight;
        int mInMinWidth;
        int mInMinHeight;
        String[] mPaths;

        public PreparePicturesAndCallbackThread(String callback, int maxWidth, int maxHeight,
                int minWidth, int minHeight, String[] paths) {
            mCallback = callback;
            mOutMaxWidth = maxWidth;
            mOutMaxHeight = maxHeight;
            mInMinWidth = minWidth;
            mInMinHeight = minHeight;
            mPaths = paths;
        }

        @Override
        public void run() {
            JSONArray images = new JSONArray();
            try {
                for (int i = 0, len = mPaths.length; i < len; ++i) {
                    if (isInterrupted()) {
                        throw new InterruptedException();
                    }
                    String path = mPaths[i];
                    if (path.startsWith("content://")) {
                        path = getFilePathFromContentUri(Uri.parse(path),
                                mRuntime.context.getContentResolver());
                    } else if (path.startsWith("file://")) {
                        path = path.substring(7);
                    }
                    JSONObject obj = packImageObject(path, mInMinWidth, mInMinHeight, mOutMaxWidth,
                            mOutMaxHeight);
                    images.put(obj);
                }
                if (isInterrupted()) {
                    throw new InterruptedException();
                }
                callJs(mCallback, "0", images.toString());
            } catch (OutOfMemoryError oome) {
                System.gc();
                callJs(mCallback, "3", "[]");
            } catch (IOException ioe) {
                callJs(mCallback, "2", "[]");
            } catch (JSONException je) {
                callJs(mCallback, "2", "[]");
            } catch (InterruptedException ie) {
                Activity activity = mRuntime.getActivity();
                if (activity != null && !activity.isFinishing()) {
                    callJs(mCallback, "1", "[]");
                }
            } finally {
                if (mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
            }
        }
    }

    private class PrepareSinglePictureAndCallbackThread extends Thread {

        String mCallback;
        int mOutMaxWidth;
        int mOutMaxHeight;
        int mInMinWidth;
        int mInMinHeight;
        String mPath;

        public PrepareSinglePictureAndCallbackThread(String callback, int maxWidth, int maxHeight,
                int minWidth, int minHeight, String path) {
            mCallback = callback;
            mOutMaxWidth = maxWidth;
            mOutMaxHeight = maxHeight;
            mInMinWidth = minWidth;
            mInMinHeight = minHeight;
            mPath = path;
        }

        @Override
        public void run() {
            String path = mPath;
            try {
                if (path.startsWith("content://")) {
                    path = getFilePathFromContentUri(Uri.parse(path),
                            mRuntime.context.getContentResolver());
                } else if (path.startsWith("file://")) {
                    path = path.substring(7);
                }
                JSONObject obj = packImageObject(path, mInMinWidth, mInMinHeight, mOutMaxWidth,
                        mOutMaxHeight);
                if (isInterrupted()) {
                    throw new InterruptedException();
                }
                callJs(mCallback, "0", obj.toString());
            } catch (OutOfMemoryError oome) {
                System.gc();
                callJs(mCallback, "3", "{}");
            } catch (IOException ioe) {
                callJs(mCallback, "2", "{}");
            } catch (JSONException je) {
                callJs(mCallback, "2", "{}");
            } catch (InterruptedException ie) {
                Activity activity = mRuntime.getActivity();
                if (activity != null && !activity.isFinishing()) {
                    callJs(mCallback, "1", "{}");
                }
            } finally {
                if (mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
            }
        }
    }

    /**
     * 从本地path读取图片, 以json对象返回base64数据返回
     *
     * @see <a href="http://mqq.oa.com/api/index.html#media.getPicture">media.getPicture</a>, <a
     * href="http://mqq.oa.com/api/index.html#data.getLocalImage">data.getLocalImage</a>
     */
    static JSONObject packImageObject(String path, int minInputWidth, int minInputHeight,
            int maxOutputWidth, int maxOutputHeight)
            throws JSONException, IOException, InterruptedException, OutOfMemoryError {
        JSONObject obj = new JSONObject();
        File file = new File(path);
        if (file.length() < 3) { //太小, 不可能是图片
            throw new IOException();
        } else {
            //先看看图片有多大
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opt);
            int realWidth = opt.outWidth;
            int realHeight = opt.outHeight;
            if (realWidth < 0 || realHeight < 0) { //不是图片
                throw new IOException();
            }
            if (realWidth < minInputWidth || realHeight < minInputHeight) {
                //这图片也太小了吧
                obj.put(KEY_MATCH, 1);
            } else if (realWidth <= maxOutputWidth && realHeight <= maxOutputHeight) {
                readImageInfo(path, obj, file);
            } else {
                //图片太大了，先确定一个inSampleSize缩放系数
                int rate = Math.max(realWidth / maxOutputWidth, realHeight / maxOutputHeight);
                opt.inJustDecodeBounds = false;
                // round down to power of 2
                // "Smear" the high-order bit all the way to the right
                rate |= rate >>> 1;
                rate |= rate >>> 2;
                rate |= rate >>> 4;
                rate |= rate >>> 8;
                rate |= rate >>> 16;
                opt.inSampleSize = (rate + 1) >>> 1;
                //解图片
                Bitmap bmp = BitmapFactory.decodeFile(path, opt);
                if (bmp == null) {
                    return obj;
                }
                float scale;
                if (realWidth * maxOutputHeight > realHeight * maxOutputWidth) {
                    scale = maxOutputWidth / (float) bmp.getWidth();
                } else {
                    scale = maxOutputHeight / (float) bmp.getHeight();
                }
                Matrix m = getMatrixFromPath(path, scale);
                Bitmap targetBmp = Bitmap
                        .createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                if (!bmp.equals(targetBmp)) { //maybe same object
                    bmp.recycle();
                }
                ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
                String minetype = opt.outMimeType;
                StringBuilder encoded;
                if ("image/png".equalsIgnoreCase(minetype) || "image/gif".equals(minetype)
                        || "image/bmp".equals(minetype)) {
                    encoded = new StringBuilder("data:image/png;base64,");
                    targetBmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                } else {
                    encoded = new StringBuilder("data:image/jpeg;base64,");
                    targetBmp.compress(Bitmap.CompressFormat.JPEG, 80, os);
                }
                targetBmp.recycle();
                encoded.append(Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP));
                obj.put(KEY_MATCH, 0);
                obj.put(KEY_DATA, encoded);
                obj.put(KEY_IMAGE_ID, path); //todo 这是原图path
            }
        }
        return obj;
    }

    @NotNull
    private static Matrix getMatrixFromPath(String path, float scale) {
        Matrix m = new Matrix();
        try {
            // 根据exif参数, 旋转到正确的方向
            // http://sylvana.net/jpegcrop/exif_orientation.html
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    m.setScale(-scale, scale);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    m.setScale(scale, scale);
                    m.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    m.setScale(scale, -scale);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    m.setScale(scale, -scale);
                    m.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    m.setScale(scale, scale);
                    m.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    m.setScale(-scale, scale);
                    m.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    m.setScale(scale, scale);
                    m.postRotate(270);
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                case ExifInterface.ORIENTATION_UNDEFINED:
                default:
                    m.setScale(scale, scale);
                    break;
            }
        } catch (IOException e) {
            m.setScale(scale, scale);
        }
        return m;
    }

    private static void readImageInfo(String path, JSONObject obj, File file)
            throws IOException, InterruptedException, JSONException {
        InputStream is = null;
        //符合大小限制，直接返回文件数据
        try {
            is = new FileInputStream(file);
            StringBuilder encoded;
            // 根据文件头2个字节判断文件格式
            int a = is.read();
            int b = is.read();
            int c = is.read();
            if (a == 0xff && b == 0xd8) {
                encoded = new StringBuilder("data:image/jpeg;base64,");
            } else if (a == 0x42 && b == 0x4d) {
                encoded = new StringBuilder("data:image/bmp;base64,");
            } else if (a == 0x89 && b == 0x50) {
                encoded = new StringBuilder("data:image/png;base64,");
            } else if (a == 0x47 && b == 0x49) {
                encoded = new StringBuilder("data:image/gif;base64,");
            } else {
                encoded = new StringBuilder("data:base64,");
            }
            encoded.append(Base64.encodeToString(new byte[]{(byte) a, (byte) b, (byte) c},
                    Base64.NO_WRAP));
            byte[] input = new byte[30720]; //分段编码, 30k
            int n;
            while ((n = is.read(input)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                if (n < 30720) {
                    byte[] tmp = new byte[n];
                    System.arraycopy(input, 0, tmp, 0, n);
                    encoded.append(Base64.encodeToString(tmp, Base64.NO_WRAP));
                } else {
                    encoded.append(Base64.encodeToString(input, Base64.NO_WRAP));
                }
            }
            obj.put(KEY_MATCH, 0);
            obj.put(KEY_DATA, encoded);
            obj.put(KEY_IMAGE_ID, path);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static String getFilePathFromContentUri(Uri selectedVideoUri,
            ContentResolver contentResolver) {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn, null, null, null);

        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }
}
