package com.example.lzh.nystagmus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.lzh.nystagmus.Utils.Box;
import com.example.lzh.nystagmus.Utils.Calculate;
import com.example.lzh.nystagmus.Utils.GetPath;
import com.example.lzh.nystagmus.Utils.L;
import com.example.lzh.nystagmus.Utils.PointFilter;
import com.example.lzh.nystagmus.Utils.T;
import com.example.lzh.nystagmus.Utils.ImgProcess;
import com.example.lzh.nystagmus.Utils.Tool;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.lzh.nystagmus.Utils.Calculate.MergeRealtimeAndMax;
import static com.example.lzh.nystagmus.Utils.Calculate.getPeriod;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView imageView_leye;
    private ImageView imageView_reye;
    private DrawerLayout mDrawerLayout;
    private static final int OPEN_VIDEO=1;
    private static final int OPEN_CAMERA=2;
    private FFmpegFrameGrabber capture;//打开本地视频
    private FFmpegFrameGrabber vacpLeft;//打开左眼网络视频
    private FFmpegFrameGrabber vacpRight;//打开右眼网络视频
    private Timer timer;//定时器
    private static final int Storage_RequestCode=1;//存储权限申请码
    private FFmpegFrameRecorder recorder;

    private Frame LeftFrame;
    private Frame AllFrame;
    private Frame RightFrame;
    private Frame tempLeftFrame;
    private Frame tempRightFrame;
    private static OpenCVFrameConverter.ToIplImage matConverter = new OpenCVFrameConverter.ToIplImage();//Mat转Frame
    private static OpenCVFrameConverter.ToIplImage matConverter_L = new OpenCVFrameConverter.ToIplImage();//Mat转Frame
    private static OpenCVFrameConverter.ToIplImage matConverter_R = new OpenCVFrameConverter.ToIplImage();//Mat转Frame
    private AndroidFrameConverter bitmapConverter = new  AndroidFrameConverter();//Frame转bitmap

    private Mat LeftFrameMat;
    private Mat RightFrameMat;
    private Bitmap LeftView;
    private Bitmap RightView;
    private Bitmap TempView;
    private Mat Leye;
    private Mat Reye;
    private Mat AllEyeMat;
    private Message message;
    private Message ChartMessage;
    private boolean IsTimerRun=false;
    private Mat blankMat;

    private int EyeNum=Tool.NOT_ALLEYE;//眼睛数目
    private boolean IsLeyeCenter=false;//用于判断左眼是否确定初始位置
    private boolean IsReyeCenter=false;//用于判断右眼是否确定初始位置
    private Box LeyeCenter=new Box();//用于保存左眼的初始位置
    private Box ReyeCenter=new Box();//用于保存右眼的初始位置
    private int FrameNum=0;//视频播放帧数

    private LineChart chart_x;//X波形图
    private LineChart chart_y;//y波形图
    private LineChart chart_rotation;//旋转图
    private int[] colors=new int[]{Color.rgb(255, 69, 0), Color.rgb(0, 128, 0)};//自定义颜色，第一种为橘黄色，第二种为纯绿色

    private SharedPreferences pref;//调用存储文件

    private Calculate calculate;//瞳孔数据计算器
    private int calNum;//计算时间间隔
    private int secondTime;//视频测试时间

    /*SPV相关*/
    private TextView LeyeXRealtimeAndMaxSPV;
    private TextView ReyeXRealtimeAndMaxSPV;
    private TextView LeyeYRealtimeAndMaxSPV;
    private TextView ReyeYRealtimeAndMaxSPV;
    private TextView LeyeHighperiod;
    private TextView ReyeHighperiod;
    private DecimalFormat df;//数据格式,double转string保留两位小数

    /*诊断相关*/
    private TextView DiagnosticResult;
    private TextView LeyeDirectionResult;
    private TextView ReyeDirectionResult;

    /*悬浮菜单按钮*/
    private FloatingActionsMenu menuChange;

    /*测试用*/
    private boolean IsTest=false;//用于判断是否开始测试

    /*视频保存名称*/
    private String VideoStorgeName;
    //private CoordinatorLayout MainContainer;

    /*滤波*/
    private PointFilter filterL;//左眼滤波器
    private PointFilter filterR;//右眼滤波器

    /*与上一帧做比较*/
    private Box preLeyeBox;
    private Box preReyeBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView_leye=(ImageView)findViewById(R.id.lefteye_view);
        imageView_reye=(ImageView)findViewById(R.id.righteye_view);
        mDrawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);

        LeyeXRealtimeAndMaxSPV=(TextView) findViewById(R.id.leyeXRealtimeAndMaxSPV);
        ReyeXRealtimeAndMaxSPV=(TextView) findViewById(R.id.reyeXRealtimeAndMaxSPV);
        LeyeYRealtimeAndMaxSPV=(TextView) findViewById(R.id.leyeYRealtimeAndMaxSPV);
        ReyeYRealtimeAndMaxSPV=(TextView) findViewById(R.id.reyeYRealtimeAndMaxSPV);
        LeyeHighperiod=(TextView) findViewById(R.id.leyeHighperiod);
        ReyeHighperiod=(TextView) findViewById(R.id.reyeHighperiod);
        DiagnosticResult=(TextView) findViewById(R.id.diagnosticResult);
        LeyeDirectionResult=(TextView) findViewById(R.id.leyeDirection);
        ReyeDirectionResult=(TextView) findViewById(R.id.reyeDirection);
        //MainContainer=(CoordinatorLayout) findViewById(R.id.main_container);

        /*初始化设置为0*/
        LeyeXRealtimeAndMaxSPV.setText("0/0");
        ReyeXRealtimeAndMaxSPV.setText("0/0");
        LeyeYRealtimeAndMaxSPV.setText("0/0");
        ReyeYRealtimeAndMaxSPV.setText("0/0");
        LeyeHighperiod.setText("0s");
        ReyeHighperiod.setText("0s");
        DiagnosticResult.setText(R.string.defalut);
        LeyeDirectionResult.setText(R.string.defalut);
        ReyeDirectionResult.setText(R.string.defalut);

        df= new DecimalFormat("##.#");//数据格式,float转string保留1位小数

        NavigationView navView=(NavigationView)findViewById(R.id.nav_view);
        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP)
        {
            //大于安卓5.0即API21版本可用
            //导航栏颜色与状态栏统一
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;//如果想要隐藏导航栏，可以加上View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            decorView.setSystemUiVisibility(option);
            //getWindow().setNavigationBarColor(Color.TRANSPARENT);//设置导航栏背景为透明
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            //getWindow().setNavigationBarColor(getResources().getColor(R.color.lightSteelBlue));
        }
        /*悬浮菜单按钮设置*/
        menuChange=(FloatingActionsMenu) findViewById(R.id.float_menu);
        ((com.getbase.floatingactionbutton.FloatingActionButton)findViewById(R.id.menu_openvideo)).setOnClickListener(this);
        ((com.getbase.floatingactionbutton.FloatingActionButton)findViewById(R.id.menu_opencamera)).setOnClickListener(this);
        ((com.getbase.floatingactionbutton.FloatingActionButton)findViewById(R.id.menu_startplay)).setOnClickListener(this);
        ((com.getbase.floatingactionbutton.FloatingActionButton)findViewById(R.id.menu_stopplay)).setOnClickListener(this);

        navView.setCheckedItem(R.id.nav_intro);//默认选中nav_intro菜单栏默认选中
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(MenuItem item)
            {
                switch (item.getItemId())
                {
                    case R.id.nav_intro:
                    {
                        //打开介绍界面活动
                        Intent intent=new Intent(MainActivity.this,IntroduceActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.nav_settings:
                    {
                        //打开软件设置界面
                        Intent intent=new Intent(MainActivity.this,SettingsActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case R.id.nav_video:
                    {
                        //打开视频列表界面
                        Intent intent=new Intent(MainActivity.this,VideoActivity.class);
                        startActivityForResult(intent,Tool.VideoTransmitTestCode);
                        break;
                    }
                    default:
                        break;
                }
                return true;
            }
        });

        chart_x=(LineChart)findViewById(R.id.xchart);
        chart_y=(LineChart)findViewById(R.id.ychart);
        chart_rotation=(LineChart) findViewById(R.id.rotation_chart);

        initialChart(chart_x,"水平位置");//初始化波形图
        initialChart(chart_y,"垂直位置");//初始化波形图
        initialChart(chart_rotation,"旋转曲线");//初始化旋转曲线

        pref=getSharedPreferences("CameraAddress",MODE_PRIVATE);
        Tool.AddressLeftEye=pref.getString("LeftCameraAddress",Tool.AddressLeftEye);
        Tool.AddressRightEye=pref.getString("RightCameraAddress",Tool.AddressRightEye);
        Tool.RecognitionGrayValue=pref.getInt("GrayValue",Tool.RecognitionGrayValue);

        blankMat=new Mat();
        TempView=BitmapFactory.decodeResource(getResources(),R.drawable.novideo);
        Frame TempFrame=bitmapConverter.convert(TempView);
        blankMat=matConverter.convertToMat(TempFrame);
        L.d("项目打开");
    }
    @Override
    public void onBackPressed()
    {
        //拦截Back键，使App进入后台而不是关闭
        Intent launcherIntent=new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(launcherIntent);
    }
    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.menu_openvideo:
            {
                if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(MainActivity.this,new String []{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},Storage_RequestCode);
                }else
                {
                    openVideo();
                }
                menuChange.collapse();
                break;
            }
            case R.id.menu_opencamera:
            {
                openCamera();
                menuChange.collapse();
                break;
            }
            case R.id.menu_startplay:
            {
                startPlay();
                menuChange.collapse();
                break;
            }
            case R.id.menu_stopplay:
            {
                stopPlay();
                menuChange.collapse();
                break;
            }
            default:
            {
                break;
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults)
    {
        switch (requestCode)
        {
            case Storage_RequestCode:
                if(grantResults.length>0&&grantResults[0]== PackageManager.PERMISSION_GRANTED)
                {
                    /*申请权限后的事情*/
                    openVideo();
                }
                else
                {
                    T.showShort(this,"拒绝权限将无法使用程序");
                    finish();
                }
                break;
            default:
                break;
        }
    }
    private void openCamera()
    {
        EyeNum=Tool.ALL_EYE;
        vacpLeft=new FFmpegFrameGrabber(Tool.AddressLeftEye);
        vacpRight=new FFmpegFrameGrabber(Tool.AddressRightEye);
        //vacpLeft=new FFmpegFrameGrabber("http://192.168.155.2:8080/video");
        //vacpRight=new FFmpegFrameGrabber("http://192.168.155.3:8080/video");
        try {
            vacpLeft.start();
        }
        catch (FrameGrabber.Exception e)
        {
            T.showShort(this,"左眼链接失败");
            L.d("左眼链接失败"+e.toString());
            EyeNum=Tool.NOT_LEYE;
        }
        try {
            vacpRight.start();
            vacpRight.setImageHeight(vacpRight.getImageHeight()-1);//此处减1是为了
        }
        catch (org.bytedeco.javacv.FrameGrabber.Exception e)
        {
            T.showShort(this,"右眼链接失败");
            L.d("右眼链接失败"+e.toString());
            if(EyeNum==Tool.ALL_EYE)
            {
                EyeNum=Tool.NOT_REYE;
            }
            else
            {
                EyeNum=Tool.NOT_ALLEYE;
                T.showShort(this,"双眼全部连接失败");
                L.d("双眼全部连接失败");
                return;
            }
        }

        if(IsTimerRun)
        {
            //如果正在处理视频，则立即停止当前定时器
            IsTimerRun = false;
            timer.cancel();
        }

        IsTest=false;//还没有开始测试
        if(EyeNum==Tool.NOT_REYE)
        {
            //如果没有右眼
            RightFrameMat=new Mat();
            TempView=BitmapFactory.decodeResource(getResources(),R.drawable.novideo);
            Frame TempFrame=bitmapConverter.convert(TempView);
            RightFrameMat=matConverter.convertToMat(TempFrame);
        }
        if(EyeNum==Tool.NOT_LEYE)
        {
            //如果没有左眼
            LeftFrameMat=new Mat();
            TempView=BitmapFactory.decodeResource(getResources(),R.drawable.novideo);
            Frame TempFrame=bitmapConverter.convert(TempView);
            LeftFrameMat=matConverter.convertToMat(TempFrame);
        }

        /*下面是参数初始化*/
        clearEntey(chart_x);
        clearEntey(chart_y);
        clearEntey(chart_rotation);
        IsLeyeCenter=false;
        IsReyeCenter=false;
        LeyeCenter=new Box();
        ReyeCenter=new Box();
        FrameNum=0;

        timer=new Timer();

        calculate=new Calculate();//瞳孔数据计算器

        filterL=new PointFilter();//左眼瞳孔坐标滤波器
        filterR=new PointFilter();//右眼瞳孔坐标滤波器

        calNum=0;//1s计算一次
        secondTime=0;//0s
        message=new Message();
        message.obj="视频开始播放";
        ToastHandle.sendMessage(message);
        IsTimerRun=true;

        LeyeXRealtimeAndMaxSPV.setText("0/0");
        ReyeXRealtimeAndMaxSPV.setText("0/0");
        LeyeYRealtimeAndMaxSPV.setText("0/0");
        ReyeYRealtimeAndMaxSPV.setText("0/0");
        LeyeHighperiod.setText("0s");
        ReyeHighperiod.setText("0s");
        DiagnosticResult.setText(R.string.defalut);
        LeyeDirectionResult.setText(R.string.defalut);
        ReyeDirectionResult.setText(R.string.defalut);
        DiagnosticResult.setTextColor(MainActivity.this.getResources().getColor(R.color.black));

        /*视频录制初始化*/
        switch (EyeNum){
            case Tool.NOT_LEYE:
            {
                //只有右眼
                VideoStorgeName=Tool.GetVideoStoragePath();
                recorder=new FFmpegFrameRecorder(VideoStorgeName,vacpRight.getImageWidth(),vacpRight.getImageHeight(),vacpRight.getAudioChannels());
                recorder.setFrameNumber(vacpRight.getFrameNumber());
                break;
            }
            case Tool.NOT_REYE:
            {
                //只有左眼
                VideoStorgeName=Tool.GetVideoStoragePath();
                recorder=new FFmpegFrameRecorder(VideoStorgeName,vacpLeft.getImageWidth(),vacpLeft.getImageHeight(),vacpLeft.getAudioChannels());
                recorder.setFrameNumber(vacpLeft.getFrameNumber());
                break;
            }
            case Tool.ALL_EYE:
            {
                //双眼都在
                VideoStorgeName=Tool.GetVideoStoragePath();
                recorder=new FFmpegFrameRecorder(VideoStorgeName,vacpLeft.getImageWidth()+vacpRight.getImageWidth(),Tool.Max(vacpLeft.getImageHeight(),vacpRight.getImageHeight()));
                recorder.setFrameNumber(Tool.Min(vacpLeft.getFrameNumber(),vacpRight.getFrameNumber()));
            break;
        }
        default:
        {
            //如果不存在以上情况的话
            return;
        }
        }
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
        recorder.setFormat("mp4");
        timer.schedule(new readFrame(),50,5);
        L.d("视频开始播放");
    }
    private void openVideo()
    {
        if(IsTimerRun)
        {
            IsTimerRun=false;
            timer.cancel();
        }
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);//和GET_CONTENT一起用
        startActivityForResult(intent,OPEN_VIDEO);
    }
    private void startPlay()
    {
        if(!(EyeNum==Tool.NOT_LEYE||EyeNum==Tool.NOT_REYE||EyeNum==Tool.ALL_EYE))
        {
            message=new Message();
            message.obj="无视频源";
            L.d("无视频源");
            ToastHandle.sendMessage(message);
            return;
        }

        /*下面是参数初始化*/
        clearEntey(chart_x);
        clearEntey(chart_y);
        IsLeyeCenter=false;
        IsReyeCenter=false;
        LeyeCenter=new Box();
        ReyeCenter=new Box();

        FrameNum=0;

        L.d("开始测试");
        calNum=0;//1s计算一次
        secondTime=0;//0s
        message=new Message();
        message.obj="开始测试";
        ToastHandle.sendMessage(message);
        IsTimerRun=true;

        //视频开始录制
        try
        {
            recorder.start();
        }
        catch (FrameRecorder.Exception e)
        {
            L.d("视频开启录制失败");
        }

        IsTest=true;//开始测试
    }
    private void stopPlay()
    {
        if(IsTimerRun)
        {
            IsTimerRun=false;
            if(IsTest&&(EyeNum==Tool.ALL_EYE||EyeNum==Tool.NOT_LEYE||EyeNum==Tool.NOT_REYE))
            {
                try
                {
                    recorder.stop();
                    recorder.release();
                }
                catch (FFmpegFrameRecorder.Exception e)
                {
                    L.d("视频保存出错");
                }
            }
            IsTest=false;
            timer.cancel();
            /*诊断结果*/
            final boolean diagnosticResult=calculate.judgeDiagnosis();//诊断结果

            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    if(diagnosticResult)
                    {
                        DiagnosticResult.setText(R.string.normal);
                    }
                    else
                    {
                        DiagnosticResult.setText(R.string.abnormal);
                        DiagnosticResult.setTextColor(MainActivity.this.getResources().getColor(R.color.red));
                    }
                    /*快相方向*/
                    if(calculate.judegeEye(true))
                    {
                        //左眼有处理结果
                        boolean leyeDir=calculate.judgeFastPhase(true);
                        LeyeDirectionResult.setText(leyeDir? R.string.left:R.string.right);
                    }
                    if(calculate.judegeEye(false))
                    {
                        //右眼有处理结果
                        boolean reyeDir=calculate.judgeFastPhase(false);
                        ReyeDirectionResult.setText(reyeDir?R.string.left:R.string.right);
                    }
                }
            });
            L.d("手动关闭视频播放，定时器关闭");
            message=new Message();
            message.obj="视频播放结束";
            ToastHandle.sendMessage(message);
            if(EyeNum==Tool.VEDIO_EYE||EyeNum==Tool.VEDIO_ONLY_EYE)
            {
                try {
                    capture.stop();
                    capture.release();
                }
                catch (org.bytedeco.javacv.FrameGrabber.Exception e)
                {
                    L.d("释放本地视频");
                }
            }
