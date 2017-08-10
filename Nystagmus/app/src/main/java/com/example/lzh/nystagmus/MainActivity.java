package com.example.lzh.nystagmus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lzh.nystagmus.Utils.Box;
import com.example.lzh.nystagmus.Utils.Calculate;
import com.example.lzh.nystagmus.Utils.GetPath;
import com.example.lzh.nystagmus.Utils.L;
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

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.breadCrumbShortTitle;
import static android.R.attr.data;
import static android.R.attr.factor;
import static android.R.attr.flipInterval;
import static android.R.attr.manageSpaceActivity;
import static android.R.attr.x;
import static android.R.attr.y;
import static android.os.Build.VERSION_CODES.N;
import static com.example.lzh.nystagmus.R.id.start;
import static com.example.lzh.nystagmus.R.id.toolbar;
import static com.example.lzh.nystagmus.Utils.Tool.AddressRightEye;
import static org.opencv.videoio.Videoio.CAP_MODE_YUYV;
import static org.opencv.videoio.Videoio.CV_CAP_FFMPEG;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView imageView_leye;
    private ImageView imageView_reye;
    private DrawerLayout mDrawerLayout;
    private static final int OPEN_VIDEO=1;
    private static final int OPEN_CAMERA=2;
    private VideoCapture capture;//打开本地视频
    private VideoCapture vacpLeft;//打开左眼网络视频
    private VideoCapture vacpRight;//打开右眼网络视频
    private Timer timer;//定时器
    private static final int Storage_RequestCode=1;//存储权限申请码

    private Mat frame;
    private Mat LeftFrame;
    private Mat RightFrame;
    private Bitmap LeftView;
    private Bitmap RightView;
    private Bitmap TempView;
    private Mat Leye;
    private Mat Reye;
    private Mat AllEye;
    private Message message;
    private Message ChartMessage;
    private boolean IsTimerRun=false;

    private int EyeNum;//眼睛数目
    private boolean IsLeyeCenter=false;//用于判断左眼是否确定初始位置
    private boolean IsReyeCenter=false;//用于判断右眼是否确定初始位置
    private Box LeyeCenter=new Box();//用于保存左眼的初始位置
    private Box ReyeCenter=new Box();//用于保存右眼的初始位置
    private int FrameNum=0;//视频播放帧数

    private LineChart chart_x;//X波形图
    private LineChart chart_y;//y波形图
    private int[] colors=new int[]{Color.rgb(255, 69, 0), Color.rgb(0, 128, 0)};//自定义颜色，第一种为橘黄色，第二种为纯绿色

    private SharedPreferences pref;//调用存储文件

    private Calculate calculate;
    private int calNum;//计算时间间隔
    private int secondTime;//视频测试时间

    /*SPV相关*/
    private TextView LeyeRealtimeSPV;
    private TextView ReyeRealtimeSPV;
    private TextView LeyeMaxSPV;
    private TextView ReyeMaxSPV;
    private TextView LeyeHighperiod;
    private TextView ReyeHighperiod;
    private DecimalFormat df;//数据格式,float转string保留两位小数

    /*悬浮菜单按钮*/
    private FloatingActionsMenu menuChange;
    /*测试用*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView_leye=(ImageView)findViewById(R.id.lefteye_view);
        imageView_reye=(ImageView)findViewById(R.id.righteye_view);
        mDrawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);

        LeyeRealtimeSPV=(TextView) findViewById(R.id.leyeRealtimeSPV);
        ReyeRealtimeSPV=(TextView) findViewById(R.id.reyeRealtimeSPV);
        LeyeMaxSPV=(TextView) findViewById(R.id.leyeMaxSPV);
        ReyeMaxSPV=(TextView) findViewById(R.id.reyeMaxSPV);
        LeyeHighperiod=(TextView) findViewById(R.id.leyeHighperiod);
        ReyeHighperiod=(TextView) findViewById(R.id.reyeHighperiod);
        /*初始化设置为0*/
        LeyeRealtimeSPV.setText("0");
        ReyeRealtimeSPV.setText("0");
        LeyeMaxSPV.setText("0");
        ReyeMaxSPV.setText("0");
        LeyeHighperiod.setText("0s");
        ReyeHighperiod.setText("0s");

        df= new DecimalFormat("##.##");//数据格式,float转string保留两位小数

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
                    default:
                        break;
                }
                return true;
            }
        });

        chart_x=(LineChart)findViewById(R.id.xchart);
        chart_y=(LineChart)findViewById(R.id.ychart);

        initialChart(chart_x,"X坐标");//初始化波形图
        initialChart(chart_y,"Y坐标");//初始化波形图

        pref=getSharedPreferences("CameraAddress",MODE_PRIVATE);
        Tool.AddressLeftEye=pref.getString("LeftCameraAddress",Tool.AddressLeftEye);
        Tool.AddressRightEye=pref.getString("RightCameraAddress",Tool.AddressRightEye);

        L.d("项目打开");
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
    @Override
    protected void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug())// 默认加载opencv_java.so库
        {}

    }
    private void openCamera()
    {
        if(IsTimerRun)
        {
            //如果正在处理视频，则立即停止当前定时器
            IsTimerRun = false;
            timer.cancel();
        }
        EyeNum=Tool.ALL_EYE;

        vacpLeft=new VideoCapture(Tool.AddressLeftEye,CV_CAP_FFMPEG);
        if(!vacpLeft.isOpened())
        {
            T.showLong(this,"左眼连接失败");
            L.d("左眼连接失败");
            EyeNum=Tool.NOT_LEYE;
        }
        vacpRight=new VideoCapture(Tool.AddressRightEye,CAP_MODE_YUYV);
        if(!vacpRight.isOpened())
        {
            T.showLong(this,"右眼连接失败");
            L.d("右眼连接失败");
            if(EyeNum==Tool.ALL_EYE)
            {
                EyeNum=Tool.NOT_REYE;
            }
            else
            {
                EyeNum=Tool.NOT_ALLEYE;
                T.showLong(this,"摄像头全部打开失败");
                L.d("摄像头全部打开失败");
                return;
            }
        }

        clearEntey(chart_x);
        clearEntey(chart_y);
        IsLeyeCenter=false;
        IsReyeCenter=false;
        LeyeCenter=new Box();
        ReyeCenter=new Box();
        FrameNum=0;

        timer=new Timer();
        timer.schedule(new readFarme(),50,20);
        message=new Message();
        message.obj="视频开始播放";
        ToastHandle.sendMessage(message);
        IsTimerRun=true;

        LeyeRealtimeSPV.setText("0");
        ReyeRealtimeSPV.setText("0");
        LeyeMaxSPV.setText("0");
        ReyeMaxSPV.setText("0");
        LeyeHighperiod.setText("0s");
        ReyeHighperiod.setText("0s");
    }
    private void openVideo()
    {
        if(IsTimerRun)
        {
            timer.cancel();
        }
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);//和GET_CONTENT一起用
        startActivityForResult(intent,OPEN_VIDEO);
    }
    private void startPlay()
    {
        if(IsTimerRun)
        {
            //如果正在处理视频，则立即停止当前定时器
            IsTimerRun = false;
            timer.cancel();
        }
        if((capture!=null)&&capture.isOpened())
        {
            Mat tempFrame=new Mat();
            capture.read(tempFrame);
            if(tempFrame.width()/((float)tempFrame.height())>1.5)
            {
                //双眼视频
                EyeNum=Tool.VEDIO_EYE;
            }
            else
            {
                //单眼视频
                EyeNum= Tool.VEDIO_ONLY_EYE;
                RightFrame=new Mat();
                TempView=BitmapFactory.decodeResource(getResources(),R.drawable.novideo);
                Utils.bitmapToMat(TempView,RightFrame);
            }
            timer=new Timer();

            /*下面是参数初始化*/
            clearEntey(chart_x);
            clearEntey(chart_y);
            IsLeyeCenter=false;
            IsReyeCenter=false;
            LeyeCenter=new Box();
            ReyeCenter=new Box();
            FrameNum=0;

            timer.schedule(new readFarme(),50,20);
            L.d("开启定时器,视频开始播放");
            calculate=new Calculate();
            calNum=0;//1s计算一次
            secondTime=0;//0s
            message=new Message();
            message.obj="视频开始播放";
            ToastHandle.sendMessage(message);
            IsTimerRun=true;

            LeyeRealtimeSPV.setText("0");
            ReyeRealtimeSPV.setText("0");
            LeyeMaxSPV.setText("0");
            ReyeMaxSPV.setText("0");
            LeyeHighperiod.setText("0s");
            ReyeHighperiod.setText("0s");
        }
        else
        {
            message=new Message();
            message.obj="未打开本地视频";
            L.d("未打开本地视频");
            ToastHandle.sendMessage(message);
        }
    }
    private void stopPlay()
    {
        if(IsTimerRun)
        {
            IsTimerRun=false;
            timer.cancel();
            L.d("手动关闭视频播放，定时器关闭");
            message=new Message();
            message.obj="视频播放结束";
            ToastHandle.sendMessage(message);
            if(capture!=null)
            {
                capture.release();
            }
            if(vacpLeft!=null)
            {
                vacpLeft.release();
            }
            if(vacpRight!=null)
            {
                vacpRight.release();
            }
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

            LineDataSet set=new LineDataSet(values,i==0?"Left":"Right");
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

            LineDataSet set=new LineDataSet(values,i==0?"Left":"Right");
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
        switch (requestCode)
        {
            case OPEN_VIDEO:
            {
                if(resultCode==RESULT_OK)
                {
                    String VideoPath= GetPath.getPath(this,data.getData());
                    //视频文件地址为：/storage/emulated/0/test.mp4
                    //视频文件必须为mjpeg编码的video
                    capture=new VideoCapture(VideoPath);

                    if(capture.isOpened())
                    {
                        T.showLong(this,"视频加载成功");
                        L.d("视频加载成功");
                    }
                    else
                    {
                        T.showLong(this,"视频加载失败");
                        L.d("视频加载失败");
                    }
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
    class readFarme extends TimerTask{
        @Override
        public void run()
        {
            Leye=new Mat();
            Reye=new Mat();

            if(EyeNum==Tool.NOT_LEYE||EyeNum==Tool.ALL_EYE)
            {
                //此时有右眼
                RightFrame=new Mat();
                vacpRight.read(RightFrame);
            }
            if(EyeNum==Tool.NOT_REYE||EyeNum==Tool.ALL_EYE)
            {
                //此时有左眼
                LeftFrame=new Mat();
                vacpLeft.read(LeftFrame);
            }
            if(EyeNum==Tool.VEDIO_ONLY_EYE)
            {
                LeftFrame=new Mat();
                if(!capture.read(LeftFrame))
                {
                    timer.cancel();
                    L.d("播放结束，定时器关闭");
                    message=new Message();
                    message.obj="视频播放结束";//代表视频播放结束
                    ToastHandle.sendMessage(message);
                    IsTimerRun=false;
                    capture.release();
                    /*后期处理,用来判断高潮期*/
                    ++secondTime;
                    calculate.processLeyeX(secondTime);
                    final int maxSecond_L=calculate.getHighTidePeriod(true);//左眼
                    //强制在UI线程下更新
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LeyeHighperiod.setText(Tool.getPeriod(maxSecond_L));
                        }
                    });
                    return;
                }
                if(calNum==Tool.TimerSecondNum)
                {
                    calNum=0;
                    ++secondTime;
                    calculate.processLeyeX(secondTime);

                    final float leyeRealtime=calculate.getRealTimeSPV(secondTime,true);
                    final float leyeMax=calculate.getMaxSPV(true);
                    final int maxSecond_L=calculate.getHighTidePeriod(true);//左眼
                    //强制在UI线程下更新
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LeyeRealtimeSPV.setText(df.format(leyeRealtime));
                            LeyeMaxSPV.setText(df.format(leyeMax));
                            LeyeHighperiod.setText(Tool.getPeriod(maxSecond_L));
                        }
                    });
                }
            }
            if(EyeNum==Tool.VEDIO_EYE)
            {
                //双眼视频都在
                AllEye=new Mat();
                if(!capture.read(AllEye))
                {
                    timer.cancel();
                    L.d("播放结束，定时器关闭");
                    message=new Message();
                    message.obj="视频播放结束";//代表视频播放结束
                    ToastHandle.sendMessage(message);
                    IsTimerRun=false;
                    capture.release();

                    /*后期处理,用来判断高潮期*/
                    ++secondTime;
                    calculate.processLeyeX(secondTime);
                    calculate.processReyeX(secondTime);
                    final int maxSecond_L=calculate.getHighTidePeriod(true);//左眼
                    final int maxSecond_R=calculate.getHighTidePeriod(false);//右眼
                    //强制在UI线程下更新
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LeyeHighperiod.setText(Tool.getPeriod(maxSecond_L));
                            ReyeHighperiod.setText(Tool.getPeriod(maxSecond_R));
                        }
                    });
                    return;
                }
                Rect leye_box=new Rect();
                leye_box.x=1;
                leye_box.y=1;
                leye_box.height=AllEye.rows()-1;
                leye_box.width=AllEye.cols()/2-1;
                Rect reye_box=new Rect();
                reye_box.x=leye_box.x+leye_box.width;
                reye_box.y=1;
                reye_box.height=AllEye.rows()-1;
                reye_box.width=AllEye.cols()/2-1;
                LeftFrame=AllEye.submat(leye_box);
                RightFrame=AllEye.submat(reye_box);

                if(calNum==Tool.TimerSecondNum)
                {
                    calNum=0;
                    ++secondTime;
                    calculate.processLeyeX(secondTime);
                    calculate.processReyeX(secondTime);
                    final float leyeRealtime=calculate.getRealTimeSPV(secondTime,true);
                    final float reyeRealtime=calculate.getRealTimeSPV(secondTime,false);
                    final float leyeMax=calculate.getMaxSPV(true);
                    final float reyeMax=calculate.getMaxSPV(false);
                    final int maxSecond_L=calculate.getHighTidePeriod(true);//左眼
                    final int maxSecond_R=calculate.getHighTidePeriod(false);//右眼
                    //强制在UI线程下更新
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LeyeRealtimeSPV.setText(df.format(leyeRealtime));//保留两位小数点
                            ReyeRealtimeSPV.setText(df.format(reyeRealtime));
                            LeyeMaxSPV.setText(df.format(leyeMax));
                            ReyeMaxSPV.setText(df.format(reyeMax));
                            LeyeHighperiod.setText(Tool.getPeriod(maxSecond_L));
                            ReyeHighperiod.setText(Tool.getPeriod(maxSecond_R));
                        }
                    });
                }
            }
            ++FrameNum;
            ++calNum;

            ImgProcess pro=new ImgProcess();
            pro.Start(LeftFrame,RightFrame,1.5,EyeNum);
            pro.ProcessSeparate();
            Leye=pro.OutLeye();
            Reye=pro.OutReye();
            LeftView=Bitmap.createBitmap(Leye.width(),Leye.height(),Bitmap.Config.RGB_565);
            RightView=Bitmap.createBitmap(Reye.width(),Reye.height(),Bitmap.Config.RGB_565);
            for(Box box:pro.Lcircles)
            {
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
                    addEntey(chart_x,FrameNum/(float)30,(float) (box.getX()-LeyeCenter.getX()),0);
                    addEntey(chart_y,FrameNum/(float)30,(float) (box.getY()-LeyeCenter.getY()),0);
                    calculate.addLeyeX((float) (box.getX()-LeyeCenter.getX()));
                }
            }
            for(Box box:pro.Rcircles)
            {
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
                    addEntey(chart_x,FrameNum/(float)30,(float)(box.getX()-ReyeCenter.getX()),1);
                    addEntey(chart_y,FrameNum/(float)30,(float)(box.getY()-ReyeCenter.getY()),1);
                    calculate.addReyeX((float) (box.getX()-ReyeCenter.getX()));
                }
            }
            try
            {
                Utils.matToBitmap(Leye,LeftView);
                Utils.matToBitmap(Reye,RightView);
            }
            catch (Exception e)
            {
                L.e("格式转换发生异常："+e.toString());
            }
            message = new Message();
            ViewHandle.sendMessage(message);
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
            T.showLong(MainActivity.this,msg.obj.toString());
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
