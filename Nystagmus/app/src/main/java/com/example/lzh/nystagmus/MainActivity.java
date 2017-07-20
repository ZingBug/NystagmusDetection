package com.example.lzh.nystagmus;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.y;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView imageView_leyedisplay;
    private static final int OPEN_VIDEO=1;
    private VideoCapture capture;
    Timer timer;

    private Mat frame;
    private Bitmap show;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button_openvideo=(Button)findViewById(R.id.open_video);
        Button button_startplay=(Button)findViewById(R.id.start_paly);
        imageView_leyedisplay=(ImageView)findViewById(R.id.picture_show);
        button_openvideo.setOnClickListener(this);
        button_startplay.setOnClickListener(this);
        Log.d("ffff","fffffffff");
        L.d("eeee","eeeeee");

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
            case R.id.start_paly:
            {
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
                        T.showShort(MainActivity.this, "视频打开成功");
                        Log.d("HJY","视频打开成功");
                        L.d("视频打开成功");
                    }
                    else
                    {
                        T.showShort(MainActivity.this, "视频打开失败");
                        L.d("视频打开失败");
                    }

                    timer=new Timer();
                    timer.schedule(new readFarme(),10,20);
                    //L.d("开启定时器");
                    /*
                    frame=new Mat();
                    while (capture.read(frame))
                    {
                        show=Bitmap.createBitmap(frame.width(),frame.height(),Bitmap.Config.RGB_565);
                        try
                        {
                            Utils.matToBitmap(frame,show);
                        }
                        catch (Exception e)
                        {
                            L.d("定时器发生异常"+e.toString());
                        }
                        imageView_leyedisplay.setImageBitmap(show);
                    }
                    */

                }
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
            frame=new Mat();
            if(!capture.read(frame))
            {
                timer.cancel();
            }

            show=Bitmap.createBitmap(frame.width(),frame.height(),Bitmap.Config.RGB_565);
            try
            {
                Utils.matToBitmap(frame,show);
            }
            catch (Exception e)
            {
                L.d("定时器发生异常"+e.toString());
            }

            Message msg = new Message();
            mHandle.sendMessage(msg);
        }
    }
    Handler mHandle=new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            imageView_leyedisplay.setImageBitmap(show);
            super.handleMessage(msg);
        }
    };
}
