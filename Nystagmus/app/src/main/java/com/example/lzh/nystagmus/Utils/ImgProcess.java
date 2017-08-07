package com.example.lzh.nystagmus.Utils;

import android.support.annotation.NonNull;

import com.example.lzh.nystagmus.Utils.Tool;
import com.example.lzh.nystagmus.Utils.Box;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import static android.os.Build.VERSION_CODES.N;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

/**
 * 与图像处理相关的函数
 * Created by LZH on 2017/7/17.
 */

public class ImgProcess {
    private double EyeRatio;
    private int EyeNum;
    private Mat inimg=new Mat();//输入双眼视频，目前用不到
    private Mat outimg=new Mat();//输出双眼视频，目前也用不到
    private Mat Reye=new Mat();
    private Mat Leye=new Mat();
    private Size size=new Size(9,9);
    private List<MatOfPoint> Rcontours=new ArrayList<>();
    private List<MatOfPoint> Lcontours=new ArrayList<>();
    private double Lmaxarea=0;//左眼最大轮廓
    private int LmaxAreaIndex=0;//左眼最大轮廓下标
    private double Rmaxarea=0;//右眼最大轮廓
    private int RmaxAreaIndex=0;//右眼最大轮廓下标
    private Rect Rrect=new Rect();
    private Rect Lrect=new Rect();
    private Scalar blue=new Scalar(0,0,255);
    private Scalar green=new Scalar(0,255,0);
    private Scalar red=new Scalar(255,0,0);
    private Mat OriginalLeftEye=new Mat();//用于保存原始图像
    private Mat OriginalRightEye=new Mat();//用于保存原始图像

    //public List<Box> Lcircles=new ArrayList<>();
    public Vector<Box> Lcircles=new Vector<Box>();
    public Vector<Box> Rcircles=new Vector<Box>();