/*
            if(EyeNum==Tool.ALL_EYE||EyeNum==Tool.NOT_REYE)
            {
                try {
                    vacpLeft.stop();
                    vacpLeft.release();
                }
                catch (org.bytedeco.javacv.FrameGrabber.Exception e)
                {
                    L.d("释放左眼连接");
                }
            }
            if(EyeNum==Tool.ALL_EYE||EyeNum==Tool.NOT_LEYE)
            {
                try {
                    vacpRight.stop();
                    vacpRight.release();
                }
                catch (org.bytedeco.javacv.FrameGrabber.Exception e)
                {
                    L.d("释放右眼连接");
                }
            }
            */
        }
        else
        {
            L.d("请先播放视频");
            message=new Message();
            message.obj="请先播放视频";
            ToastHandle.sendMessage(message);
        }
    }
    private void initialChart(LineChart chart,String label)
    {
        Description description=new Description();
        description.setText(label);
        chart.setDescription(description);//增加描述

        chart.setDrawGridBackground(false);//不绘制背景颜色
        chart.setTouchEnabled(true);//可点击
        chart.setDragEnabled(true);//可拖拽
        chart.setScaleEnabled(true);//可缩放
        chart.setPinchZoom(false);//如果设置为false,那么x轴,y轴可以分别放大

        XAxis xAxis=chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);//设置X坐标轴在底部，默认在顶部

        chart.getAxisLeft().setDrawGridLines(false);//不绘制网格线
        chart.getAxisRight().setDrawGridLines(false);//不绘制网格线

        /*绘制两条曲线*/
        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        for(int i=0;i<2;++i)
        {
            ArrayList<Entry> values=new ArrayList<>();
            values.add(new Entry(0,0));//初始设置为(0,0)坐标

            LineDataSet set=new LineDataSet(values,i==0?"左眼":"右眼");
            set.setMode(set.getMode()==LineDataSet.Mode.CUBIC_BEZIER?LineDataSet.Mode.LINEAR:LineDataSet.Mode.CUBIC_BEZIER);//设置为平滑曲线
            set.setDrawCircles(false);//取消显示坐标点圆圈
            set.setDrawValues(false);//取消显示坐标值
            set.setCubicIntensity(0.15f);//设置曲线曲率
            set.setLineWidth(2f);//设置线的宽度
            set.setColor(colors[i]);//设置线的颜色
            dataSets.add(set);
        }
        LineData data=new LineData(dataSets);
        chart.setData(data);

    }
    private void addEntey(final LineChart add_chart, final float add_x,final float add_y,final int add_flag)
    {
        //flag:0 左眼; flag:1 右眼
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LineData data=add_chart.getData();
                Entry entry=new Entry(add_x,add_y);
                data.addEntry(entry,add_flag);
                add_chart.notifyDataSetChanged();
                add_chart.invalidate();
            }
        });
    }
    private void clearEntey(LineChart chart)
    {
        LineData oldData=chart.getData();
        oldData.clearValues();
        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        for(int i=0;i<2;++i)
        {
            ArrayList<Entry> values=new ArrayList<>();
            values.add(new Entry(0,0));//初始设置为(0,0)坐标

            LineDataSet set=new LineDataSet(values,i==0?"左眼":"右眼");
            set.setMode(set.getMode()==LineDataSet.Mode.CUBIC_BEZIER?LineDataSet.Mode.LINEAR:LineDataSet.Mode.CUBIC_BEZIER);//设置为平滑曲线
            set.setDrawCircles(false);//取消显示坐标点圆圈
            set.setDrawValues(false);//取消显示坐标值
            set.setCubicIntensity(0.15f);//设置曲线曲率
            set.setLineWidth(2f);//设置线的宽度
            set.setColor(colors[i]);//设置线的颜色
            dataSets.add(set);
        }
        LineData data=new LineData(dataSets);
        chart.setData(data);
        chart.notifyDataSetChanged();
        ChartMessage=new Message();
        ChartHandle.sendMessage(ChartMessage);
    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data)
    {
        String VideoPath="";
        boolean isTransmit=false;
        switch (requestCode)
        {
            case Tool.VideoTransmitTestCode:
            {
                if(resultCode==RESULT_OK)
                {
                    Bundle bundle=data.getExtras();
                    VideoPath=bundle.getString("VideoPath");
                    isTransmit=true;
                    mDrawerLayout.closeDrawers();//关闭侧滑栏
                }
            }
            case OPEN_VIDEO:
            {
                if(resultCode==RESULT_OK)
                {
                    if(!isTransmit)
                    {
                        VideoPath= GetPath.getPath(this,data.getData());
                    }
                    //视频文件地址为：/storage/emulated/0/test.mp4
                    //视频文件必须为mjpeg编码的video
                    capture=new FFmpegFrameGrabber(VideoPath);
                    try
                    {
                        capture.start();
                    }
                    catch (org.bytedeco.javacv.FrameGrabber.Exception e)
                    {
                        T.showShort(this,"视频加载失败");
                        L.d("视频加载失败"+e.toString());
                        break;
                    }
                    T.showShort(this,"视频加载成功");
                    L.d("视频加载成功");

                    //开始播放
                    Frame tempFrame=null;
                    try {

                        tempFrame=capture.grabFrame();
                        if(tempFrame==null)
                        {
                            T.showShort(this,"播放失败");
                            L.d("播放失败");
                            return;
                        }
                    }
                    catch (org.bytedeco.javacv.FrameGrabber.Exception e)
                    {
                        T.showShort(this,"播放失败");
                        L.d("播放失败");
                        return;
                    }
                    if(tempFrame.imageWidth/((float)tempFrame.imageHeight)>1.5)
                    {
                        //双眼视频
                        EyeNum=Tool.VEDIO_EYE;
                    }
                    else
                    {
                        //单眼视频
                        EyeNum= Tool.VEDIO_ONLY_EYE;
                        RightFrameMat=new Mat();
                        TempView=BitmapFactory.decodeResource(getResources(),R.drawable.novideo);
                        Frame TempFrame=bitmapConverter.convert(TempView);
                        RightFrameMat=matConverter.convertToMat(TempFrame);
                    }

                    /*下面是参数初始化*/
                    clearEntey(chart_x);
                    clearEntey(chart_y);
                    clearEntey(chart_rotation);
                    IsLeyeCenter=false;
                    IsReyeCenter=false;
                    LeyeCenter=new Box();
                    ReyeCenter=new Box();
                    FrameNum=0;

                    timer=new Timer();

                    L.d("视频开始播放");
                    calculate=new Calculate();//瞳孔数据计算器

                    filterL=new PointFilter();//左眼瞳孔坐标滤波器
                    filterR=new PointFilter();//右眼瞳孔坐标滤波器

                    calNum=0;//1s计算一次
                    secondTime=0;//0s
                    message=new Message();
                    message.obj="视频开始播放";
                    ToastHandle.sendMessage(message);
                    IsTimerRun=true;

                    LeyeXRealtimeAndMaxSPV.setText("0/0");
                    ReyeXRealtimeAndMaxSPV.setText("0/0");
                    LeyeYRealtimeAndMaxSPV.setText("0/0");
                    ReyeYRealtimeAndMaxSPV.setText("0/0");
                    LeyeHighperiod.setText("0s");
                    ReyeHighperiod.setText("0s");
                    DiagnosticResult.setText(R.string.defalut);
                    LeyeDirectionResult.setText(R.string.defalut);
                    ReyeDirectionResult.setText(R.string.defalut);
                    DiagnosticResult.setTextColor(MainActivity.this.getResources().getColor(R.color.black));

                    IsTest=true;
                    timer.schedule(new readFrame(),50,10);
                }
                break;
            }
            case OPEN_CAMERA:
            {
                break;
            }
            default:
                break;
        }
    }
    class readFrame extends TimerTask{
        @Override
        public void run()
        {
            Leye=new Mat();
            Reye=new Mat();

            if(EyeNum==Tool.NOT_LEYE||EyeNum==Tool.ALL_EYE)
            {
                //此时有右眼
                try {
                    RightFrame=new Frame();
                    if((RightFrame=vacpRight.grabImage())==null)
                    {
                        //视频播放结束
                        videoStop(1);
                        return;
                    }
                }
                catch (org.bytedeco.javacv.FrameGrabber.Exception e)
                {
                    //视频播放结束
                    videoStop(1);
                    return;
                }
                /*图像旋转180°*/
                RightFrameMat=new Mat();
                Mat RightFrameSrcMat=matConverter_R.convertToMat(RightFrame).clone();
                RightFrameMat=RightFrameSrcMat.clone();
                Point2f center=new Point2f(RightFrameSrcMat.cols()/2,RightFrameSrcMat.rows()/2);
                Mat affineTrans=opencv_imgproc.getRotationMatrix2D(center,180.0,1.0);
                opencv_imgproc.warpAffine(RightFrameSrcMat,RightFrameMat,affineTrans,RightFrameMat.size());

                //如果只有右眼的情况下
                if(EyeNum==Tool.NOT_LEYE&&IsTest)
                {
                    try
                    {
                        recorder.record(RightFrame);
                    }
                    catch (FFmpegFrameRecorder.Exception e)
                    {
                        L.d("视频记录出错");
                    }
                }
            }
            if(EyeNum==Tool.NOT_REYE||EyeNum==Tool.ALL_EYE)
            {
                //此时有左眼
                try {
                    LeftFrame=new Frame();
                    if((LeftFrame=vacpLeft.grabImage())==null)
                    {
                        //视频播放结束
                        videoStop(0);
                        return;
                    }
                }
                catch (org.bytedeco.javacv.FrameGrabber.Exception e)
                {
                    //视频播放结束
                    videoStop(0);
                    return;
                }
                /*图像旋转180°*/
                LeftFrameMat=new Mat();
                Mat LeftFrameSrcMat=matConverter_L.convertToMat(LeftFrame).clone();
                LeftFrameMat=LeftFrameSrcMat.clone();
                Point2f center=new Point2f(LeftFrameSrcMat.cols()/2,LeftFrameSrcMat.rows()/2);
                Mat affineTrans=opencv_imgproc.getRotationMatrix2D(center,180.0,1.0);
                opencv_imgproc.warpAffine(LeftFrameSrcMat,LeftFrameMat,affineTrans,LeftFrameMat.size());

                //如果只有左眼的情况下
                if(EyeNum==Tool.NOT_REYE&&IsTest)
                {
                    try
                    {
                        recorder.record(LeftFrame);
                    }
                    catch (FFmpegFrameRecorder.Exception e)
                    {
                        L.d("视频记录出错");
                    }
                }
                //如果存在双眼视频
                if(EyeNum==Tool.ALL_EYE&&IsTest)
                {
                    Mat mergeMat;
                    mergeMat=Tool.MergeMat(LeftFrameMat,RightFrameMat);
                    Frame mergeFrame=matConverter.convert(mergeMat);
                    try
                    {
                        recorder.record(mergeFrame);
                    }
                    catch (FFmpegFrameRecorder.Exception e)
                    {
                        L.d("视频记录出错");
                    }
                }
            }
            if(EyeNum==Tool.VEDIO_ONLY_EYE)
            {
                //单眼视频
                LeftFrame=new Frame();
                LeftFrameMat=new Mat();
                try
                {
                    LeftFrame=capture.grabFrame();
                    if(LeftFrame==null) {
                        //停止了
                        videoStop(0);
                        return;
                    }
                }
                catch (org.bytedeco.javacv.FrameGrabber.Exception e)
                {
                    //停止了
                    videoStop(0);
                    return;
                }

                LeftFrameMat=matConverter.convertToMat(LeftFrame);

                if(IsTest&&calNum==Tool.TimerSecondNum)
                {
                    //进行SPV分析
                    calNum=0;
                    ++secondTime;
                    calculate.processLeyeX(secondTime);
                    calculate.processLeyeY(secondTime);

                    final double leyeXRealtime=calculate.getRealTimeSPVX(secondTime,true);
                    final double leyeXMax=calculate.getMaxSPVX(true);
                    final double leyeYRealtime=calculate.getRealTimeSPVY(secondTime,true);
                    final double leyeYMax=calculate.getMaxSPVY(true);
                    final int maxSecond_L=calculate.getHighTidePeriod(true);//左眼
                    final String period_L=getPeriod(maxSecond_L);
                    //强制在UI线程下更新
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LeyeXRealtimeAndMaxSPV.setText(MergeRealtimeAndMax(df.format(leyeXRealtime),df.format(leyeXMax)));
                            LeyeYRealtimeAndMaxSPV.setText(MergeRealtimeAndMax(df.format(leyeYRealtime),df.format(leyeYMax)));
                            LeyeHighperiod.setText(period_L);
                        }
                    });
                }
            }
            if(EyeNum==Tool.VEDIO_EYE) {
                //双眼视频都在
                AllEyeMat = new Mat();
                AllFrame = new Frame();
                try {
                    AllFrame = capture.grabFrame();
                    if (AllFrame == null) {
                        //视频播放结束
                        videoStop(2);
                        return;
                    }
                } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                    //视频播放结束
                    videoStop(2);
                    return;
                }

                //图像切割
                //借助于Rect的ROI分割
                AllEyeMat = matConverter.convertToMat(AllFrame);
                Rect reye_box = new Rect(0, 1, AllEyeMat.cols()/2, AllEyeMat.rows() - 1);
                Rect leye_box = new Rect(AllEyeMat.cols()/2, 1, AllEyeMat.cols()/2-1, AllEyeMat.rows() - 1);
                LeftFrameMat=new Mat(AllEyeMat,reye_box);
                RightFrameMat=new Mat(AllEyeMat,leye_box);

                if(IsTest&&calNum==Tool.TimerSecondNum)
                {
                    calNum=0;
                    ++secondTime;
                    calculate.processLeyeX(secondTime);
                    calculate.processReyeX(secondTime);
                    calculate.processLeyeY(secondTime);
                    calculate.processReyeY(secondTime);
                    final double leyeXRealtime=calculate.getRealTimeSPVX(secondTime,true);
                    final double reyeXRealtime=calculate.getRealTimeSPVX(secondTime,false);
                    final double leyeXMax=calculate.getMaxSPVX(true);
                    final double reyeXMax=calculate.getMaxSPVX(false);
                    final double leyeYRealtime=calculate.getRealTimeSPVY(secondTime,true);
                    final double reyeYRealtime=calculate.getRealTimeSPVY(secondTime,false);
                    final double leyeYMax=calculate.getMaxSPVY(true);
                    final double reyeYMax=calculate.getMaxSPVY(false);
                    final int maxSecond_L=calculate.getHighTidePeriod(true);//左眼
                    final int maxSecond_R=calculate.getHighTidePeriod(false);//右眼
                    final String period_L=getPeriod(maxSecond_L);
                    final String period_R=getPeriod(maxSecond_R);
                    //强制在UI线程下更新
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LeyeXRealtimeAndMaxSPV.setText(MergeRealtimeAndMax(df.format(leyeXRealtime),df.format(leyeXMax)));//保留两位小数点
                            ReyeXRealtimeAndMaxSPV.setText(MergeRealtimeAndMax(df.format(reyeXRealtime),df.format(reyeXMax)));
                            LeyeYRealtimeAndMaxSPV.setText(MergeRealtimeAndMax(df.format(leyeYRealtime),df.format(leyeYMax)));
                            ReyeYRealtimeAndMaxSPV.setText(MergeRealtimeAndMax(df.format(reyeYRealtime),df.format(reyeYMax)));
                            LeyeHighperiod.setText(period_L);
                            ReyeHighperiod.setText(period_R);
                        }
                    });
                }
            }
            if(IsTest&&(EyeNum==Tool.NOT_LEYE||EyeNum==Tool.NOT_REYE||EyeNum==Tool.ALL_EYE))
            {
                //用于播放视频时更新SPV
                if(calNum==Tool.TimerSecondNum)
                {
                    calNum=0;
                    ++secondTime;
                    if(EyeNum==Tool.NOT_REYE||EyeNum==Tool.ALL_EYE)
                    {
                        //左眼
                        calculate.processLeyeX(secondTime);
                        calculate.processLeyeY(secondTime);
                        final double leyeXRealtime=calculate.getRealTimeSPVX(secondTime,true);
                        final double leyeXMax=calculate.getMaxSPVX(true);
                        final double leyeYRealtime=calculate.getRealTimeSPVY(secondTime,true);
                        final double leyeYMax=calculate.getMaxSPVY(true);
                        final int maxSecond_L=calculate.getHighTidePeriod(true);//左眼
                        final String period_L=getPeriod(maxSecond_L);
                        //强制在UI线程下更新
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LeyeXRealtimeAndMaxSPV.setText(MergeRealtimeAndMax(df.format(leyeXRealtime),df.format(leyeXMax)));//保留两位小数点
                                LeyeYRealtimeAndMaxSPV.setText(MergeRealtimeAndMax(df.format(leyeYRealtime),df.format(leyeYMax)));
                                LeyeHighperiod.setText(period_L);
                            }
                        });
                    }
                    if(EyeNum==Tool.NOT_LEYE||EyeNum==Tool.ALL_EYE)
                    {
                        //右眼
                        calculate.processReyeX(secondTime);
                        calculate.processReyeY(secondTime);
                        final double reyeXRealtime=calculate.getRealTimeSPVX(secondTime,false);
                        final double reyeXMax=calculate.getMaxSPVX(false);
                        final double reyeYRealtime=calculate.getRealTimeSPVY(secondTime,false);
                        final double reyeYMax=calculate.getMaxSPVY(false);
                        final int maxSecond_R=calculate.getHighTidePeriod(false);//右眼
                        final String period_R=getPeriod(maxSecond_R);
                        //强制在UI线程下更新
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ReyeXRealtimeAndMaxSPV.setText(MergeRealtimeAndMax(df.format(reyeXRealtime),df.format(reyeXMax)));
                                ReyeYRealtimeAndMaxSPV.setText(MergeRealtimeAndMax(df.format(reyeYRealtime),df.format(reyeYMax)));
                                ReyeHighperiod.setText(period_R);
                            }
                        });
                    }
                }
            }
            if(IsTest)
            {
                ++FrameNum;
                ++calNum;
            }
            ImgProcess pro=new ImgProcess();
            pro.Start(LeftFrameMat,RightFrameMat,1.8,EyeNum);
            pro.ProcessSeparate();
            Leye=pro.OutLeye();
            Reye=pro.OutReye();
            if(IsTest)
            {
                //开始测试后进行波形分析
                for(Box box:pro.Lcircles())
                {
                    //先滤波处理
                    filterL.add(box);
                    box=filterL.get();

                    //圆心坐标更新
                    if(preLeyeBox==null)
                    {
                        preLeyeBox=box;
                        break;
                    }

                    //与上一帧做对比
                    if(Tool.distance(box,preLeyeBox)>(box.getR()+preLeyeBox.getR()/1.5)&&(Math.abs(box.getR()-preLeyeBox.getR())>box.getR()/2.0))
                    {
                        return;
                    }

                    //左眼坐标
                    if(!IsLeyeCenter)
                    {
                        //左眼初始坐标
                        IsLeyeCenter=true;
                        LeyeCenter.setX(box.getX());
                        LeyeCenter.setY(box.getY());
                    }
                    else
                    {
                        //后续相对地址是基于第一帧位置的
                        double tempL=Math.atan((box.getY()-preLeyeBox.getY())/(box.getX()-preLeyeBox.getX()));
                        if(Double.isNaN(tempL))
                        {
                            tempL=0;
                        }
                        addEntey(chart_rotation,FrameNum/(float)30,(float) tempL,0);
                        addEntey(chart_x,FrameNum/(float)30,(float) (box.getX()-LeyeCenter.getX()),0);
                        addEntey(chart_y,FrameNum/(float)30,(float) (box.getY()-LeyeCenter.getY()),0);
                        calculate.addLeyeX(box.getX()-LeyeCenter.getX());
                        calculate.addLeyeY(box.getY()-LeyeCenter.getY());
                    }
                    preLeyeBox=box;
                }
                for(Box box:pro.Rcircles())
                {
                    //先滤波处理
                    filterR.add(box);
                    box=filterR.get();

                    //圆心坐标更新
                    if(preReyeBox==null)
                    {
                        preReyeBox=box;
                        break;
                    }

                    //与上一帧做对比
                    if(Tool.distance(box,preReyeBox)>(box.getR()+preReyeBox.getR()/1.5)&&(Math.abs(box.getR()-preReyeBox.getR())>box.getR()/2.0))
                    {
                        return;
                    }

                    //右眼坐标
                    if(!IsReyeCenter)
                    {
                        //右眼初始坐标
                        IsReyeCenter=true;
                        ReyeCenter.setX(box.getX());
                        ReyeCenter.setY(box.getY());
                    }
                    else
                    {
                        //后续相对地址是基于第一帧位置的
                        double tempR=Math.atan((box.getY()-preReyeBox.getY())/(box.getX()-preReyeBox.getX()));
                        if(Double.isNaN(tempR))
                        {
                            tempR=0;
                        }
                        addEntey(chart_rotation,FrameNum/(float)30,(float) tempR,1);
                        addEntey(chart_x,FrameNum/(float)30,(float)(box.getX()-ReyeCenter.getX()),1);
                        addEntey(chart_y,FrameNum/(float)30,(float)(box.getY()-ReyeCenter.getY()),1);
                        calculate.addReyeX(box.getX()-ReyeCenter.getX());
                        calculate.addReyeY(box.getY()-ReyeCenter.getY());
                    }
                    preReyeBox=box;
                }
            }
            try
            {
                tempLeftFrame=matConverter.convert(Leye);
                tempRightFrame=matConverter.convert(Reye);
                LeftView=bitmapConverter.convert(tempLeftFrame);
                RightView=bitmapConverter.convert(tempRightFrame);
            }
            catch (Exception e)
            {
                L.e("格式转换发生异常："+e.toString());
            }
            message = new Message();
            ViewHandle.sendMessage(message);
        }
    }
    //视频停止操作
    private void videoStop(int eye)//0代表左眼，1代表右眼，2代表双眼
    {
        timer.cancel();
        IsTimerRun=false;
        if(IsTest&&(EyeNum==Tool.ALL_EYE||EyeNum==Tool.NOT_LEYE||EyeNum==Tool.NOT_REYE))
        {
            try
            {
                recorder.stop();
                recorder.release();
            }
            catch (FFmpegFrameRecorder.Exception e)
            {
                L.d("视频保存出错");
            }
        }
        IsTest=false;
        /*诊断结果*/
        final boolean diagnosticResult=calculate.judgeDiagnosis();//诊断结果

        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if(diagnosticResult)
                {
                    DiagnosticResult.setText(R.string.normal);
                }
                else
                {
                    DiagnosticResult.setText(R.string.abnormal);
                    DiagnosticResult.setTextColor(MainActivity.this.getResources().getColor(R.color.red));
                }
                /*快相方向*/
                if(calculate.judegeEye(true))
                {
                    //左眼有处理结果
                    boolean leyeDir=calculate.judgeFastPhase(true);
                    LeyeDirectionResult.setText(leyeDir? R.string.left:R.string.right);
                }
                if(calculate.judegeEye(false))
                {
                    //右眼有处理结果
                    boolean reyeDir=calculate.judgeFastPhase(false);
                    ReyeDirectionResult.setText(reyeDir?R.string.left:R.string.right);
                }
            }
        });
        if(EyeNum==Tool.VEDIO_ONLY_EYE||EyeNum==Tool.VEDIO_EYE)
        {
            try {
                capture.release();
            }
            catch (org.bytedeco.javacv.FrameGrabber.Exception e)
            {
                L.d("释放本地视频");
            }
            message=new Message();
            message.obj="视频播放结束";//代表视频播放结束
            ToastHandle.sendMessage(message);
        }
    }
    Handler ViewHandle=new Handler()//用以实时刷新显示左右眼,异步消息处理
    {
        @Override
        public void handleMessage(Message msg)
        {
            imageView_leye.setImageBitmap(LeftView);
            imageView_reye.setImageBitmap(RightView);
            super.handleMessage(msg);
        }
    };
    Handler ToastHandle=new Handler()//用于显示Toast消息
    {
        @Override
        public void handleMessage(Message msg)
        {
            T.showShort(MainActivity.this,msg.obj.toString());
            super.handleMessage(msg);
        }
    };
    Handler ChartHandle=new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            try
            {
                chart_x.invalidate();
                chart_y.invalidate();
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                L.d(e.toString());
            }
            super.handleMessage(msg);
        }
    };
}
