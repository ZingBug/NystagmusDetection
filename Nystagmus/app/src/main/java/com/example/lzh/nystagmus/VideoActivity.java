package com.example.lzh.nystagmus;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.lzh.nystagmus.Utils.T;
import com.example.lzh.nystagmus.Utils.Tool;
import com.example.lzh.nystagmus.Utils.VideoInfo;
import com.example.lzh.nystagmus.Utils.VideoInfoAdapter;
import com.example.lzh.nystagmus.Utils.VideoItemDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static android.media.MediaMetadataRetriever.METADATA_KEY_DATE;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;

public class VideoActivity extends AppCompatActivity {

    private List<VideoInfo> videoInfoList=new ArrayList<>();
    private RecyclerView recyclerView;
    private VideoInfoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP)
        {
            //大于安卓5.0即API21版本可用
            //导航栏颜色与状态栏统一
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;//如果想要隐藏导航栏，可以加上View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(getResources().getColor(R.color.blue));
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // 设置返回键可用，可见
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView=(RecyclerView)findViewById(R.id.recycler_video);
        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new VideoItemDecoration());

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                //返回
                this.finish();
                break;
            }
            case R.id.removeAllVideo:
            {
                //删除所有视频
                if(adapter.removeAllItem())
                {
                    T.showShort(VideoActivity.this,"清空所有视频成功");
                }
                else
                {
                    T.showShort(VideoActivity.this,"清空所有视频失败");
                }
                break;
            }
            default:break;
        }
        return true;
    }
    @Override
    public void onResume()
    {
        //在准备交互时加载列表
        super.onResume();
        videoInfoList.clear();
        loadVideoList();
        adapter=new VideoInfoAdapter(this,videoInfoList);
        adapter.setOnItemOnClickListener(new VideoInfoAdapter.OnItemOnClickListener() {
            @Override
            public void onItemOnClick(View view,int pos)
            {
                //短暂按监听
                //T.showShort(VideoActivity.this,"短");
            }
            @Override
            public void onItemLongOnClick(View view,int pos)
            {
                //长按监听
                //T.showShort(VideoActivity.this,"长");
                showPopMenu(view,pos);
            }
        });
        recyclerView.setAdapter(adapter);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //创建右上方菜单栏按钮
        getMenuInflater().inflate(R.menu.video_menu,menu);
        return true;
    }
    private void showPopMenu(View view,final int pos)
    {
        PopupMenu popupMenu=new PopupMenu(this,view);
        popupMenu.getMenuInflater().inflate(R.menu.video_popup_menu,popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                switch (item.getItemId())
                {
                    case R.id.testItem:
                    {
                        String videoPath=adapter.testItem(pos);
                        if(!videoPath.equals(""))
                        {
                            Intent intent=new Intent(VideoActivity.this,MainActivity.class);
                            intent.putExtra("VideoPath",videoPath);
                            setResult(RESULT_OK,intent);
                            finish();
                        }
                        else
                        {
                            T.showShort(VideoActivity.this,"视频测试失败");
                        }
                        break;
                    }
                    case R.id.renameItem:
                    {
                        //重命名操作
                        showInputDialog(pos);
                        break;
                    }
                    case R.id.removeItem:
                    {
                        //删除操作
                        if(adapter.removeItem(pos))
                        {
                            T.showShort(VideoActivity.this,"删除成功");
                        }
                        else
                        {
                            T.showShort(VideoActivity.this,"删除失败");
                        }
                        break;
                    }
                    default:break;
                }
                return false;
            }
        });
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener(){
            @Override
            public void onDismiss(PopupMenu menu)
            {
                //PopupMenu关闭事件
            }
        });
        popupMenu.show();
    }
    private void loadVideoList()
    {
        Vector<String> vecName=getVideoFileName(Tool.VideoStoragePath);
        for(String name:vecName)
        {
            String absolutePath=Tool.VideoStoragePath+"/"+name;
            MediaMetadataRetriever retriever=new MediaMetadataRetriever();
            retriever.setDataSource(absolutePath);
            String periodTime=getVideoDuration(retriever.extractMetadata(METADATA_KEY_DURATION));
            String date=retriever.extractMetadata(METADATA_KEY_DATE);
            VideoInfo videoInfo=new VideoInfo(name,date,periodTime,absolutePath);
            videoInfoList.add(videoInfo);
        }
    }
    private Vector<String> getVideoFileName(String fileAbsolutePath)
    {
        //获取目录下所有mp4文件
        Vector<String> vector=new Vector<>();
        File file=new File(fileAbsolutePath);
        File[] subFile=file.listFiles();
        for(int iFileLength=0;iFileLength<subFile.length;iFileLength++)
        {
            //判断是否为文件夹
            if(!subFile[iFileLength].isDirectory())
            {
                //不是文件夹，则是文件
                String filename=subFile[iFileLength].getName();
                //判断是否为MP4结尾
                if(filename.trim().toLowerCase().endsWith(".mp4"))
                {
                    //是MP4文件
                    vector.add(filename);
                }
            }
        }
        return vector;
    }
    private String getVideoDuration(String src)
    {
        //讲毫秒形式转为正常时间格式
        long time=Long.parseLong(src);
        Date date=new Date(time);
        int second=date.getSeconds();//获取秒
        int minute=date.getMinutes();//获取分钟
        int hour=date.getHours()-8;//获取小时，需要减去默认的8小时
        String sec=String.valueOf(second);
        String min=String.valueOf(minute);
        String h=String.valueOf(hour);
        return h+":"+min+":"+sec;
    }
    private void showInputDialog(final int pos)
    {
        final EditText editText=new EditText(VideoActivity.this);
        AlertDialog.Builder dialog=new AlertDialog.Builder(VideoActivity.this);
        dialog.setTitle("请输入新文件名:").setView(editText);
        dialog.setIcon(R.drawable.itemrename);
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if(adapter.renameItem(pos,editText.getText().toString()))
                {
                    T.showShort(VideoActivity.this,"重命名成功");
                }
                else
                {
                    T.showShort(VideoActivity.this,"重命名失败");
                }
            }
        }).setNegativeButton("取消",null).show();
    }
}