    //构造函数
    public ImgProcess()
    {}
    //开始
    public void Start(Mat leye, Mat reye, double eyeratio, int eyenum)
    {
        Reye=reye;
        Leye=leye;
        EyeRatio=eyeratio;
        EyeNum=eyenum;
        //保存原始图像数据
        OriginalLeftEye=Leye;
        OriginalRightEye=Reye;
    }
    //输出双眼
    public Mat Outputimg()
    {
        return outimg;
    }
    //输出右眼
    public Mat OutReye()
    {
        return Reye;
    }
    //输出左眼
    public Mat OutLeye()
    {
        return Leye;
        //return OriginalLeftEye;
    }
    //图像分割
    private boolean DivideEye(final Mat divedeImg)
    {
        if(divedeImg.rows()>0&&divedeImg.cols()>0)
        {
            Rect leye_box=new Rect();
            leye_box.x=1;
            leye_box.y=1;
            leye_box.height=divedeImg.rows()-1;
            leye_box.width=divedeImg.cols()/2-1;
            Rect reye_box=new Rect();
            reye_box.x=leye_box.x+leye_box.width;
            reye_box.y=1;
            reye_box.height=divedeImg.rows()-1;
            reye_box.width=divedeImg.cols()/2-1;
            Leye=divedeImg.submat(leye_box);
            Reye=divedeImg.submat(reye_box);
            return true;
        }
        else
        {
            return false;
        }
    }
    //灰度化处理
    private Mat GrayDetect(Mat grayimg0)
    {
        Mat grayout=new Mat();
        Mat grayimg =grayimg0.clone();
        Imgproc.cvtColor(grayimg,grayimg,Imgproc.COLOR_RGB2GRAY);
        Imgproc.medianBlur(grayimg,grayimg,9);
        Imgproc.blur(grayimg,grayimg,size);
        grayout=Binary(grayimg,50);
        return grayout;
    }
    //二值化处理
    private Mat Binary(Mat binaryimg, int value)
    {
        Mat binaryout=new Mat();
        Imgproc.threshold(binaryimg,binaryout,value,255,Imgproc.THRESH_BINARY);
        return binaryout;
    }
    //二乘法拟合圆
    private Box circleLeastFit(MatOfPoint points)
    {
        List<Point> points0=points.toList();
        Box box=new Box(0.0d,0.0d,0.0d);
        int Sum=points0.size();
        //如果少于三点，不能拟合圆，直接返回
        if(Sum<3)
        {
            return box;
        }

        int i = 0;
        double X1 = 0;
        double Y1 = 0;
        double X2 = 0;
        double Y2 = 0;
        double X3 = 0;
        double Y3 = 0;
        double X1Y1 = 0;
        double X1Y2 = 0;
        double X2Y1 = 0;

        for (i = 0; i < Sum; ++i)
        {
            X1 += points0.get(i).x;
            Y1 += points0.get(i).y;
            X2 += points0.get(i).x*points0.get(i).x;
            Y2 += points0.get(i).y*points0.get(i).y;
            X3 += points0.get(i).x*points0.get(i).x*points0.get(i).x;
            Y3 += points0.get(i).y*points0.get(i).y*points0.get(i).y;
            X1Y1 += points0.get(i).x*points0.get(i).y;
            X1Y2 += points0.get(i).x*points0.get(i).y*points0.get(i).y;
            X2Y1 += points0.get(i).x*points0.get(i).x*points0.get(i).y;
        }

        double C, D, E, G, H, N;
        double a, b, c;
        N = points0.size();
        C = N*X2 - X1*X1;
        D = N*X1Y1 - X1*Y1;
        E = N*X3 + N*X1Y2 - (X2 + Y2)*X1;
        G = N*Y2 - Y1*Y1;
        H = N*X2Y1 + N*Y3 - (X2 + Y2)*Y1;
        a = (H*D - E*G) / (C*G - D*D);
        b = (H*C - E*D) / (D*D - G*C);
        c = -(a*X1 + b*Y1 + X2 + Y2) / N;

        box.setX(a/(-2));
        box.setY(b/(-2));
        box.setR(sqrt(a*a + b*b - 4 * c) / 2);
        return box;
    }
    //边缘检测
    private Mat EdgeDetect(Mat edgeimg)
    {
        Mat edgeout=new Mat();
        Imgproc.Canny(edgeimg,edgeout,100,250,3,false);
        return edgeout;
    }
    //绘制圆
    private Mat PlotC(Vector<Box> circles,Mat midImage)
    {
        Mat tempMat=new Mat();
        tempMat=midImage;
        for(int i=0;i<circles.size();++i)
        {
            Point center=new Point(circles.get(i).getX(),circles.get(i).getY());
            int radius=(int)circles.get(i).getR();
            Imgproc.circle(tempMat,center,1,blue,-1,8,0);//画圆心
            Imgproc.circle(tempMat,center,radius,red,1,8,0);//画圆轮廓
        }
        return midImage;
    }
    //左右眼分开处理
    public void ProcessSeparate()
    {
        Mat Rgryaimg=new Mat();
        Mat Lgrayimg=new Mat();
        Mat Redgeimg=new Mat();
        Mat Ledgeimg=new Mat();
        Mat Rhiberarchy=new Mat();
        Mat Lhiberarchy=new Mat();
        double temparea;
        boolean IsLeye=false;
        boolean IsReye=false;

        if(EyeNum==Tool.NOT_LEYE||EyeNum==Tool.ALL_EYE||EyeNum==Tool.VEDIO_EYE)
        {
            //此时有右眼
            Rgryaimg=GrayDetect(Reye);
            Redgeimg=EdgeDetect(Rgryaimg);
            Imgproc.findContours(Redgeimg,Rcontours,Rhiberarchy,Imgproc.RETR_CCOMP,Imgproc.CHAIN_APPROX_NONE);
            IsReye=true;
        }
        if(EyeNum==Tool.NOT_REYE||EyeNum==Tool.ALL_EYE||EyeNum==Tool.VEDIO_EYE||EyeNum==Tool.VEDIO_ONLY_EYE)
        {
            //此时有左眼
            Lgrayimg=GrayDetect(Leye);
            Ledgeimg=EdgeDetect(Lgrayimg);
            Imgproc.findContours(Ledgeimg,Lcontours,Lhiberarchy,Imgproc.RETR_CCOMP,Imgproc.CHAIN_APPROX_NONE);
            IsLeye=true;
        }
        if((Lcontours.size()>0)&&IsLeye)
        {
            //左眼有轮廓
            for(int i=0;i<Lcontours.size();++i)
            {
                temparea=Imgproc.contourArea(Lcontours.get(i));
                if(temparea>Lmaxarea)
                {
                    Lmaxarea=temparea;
                    LmaxAreaIndex=i;
                    continue;
                }
            }
            //闭眼检测
            Lrect=Imgproc.boundingRect(Lcontours.get(LmaxAreaIndex));
            if((Lrect.width/(float)Lrect.height)<EyeRatio&&(Lrect.height/(float)Lrect.width)<EyeRatio&&Lrect.width>0&&Lrect.height>0)
            {
                Box Lbox=circleLeastFit(Lcontours.get(LmaxAreaIndex));//左眼拟合圆检测
                if(Lbox.getR()!=0)
                {
                    //如果半径不为0
                    Lcircles.add(Lbox);
                }
            }
        }
        if((Rcontours.size()>0)&&IsReye)
        {
            //右眼有轮廓
            for(int i=0;i<Rcontours.size();++i)
            {
                temparea=Imgproc.contourArea(Rcontours.get(i));
                if(temparea>Rmaxarea)
                {
                    Rmaxarea=temparea;
                    RmaxAreaIndex=i;
                    continue;
                }
            }
            //闭眼检测
            Rrect=Imgproc.boundingRect(Rcontours.get(RmaxAreaIndex));
            if((Rrect.width/(float)Rrect.height)<EyeRatio&&(Rrect.height/(float)Rrect.width)<EyeRatio&&Rrect.width>0&&Rrect.height>0)
            {
                Box Rbox=circleLeastFit(Rcontours.get(RmaxAreaIndex));//左眼拟合圆检测
                if(Rbox.getR()!=0)
                {
                    //如果半径不为0
                    Rcircles.add(Rbox);
                }
            }
        }
        if(Lcircles.size()>0)
        {
            Leye=OriginalLeftEye.clone();
            Leye=PlotC(Lcircles,Leye);//绘制左眼圆心和圆轮廓
        }
        if(Rcircles.size()>0)
        {
            Reye=OriginalRightEye.clone();
            Reye=PlotC(Rcircles,Reye);//绘制右眼圆心和圆轮廓
        }
        if(Lcontours.size()>0)
        {
            Imgproc.drawContours(Leye,Lcontours,LmaxAreaIndex,green,1);
        }
        if(Rcontours.size()>0)
        {
            Imgproc.drawContours(Reye,Rcontours,RmaxAreaIndex,green,1);
        }
    }
}
