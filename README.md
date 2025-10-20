## 🌐 語言 / Languages

- [🇨🇳 中文說明](README.MD)
- [🇺🇸 English Version](README.en.md)
# 你的電視：安卓電視/手机直播APK
支持安卓6.0(API23)級以上版本<br>
綜合my-tv/my-tv-0/my-tv-1/mytv-android/WebViewTVLive等項目的功能。<br>  
IPTV/網頁視頻播放安卓APK軟件，支持腾讯webview x5，<br>
可自定義源(支持webview://格式網頁視頻源)，支持手機畫中畫，IPTV支持手機熄屏播放。<br>
[yourtv](https://github.com/horsemail/yourtv)
<br>
# <span style="color:red; font-weight:bold;">‼️僅供測試，測試完，請24小时内及時刪除‼️</span><br>
# <span style="color:red; font-weight:bold;">‼️嚴禁轉發到中國大陸地區的任何平台‼️已發布的請立即刪除‼️</span><br>
## **請仔細閱讀後面的[使用說明](#使用)。**
## 在線加密解密：（兼容Tvbox的接口源加密解密）
https://yourtvcrypto.horsenma.net<br>
與項目內加密解密邏輯完全一致<br>

電報群組<br>
https://t.me/yourtvapp<br>
<img src="./screenshots/appreciate.jpg" alt="image" width=200 /><br><br>
<br>
手機使用，最好進入設置界麵切換為軟解碼，否則有的直播源會沒聲音。<br>
<img src="./screenshots/527.jpg" alt="image"/><br><br>
<img src="./screenshots/090901.jpg" alt="image"/><br><br>
<img src="./screenshots/090902.jpg" alt="image"/><br><br>
<br>
###  2025年4月21日第一次正式發佈，使用說明：<br>
APP本身有自動切換直播源，你也可以手動切換直播源。<br>
<br>
## 使用

電視機：<br>
1、開機使用，下載直播源資源，請耐心等待3-5秒，<br>
2、確定/中心鍵：彈出組/頻道清單，上下左右選擇組/頻道，確定選擇頻道，右鍵收藏/取消收藏<br>
3、上鍵/下鍵：切換頻道<br>
4、左鍵：顯示節目單信息<br>
5、右鍵：切換同一頻道的不通直播源地址<br>
、长按菜单键/右键，或快速按4次菜单键/右键，显示设置界面<br>
7、长按确认中心键，或快速连按4次，显示当前频道直播源信息，并可选择不同直播源<br>
8、長按分組隱藏，雙擊紅色X恢復。<br>

<br>
觸摸屏：<br>
1、开机使用，下载直播源资源，请等待3-5秒，<br>
2、左侧上屏快速调节宽度<br>
3、右侧上屏：调节声音<br>
4、中间部分滑屏：切换频道<br>
5、单击屏幕：弹出组/频道列表，点击选择，点击爱心收藏/取消收藏<br>
6、双击频幕，显示IPTV直播源的进度条。<br>
7、连续点击屏幕4次：显示设置界面<br>
8、点击虚拟换源健：切换直播源（APP也根据卡顿情况自动切换直播源），设置界面可切换显示虚拟键<br>
长按虚拟换源健：显示同一频道的所有不同直播源并可切换。<br>
9、长按触摸屏幕：显示当前频道节目单<br>
10、按主页（手机虚拟请求键）键进入画中画<br>
11、触摸屏熄屏仍可播放（设置界面有取消开关）<br>
12、長按分組隱藏，雙擊紅色X恢復。<br>


* 打開配置后，選擇遠程配置，掃描二維碼可以配置視頻源等。也可以直接遠程配置地址 http://0.0.0.0:34567
* 打開“每天自動更新直播源”后，應用啟動后會自動更新直播源

注意：

* 遇到問題可以先考慮重啟/恢復默認/清除數據/重新安裝等方式自助解決

下載安裝 [releases](https://github.com/horsemail/yourtv)

## 其他

建議通過ADB進行安裝：

```shell
adb install YourTV.apk
```

小米電視可以使用小米電視助手進行安裝

## 常見問題

* 為什麼遠程配置視頻源文本後，再次打開應用後又恢復到原來的配置？<br>

  如果“應用啟動后更新視頻源”開啟後，且存在視頻源地址，則會自動更新，可能會覆蓋已保存的視頻源文本。<br>

* 自己編譯APP注意事項：<br>
  1、資源文件需要自己逐個確認設置為自己的信息，特別是cloudflare.txt/github_private.txt/sources.txt<br>
  需使用加密解密工具網站 https://yourtvcrypto.horsenma.net  加密後存儲。<br>
  2、我上傳的APK文件與源碼可能不同步，APK文件比較新，源碼更新一般落後幾天，請注意查看，<br>
  3、我上傳的APK文件使用的加密解密邏輯與項目內加密解密邏輯：https://yourtvcrypto.horsenma.net  不同，目的保護我的私有資源信息。<br>
* 旧电视机无法观看webview网页视频频道的，手动强制安装<br>
**[Android System WebView 6.0+ 下载](https://ftp.horsenma.net/tv/Android_System_WebView_Android_6_0.apk)**<br>

## 感謝

[live](https://github.com/fanmingming/live)<br>
[my-tv-0](https://github.com/lizongying/my-tv-0)<br>
[my-tv-1](https://github.com/lizongying/my-tv-1)<br>

## 致歉

本人並不懂代碼，更不懂開發，純粹現在空閒愛好打發時間。所有代碼都AI實現。
github上很多功能也還不會用。
