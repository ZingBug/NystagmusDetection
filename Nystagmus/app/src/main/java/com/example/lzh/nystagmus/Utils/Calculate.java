package com.example.lzh.nystagmus.Utils;
import android.support.annotation.NonNull;

import org.bytedeco.javacpp.presets.opencv_core;

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
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import java.math.BigDecimal;

import static com.example.lzh.nystagmus.Utils.Tool.HighTidePeriodSecond;

/**
 * Created by HJY on 2017/8/4.
 */

public class Calculate {
    private LinkedList<Double> LeyeX;//左眼x轴坐标集合,链表易于频繁数据操作
    private LinkedList<Double> ReyeX;//右眼x轴坐标集合,链表易于频繁数据操作
    private LinkedList<Double> LeyeY;
    private LinkedList<Double> ReyeY;
    private Hashtable<Integer,Double> LeyeSecondX;//用哈希表来存储左眼每秒的平均SPV
    private Hashtable<Integer,Double> ReyeSecondX;//用哈希表来存储右眼每秒的平均SPV
    private Hashtable<Integer,Double> LeyeSecondY;
    private Hashtable<Integer,Double> ReyeSecondY;
    private Hashtable<Integer,Double> LeyeDynamicPeriodSPVX;//用以保存x轴动态周期内的SPV,比如1-3秒,2-4秒等,key是结束时间
    private Hashtable<Integer,Double> ReyeDynamicPeriodSPVX;
    private Hashtable<Integer,Double> LeyeDynamicPeriodSPVY;//用以保存y轴动态周期内的SPV,比如1-3秒,2-4秒等,key是结束时间
    private Hashtable<Integer,Double> ReyeDynamicPeriodSPVY;
    private Hashtable<Integer,Integer> LeyeSecondFastPhaseNumX;//用于x轴保存各秒的快相方向个数
    private Hashtable<Integer,Integer> ReyeSecondFastPhaseNumX;
    private Hashtable<Integer,Integer> LeyeSecondFastPhaseNumY;//用于y轴保存各秒的快相方向个数
    private Hashtable<Integer,Integer> ReyeSecondFastPhaseNumY;

