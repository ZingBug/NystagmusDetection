
### 2017.7.20
- 经过几天测试，可以实现用opencv的VideoCapture进行本地视频读取。
- VideoCapture要求视频编码必须为MJPEG格式。
- 用定时器的时候，注意不要直接在其他线程里修改UI，可以调用来Handler来间接修改更新UI。

