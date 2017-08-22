package com.example.lzh.nystagmus.Utils;


import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import static android.R.attr.end;
import static android.R.attr.endX;
import static android.R.attr.flipInterval;
import static android.R.attr.max;
import static android.R.attr.yesNoPreferenceStyle;
import static com.example.lzh.nystagmus.Utils.Tool.HighTidePeriodSecond;

/**
 * Created by LZH on 2017/8/4.
 */

public class Calculate {
    private LinkedList<Float> LeyeX;//左眼x轴坐标集合,链表易于频繁数据操作
    private LinkedList<Float> ReyeX;//右眼x轴坐标集合,链表易于频繁数据操作
    private Hashtable<Integer,Float> LeyeSecondX;//用哈希表来存储左眼每秒的平均SPV
    private Hashtable<Integer,Float> ReyeSecondX;//用哈希表来存储右眼每秒的平均SPV
    private List<Float> slopeList_L;//左眼所有锯齿波SPV的集合 目前没有用到
    private List<Float> slopeList_R;//右眼所有锯齿波SPV的集合
    private List<Float> slopeSecondList_L;//左眼某秒锯齿波SPV的集合
    private List<Float> slopeSecondList_R;//右眼某秒锯齿波SPV的集合
    private Hashtable<Integer,Float> LeyeDynamicPeriodSPV;//用以保存动态周期内的SPV,比如1-3秒,2-4秒等,key是结束时间
    private Hashtable<Integer,Float> ReyeDynamicPeriodSPV;
    private Hashtable<Integer,Integer> LeyeSecondFastPhaseNum;//用于保存各秒的快相方向个数
    private Hashtable<Integer,Integer> ReyeSecondFastPhaseNum;

    /*暂时变量集合*/
    private Float startX_L;//某一段直线最开始值
    private Float endX_L;//某一段直线末尾值
    private boolean lineBegin_L;//是否开始新的一段直线
    private float lineLong_L;//某一段直线长度
    private boolean lineDir_L;//某一段直线方向  true代表正斜率  false代表负斜率
    private Float slope_L;//某一段直线斜率
    private HashSet<Float> waveSlope_L;//用来保存一个完整锯齿波内的斜率，即一个正斜率，一个负斜率

    private Float startX_R;//某一段直线最开始值
    private Float endX_R;//某一段直线末尾值
    private boolean lineBegin_R;//是否开始新的一段直线
    private float lineLong_R;//某一段直线长度
    private boolean lineDir_R;//某一段直线方向  true代表正斜率  false代表负斜率
    private Float slope_R;//某一段直线斜率
    private HashSet<Float> waveSlope_R;//用来保存一个完整锯齿波内的斜率，即一个正斜率，一个负斜率

    private boolean IsOverSpvPeriodLeye=false;
    private boolean IsOverSpvPeriodReye=false;

