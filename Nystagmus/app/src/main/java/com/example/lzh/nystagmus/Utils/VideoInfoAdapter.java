package com.example.lzh.nystagmus.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.lzh.nystagmus.MainActivity;
import com.example.lzh.nystagmus.R;

import java.io.File;
import java.util.List;


/**
 * Created by LZH on 2017/9/12.
 */

public class VideoInfoAdapter extends RecyclerView.Adapter<VideoInfoAdapter.ViewHolder> {
    private List<VideoInfo> mVideoInfoList;
    private Context mContext;

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        View VideoView;
        TextView VideoName;
        TextView VideoDate;
        TextView VideoPeriodTime;
        ImageView VideoCover;
        public ViewHolder(View view)
        {
            super(view);
            VideoView=view;
            VideoName=(TextView)view.findViewById(R.id.video_name);
            VideoDate=(TextView)view.findViewById(R.id.video_date);
            VideoPeriodTime=(TextView)view.findViewById(R.id.video_periodtime);
            VideoCover=(ImageView)view.findViewById(R.id.video_cover);
        }
    }
    public VideoInfoAdapter(Context context,List<VideoInfo> videoInfoList)
    {
        mVideoInfoList=videoInfoList;
        mContext=context;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,int viweType)
    {
        //用于创建ViewHolder实例，并将video_item布局加载进去
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item,parent,false);
        ViewHolder holder=new ViewHolder(view);
        return holder;
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, final int pos)
    {
        //用于对RecyclerView子项的数据进行赋值，会在每个子项被滚动到屏幕内的时候执行，position参数得到当前项的VideoInfo实例
        final int position=holder.getLayoutPosition();
        VideoInfo videoInfo=mVideoInfoList.get(position);
        holder.VideoName.setText(videoInfo.getName());
        holder.VideoPeriodTime.setText(videoInfo.getPeriodTime());
        holder.VideoDate.setText(videoInfo.getDate());
        Glide.with(mContext).load(Uri.fromFile(new File(videoInfo.getAbsolutePath()))).into(holder.VideoCover);
        if(onItemOnClickListener!=null)
        {
            //增加短暂按监听
            holder.VideoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    onItemOnClickListener.onItemOnClick(view,position);
                }
            });
            //增加长按监听
            holder.VideoView.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View view)
                {
                    onItemOnClickListener.onItemLongOnClick(view,position);
                    return false;
                }
            });
        }
    }
    @Override
    public int getItemCount()
    {
        return mVideoInfoList.size();
    }
    //监听接口
    public interface OnItemOnClickListener
    {
        void onItemOnClick(View view,int pos);//短暂按
        void onItemLongOnClick(View view,int pos);//长按
    }
    private OnItemOnClickListener onItemOnClickListener;
    public void setOnItemOnClickListener(OnItemOnClickListener listener)
    {
        this.onItemOnClickListener=listener;
    }
    //删除条目
    public boolean removeItem(int pos)
    {
        VideoInfo removeVideo=mVideoInfoList.get(pos);
        if(!deleteFile(removeVideo.getAbsolutePath()))
        {
            return false;
        }
        mVideoInfoList.remove(pos);
        notifyItemRemoved(pos);
        //在删除同时，刷新改变位置item下方的所有Item的位置
        if(pos!=mVideoInfoList.size())
        {
            notifyItemRangeChanged(pos,mVideoInfoList.size()-pos);
        }
        return true;
    }
    private boolean deleteFile(String filePath)
    {
        //删除单个文件
        File file=new File(filePath);
        if(file.isFile()&&file.exists())
        {
            return file.delete();
        }
        else
        {
            return false;
        }
    }
    public boolean renameItem(int pos,String rename)
    {
        //重命名
        String name=rename;
        //先判断是否有.mp4
        if(rename.indexOf(".")!=-1)
        {
            //如果有.
            name=rename.substring(0,rename.lastIndexOf("."));
        }
        VideoInfo videoInfo=mVideoInfoList.get(pos);
        String oldPath=videoInfo.getAbsolutePath();
        String newName=name+".mp4";
        if(!renameFile(oldPath,newName))
        {
            return false;
        }
        String newPath=oldPath.substring(0,oldPath.lastIndexOf("/")+1)+newName;
        videoInfo.setAbsolutePath(newPath);
        videoInfo.setName(newName);
        mVideoInfoList.set(pos,videoInfo);
        notifyDataSetChanged();
        return true;
    }
    private boolean renameFile(String filePath,String rename)
    {
        File file=new File(filePath);
        if(file.isFile()&&file.exists())
        {
            String path=filePath.substring(0,filePath.lastIndexOf("/")+1)+rename;
            File newFile=new File(path);
            file.renameTo(newFile);
            return true;
        }
        else
        {
            return false;
        }
    }
    public String testItem(int pos)
    {
        String videoPath="";
        VideoInfo videoInfo=mVideoInfoList.get(pos);
        videoPath=videoInfo.getAbsolutePath();
        return videoPath;
    }
    public boolean removeAllItem()
    {
        //删除所有条目
        int len=mVideoInfoList.size();
        for(int i=len-1;i>=0;i--)
        {
            VideoInfo videoInfo=mVideoInfoList.get(i);
            if(!deleteFile(videoInfo.getAbsolutePath()))
            {
                notifyDataSetChanged();
                return false;
            }
            mVideoInfoList.remove(i);
        }
        notifyDataSetChanged();
        return true;
    }
}
