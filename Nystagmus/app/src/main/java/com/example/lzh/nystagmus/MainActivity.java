package com.example.lzh.nystagmus;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.lzh.nystagmus.Utils.Box;
import com.example.lzh.nystagmus.Utils.GetPath;
import com.example.lzh.nystagmus.Utils.L;
import com.example.lzh.nystagmus.Utils.T;
import com.example.lzh.nystagmus.Utils.ImgProcess;
import com.example.lzh.nystagmus.Utils.Tool;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.breadCrumbShortTitle;
import static android.R.attr.data;
import static android.R.attr.factor;
import static android.R.attr.manageSpaceActivity;
import static android.R.attr.x;
import static android.R.attr.y;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView imageView_leye;
    private ImageView imageView_reye;
    private static final int OPEN_VIDEO=1;
    private static final int OPEN_CAMERA=2;
    private VideoCapture capture;
    private Timer timer;

    private Mat frame;
    private Mat LeftFrame;
    private Mat RightFrame;
    private Bitmap LeftView;
    private Bitmap RightView;
    private Bitmap TempView;
    private Mat Leye;
    private Mat Reye;
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
    private int BarColor=Color.rgb(48,70,155);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView_leye=(ImageView)findViewById(R.id.lefteye_view);
        imageView_reye=(ImageView)findViewById(R.id.righteye_view);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)
        {
            WindowManager.LayoutParams localLayoutParams=getWindow().getAttributes();
            localLayoutParams.flags=(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS|localLayoutParams.flags);
            getWindow().setNavigationBarColor(BarColor);
        }

        ((Button)findViewById(R.id.open_video)).setOnClickListener(this);
        ((Button)findViewById(R.id.start_paly)).setOnClickListener(this);
        ((Button)findViewById(R.id.open_camera)).setOnClickListener(this);
        ((Button)findViewById(R.id.stop_play)).setOnClickListener(this);

        chart_x=(LineChart)findViewById(R.id.xchart);
        chart_y=(LineChart)findViewById(R.id.ychart);

        InitialChart(chart_x);//初始化波形图
        InitialChart(chart_y);//初始化波形图

        L.d("项目打开");
    }
    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.open_video:
            {
                if(ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(MainActivity.this,new String []{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }else
                {
                    OpenVideo();
                }
                break;
            }
            case R.id.open_camera:
            {
                T.showLong(this,"网络摄像头未连接");
                break;
            }
            case R.id.start_paly:
            {
                StartPlay();
                break;
            }
            case R.id.stop_play:
            {
                StopPlay();
                break;
            }
            default:
                break;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults)
    {
        switch (requestCode)
        {
            case 1:
                if(grantResults.length>0&&grantResults[0]== PackageManager.PERMISSION_GRANTED)
                {
                    /*申请权限后的事情*/
                    OpenVideo();
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
    private void OpenVideo()
    {
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);//和GET_CONTENT一起用
        startActivityForResult(intent,OPEN_VIDEO);
    }
    private void StartPlay()
    {
        if((capture!=null)&&capture.isOpened())
        {
            EyeNum= Tool.VEDIO_ONLY_EYE;
            RightFrame=new Mat();
            TempView=BitmapFactory.decodeResource(getResources(),R.drawable.novideo);
            Utils.bitmapToMat(TempView,RightFrame);
            timer=new Timer();

            /*下面是参数初始化*/
            ClearEntey(chart_x);
            ClearEntey(chart_y);
            IsLeyeCenter=false;
            IsReyeCenter=false;
            LeyeCenter=new Box();
            ReyeCenter=new Box();
            FrameNum=0;

            timer.schedule(new ReadFarme(),100,20);
            L.d("开启定时器,视频开始播放");
            message=new Message();
            message.obj="视频开始播放";
            ToastHandle.sendMessage(message);
            IsTimerRun=true;
        }
        else
        {
            message=new Message();
            message.obj="未打开本地视频";
            L.d("未打开本地视频");
            ToastHandle.sendMessage(message);
        }
    }
    private void StopPlay()
    {
        if(IsTimerRun)
        {
            timer.cancel();
            L.d("手动关闭视频播放，定时器关闭");
            message=new Message();
            message.obj="视频播放结束";
            ToastHandle.sendMessage(message);
            capture.release();
        }
        else
        {
            L.d("请先播放视频");
            message=new Message();
            message.obj="请先播放视频";
            ToastHandle.sendMessage(message);
        }
    }
    private void InitialChart(LineChart chart)
    {
        Description description=new Description();
        description.setText(Tool.TAG);
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
    private void AddEntey(final LineChart add_chart, final float add_x,final float add_y,final int add_flag)
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
    private void ClearEntey(LineChart chart)
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
                    String x=getApplicationContext().getFilesDir().getAbsolutePath();
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
    class ReadFarme extends TimerTask{

        @Override
        public void run()
        {
            Leye=new Mat();
            Reye=new Mat();
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
                    return;
                }
            }
            ++FrameNum;
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
                    AddEntey(chart_x,FrameNum,(float) (box.getX()-LeyeCenter.getX()),0);
                    AddEntey(chart_y,FrameNum,(float) (box.getY()-LeyeCenter.getY()),0);
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
                    AddEntey(chart_x,FrameNum,(float)(box.getX()-ReyeCenter.getX()),1);
                    AddEntey(chart_y,FrameNum,(float)(box.getY()-ReyeCenter.getY()),1);
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
