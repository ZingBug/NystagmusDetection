package com.example.lzh.nystagmus.Utils;

/**
 * Created by LZH on 2017/7/17.
 */

public class Tool {
    public static final int ALL_EYE=5;
    public static final int NOT_LEYE=2;
    public static final int NOT_REYE=0;
    public static final int NOT_ALLEYE=8;
    public static final int VEDIO_EYE=1;
    public static final int VEDIO_ONLY_EYE=6;

    public static final String TAG="H.J.Y";

    public static String AddressLeftEye="http://192.168.1.233:8080/?action=stream?dummy=param.mjpg";//左眼网络地址
    public static String AddressRightEye="http://192.168.1.233:8090/?action=stream?dummy=param.mjpg";//右眼网络地址

    public static final String AddressLeftEyeDefault="http://192.168.1.233:8080/?action=stream?dummy=param.mjpg";//左眼网络地址
    public static final String AddressRightEyeDefault="http://192.168.1.233:8090/?action=stream?dummy=param.mjpg";//右眼网络地址
}
