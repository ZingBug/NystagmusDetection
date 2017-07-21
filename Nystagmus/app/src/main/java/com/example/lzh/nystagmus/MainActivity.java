package com.example.lzh.nystagmus;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.lzh.nystagmus.Utils.GetPath;
import com.example.lzh.nystagmus.Utils.L;
import com.example.lzh.nystagmus.Utils.T;
import com.example.lzh.nystagmus.Utils.ImgProcess;
import com.example.lzh.nystagmus.Utils.Tool;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.breadCrumbShortTitle;
import static android.R.attr.y;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView imageView_leye;
    private ImageView imageView_reye;
    private static final int OPEN_VIDEO=1;
    private static final int OPEN_CAMERA=2;
    private VideoCapture capture;
    Timer timer;

    private Mat frame;
    private Mat LeftFrame;
    private Mat RightFrame;
    private Bitmap LeftView;
    private Bitmap RightView;
    private Bitmap TempView;
    private Mat Leye;
    private Mat Reye;
    private Message msg;
    private boolean IsTimerRun=false;

    private int EyeNum;//眼睛数目

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView_leye=(ImageView)findViewById(R.id.lefteye_view);
        imageView_reye=(ImageView)findViewById(R.id.righteye_view);

        ((Button)findViewById(R.id.open_video)).setOnClickListener(this);
        ((Button)findViewById(R.id.start_paly)).setOnClickListener(this);
        ((Button)findViewById(R.id.open_camera)).setOnClickListener(this);
        ((Button)findViewById(R.id.stop_play)).setOnClickListener(this);

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
            timer.schedule(new ReadFarme(),10,20);
            L.d("开启定时器,视频开始播放");
            msg=new Message();
            msg.obj="视频开始播放";
            ToastHandle.sendMessage(msg);
            IsTimerRun=true;
        }
        else
        {
            msg=new Message();
            msg.obj="未打开本地视频";
            L.d("未打开本地视频");
            ToastHandle.sendMessage(msg);
        }
    }
    private void StopPlay()
    {
        if(IsTimerRun)
        {
            timer.cancel();
            L.d("手动关闭视频播放，定时器关闭");
            msg=new Message();
            msg.obj="视频播放结束";
            ToastHandle.sendMessage(msg);
            capture.release();
        }
        else
        {
            L.d("请先播放视频");
            msg=new Message();
            msg.obj="请先播放视频";
            ToastHandle.sendMessage(msg);
        }
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
                    msg=new Message();
                    msg.obj="视频播放结束";//代表视频播放结束
                    ToastHandle.sendMessage(msg);
                    IsTimerRun=false;
                    capture.release();
                    return;
                }
            }
            ImgProcess pro=new ImgProcess();
            pro.Start(LeftFrame,RightFrame,1.5,EyeNum);
            pro.ProcessSeparate();
            Leye=pro.OutLeye();
            Reye=pro.OutReye();
            LeftView=Bitmap.createBitmap(Leye.width(),Leye.height(),Bitmap.Config.RGB_565);
            RightView=Bitmap.createBitmap(Reye.width(),Reye.height(),Bitmap.Config.RGB_565);
            try
            {
                Utils.matToBitmap(Leye,LeftView);
                Utils.matToBitmap(Reye,RightView);
            }
            catch (Exception e)
            {
                L.e("格式转换发生异常："+e.toString());
            }
            msg = new Message();
            ViewHandle.sendMessage(msg);
        }
    }
    Handler ViewHandle=new Handler()//用以实时刷新显示左右眼
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
        }
    };
}
