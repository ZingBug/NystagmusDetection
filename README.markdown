### NystagmusDetection 
### Eye Pupil Localization OpenCV 3.20 for Android 
### 眼球震动定位检测

### 2017.7.21
- 重绘界面，并使图像充满ImageView控件播放。
- 选取本地视频能够走一边opencv的图像识别处理过程，并能正常显示。
### 2017.7.20
- 经过几天测试，可以实现用opencv的VideoCapture进行本地视频读取。
- VideoCapture要求视频编码必须为MJPEG格式。
- 用定时器的时候，注意不要直接在其他线程里修改UI，可以调用来Handler来间接修改更新UI。

