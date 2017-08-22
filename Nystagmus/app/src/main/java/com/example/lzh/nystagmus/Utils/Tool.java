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

    public static String AddressLeftEye="http://192.168.43.119:8080/?action=stream?dummy=param.mjpg";//左眼网络地址
    public static String AddressRightEye="http://192.168.43.119:8090/?action=stream?dummy=param.mjpg";//右眼网络地址

    public static final String AddressLeftEyeDefault="http://192.168.43.119:8080/?action=stream?dummy=param.mjpg";//左眼网络地址
    public static final String AddressRightEyeDefault="http://192.168.43.119:8090/?action=stream?dummy=param.mjpg";//右眼网络地址

    public static final String RequestBingPic="http://guolin.tech/api/bing_pic";
    public static final String EggRoseAddress="http://img.ivsky.com/img/tupian/pre/201611/10/piaoliang_de_meigui-008.jpg";
    public static final String EggCakeAddress="http://img.wmtp.net/wp-content/uploads/2016/11/1125_cake_1.jpeg";

    public static final int TimerSecondNum=50;//1s时间内定时器的间隔
    public static final int HighTidePeriodSecond=10;//最大眼震反应期时间,单位为s
    public static final float SPVMaxValue=0.5f;//SPV最大临界值，超过这个值即眼震眩晕异常
    public static final float SPVConversionRatio=10f;//在计算波形斜率时所用的换算比例

    public static int RecognitionGrayValue=45;
    public static final int RecognitionGrayValueDefault=45;

}