    /**
     * 构造函数
     */
    public Calculate()
    {
        this.LeyeX=new LinkedList<Double>();//初始化左眼X轴坐标容器
        this.ReyeX=new LinkedList<Double>();//初始化右眼X轴坐标容器
        this.LeyeY=new LinkedList<Double>();
        this.ReyeY=new LinkedList<Double>();
        this.LeyeSecondX=new Hashtable<Integer,Double>();//哈希表初始化
        this.ReyeSecondX=new Hashtable<Integer,Double>();//哈希表初始化
        this.LeyeSecondY=new Hashtable<>();
        this.ReyeSecondY=new Hashtable<>();
        this.LeyeDynamicPeriodSPVX=new Hashtable<Integer, Double>();//哈希表初始化
        this.ReyeDynamicPeriodSPVX=new Hashtable<Integer, Double>();//哈希表初始化
        this.LeyeDynamicPeriodSPVY=new Hashtable<Integer, Double>();//哈希表初始化
        this.ReyeDynamicPeriodSPVY=new Hashtable<Integer, Double>();//哈希表初始化
        this.LeyeSecondFastPhaseNumX=new Hashtable<Integer, Integer>();//哈希表初始化
        this.ReyeSecondFastPhaseNumX=new Hashtable<Integer, Integer>();//哈希表初始化
        this.LeyeSecondFastPhaseNumY=new Hashtable<Integer, Integer>();//哈希表初始化
        this.ReyeSecondFastPhaseNumY=new Hashtable<Integer, Integer>();//哈希表初始化
    }
    /**
     * 添加左眼X轴坐标
     * @param x x轴坐标添加值
     */
    public void addLeyeX(double x)
    {
        LeyeX.add(x);
    }
    /**
     * 添加右眼X轴坐标
     * @param x x轴坐标添加值
     */
    public void addReyeX(double x)
    {
        ReyeX.add(x);
    }
    /**
     * 添加左眼Y轴坐标
     * @param x y轴坐标添加值
     */
    public void addLeyeY(double x)
    {
        LeyeY.add(x);
    }
    /**
     * 添加右眼Y轴坐标
     * @param x y轴坐标添加值
     */
    public void addReyeY(double x)
    {
        ReyeY.add(x);
    }
    /**
     * 处理左眼X轴坐标并保存各秒平均SPV,上锁
     * @param second 当前秒数
     */
    public void processLeyeX(int second) throws NumberFormatException
    {
        processEyePoints(second,LeyeX,LeyeSecondX,LeyeSecondFastPhaseNumX);
    }
    /**
     * 处理右眼X轴坐标并保存各秒平均SPV,上锁
     * @param second 当前秒数
     */
    public void processReyeX(int second) throws NumberFormatException
    {
        processEyePoints(second,ReyeX,ReyeSecondX,ReyeSecondFastPhaseNumX);
    }
    /**
     * 处理左眼Y轴坐标并保存各秒平均SPV,上锁
     * @param second 当前秒数
     */
    public void processLeyeY(int second) throws NumberFormatException
    {
        processEyePoints(second,LeyeY,LeyeSecondY,LeyeSecondFastPhaseNumY);
    }
    /**
     * 处理右眼Y轴坐标并保存各秒平均SPV,上锁
     * @param second 当前秒数
     */
    public void processReyeY(int second) throws NumberFormatException
    {
        processEyePoints(second,ReyeY,ReyeSecondY,ReyeSecondFastPhaseNumY);
    }
    /**
     * 某点坐标处理
     * @param second 时间秒数
     * @param eyeList 存放坐标点的列表
     * @param eyeSecondSPV 需要存放该秒时间的SPV的列表
     * @param eyeSecondFastPhaseNum 需要存放该秒快相的列表
     * @throws NumberFormatException 抛出异常处理
     */
    private void processEyePoints(int second,LinkedList<Double> eyeList,Hashtable<Integer,Double> eyeSecondSPV,Hashtable<Integer,Integer> eyeSecondFastPhaseNum)
            throws NumberFormatException
    {
        //eyeList内保存的点数未超过1s且需要最后结束处理剩下点
        boolean lineBegin=true;
        LinkedList<Double> waveSlope=new LinkedList<>();//用于保存一个完整的锯齿波内的斜率，即一个正斜率，一个负斜率
        List<Double> slowSlope=new LinkedList<>();//慢相斜率集合
        boolean lineDir=true;//当前斜率方向
        int fastPhaseNum=0;//用于计算快相方向,快相为正则+1,快相为负则-1
        double end=0f;
        Hashtable<Integer,Double> points=new Hashtable<>();//两个极值点之间的点，用于拟合
        int frameNum=0;//用来表示第几个点
        for(double x:eyeList)
        {
            //遍历所有1s,一秒一秒的来处理
            frameNum++;
            //先来判断是否完成一个完整的锯齿波
            if(waveSlope.size()==2)
            {
                /*取慢相,返回值为绝对值*/
                slowSlope.add(getMiniSlope(waveSlope));

                /*取快相，返回值为正常值*/
                double fastSlope=getMaxSlope(waveSlope);
                if(fastSlope>0)
                {
                    ++fastPhaseNum;
                }
                else if(fastSlope<0)
                {
                    --fastPhaseNum;
                }
                waveSlope.clear();
            }
            if(lineBegin)
            {
                //先来确定第一条直线是正斜率还是负斜率
                if(frameNum==1)
                {
                    //第一个点
                    end=x;
                }
                else if(frameNum==2)
                {
                    //第二个点
                    if(x>=end)
                    {
                        //正斜率
                        lineDir=true;
                    }
                    else
                    {
                        //负斜率
                        lineDir=false;
                    }
                    end=x;
                    lineBegin=false;//开始已经结束
                }
            }
            else
            {
                if(x>end)
                {
                    //正斜率 往上走
                    if(!lineDir)
                    {
                        //如果之前是往下走的，现在已经往上走了，所以代表之前那段直线结束
                        //开始计算上一段负斜率
                        if(points.size()>4)
                        {
                            //上一段直线大于4个点的时候才开始进行检测
                            double slope=getSlope(points);
                            waveSlope.add(slope);
                        }
                        //保存极小值点
                        double min=points.get(frameNum-1);
                        points.clear();
                        //将上一次的极小值点放入下一次计算直线中
                        points.put(frameNum-1,min);
                    }
                    lineDir=true;
                    end=x;
                }
                else
                {
                    //负斜率  往下走
                    if(lineDir)
                    {
                        //如果之前是往上走的，现在已经往下走了，所以代表之前那段直线结束
                        //开始计算上一段正斜率
                        if(points.size()>4)
                        {
                            //上一段直线大于四个点的时候才开始进行检测
                            double slope=getSlope(points);
                            waveSlope.add(slope);
                        }
                        //保存极大值点
                        double max=points.get(frameNum-1);
                        points.clear();
                        //将上一次极值点放入下一次的计算直线中去
                        points.put(frameNum-1,max);
                    }
                    lineDir=false;
                    end=x;
                }
            }
            points.put(frameNum,x);
        }
        LeyeX.clear();//清除剩下的所有数据了
        double sumSPV=0f;

        if(slowSlope.size()>0)
        {
            //队列不为空时
            for(double tempSPV:slowSlope)
            {
                sumSPV+=tempSPV;
            }
            eyeSecondSPV.put(second,sumSPV/slowSlope.size());//存入每秒的平均SPV
        }
        else
        {
            //队列为空时
            eyeSecondSPV.put(second,sumSPV);//存入每秒的平均SPV
        }

        eyeSecondFastPhaseNum.put(second,fastPhaseNum);//存入每秒的快相方向个数
    }
    /**
     * 根据最小二乘法拟合得到直线斜率
     * @param points 点的集合
     * @return 拟合斜率
     */
    private double getSlope(Hashtable<Integer,Double> points)
    {
        WeightedObservedPoints obs=new WeightedObservedPoints();
        Enumeration e=points.keys();
        int key;
        while (e.hasMoreElements())
        {
            key=(int)e.nextElement();
            obs.add(key,points.get(key));
        }
        PolynomialCurveFitter fitter=PolynomialCurveFitter.create(1);
        double[] coeff=fitter.fit(obs.toList());

        BigDecimal bd = new BigDecimal(coeff[1]);
        BigDecimal slope=bd.setScale(3,BigDecimal.ROUND_HALF_UP);
        return slope.doubleValue()*10;
    }
    /**
     * 取一个完整波形斜率中的绝对值的小值,返回也是绝对值
     * @param slopes 哈希队列
     * @return 哈希队列中所有元素绝对值的最小值，但返回值为绝对值，非正常值
     */
    private double getMiniSlope(LinkedList<Double> slopes)
    {
        double slope=Double.POSITIVE_INFINITY;//初始值为最大值
        for(double s:slopes)
        {
            double temp=Math.abs(s);
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
    private double getMaxSlope(LinkedList<Double> slopes)
    {
        double absSlope= Double.NEGATIVE_INFINITY;
        double slope=0f;
        for(double s:slopes)
        {
            double temp= Math.abs(s);
            if(temp>absSlope)
            {
                absSlope=temp;
                slope=s;
            }
        }
        return slope;
    }
    /**
     * 用以获取最大眼震反应期  目前仅用在x轴
     * @param eye 用来表示左右眼睛,true代表左眼,false代表右眼
     * @return 最大眼震反应期的结束时间*/
    public int getHighTidePeriod(boolean eye)
    {
        double temp;
        double max=Double.MIN_VALUE;
        int tempSecond;
        int maxSecond=0;//最大眼震反应期开始时间
        Enumeration e;
        if(eye)
        {
            //左眼
            e=LeyeDynamicPeriodSPVX.keys();
            while (e.hasMoreElements())
            {
                tempSecond=(int)e.nextElement();
                temp=LeyeDynamicPeriodSPVX.get(tempSecond);
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
            e=ReyeDynamicPeriodSPVX.keys();
            while (e.hasMoreElements())
            {
                tempSecond=(int)e.nextElement();
                temp=ReyeDynamicPeriodSPVX.get(tempSecond);
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
     * 用以计算x轴实时SPV
    * second用来表示当前时间
    * @return eye用来表示左右眼睛,true代表左眼,false代表右眼
    */
    public double getRealTimeSPVX(int second,boolean eye)
    {
        int secondStart;
        double totalSPV=0f;
        double tempSPV;
        int num=0;//次数
        if(second>=HighTidePeriodSecond)
        {
            //删掉之前的不到一个完整周期的平均SPV
            for(int i=1;i<HighTidePeriodSecond;i++)
            {
                if(eye)
                {
                    //左眼
                    if(LeyeDynamicPeriodSPVX.containsKey(i))
                    {
                        LeyeDynamicPeriodSPVX.remove(i);
                    }
                    else
                    {
                        break;
                    }
                }
                else
                {
                    //右眼
                    if(ReyeDynamicPeriodSPVX.containsKey(i))
                    {
                        ReyeDynamicPeriodSPVX.remove(i);
                    }
                    else
                    {
                        break;
                    }
                }
            }
            //重新开始计算
            secondStart=second+1-HighTidePeriodSecond;
        }else
        {
            secondStart=1;
        }
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
            num++;
        }
        tempSPV=totalSPV/num;
        if(eye)
        {
            //左眼
            LeyeDynamicPeriodSPVX.put(second,tempSPV);
        }
        else
        {
            //右眼
            ReyeDynamicPeriodSPVX.put(second,tempSPV);
        }
        return tempSPV;
    }
    /**
     * 用以计算y轴实时SPV
     * second用来表示当前时间
     * @return eye用来表示左右眼睛,true代表左眼,false代表右眼
     */
    public double getRealTimeSPVY(int second,boolean eye)
    {
        int secondStart;
        double totalSPV=0f;
        double tempSPV;
        int num=0;
        if(second>= HighTidePeriodSecond)
        {
            //可以计算要给完整的周期
            secondStart=second+1- HighTidePeriodSecond;

        }
        else
        {
            //假如周期为10s,假设现在是4s,则只计算前4s的
            secondStart=1;
        }
        for(;secondStart<=second;++secondStart)
        {
            if(eye)
            {
                //左眼
                totalSPV+=LeyeSecondY.get(secondStart);
            }
            else
            {
                //右眼
                totalSPV+=ReyeSecondY.get(secondStart);
            }
            num++;
        }
        tempSPV=totalSPV/num;

        if(eye)
        {
            //左眼
            LeyeDynamicPeriodSPVY.put(second,tempSPV);
        }
        else
        {
            //右眼
            ReyeDynamicPeriodSPVY.put(second,tempSPV);
        }

        return tempSPV;
    }
    /**
     * 用于轴计算之前时间最大的SPV值
     * @param eye 用来表示左右眼睛,true代表左眼,false代表右眼
     * @return 返回已经处理过的最大的SPV值(平均值)
     */
    public double getMaxSPVX(boolean eye)
    {
        double tempMax=Double.MIN_VALUE;
        if(eye)
        {
            //左眼
            for(double periodSPV:LeyeDynamicPeriodSPVX.values())
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
            for(double periodSPV:ReyeDynamicPeriodSPVX.values())
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
     * 用于y轴计算之前时间最大的SPV值
     * @param eye 用来表示左右眼睛,true代表左眼,false代表右眼
     * @return 返回已经处理过的最大的SPV值(平均值)
     */
    public double getMaxSPVY(boolean eye)
    {
        double tempMax=Double.MIN_VALUE;
        if(eye)
        {
            //左眼
            for(double periodSPV:LeyeDynamicPeriodSPVY.values())
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
            for(double periodSPV:ReyeDynamicPeriodSPVY.values())
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
    * 用于判断中枢是否病变 目前只判断X
    * @return ture:正常  false:异常*/
    public boolean judgeDiagnosis()
    {
        for(double tempSPV:LeyeDynamicPeriodSPVX.values())
        {
            if(tempSPV>Tool.SPVMaxValue)
            {
                return false;
            }
        }
        for(double tempSPV:ReyeDynamicPeriodSPVX.values())
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
            for(int single:LeyeSecondFastPhaseNumX.values())
            {
                num+=single;
            }
        }
        else
        {
            //右眼
            for(int single:ReyeSecondFastPhaseNumX.values())
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
            return LeyeSecondFastPhaseNumX.size()>0;
        }
        else
        {
            return ReyeSecondFastPhaseNumX.size()>0;
        }
    }
    /**
     * 返回时间区间字符串
     * @param endTime 最大眼震高潮期结束时间
     * @return 最大眼震高潮期时间区间字符串
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
    /**
     * 组合当前SPV值与最大SPV值的字符串
     * @param Realtime 当前SPV值
     * @param Max 最大SPV值
     * @return 字符串
     */
    public static String MergeRealtimeAndMax(String Realtime, String Max)
    {
        return Realtime+"/"+Max;
    }
}
