package com.tencent.mobileqq.webviewplugin.util;

import android.util.Base64;
import java.io.UnsupportedEncodingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class TripleDes {

    private static final String CHAR_SET = "UTF-8";
    private static final String ALGORITHM_3DES = "DESede"; // 3DES加密

    /**
     * 加密方法
     *
     * @param data 源数据
     * @param key 加密的key,
     */
    public static byte[] encrypt(byte[] data, byte[] key) {
        try {
            SecretKey deskey = new SecretKeySpec(build3DesKey(key), ALGORITHM_3DES); // 生成密钥
            Cipher c1 = Cipher.getInstance(ALGORITHM_3DES); // 实例化负责加密/解密的Cipher工具类
            c1.init(Cipher.ENCRYPT_MODE, deskey); // 初始化为加密模式
            return c1.doFinal(data);
        } catch (java.security.NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (javax.crypto.NoSuchPaddingException e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }

        return null;

    }

    /**
     * @param encryptData 密文数据
     * @param key 解密的key
     * @return 解密后的内容
     */
    public static byte[] decrypt(byte[] encryptData, byte[] key) {
        try {
            SecretKey deskey = new SecretKeySpec(build3DesKey(key), ALGORITHM_3DES);
            Cipher c1 = Cipher.getInstance(ALGORITHM_3DES);
            c1.init(Cipher.DECRYPT_MODE, deskey); // 初始化为解密模式
            return c1.doFinal(encryptData);
        } catch (java.security.NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (javax.crypto.NoSuchPaddingException e2) {
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        return null;
    }

    /**
     * 加密方法
     *
     * @param data 源数据
     * @param key 加密的key,
     */
    public static String encode(String data, String key) {
        try {
            byte[] srcByte = data.getBytes(CHAR_SET);
            byte[] keyByte = key.getBytes(CHAR_SET);
            byte[] result = encrypt(srcByte, keyByte);
            return result != null ? Base64.encodeToString(result, Base64.NO_WRAP) : null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param encryptData 密文数据
     * @param key 解密的key
     * @return 解密后的内容
     */
    public static String decode(String encryptData, String key) {
        try {
            byte[] keyByte = key.getBytes(CHAR_SET);
            byte[] result = decrypt(Base64.decode(encryptData, Base64.DEFAULT), keyByte);
            return result != null ? new String(result, CHAR_SET) : null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
     * 根据字符串生成密钥字节数组
     * 因为3des最大只能用24字节的key, 所以对传入的key要做裁减.
     * @param keyByte
     *      源key
     * @return
     *      返回一个24字节长度的key
     * @throws UnsupportedEncodingException
     */
    private static byte[] build3DesKey(byte[] keyByte) throws UnsupportedEncodingException {
        byte[] key = new byte[24]; // 声明一个24位的字节数组，默认里面都是0
        /*
         * 执行数组拷贝
         * System.arraycopy(源数组，从源数组哪里开始拷贝，目标数组，拷贝多少位)
         */
        if (key.length > keyByte.length) {
            // 如果temp不够24位，则拷贝temp数组整个长度的内容到key数组中
            System.arraycopy(keyByte, 0, key, 0, keyByte.length);
        } else {
            // 如果temp大于24位，则拷贝temp数组24个长度的内容到key数组中
            System.arraycopy(keyByte, 0, key, 0, key.length);
        }

        return key;
    }
}
