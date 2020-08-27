package com.tencent.mobileqq.webviewplugin.util;


import com.tencent.mobileqq.webviewplugin.annotation.Public;

public abstract class Singleton<T, P> {

    private volatile T mInstance;

    protected abstract T create(P p);

    @Public
    public final T get(P p) {
        if (mInstance == null) {
            synchronized (this) {
                if (mInstance == null) {
                    mInstance = create(p);
                }
            }
        }
        return mInstance;
    }
}