    /**
     * 构造函数
     */
    public Calculate()
    {
        this.lineDir_L=true;
        this.lineLong_L=0f;
        this.lineBegin_L=true;
        this.lineDir_R=true;
        this.lineLong_R=0f;
        this.lineBegin_R=true;

        this.waveSlope_L=new HashSet<Float>();
        this.waveSlope_R=new HashSet<Float>();
        this.slopeSecondList_L=new ArrayList<Float>();
        this.slopeSecondList_R=new ArrayList<Float>();

        this.LeyeX=new LinkedList<Float>();//初始化左眼X轴坐标容器
        this.ReyeX=new LinkedList<Float>();//初始化右眼X轴坐标容器
        this.LeyeSecondX=new Hashtable<Integer,Float>();//哈希表初始化
        this.ReyeSecondX=new Hashtable<Integer,Float>();//哈希表初始化
        this.LeyeDynamicPeriodSPV=new Hashtable<Integer, Float>();//哈希表初始化
        this.ReyeDynamicPeriodSPV=new Hashtable<Integer, Float>();//哈希表初始化
        this.LeyeSecondFastPhaseNum=new Hashtable<Integer, Integer>();//哈希表初始化
        this.ReyeSecondFastPhaseNum=new Hashtable<Integer, Integer>();//哈希表初始化
    }
    /**
     * 添加左眼X轴坐标
     * @param x x轴坐标添加值
     */
    public void addLeyeX(float x)
    {
        LeyeX.add(x);
    }
    /**
     * 添加右眼X轴坐标
     * @param x x轴坐标添加值
     */
    public void addReyeX(float x)
    {
        ReyeX.add(x);
    }
    /**
     * 处理左眼X轴坐标并保存各秒平均SPV
     * @param second 当前秒数
     */
    public void processLeyeX(int second) throws NumberFormatException
    {
        //LeyeX内保存的点数未超过1s且需要最后结束处理剩下点
        lineBegin_L=true;
        waveSlope_L.clear();
        slopeSecondList_L.clear();
        lineDir_L=true;
        lineLong_L=0f;
        int num=0;//用来表示第几个点
        int fastPhaseNum=0;//用于计算快相方向,快相为正则+1,快相为负则-1
        for(Float x:LeyeX)
        {
            //遍历所有1s,一秒一秒的来处理
            //先来判断是否完成一个完整的锯齿波
            if(waveSlope_L.size()==2)
            {
                /*取慢相,返回值为绝对值*/
                slope_L=miniSlope(waveSlope_L);
                slopeSecondList_L.add(slope_L);
                /*取快相，返回值为正常值*/
                slope_L=maxSlope(waveSlope_L);
                if(slope_L>0)
                {
                    ++fastPhaseNum;
                }
                else if(slope_L<0)
                {
                    --fastPhaseNum;
                }
                waveSlope_L.clear();
            }
            ++lineLong_L;//虚线长度
            if(lineBegin_L)
            {
                ++num;
                //先来确定第一条直线是正斜率还是负斜率
                if(num==1)
                {
                    //第一个点
                    startX_L = x;
                    endX_L = x;
                }
                else if(num==2)
                {
                    //第二个点
                    if(x>startX_L)
                    {
                        //正斜率
                        lineDir_L=true;
                    }
                    else
                    {
                        //负斜率
                        lineDir_L=false;
                    }
                    endX_L=x;
                    --lineLong_L;//需要减去一个坐标位置
                    lineBegin_L=false;
                }
            }
            else
            {
                if(x>endX_L)
                {
                    //正斜率 往上走
                    if(!lineDir_L)
                    {
                        //如果之前是往下走的，现在已经往上走了，所以代表之前那段直线结束
                        //开始计算上一段负斜率
                        slope_L=(endX_L-startX_L)*Tool.SPVConversionRatio/(lineLong_L-1f);
                        waveSlope_L.add(slope_L);
                        lineLong_L=1f;
                        startX_L=endX_L;
                        endX_L=x;
                    }
                    lineDir_L=true;
                    endX_L=x;
                }
                else
                {
                    //负斜率  往下走
                    if(lineDir_L)
                    {
                        //如果之前是往上走的，现在已经往下走了，所以代表之前那段直线结束
                        //开始计算上一段正斜率
                        slope_L=(endX_L-startX_L)*Tool.SPVConversionRatio/(lineLong_L-1f);
                        waveSlope_L.add(slope_L);
                        lineLong_L=1f;
                        startX_L=endX_L;
                        endX_L=x;
                    }
                    lineDir_L=false;
                    endX_L=x;
                }
            }
        }
        LeyeX.clear();//清除剩下的所有数据了
        float sumSPV=0f;

        if(slopeSecondList_L.size()>0)
        {
            //队列不为空时
            for(Float tempSPV:slopeSecondList_L)
            {
                sumSPV+=tempSPV;
            }
            LeyeSecondX.put(second,sumSPV/(float) slopeSecondList_L.size());//存入每秒的平均SPV
        }
        else
        {
            //队列为空时
            LeyeSecondX.put(second,0f);//存入每秒的平均SPV
        }

        LeyeSecondFastPhaseNum.put(second,fastPhaseNum);//存入每秒的快相方向个数
    }
    /**
     * 处理右眼X轴坐标并保存各秒平均SPV
     * @param second 当前秒数
     */
    public void processReyeX(int second) throws NumberFormatException
    {
        //ReyeX内保存的点数未超过1s且需要最后结束处理剩下点
        lineBegin_R=true;
        waveSlope_R.clear();
        slopeSecondList_R.clear();
        lineDir_R=true;
        lineLong_R=0f;
        int num=0;//用来表示第几个点
        int fastPhaseNum=0;
        for(Float x:ReyeX)
        {
            //遍历所有1s,一秒一秒的来处理
            //先来判断是否完成一个完整的锯齿波
            if(waveSlope_R.size()==2)
            {
                /*取慢相,返回值为绝对值*/
                slope_R=miniSlope(waveSlope_R);
                slopeSecondList_R.add(slope_R);
                /*取快相，返回值为正常值*/
                slope_R=maxSlope(waveSlope_R);
                if(slope_R>0)
                {
                    ++fastPhaseNum;
                }
                else if(slope_R<0)
                {
                    --fastPhaseNum;
                }
                waveSlope_R.clear();
            }
            ++lineLong_R;//虚线长度
            if(lineBegin_R)
            {
                ++num;
                //先来确定第一条直线是正斜率还是负斜率
                if(num==1)
                {
                    //第一个点
                    startX_R = x;
                    endX_R = x;
                }
                else if(num==2)
                {
                    //第二个点
                    if(x>startX_R)
                    {
                        //正斜率
                        lineDir_R=true;
                    }
                    else
                    {
                        //负斜率
                        lineDir_R=false;
                    }
                    endX_R=x;
                    --lineLong_R;//需要减去一个坐标位置
                    lineBegin_R=false;
                }
            }
            else
            {
                if(x>endX_R)
                {
                    //正斜率 往上走
                    if(!lineDir_R)
                    {
                        //如果之前是往下走的，现在已经往上走了，所以代表之前那段直线结束
                        //开始计算上一段负斜率
                        slope_R=(endX_R-startX_R)*Tool.SPVConversionRatio/(lineLong_R-1f);
                        waveSlope_R.add(slope_R);
                        lineLong_R=1f;
                        startX_R=endX_R;
                        endX_R=x;
                    }
                    lineDir_R=true;
                    endX_R=x;
                }
                else
                {
                    //负斜率  往下走
                    if(lineDir_R)
                    {
                        //如果之前是往上走的，现在已经往下走了，所以代表之前那段直线结束
                        //开始计算上一段正斜率
                        slope_R=(endX_R-startX_R)*Tool.SPVConversionRatio/(lineLong_R-1f);
                        waveSlope_R.add(slope_R);
                        lineLong_R=1f;
                        startX_R=endX_R;
                        endX_R=x;
                    }
                    lineDir_R=false;
                    endX_R=x;
                }
            }
        }
        ReyeX.clear();//清除剩下的所有数据了
        float sumSPV=0f;

        if(slopeSecondList_R.size()>0)
        {
            //队列不为空时
            for(Float tempSPV:slopeSecondList_R)
            {
                sumSPV+=tempSPV;
            }
            ReyeSecondX.put(second,sumSPV/(float) slopeSecondList_R.size());//存入每秒的平均SPV
        }
        else
        {
            //队列为空时
            ReyeSecondX.put(second,0f);//存入每秒的平均SPV
        }
        ReyeSecondFastPhaseNum.put(second,fastPhaseNum);//存入每秒的快相方向个数
    }
    /**
     * 取一个完整波形斜率中的绝对值的小值,返回也是绝对值
     * @param slopes 哈希队列
     * @return 哈希队列中所有元素绝对值的最小值，但返回值为绝对值，非正常值
     */
    private Float miniSlope(HashSet<Float> slopes)
    {
        float slope=Float.POSITIVE_INFINITY;//初始值为最大值
        boolean first=true;
        for(float single:slopes)
        {
            float temp=Math.abs(single);
            if(temp<slope)
            {
                slope=temp;
            }
        }
        return slope;
    }
    /**
     * 取一个完整波形斜率中的绝对值的小值,返回是原始值,非绝对值
     * @param slopes 哈希队列
     * @return 哈希队列中所有元素绝对值的最小值，但返回值为正常值，非绝对值
     */
    private Float maxSlope(HashSet<Float> slopes)
    {
        float absSlope=Float.NEGATIVE_INFINITY;//用于保存绝对值,初始值为最小值
        float slope=0f;
        for(float single:slopes)
        {
            float temp=Math.abs(single);
            if(temp>absSlope)
            {
                absSlope=temp;
                slope=single;
            }
        }
        return slope;
    }
    /**
     * 用以获取最大眼震反应期
     * @param eye 用来表示左右眼睛,true代表左眼,false代表右眼
     * @return 最大眼震反应期的结束时间*/
    public int getHighTidePeriod(boolean eye)
    {
        float temp=0f;
        float max=0;
        int tempSecond=0;
        int maxSecond=0;//最大眼震反应期开始时间
        Enumeration e;
        if(eye)
        {
            //左眼
            e=LeyeDynamicPeriodSPV.keys();
            while (e.hasMoreElements())
            {
                tempSecond=(int)e.nextElement();
                temp=LeyeDynamicPeriodSPV.get(tempSecond);
                if(temp>max)
                {
                    max=temp;
                    maxSecond=tempSecond;
                }
            }
        }
        else
        {
            //右眼
            e=ReyeDynamicPeriodSPV.keys();
            while (e.hasMoreElements())
            {
                tempSecond=(int)e.nextElement();
                temp=ReyeDynamicPeriodSPV.get(tempSecond);
                if(temp>max)
                {
                    max=temp;
                    maxSecond=tempSecond;
                }
            }
        }

        return maxSecond;
    }
    /**
     * 用以计算实时SPV
    * second用来表示当前时间
    * @return eye用来表示左右眼睛,true代表左眼,false代表右眼
    */
    public float getRealTimeSPV(int second,boolean eye)
    {
        int secondStart;
        float totalSPV=0f;
        float tempSPV=0f;
        if(second>= HighTidePeriodSecond)
        {
            //清空测试前段时间非完整周期数据
            if((!IsOverSpvPeriodLeye)&&eye)
            {
                //左眼
                IsOverSpvPeriodLeye=true;
                for(int i=1;i<HighTidePeriodSecond;++i)
                {
                    LeyeDynamicPeriodSPV.remove(i);
                }
            }
            else if((!IsOverSpvPeriodReye)&&(!eye))
            {
                //右眼
                IsOverSpvPeriodReye=true;
                for(int i=1;i<HighTidePeriodSecond;++i)
                {
                    ReyeDynamicPeriodSPV.remove(i);
                }
            }
            //可以计算要给完整的周期
            secondStart=second+1- HighTidePeriodSecond;
            for(; secondStart<= second; ++secondStart)
            {
                if(eye)
                {
                    //左眼
                    totalSPV+=LeyeSecondX.get(secondStart);
                }
                else
                {
                    //右眼
                    totalSPV+=ReyeSecondX.get(secondStart);
                }
            }
            tempSPV=totalSPV/(float) HighTidePeriodSecond;
        }
        else
        {
            //假如周期为10s,假设现在是4s,则只计算前4s的
            IsOverSpvPeriodLeye=false;
            IsOverSpvPeriodReye=false;
            secondStart=1;
            for(;secondStart<=second;++secondStart)
            {
                if(eye)
                {
                    //左眼
                    totalSPV+=LeyeSecondX.get(secondStart);
                }
                else
                {
                    //右眼
                    totalSPV+=ReyeSecondX.get(secondStart);
                }
            }
            tempSPV=totalSPV/(float)second;
        }
        if(eye)
        {
            //左眼
            LeyeDynamicPeriodSPV.put(second,tempSPV);
        }
        else
        {
            //右眼
            ReyeDynamicPeriodSPV.put(second,tempSPV);
        }

        return tempSPV;
    }
    /**
     * 用于计算之前时间最大的SPV值
     * @param eye 用来表示左右眼睛,true代表左眼,false代表右眼
     * @return 返回已经处理过的最大的SPV值(平均值)
     */
    public float getMaxSPV(boolean eye)
    {
        float tempMax=0f;
        if(eye)
        {
            //左眼
            for(float periodSPV:LeyeDynamicPeriodSPV.values())
            {
                if(periodSPV>tempMax)
                {
                    tempMax=periodSPV;
                }
            }
        }
        else
        {
            //右眼
            for(float periodSPV:ReyeDynamicPeriodSPV.values())
            {
                if(periodSPV>tempMax)
                {
                    tempMax=periodSPV;
                }
            }
        }
        return tempMax;
    }
    /**
    * 用于判断中枢是否病变
    * @return ture:正常  false:异常*/
    public boolean judgeDiagnosis()
    {
        for(float tempSPV:LeyeDynamicPeriodSPV.values())
        {
            if(tempSPV>Tool.SPVMaxValue)
            {
                return false;
            }
        }
        for(float tempSPV:ReyeDynamicPeriodSPV.values())
        {
            if(tempSPV>Tool.SPVMaxValue)
            {
                return false;
            }
        }
        return true;
    }
    /**
    * 用于判断眼睛快相方向
    * @param eye 用来表示左右眼睛,true代表左眼,false代表右眼
    * @return ture:左  false:右*/
    public boolean judgeFastPhase(boolean eye)
    {
        int num=0;
        if(eye)
        {
            //左眼
            for(int single:LeyeSecondFastPhaseNum.values())
            {
                num+=single;
            }
        }
        else
        {
            //右眼
            for(int single:ReyeSecondFastPhaseNum.values())
            {
                num+=single;
            }
        }
        return num<0;
    }
    /**
    * 判断是否存在眼睛处理结果
    * @param eye 用来表示左右眼睛,true代表左眼,false代表右眼
    * @return ture:有  false:无*/
    public boolean judegeEye(boolean eye)
    {
        if(eye)
        {
            return LeyeSecondFastPhaseNum.size()>0;
        }
        else
        {
            return ReyeSecondFastPhaseNum.size()>0;
        }
    }
    /**
     * @param endTime 最大眼震高潮期结束时间
     * @return 最大眼震高潮期时间区间
     */
    public static String getPeriod(int endTime) {
        if(endTime==1)
        {
            return "1s";
        }
        if(endTime<=HighTidePeriodSecond)
        {
            return "1s-"+endTime+"s";
        }
        else
        {
            return (endTime-HighTidePeriodSecond+1)+"s-"+endTime+"s";
        }
    }
}
