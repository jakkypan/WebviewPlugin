package com.tencent.mobileqq.webviewplugin.util;

import android.content.Context;
import android.util.Log;
import com.tencent.mobileqq.webviewplugin.annotation.Public;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Tencent. Author: raezlu Date: 13-7-8 Time: 涓嬪崍4:15
 */
@Public
public class LogUtil {

    private static final String TAG = "webPlugin.LogUtil";
    public static final boolean D_FLAG = false;

    public static final boolean I_FLAG = false;

    public static final int VERBOSE = 2;

    public static final int DEBUG = 3;

    public static final int INFO = 4;

    public static final int WARN = 5;

    public static final int ERROR = 6;

    private final static String LOG_DIR_NAME = "log";

    private final static String LOG_FILE_NAME = "runtime.log";

    private final static String[] LOGCAT_COMMAND = new String[]{
            "logcat",
            "-d",
            "-v",
            "time"
    };

    public interface LogProxy {

        public void v(String tag, String msg);

        public void d(String tag, String msg);

        public void i(String tag, String msg);

        public void w(String tag, String msg);

        public void e(String tag, String msg);

        public void flush();
    }

    private final static LogProxy DEFAULT_PROXY = new LogProxy() {
        @Override
        public void v(String tag, String msg) {
            Log.v(tag, String.valueOf(msg));
        }

        @Override
        public void d(String tag, String msg) {
            Log.d(tag, String.valueOf(msg));
        }

        @Override
        public void i(String tag, String msg) {
            Log.i(tag, String.valueOf(msg));
        }

        @Override
        public void w(String tag, String msg) {
            Log.w(tag, String.valueOf(msg));
        }

        @Override
        public void e(String tag, String msg) {
            Log.e(tag, String.valueOf(msg));
        }

        @Override
        public void flush() {
            // empty.
        }
    };

    private static volatile LogProxy sProxy = DEFAULT_PROXY;

    @Public
    public static void v(String tag, String msg) {
        LogProxy proxy = getProxy();
        proxy.v(tag, msg);
    }

    @Public
    public static void v(String tag, String msg, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.v(tag, msg + '\n' + getStackTraceString(tr));
    }

    @Public
    public static void d(String tag, String msg) {
        LogProxy proxy = getProxy();
        proxy.d(tag, msg);
    }

    @Public
    public static void d(String tag, String msg, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.d(tag, msg + '\n' + getStackTraceString(tr));
    }

    @Public
    public static void i(String tag, String msg) {
        LogProxy proxy = getProxy();
        proxy.i(tag, msg);
    }

    @Public
    public static void i(String tag, String msg, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.i(tag, msg + '\n' + getStackTraceString(tr));
    }

    @Public
    public static void w(String tag, String msg) {
        LogProxy proxy = getProxy();
        proxy.w(tag, msg);
    }

    @Public
    public static void w(String tag, String msg, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.w(tag, msg + '\n' + getStackTraceString(tr));
    }

    @Public
    public static void w(String tag, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.w(tag, getStackTraceString(tr));
    }

    @Public
    public static void e(String tag, String msg) {
        LogProxy proxy = getProxy();
        proxy.e(tag, msg);
    }

    @Public
    public static void e(String tag, String msg, Throwable tr) {
        LogProxy proxy = getProxy();
        proxy.e(tag, msg + '\n' + getStackTraceString(tr));
    }

    @Public
    public static void flush() {
        LogProxy proxy = getProxy();
        proxy.flush();
    }

    public static void setProxy(LogProxy proxy) {
        synchronized (LogUtil.class) {
            sProxy = proxy;
        }
    }

    private static LogProxy getProxy() {
        LogProxy proxy = sProxy;
        return proxy != null ? proxy : DEFAULT_PROXY;
    }

    private static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    @Public
    public static void saveCurrentLogcat(Context context) {
        LogWriter logWriter = null;
        try {
            File logFile = getLogFile(context);
            if (logFile == null || !logFile.exists()) {
                return;
            }
            logWriter = new LogFileWriter(logFile);
            saveLogcat(logWriter);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (logWriter != null) {
                try {
                    logWriter.flush();
                    logWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void saveLogcat(LogWriter logWriter) {

        Process process = null;
        BufferedReader reader = null;
        int count = 0;
        try {
            process = Runtime.getRuntime().exec(LOGCAT_COMMAND);
            reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));

            String line;
            while ((line = reader.readLine()) != null) {
                logWriter.write(line);
                ++count;
            }

        } catch (Throwable e) {
            e.printStackTrace();
            // empty.
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
        try {
            if (count == 0) {
                ProcessBuilder pb = new ProcessBuilder(LOGCAT_COMMAND);
                pb.redirectErrorStream(true);
                if (process != null) {
                    process.destroy();
                }
                process = pb.start();
                if (process == null) {
                    return;
                }
                OutputStream outputStream = process.getOutputStream();
                if (outputStream != null) {
                    outputStream.close();
                }
                reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
                String line;
                while ((line = reader.readLine()) != null) {
                    logWriter.write(line);
                    ++count;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            // empty.
        } finally {
            IoUtils.close(reader);
            if (process != null) {
                process.destroy();
            }
        }

    }

    public static File getLogFile(Context context) {
        String path = StorageUtils.getExternalCacheDirExt(context, LOG_DIR_NAME, true);
        if (path == null) {
            return null;
        }
        File file = new File(path + File.separator + LOG_FILE_NAME);
        File dir = file.getParentFile();
        if (dir == null) {
            return null;
        }
        if (!dir.exists()) {
            boolean mkFlag = dir.mkdirs();
            if (!mkFlag) {
                LogUtil.e(TAG, "[getLogFile] mkdirs failed!");
            }
        }
        if (!file.exists()) {
            try {
                boolean createFlag = file.createNewFile();
                if (!createFlag) {
                    LogUtil.e(TAG, "[getLogFile] createNewFile exists!");
                }
            } catch (IOException e) {
                LogUtil.e(TAG, "[getLogFile] createFile:" + e);
            }
        }
        return file;
    }

    private static interface LogWriter {

        public void close() throws IOException;

        public void flush() throws IOException;

        public void write(String str) throws IOException;
    }

    private static class LogFileWriter implements LogWriter {

        private final Writer mWriter;

        public LogFileWriter(File file) throws IOException {
            mWriter = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), Charset.defaultCharset()));
        }

        @Override
        public void close() throws IOException {
            mWriter.close();
        }

        @Override
        public void flush() throws IOException {
            LogUtil.flush();
            mWriter.flush();
        }

        @Override
        public void write(String str) throws IOException {
            mWriter.write(str);
            mWriter.flush();
        }
    }

}
