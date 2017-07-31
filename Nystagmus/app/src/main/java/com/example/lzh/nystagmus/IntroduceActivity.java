package com.example.lzh.nystagmus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.lzh.nystagmus.Utils.HttpUtil;
import com.example.lzh.nystagmus.Utils.L;
import com.example.lzh.nystagmus.Utils.T;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.lzh.nystagmus.R.id.camera_address_left_input;
import static com.example.lzh.nystagmus.R.id.fab;
import static com.example.lzh.nystagmus.R.id.pin;

public class IntroduceActivity extends AppCompatActivity {

    private ImageView bingPicImg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduce);

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 设置返回键和菜单栏可用，可见
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bingPicImg=(ImageView) findViewById(R.id.toolbar_image_view);
        if(isNetworkAvailable()&&ping())
        {
            //有网络连接并且连接到外网
            loadBingPic();
            L.d("已更新图片");
        }
        else
        {
            Glide.with(this).load(R.drawable.background).into(bingPicImg);
            L.d("加载保存照片");
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                T.showShort(IntroduceActivity.this,"正在访问学校主页");
                L.d("正在访问学校主页");
                Intent intent=new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://www.hit.edu.cn/"));
                startActivity(intent);
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case  android.R.id.home:
            {
                this.finish();
                return true;
            }
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    //加载必应每日一图
    private void loadBingPic()
    {
        String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic,new okhttp3.Callback(){
            @Override
            public void onResponse(Call call,Response response) throws IOException{
                final String bingPic=response.body().string();
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        Glide.with(IntroduceActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }

            @Override
            public void onFailure(Call call,IOException e)
            {
                L.d(e.toString());
            }
        });
    }
    /**
     * 判断网络情况
     * @return false 表示没有网络 true 表示有网络
     */
    private boolean isNetworkAvailable()
    {
        // 得到网络连接信息
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // 去进行判断网络是否连接
        if (manager.getActiveNetworkInfo() != null) {
            return manager.getActiveNetworkInfo().isAvailable();
        }
        return false;
    }

    /**
     * 通过Ping来判断是否真有外网连接
     * @return flase 表示没有网络 true 表示有网络
     */
    private boolean ping()
    {
        try {
            String ip = "www.baidu.com";// ping 的地址，可以换成任何一种可靠的外网
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 100 " + ip);// ping网址1次

            // ping的状态
            //status 等于0的时候表示网络可用，status等于2时表示当前网络不可用。
            int status = p.waitFor();
            if (status == 0) {
                //有网
                return true;
            } else {
                //无网
                return false;
            }
        }
        catch (IOException e)
        {
            return false;
        }
        catch (InterruptedException ie)
        {
            return false;
        }
    }
}
