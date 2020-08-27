package com.tencent.mobileqq.webviewplugin.util;

import java.io.File;


public class FileUtil {

    /**
     * 删除文件
     **/
    public static boolean deleteFile(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; ++i) {
                    deleteFile(files[i]);
                }
            }
        }
        return f.delete();
    }

}
