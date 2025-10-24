## 🌐 語言 / Languages

- [🇨🇳 中文說明](README.md)
- [🇺🇸 English Version](README.en.md)
# Your TV：Android Live TV APK
Support Android 6.0 (API23) and above<br>
Combines the functions of my-tv/my-tv-0/my-tv-1/mytv-android/WebViewTVLive and other projects.<br>
IPTV/web video player android APK software, customizable sources(
(Supports webview:// format web video source)), <br>
IPTV supports picture-in-picture and off-screen playback.<br>
[yourtv](https://github.com/horsemail/yourtv)
<br>
# <span style="color:red; font-weight:bold;">‼️For testing only. After testing, please delete in 24 hours.‼️ </span><br>
# <span style="color:red; font-weight:bold;">‼️Forwarding to any platform in mainland China is strictly prohibited.‼ ️Please delete immediately if already published‼️ </span><br>
### 🔴 **Please read the [Instructions](#use) carefully. **
## Online encryption and decryption: (TVBox-compatible interface source encryption and decryption)
https://yourtvcrypto.horsenma.net<br>
The encryption and decryption logic is identical to the project.<br>

Telegram Group<br>
https://t.me/yourtvapp<br>
<img src="./screenshots/appreciate.jpg" alt="image" width=200 /><br><br>
=<br>
For mobile phones, it is best to enter the settings interface to switch to soft decoding, otherwise some live sources will have no sound. <br>
<img src="./screenshots/527.jpg" alt="image"/><br><br>
<img src="./screenshots/090901.jpg" alt="image"/><br><br>
<img src="./screenshots/090902.jpg" alt="image"/><br><br>
### The first official release was on April 21, 2025. Instructions for use:<br>
The APP itself can automatically switch the live source, and you can also switch the live source manually. <br>
<br>
## Use

TV:<br>
1. Turn on the TV and download the live source resources. Please wait patiently for 5-30 seconds.<br>
2. Confirm/center key: pop up the group/channel list, select the group/channel up, down, left, and right, confirm the selected channel, right key to collect/cancel collection<br>
3. Up/down key: switch channels<br>
4. Left key: display program list information<br>
5. Right key: switch different live source addresses of the same channel<br>
6. Long press the menu key, or quickly press the menu key multiple times to display the settings interface<br>
7. Switch IPTV/web video: Settings interface--Switch IPTV/Switch web video switch<br>
8. Other functions, test them yourself. <br>

<br>
Touch screen:<br>
1. Turn on the device and download live source resources. Please wait patiently for 5-30 seconds.<br>
2. Swipe up and down on the left side: adjust brightness<br>
3. Swipe up and down on the right side: adjust sound<br>
4. Swipe in the middle: switch channels<br>
5. Bipolar screen: pop up group/channel list, click to select, click heart to collect/cancel collection<br>
6. Continuously and quickly click the screen: display the settings interface<br>
7. Click the virtual source change key: switch live source (APP will also automatically switch live source according to the freeze situation), the settings interface can turn on and off the display of virtual keys<br>
8. Long press the touch screen: display the current channel program list<br>
9. Press the home page (mobile phone virtual circle key) key to enter picture-in-picture<br>
10. The touch screen can still play when the screen is turned off (there is a cancel switch in the settings interface)<br>
11. Switch IPTV/web video: Settings interface--Switch IPTV/Switch web video switch<br>
12. Other functions, test them yourself. <br>

* After opening the configuration, select remote configuration, scan the QR code to configure the video source, etc. You can also directly configure the address remotely http://0.0.0.0:34567
* After turning on "Automatically update live source every day", the live source will be automatically updated after the application is started
Note:

* If you encounter a problem, you can first consider restarting/restoring to default/clearing data/reinstalling to solve it yourself

Download and install [releases](https://github.com/horsemail/yourtv)

Note that "*-kitkat" is an Android 4.4 compatible version

More download addresses

## Others

It is recommended to install via ADB:

```shell
adb install YourTV.apk
```

Xiaomi TV can be installed using Xiaomi TV Assistant

## Frequently Asked Questions

* Why does the video source text return to the original configuration after opening the app again after configuring it remotely? <br>

If "Update video source after app launch" is turned on and the video source address exists, it will be automatically updated, which may overwrite the saved video source text. <br>

* Notes for compiling APP by yourself: <br>
1. Resource files need to be confirmed one by one and set to their own information, especially cloudflare.txt/github_private.txt/sources.txt<br>
Need to use the encryption and decryption tool website https://yourtvcrypto.horsenma.net to encrypt and store. <br>
2. The APK file I uploaded may not be synchronized with the source code. The APK file is relatively new, and the source code update is generally a few days behind. Please check it carefully. <br>
3. The encryption and decryption logic used in the APK file I uploaded is different from the encryption and decryption logic in the project: https://yourtvcrypto.horsenma.net, in order to protect my private resource information. <br>

## Thanks

[live](https://github.com/fanmingming/live)<br>
[my-tv-0](https://github.com/lizongying/my-tv-0)<br>
[my-tv-1](https://github.com/lizongying/my-tv-1)<br>
