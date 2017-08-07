package com.example.lzh.nystagmus.Utils;

/**
 * Created by LZH on 2017/7/17.
 */

public class Tool {
    public static final int ALL_EYE=5;//摄像头双眼都在
    public static final int NOT_LEYE=2;//摄像头没有左眼
    public static final int NOT_REYE=0;//摄像头没有右眼
    public static final int NOT_ALLEYE=8;//摄像头双眼都没有
    public static final int VEDIO_EYE=1;//本地视频双眼都在
    public static final int VEDIO_ONLY_EYE=6;//本地视频单眼

    public static final String TAG="H.J.Y";

    public static String AddressLeftEye="http://192.168.1.22:8080/?action=stream?dummy=param.mjpg";//左眼网络地址
    public static String AddressRightEye="http://192.168.1.22:8090/?action=stream?dummy=param.mjpg";//右眼网络地址

    public static final String AddressLeftEyeDefault="http://192.168.1.22:8080/?action=stream?dummy=param.mjpg";//左眼网络地址
    public static final String AddressRightEyeDefault="http://192.168.1.22:8090/?action=stream?dummy=param.mjpg";//右眼网络地址

    public static final int TimerSecondNum=30;//1s时间内定时器的间隔
    public static final int HighTidePeriodSecond=3;//最大眼震反应期时间
}
