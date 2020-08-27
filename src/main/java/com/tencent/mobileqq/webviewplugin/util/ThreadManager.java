package com.tencent.mobileqq.webviewplugin.util;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import com.tencent.mobileqq.webviewplugin.BuildConfig;
import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadManager {

    // 本地改就行..不要传svn
    // 这里为了监控FileThread的耗时，先把它在debug版本打开
    public static final boolean DEBUG_THREAD = BuildConfig.DEBUG;

    /**
     * AsyncTask的默认线程池Executor. 负责长时间的任务(网络访问) 默认3个线程
     */
    public static final Executor NETWORK_EXECUTOR;

    /**
     * 副线程的Handle, 只有一个线程 可以执行比较快但不能在ui线程执行的操作. 文件读写不建议在此线程执行, 请使用FILE_THREAD_HANDLER
     * 此线程禁止进行网络操作.如果需要进行网络操作. 请使用NETWORK_EXECUTOR
     */
    private volatile static Handler SUB_THREAD_HANDLER;

    private static HandlerThread SUB_THREAD;

    /**
     * 文件读写线程的Handle, 只有一个线程 可以执行文件读写操作, 如图片解码等 此线程禁止进行网络操作.如果需要进行网络操作. 请使用NETWORK_EXECUTOR
     */
    private volatile static Handler FILE_THREAD_HANDLER;
    /**
     * 文件读写用的线程
     */
    private static HandlerThread FILE_THREAD;

    static {
        NETWORK_EXECUTOR = initNetworkExecutor();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static Executor initNetworkExecutor() {
        Executor result = null;
        // 3.0以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (DEBUG_THREAD) {
                try {
                    ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 128, 1,
                            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10),
                            new ThreadFactory() {
                                private final AtomicInteger mCount = new AtomicInteger(1);

                                public Thread newThread(Runnable r) {
                                    return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
                                }
                            });
                    Field field = AsyncTask.class.getDeclaredField("THREAD_POOL_EXECUTOR");
                    field.setAccessible(true);
                    field.set(null, executor);

                    field = AsyncTask.class.getDeclaredField("sDefaultExecutor");
                    field.setAccessible(true);
                    field.set(null, executor);

                    result = executor;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                result = AsyncTask.THREAD_POOL_EXECUTOR;
            }
        }
        // 3.0以下, 反射获取
        else {
            Executor tmp = null;
            try {
                Field field = AsyncTask.class.getDeclaredField("sExecutor");
                field.setAccessible(true);
                tmp = (Executor) field.get(null);
            } catch (Exception e) {
                // 反射失败
                tmp = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>());
            }
            result = tmp;
        }

        if (result instanceof ThreadPoolExecutor) {
            // core size减少为3个
            ThreadPoolExecutor tmp = (ThreadPoolExecutor) result;
            tmp.setCorePoolSize(3);
            tmp.setRejectedExecutionHandler(new ShowQueueAbortPolicy());

        }

        return result;
    }

    public static void init() {

    }

    /**
     * 在网络线程上执行异步操作. 该线程池负责网络请求等操作 长时间的执行(如网络请求使用此方法执行) 当然也可以执行其他 线程和AsyncTask公用
     */
    public static void executeOnNetWorkThread(Runnable run) {
        try {
            NETWORK_EXECUTOR.execute(run);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得文件线程的Handler.<br> 副线程可以执行本地文件读写等比较快但不能在ui线程执行的操作.<br>
     * <b>此线程禁止进行网络操作.如果需要进行网络操作. 请使用NETWORK_EXECUTOR</b>
     *
     * @return handler
     */
    public static Handler getFileThreadHandler() {
        if (FILE_THREAD_HANDLER == null) {
            synchronized (ThreadManager.class) {
                if (FILE_THREAD_HANDLER == null) {
                    FILE_THREAD = new HandlerThread("QQ_FILE_RW");
                    FILE_THREAD.start();
                    FILE_THREAD_HANDLER = new Handler(FILE_THREAD.getLooper());
                }
            }
        }
        return FILE_THREAD_HANDLER;
    }

    public static Looper getFileThreadLooper() {
        return getFileThreadHandler().getLooper();
    }

    public static Thread getSubThread() {
        if (SUB_THREAD == null) {
            getSubThreadHandler();
        }
        return SUB_THREAD;
    }

    /**
     * 获得副线程的Handler.<br> 副线程可以执行比较快但不能在ui线程执行的操作.<br> 另外, 文件读写建议放到FileThread中执行
     * <b>此线程禁止进行网络操作.如果需要进行网络操作. 请使用NETWORK_EXECUTOR</b>
     *
     * @return handler
     */
    public static Handler getSubThreadHandler() {
        if (SUB_THREAD_HANDLER == null) {
            synchronized (ThreadManager.class) {
                if (SUB_THREAD_HANDLER == null) {
                    SUB_THREAD = new HandlerThread("QQ_SUB");
                    SUB_THREAD.start();
                    SUB_THREAD_HANDLER = new Handler(SUB_THREAD.getLooper());
                }
            }
        }
        return SUB_THREAD_HANDLER;
    }

    public static Looper getSubThreadLooper() {
        return getSubThreadHandler().getLooper();
    }

    /**
     * 在副线程执行. <br> 可以执行本地文件读写等比较快但不能在ui线程执行的操作.<br>
     * <b>此线程禁止进行网络操作.如果需要进行网络操作. 请使用NETWORK_EXECUTOR</b>
     */
    public static void executeOnSubThread(Runnable run) {
        getSubThreadHandler().post(run);
    }

    /**
     * 副线程可以执行本地文件读写等比较快但不能在ui线程执行的操作.<br>
     * <b>此线程禁止进行网络操作.如果需要进行网络操作. 请使用NETWORK_EXECUTOR</b>
     */
    public static void executeOnFileThread(Runnable run) {
        getFileThreadHandler().post(run);
    }

    public static class ShowQueueAbortPolicy extends AbortPolicy {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            BlockingQueue<Runnable> bq = e.getQueue();
            if (bq != null && !bq.isEmpty()) {
                try {
                    for (Runnable bu : bq) {
                        try {
                            Field field;
                            field = bu.getClass().getDeclaredField("this$0");
                            field.setAccessible(true);
                        } catch (NoSuchFieldException e1) {
                            e1.printStackTrace();
                        }
                    }
                } catch (IllegalArgumentException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
            super.rejectedExecution(r, e);
        }
    }
}
