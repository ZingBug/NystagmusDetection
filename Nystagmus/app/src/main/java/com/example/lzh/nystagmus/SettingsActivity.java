package com.example.lzh.nystagmus;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.example.lzh.nystagmus.Utils.L;
import com.example.lzh.nystagmus.Utils.T;
import com.example.lzh.nystagmus.Utils.Tool;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import static com.example.lzh.nystagmus.R.drawable.abc_cab_background_internal_bg;
import static com.example.lzh.nystagmus.R.drawable.settings;

public class SettingsActivity extends AppCompatActivity {

    private EditText edit_LeftCameraAddress;
    private EditText edit_RightCameraAddress;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private static String tempLeftAddress="";//用于在应用打开生存周期内暂存地址信息
    private static String tempRightAddress="";//用于在应用打开生存周期内暂存地址信息
    private static boolean isTempAddress=false;//用于判断应用是否是第一次打开

    private Button cameraAddressUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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

        edit_LeftCameraAddress=(EditText)findViewById(R.id.camera_address_left_input);
        edit_RightCameraAddress=(EditText)findViewById(R.id.camera_address_right_input);

        cameraAddressUpdate=(Button)findViewById(R.id.camera_address_update_button);

        pref=getSharedPreferences("CameraAddress",MODE_PRIVATE);
        editor=pref.edit();

        cameraAddressUpdate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                switch (v.getId())
                {
                    case R.id.camera_address_update_button:
                    {
                        //更新地址
                        String inputLeftAddress=edit_LeftCameraAddress.getText().toString();
                        String inputRightAddress=edit_RightCameraAddress.getText().toString();
                        save(inputLeftAddress,inputRightAddress);
                        Tool.AddressLeftEye=inputLeftAddress;
                        Tool.AddressRightEye=inputRightAddress;
                        T.showShort(SettingsActivity.this,"网络摄像头地址修改成功");
                        L.d("网络摄像头地址修改成功");
                        break;
                    }
                    default:
                        break;
                }
            }
        });
    }
    @Override
    public void onResume()
    {
        //在活动交互时调用
        String leftAddress;
        String rightAddress;
        if(!isTempAddress)
        {
            leftAddress=pref.getString("LeftCameraAddress",Tool.AddressLeftEye);
            rightAddress=pref.getString("RightCameraAddress",Tool.AddressRightEye);
            isTempAddress=true;
        }
        else
        {
            leftAddress=tempLeftAddress;
            rightAddress=tempRightAddress;
        }
        edit_LeftCameraAddress.setText(leftAddress);
        edit_RightCameraAddress.setText(rightAddress);
        super.onResume();
    }
    @Override
    public void onPause()
    {
        //在退出活动时暂存信息，下次打开时直接可以显示
        tempLeftAddress=edit_LeftCameraAddress.getText().toString();
        tempRightAddress=edit_RightCameraAddress.getText().toString();
        super.onPause();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case  android.R.id.home:
            {
                //返回
                this.finish();
                break;
            }
            case R.id.settings_defalut:
            {
                //恢复默认设置
                edit_LeftCameraAddress.setText(Tool.AddressLeftEyeDefault);
                edit_RightCameraAddress.setText(Tool.AddressRightEyeDefault);
                //cameraAddressUpdate.performClick();//模拟点击事件
                //为了给出不一样的提醒，不采用模拟点击按钮了
                save(Tool.AddressLeftEyeDefault,Tool.AddressRightEyeDefault);
                Tool.AddressLeftEye=Tool.AddressLeftEyeDefault;
                Tool.AddressRightEye=Tool.AddressRightEyeDefault;
                T.showShort(SettingsActivity.this,"恢复默认地址成功");
                L.d("恢复默认地址成功");
                break;
            }
            default:
                break;
        }
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.settings_menu,menu);
        return true;
    }
    public void save(String leftAddress,String rightAddress)
    {
        editor.putString("LeftCameraAddress",leftAddress);//左眼写入
        editor.putString("RightCameraAddress",rightAddress);//右眼写入
        editor.apply();//执行应用
    }
}
