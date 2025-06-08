package com.horsenma.yourtv


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonSyntaxException
import com.horsenma.yourtv.ImageHelper
import com.horsenma.yourtv.R
import com.horsenma.yourtv.SP
import com.horsenma.yourtv.Utils.getDateFormat
import com.horsenma.yourtv.Utils.getUrls
import com.horsenma.yourtv.bodyAlias
import com.horsenma.yourtv.codeAlias
import com.horsenma.yourtv.data.EPG
import com.horsenma.yourtv.data.Global.gson
import com.horsenma.yourtv.data.Global.typeEPGMap
import com.horsenma.yourtv.data.Global.typeTvList
import com.horsenma.yourtv.data.Source
import com.horsenma.yourtv.data.SourceType
import com.horsenma.yourtv.data.TV
import com.horsenma.yourtv.models.EPGXmlParser
import com.horsenma.yourtv.models.Sources
import com.horsenma.yourtv.models.TVGroupModel
import com.horsenma.yourtv.models.TVListModel
import com.horsenma.yourtv.models.TVModel
import com.horsenma.yourtv.requests.HttpClient
import com.horsenma.yourtv.showToast
import com.horsenma.yourtv.SourceDecoder
import com.horsenma.yourtv.SourceEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.horsenma.yourtv.YourTVApplication
import java.io.File
import java.io.InputStream
import com.horsenma.yourtv.data.Global.typeSourceList
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import com.google.gson.JsonObject
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.horsenma.yourtv.data.StableSource
import android.widget.Toast
import kotlinx.coroutines.delay



class MainViewModel : ViewModel() {

    private var firstloadcode = false
    private var cachedCodeContent: String? = null
    private var cachedFileContent: String? = null
    private lateinit var context: Context // 存储 Context

    private val _playTrigger = MutableLiveData<TVModel?>()
    val playTrigger: LiveData<TVModel?> get() = _playTrigger

    // 添加公共方法来触发播放
    fun triggerPlay(tvModel: TVModel?) {
        _playTrigger.postValue(tvModel)
    }

    private val _currentTvModel = MutableLiveData<TVModel?>()
    val currentTvModel: LiveData<TVModel?> get() = _currentTvModel

    fun setCurrentTvModel(model: TVModel?) {
        _currentTvModel.value = model
    }

    fun storeUsersInfo(usersInfo: List<String>) {
        Log.d(TAG, "Storing users_info: $usersInfo")
    }
    private var timeFormat = if (SP.displaySeconds) "HH:mm:ss" else "HH:mm"

    private lateinit var appDirectory: File
    var listModel: List<TVModel> = emptyList()
    val groupModel = TVGroupModel()
    private var cacheFile: File? = null
    private var cacheChannels = ""
    private var initialized = false

    private lateinit var cacheEPG: File
    private var epgUrl = SP.epg

    private lateinit var imageHelper: ImageHelper

    val sources = Sources()

    private val _channelsOk = MutableLiveData<Boolean>()
    val channelsOk: LiveData<Boolean>
        get() = _channelsOk

    fun setDisplaySeconds(displaySeconds: Boolean) {
        timeFormat = if (displaySeconds) "HH:mm:ss" else "HH:mm"
        SP.displaySeconds = displaySeconds
    }

    fun setChannelsOk(value: Boolean) {
        _channelsOk.postValue(value)
    }

    fun getTime(): String {
        return getDateFormat(timeFormat)
    }

    fun updateEPG() {
        viewModelScope.launch {
            var success = false
            if (!epgUrl.isNullOrEmpty()) {
                success = updateEPG(epgUrl!!)
            }
            if (!success && !SP.epg.isNullOrEmpty()) {
                updateEPG(SP.epg!!)
            }
        }
    }

    fun updateConfig() {
        if (SP.configAutoLoad) {
            SP.configUrl?.let {
                if (it.startsWith("http")) {
                    viewModelScope.launch {
                        importFromUrl(it,"")
                        updateEPG()
                    }
                }
            }
        }
    }

    private fun getCache(): String {
        return if (cacheFile!!.exists()) {
            cacheFile!!.readText()
        } else {
            ""
        }
    }

    fun init(context: Context) {
        this.context = context
        val application = context.applicationContext as YourTVApplication
        imageHelper = application.imageHelper

        if (groupModel.getAllList() == null || groupModel.getAllList()!!.tvList.value.isNullOrEmpty()) {
            groupModel.addTVListModel(TVListModel("我的收藏", 0))
            groupModel.addTVListModel(TVListModel("全部頻道", 1))
        }

        appDirectory = context.filesDir
        cacheFile = File(appDirectory, CACHE_FILE_NAME)
        try {
            if (!cacheFile!!.exists()) {
                cacheFile!!.createNewFile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create cache file: ${e.message}", e)
        }

        // Step 1: Immediately play the latest stable source
        viewModelScope.launch(Dispatchers.Main) {
            // 播放稳定源
            val stableSources = SP.getStableSources()
            var defaultChannel: TVModel? = null
            if (stableSources.isNotEmpty()) {
                val selectedSource = stableSources.maxByOrNull { it.timestamp }
                if (selectedSource != null) {
                    val tv = TV(
                        id = selectedSource.id,
                        name = selectedSource.name,
                        title = selectedSource.title,
                        description = selectedSource.description,
                        logo = selectedSource.logo,
                        image = selectedSource.image,
                        uris = selectedSource.uris,
                        videoIndex = selectedSource.videoIndex,
                        headers = selectedSource.headers,
                        group = selectedSource.group,
                        sourceType = SourceType.valueOf(selectedSource.sourceType),
                        number = selectedSource.number,
                        child = selectedSource.child
                    )
                    defaultChannel = TVModel(tv).apply {
                        setLike(SP.getLike(tv.id))
                        setGroupIndex(2)
                        listIndex = 0
                    }
                    groupModel.setCurrent(defaultChannel)
                    triggerPlay(defaultChannel)
                    Log.i(TAG, "Playing latest stable channel immediately: ${defaultChannel.tv.title}, url: ${defaultChannel.getVideoUrl()}")
                } else {
                    Log.w(TAG, "Selected stable source is null")
                    // R.string.channel_read_error.showToast()
                }
            } else {
                Log.w(TAG, "No stable sources available")
                // R.string.channel_read_error.showToast()
            }

            // 同步设置 groupModel.current
            if (defaultChannel != null) {
                groupModel.setCurrent(defaultChannel)
                Log.d(TAG, "Stable source set: ${defaultChannel.tv.title}, current: ${groupModel.getCurrent()?.tv?.title}")
            } // 修改：添加二次设置和日志，确认 groupModel.current 在 str2Channels 前生效

            // Step 2: Delay 3 seconds to load channels from cache or default, update list without interrupting playback
            if (stableSources.isNotEmpty()) {
                delay(5_000L)
            } else {
                delay(100L)
            }
            var channelsLoaded = false
            val cacheCodeFile = File(appDirectory, CACHE_CODE_FILE)
            if (cacheCodeFile.exists()) {
                try {
                    cachedCodeContent = cachedCodeContent ?: withContext(Dispatchers.IO) { cacheCodeFile.readText() }
                    tryStr2Channels(cachedCodeContent!!, cacheCodeFile, "", "")
                    Log.d(TAG, "Channels loaded from cacheCodeFile")
                    channelsLoaded = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load cacheCodeFile: ${e.message}", e)
                }
            }
            if (!channelsLoaded && cacheFile!!.exists()) {
                try {
                    cachedFileContent = cachedFileContent ?: withContext(Dispatchers.IO) { cacheFile!!.readText() }
                    if (cachedFileContent!!.isNotEmpty()) {
                        tryStr2Channels(cachedFileContent!!, cacheFile, "", "")
                        Log.d(TAG, "Channels loaded from cacheFile")
                        channelsLoaded = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load cacheFile: ${e.message}", e)
                }
            }
            if (!channelsLoaded) {
                try {
                    cacheChannels = withContext(Dispatchers.IO) {
                        context.resources.openRawResource(DEFAULT_CHANNELS_FILE).bufferedReader().use { it.readText() }
                    }
                    if (cacheChannels.isNotEmpty()) {
                        tryStr2Channels(cacheChannels, null, "", "")
                        Log.d(TAG, "Channels loaded from /raw/codechannels.txt")
                        channelsLoaded = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load /raw/codechannels.txt: ${e.message}", e)
                }
            }

            if (channelsLoaded && stableSources.isEmpty() && defaultChannel == null) {
                val firstChannel = listModel.firstOrNull()
                if (firstChannel != null) {
                    groupModel.setCurrent(firstChannel)
                    triggerPlay(firstChannel)
                    Log.i(TAG, "Playing default channel from cache/raw: ${firstChannel.tv.title}, url=${firstChannel.getVideoUrl()}")
                } else {
                    Log.w(TAG, "No channels available in listModel after loading")
                }
            }

            initialized = true
            _channelsOk.value = channelsLoaded

            val delayMillis = 3_000L
            var hasScheduledDownload = false
            channelsOk.observeForever { isInitialized ->
                if (isInitialized && !hasScheduledDownload) {
                    Log.d(TAG, "channelsOk: Initialized, scheduling delayed download")
                    if (SP.autoUpdateSources || SP.getStableSources().isEmpty() || cacheFile?.exists() != true) {
                        hasScheduledDownload = true
                        viewModelScope.launch {
                            Log.d(TAG, "Scheduling delayed download after ${delayMillis}ms")
                            delay(delayMillis)
                            Log.d(TAG, "Starting delayed download")
                            startDelayedDownload(context)
                        }
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            cacheEPG = File(appDirectory, CACHE_EPG)
            try {
                if (!cacheEPG.exists()) {
                    cacheEPG.createNewFile()
                } else if (readEPG(cacheEPG.readText())) {
                    Log.i(TAG, "cacheEPG success")
                } else {
                    Log.i(TAG, "cacheEPG failure")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle EPG cache: ${e.message}", e)
            }
        }
    }

    private fun startDelayedDownload(context: Context) {
        viewModelScope.launch(Dispatchers.Main) {
            val toast = Toast.makeText(context, "正在后台下載直播源...", Toast.LENGTH_LONG)
            toast.show()

            withContext(Dispatchers.IO) {
                try {
                    // Load user info and remote users
                    val userInfo = UserInfoManager.loadUserInfo()
                    val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                    if (userInfo != null && userInfo.userId.isNotEmpty() && userInfo.updateDate == today && userInfo.userUpdateStatus == true) {
                        Log.d(TAG, "Today's download already completed, skipping")
                        return@withContext
                    }
                    val (warningMessages, remoteUsers) = UserInfoManager.downloadRemoteUserInfo()

                    if (userInfo != null && userInfo.userId.isNotEmpty()) {
                        // Validate test code
                        val validatedUser = UserInfoManager.validateKey(userInfo.userId, remoteUsers)
                        if (validatedUser != null) {
                            // Check binding
                            val deviceId = UserInfoManager.getDeviceId()
                            val (isValid, errorMessage) = UserInfoManager.checkBinding(userInfo.userId, deviceId)
                            if (!isValid) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, errorMessage ?: "测试码绑定失败", Toast.LENGTH_SHORT).show()
                                }
                                return@withContext
                            }
                            // Update binding
                            val (bindingSuccess, bindingError) = UserInfoManager.updateBinding(userInfo.userId, deviceId)
                            if (!bindingSuccess) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, bindingError ?: "测试码绑定失败", Toast.LENGTH_SHORT).show()
                                }
                                return@withContext
                            }
                            // Check cacheCodeFile
                            val cacheCodeFile = File(appDirectory, CACHE_CODE_FILE)
                            if (cacheCodeFile.exists()) {
                                val encryptedContent = cacheCodeFile.readText()
                                if (encryptedContent != null) {
                                    withContext(Dispatchers.Main) {
                                        tryStr2Channels(encryptedContent, cacheCodeFile, "", "")
                                        Log.d(TAG, "Channels loaded from cacheCodeFile")
                                        Toast.makeText(context, "直播源下載完成", Toast.LENGTH_SHORT).show()
                                        _channelsOk.value = true // Notify UI to update
                                    }
                                    return@withContext
                                }
                            }

                            // Download from vipUserUrl
                            if (validatedUser.vipUserUrl.isNotEmpty()) {
                                importFromUrl(validatedUser.vipUserUrl, skipHistory = true)
                                if (listModel.isNotEmpty()) {
                                    val encryptedContent = SourceEncoder.encodeJsonSource(cacheChannels)
                                    cacheCodeFile.writeText(encryptedContent)
                                    val updatedUserInfo = UserInfo(
                                        userId = userInfo.userId,
                                        userLimitDate = validatedUser.userLimitDate,
                                        userType = validatedUser.userType,
                                        vipUserUrl = validatedUser.vipUserUrl,
                                        maxDevices = validatedUser.maxDevices,
                                        devices = validatedUser.devices,
                                        userUpdateStatus = true,
                                        updateDate = today
                                    )
                                    UserInfoManager.saveUserInfo(updatedUserInfo)
                                    withContext(Dispatchers.Main) {
                                        Log.d(TAG, "Channels loaded from vipUserUrl: ${validatedUser.vipUserUrl}")
                                        Toast.makeText(context, "直播源下載完成", Toast.LENGTH_SHORT).show()
                                        _channelsOk.value = true // Notify UI to update
                                    }
                                    return@withContext
                                }
                            }
                        }
                    }

                    // Try SP.sources
                    val sourcesJson = SP.sources
                    if (!sourcesJson.isNullOrEmpty()) {
                        val sourceUrls = try {
                            val sourceList = gson.fromJson(sourcesJson, typeSourceList) as? List<Source>
                            if (sourceList != null) {
                                sourceList.mapNotNull { it.uri.takeIf { it.isNotBlank() } }
                            } else {
                                sourcesJson.trim().split("\n", ",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse SP.sources: ${e.message}", e)
                            sourcesJson.trim().split("\n", ",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }
                        }
                        for (url in sourceUrls) {
                            if (url.isNotBlank()) {
                                try {
                                    importFromUrl(url, "")
                                    if (listModel.isNotEmpty()) {
                                        cacheChannels = listModel.joinToString("\n") { it.tv.toString() }
                                        val updatedUserInfo = UserInfo(
                                            userId = userInfo?.userId ?: "testuser",
                                            userLimitDate = userInfo?.userLimitDate ?: "19700101",
                                            userType = userInfo?.userType ?: "",
                                            vipUserUrl = userInfo?.vipUserUrl ?: "",
                                            maxDevices = userInfo?.maxDevices ?: 5,
                                            devices = userInfo?.devices ?: emptyList(),
                                            userUpdateStatus = true,
                                            updateDate = today
                                        )
                                        UserInfoManager.saveUserInfo(updatedUserInfo)
                                        withContext(Dispatchers.Main) {
                                            Log.d(TAG, "Channels loaded from SP.sources: $url")
                                            Toast.makeText(context, "直播源下載完成", Toast.LENGTH_SHORT).show()
                                            _channelsOk.value = true // Notify UI to update
                                        }
                                        return@withContext
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed to import from URL $url: ${e.message}", e)
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Log.w(TAG, "All remote sources failed, retaining init's listModel")
                        Toast.makeText(context, "直播源下載失敗，保留本地源", Toast.LENGTH_SHORT).show()
                        _channelsOk.value = true // Ensure UI updates even on failure
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "Failed to load remote sources: ${e.message}", e)
                        Toast.makeText(context, "直播源下載失敗", Toast.LENGTH_SHORT).show()
                        _channelsOk.value = true // Ensure UI updates even on failure
                    }
                } finally {
                    toast.cancel()
                }
            }
        }
    }

    suspend fun preloadLogo() {
        if (!this::imageHelper.isInitialized) {
            return
        }

        withContext(Dispatchers.IO) { // 添加后台线程调度
            for (tvModel in listModel) {
                var name = tvModel.tv.name
                if (name.isEmpty()) {
                    name = tvModel.tv.title
                }
                val url = tvModel.tv.logo
                var urls =
                    listOf(
                        "https://live.fanmingming.cn/tv/$name.png"
                    ) + getUrls("https://raw.githubusercontent.com/fanmingming/live/main/tv/$name.png")
                if (url.isNotEmpty()) {
                    urls = (getUrls(url) + urls).distinct()
                }

                imageHelper.preloadImage(name, urls)
            }
        }
    }

    suspend fun readEPG(input: InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = EPGXmlParser().parse(input)

            withContext(Dispatchers.Main) {
                val e1 = mutableMapOf<String, List<EPG>>()
                for (m in listModel) {
                    val name = m.tv.name.ifEmpty { m.tv.title }.lowercase()
                    if (name.isEmpty()) {
                        continue
                    }

                    for ((n, epg) in res) {
                        if (name.contains(n, ignoreCase = true)) {
                            m.setEpg(epg)
                            e1[name] = epg
                            break
                        }
                    }
                }
                cacheEPG.writeText(gson.toJson(e1))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun readEPG(str: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val res: Map<String, List<EPG>> = gson.fromJson(str, typeEPGMap)

            withContext(Dispatchers.Main) {
                for (m in listModel) {
                    val name = m.tv.name.ifEmpty { m.tv.title }.lowercase()
                    if (name.isEmpty()) {
                        continue
                    }

                    val epg = res[name]
                    if (epg != null) {
                        m.setEpg(epg)
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun updateEPG(url: String): Boolean {
        val urls = url.split(",").flatMap { u -> getUrls(u) }

        var success = false
        for (a in urls) {
            withContext(Dispatchers.IO) {
                try {
                    val request = okhttp3.Request.Builder().url(a).build()
                    val response = HttpClient.okHttpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        response.bodyAlias()?.byteStream()?.let { stream ->
                            if (readEPG(stream)) {
                                success = true
                            }
                        } ?: run {
                            Log.e(TAG, "EPG $a response body is null")
                        }
                    } else {
                        Log.e(TAG, "EPG $a ${response.codeAlias()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "EPG $a error")
                }
            }

            if (success) {
                break
            }
        }

        return success
    }

    suspend fun importFromUrl(url: String, id: String = "", skipHistory: Boolean = false) {
        Log.d(TAG, "importFromUrl: url=$url, id=$id, skipHistory=$skipHistory")
        if (url.isBlank()) {
            Log.w(TAG, "importFromUrl: Skipping empty URL")
            R.string.sources_download_error.showToast()
            return
        }

        //val filename = if (id.isNotBlank()) id else url.substringAfterLast("/").takeIf { it.isNotBlank() } ?: "source_${url.hashCode()}.txt"
        val rawFilename = url.substringAfterLast("/").takeIf { it.isNotBlank() }?.substringBeforeLast(".") ?: "source_${url.hashCode()}"
        val filename = "$rawFilename.txt"
        val prefs = context.getSharedPreferences("SourceCache", Context.MODE_PRIVATE)
        val cacheKey = "cache_$filename"
        val cacheTimeKey = "cache_time_$filename"
        val urlKey = "url_$filename"
        val cachedContent = prefs.getString(cacheKey, null)
        val cacheTime = prefs.getLong(cacheTimeKey, 0)
        val cacheDuration = 24 * 60 * 60 * 1000
        val cacheCodeFile = File(appDirectory, "cache_$filename")
        val cacheUseFile = File(appDirectory, CACHE_CODE_FILE)

        // 检查缓存
        if (cachedContent != null && System.currentTimeMillis() - cacheTime < cacheDuration && cacheCodeFile.exists()) {
            Log.d(TAG, "importFromUrl: Using cached content for filename=$filename, cacheTime=$cacheTime")
            withContext(Dispatchers.IO) {
                cacheCodeFile.writeText(cachedContent)
            }
            withContext(Dispatchers.Main) {
                val isHex = cachedContent.trim().matches(Regex("^[0-9a-fA-F]+$"))
                val contentToParse = if (isHex) SourceDecoder.decodeHexSource(cachedContent) ?: cachedContent else cachedContent
                tryStr2Channels(contentToParse, cacheCodeFile, if (skipHistory) "" else url, id)
                _channelsOk.value = true
                prefs.edit().putString("active_source", filename).apply()
            }
            return
        }

        // 下载
        Log.d(TAG, "importFromUrl: Download filename=$filename")
        Log.d(TAG, "importFromUrl: Download url=$url")
        val result = withContext(Dispatchers.IO) {
            DownGithubPrivate.download(context, url, id)
        }
        when {
            result.isSuccess -> {
                val content = result.getOrNull() ?: ""
                if (content.isEmpty()) {
                    Log.w(TAG, "importFromUrl: Downloaded empty content for url=$url")
                    R.string.sources_download_error.showToast()
                    return
                }
                val isHex = content.trim().matches(Regex("^[0-9a-fA-F]+$"))
                val normalizedContent = if (isHex) {
                    SourceDecoder.decodeHexSource(content) ?: content
                } else {
                    content.replace("\r\n", "\n").replace("\r", "\n")
                }
                val contentToCache = if (isHex) content else SourceEncoder.encodeJsonSource(normalizedContent)
                withContext(Dispatchers.IO) {
                    try {
                        cacheCodeFile.writeText(contentToCache)
                        cacheUseFile.writeText(contentToCache)
                        Log.d(TAG, "importFromUrl: Wrote cache_$filename for filename=$filename, content length=${contentToCache.length}")
                    } catch (e: Exception) {
                        Log.e(TAG, "importFromUrl: Failed to write cache_$filename: ${e.message}")
                    }
                }
                withContext(Dispatchers.Main) {
                    tryStr2Channels(normalizedContent, cacheCodeFile, if (skipHistory) "" else url, id)
                    SP.lastDownloadTime = System.currentTimeMillis()
                    with(prefs.edit()) {
                        putString(cacheKey, contentToCache)
                        putLong(cacheTimeKey, System.currentTimeMillis())
                        putString(urlKey, url)
                        putString("active_source", filename)
                        apply()
                    }
                    Log.d(TAG, "importFromUrl: Cached content for filename=$filename, isHex=$isHex")
                    _channelsOk.value = true
                }
            }
            result.isFailure -> {
                Log.e(TAG, "importFromUrl: Download failed for url=$url: ${result.exceptionOrNull()?.message}")
                R.string.sources_download_error.showToast()
            }
        }
    }

    fun reset(context: Context) {
        val filename = "default_channels.txt"
        val defaultUrl = "default://channels"
        val prefs = context.getSharedPreferences("SourceCache", Context.MODE_PRIVATE)

        val str = try {
            context.resources.openRawResource(R.raw.channels).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "reset: Failed to read R.raw.channels: ${e.message}")
            R.string.channel_read_error.showToast()
            return
        }

        try {
            with(prefs.edit()) {
                putString("active_source", filename)
                putString("url_$filename", defaultUrl)
                apply()
            }
            str2Channels(str)
            Log.d(TAG, "reset: Processed default channels from R.raw.channels")
            _channelsOk.value = true
        } catch (e: Exception) {
            Log.e(TAG, "reset: Failed to process default channels: ${e.message}")
            R.string.channel_read_error.showToast()
        }
    }

    fun importFromUri(uri: Uri, id: String = "") {
        if (uri.scheme == "file") {
            val file = uri.toFile()
            Log.i(TAG, "file $file")
            val str = if (file.exists()) {
                file.readText()
            } else {
                R.string.file_not_exist.showToast()
                return
            }
            tryStr2Channels(str, file, uri.toString(), id)
        } else {
            viewModelScope.launch {
                importFromUrl(uri.toString(), id = id)
                Log.d(TAG, "SP.sources after importFromUri: ${SP.sources}")
                // 通知 init 重新加载
                if (listModel.isNotEmpty()) {
                    _channelsOk.value = true
                }
            }
        }
    }

    fun tryStr2Channels(str: String, file: File?, url: String, id: String = "") {
        try {
            if (str.isEmpty()) {
                Log.w(TAG, "Input string is empty for url=$url")
                R.string.channel_import_error.showToast()
                return
            }
            val isPlainText = str.trim().startsWith("#EXTM3U") ||
                    str.trim().startsWith("http://") ||
                    str.trim().startsWith("https://")
            val isHex = str.trim().matches(Regex("^[0-9a-fA-F]+$"))
            val targetFile = file ?: cacheFile
            Log.d(TAG, "tryStr2Channels: Input str length=${str.length}, isPlainText=$isPlainText, isHex=$isHex, url=$url")
            if (str2Channels(str)) {
                if (isPlainText) {
                    val encryptedStr = SourceEncoder.encodeJsonSource(str)
                    if (targetFile != null) {
                        targetFile.writeText(encryptedStr)
                    }
                    cacheChannels = str
                } else if (isHex) {
                    if (targetFile != null) {
                        targetFile.writeText(str)
                    }
                    val decryptedStr = SourceDecoder.decodeHexSource(str) ?: str
                    cacheChannels = decryptedStr
                } else {
                    try {
                        val decodedStr = SourceDecoder.decodeHexSource(str) ?: str
                        val encryptedStr = SourceEncoder.encodeJsonSource(decodedStr)
                        if (targetFile != null) {
                            targetFile.writeText(encryptedStr)
                        }
                        cacheChannels = decodedStr
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process non-plaintext, non-hex content: ${e.message}")
                        cacheChannels = str
                    }
                }
                if (url.isNotEmpty()) {
                    SP.configUrl = url
                    val source = Source(id = id, uri = url)
                    sources.addSource(source)
                    Log.d(TAG, "tryStr2Channels: Added source: $source, SP.sources: ${SP.sources}")
                }
                viewModelScope.launch {
                    withContext(Dispatchers.Main) {
                        _channelsOk.value = true
                    }
                }
                R.string.channel_import_success.showToast()
            } else {
                Log.w(TAG, "str2Channels failed for url=$url")
                R.string.channel_import_error.showToast()
            }
        } catch (e: Exception) {
            Log.e(TAG, "tryStr2Channels: Failed for url=$url: ${e.message}", e)
            R.string.channel_read_error.showToast()
        }
    }

    private fun str2Channels(str: String): Boolean {
        if (initialized && str == cacheChannels) {
            Log.w(TAG, "same channels, skipping parsing")
            return true
        }

        if (str.isEmpty()) {
            Log.w(TAG, "Input string is empty")
            return false
        }

        var string = str
        val isPlainText = str.trim().startsWith("#EXTM3U") ||
                str.trim().startsWith("http://") ||
                str.trim().startsWith("https://")
        val isHex = str.trim().matches(Regex("^[0-9a-fA-F]+$"))

        Log.d(TAG, "str2Channels: isPlainText=$isPlainText, isHex=$isHex, str length=${str.length}, sample=${str.take(50)}")

        try {
            if (isHex) {
                val decoded = SourceDecoder.decodeHexSource(str)
                if (decoded != null) {
                    string = decoded
                    Log.d(TAG, "str2Channels: Decoded HEX, new string length=${string.length}, sample=${string.take(50)}")
                } else {
                    Log.w(TAG, "str2Channels: Failed to decode HEX, using original string")
                }
            } else if (isPlainText) {
                string = str
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode string: ${e.message}")
        }

        if (string.isEmpty()) {
            Log.w(TAG, "channels is empty after processing")
            return false
        }

        val currentTvTitle = groupModel.getCurrent()?.tv?.title
        Log.d(TAG, "str2Channels: Saving currentTvTitle=$currentTvTitle")

        val isStableSourcePlaying = SP.getStableSources().isNotEmpty() && groupModel.current.value != null

        val list: List<TV> = when (string[0]) {
            '[' -> {
                try {
                    gson.fromJson(string, typeTvList) ?: emptyList()
                } catch (e: Exception) {
                    Log.e(TAG, "str2Channels JSON parsing failed: ${e.message}")
                    return false
                }
            }
            '#' -> {
                // 尝试多种分行方式
                val lines = string.split("\n", "\r\n", "\r").filter { it.isNotBlank() }
                Log.d(TAG, "str2Channels: Parsing M3U, line count=${lines.size}, first lines=${lines.take(5).joinToString("|")}")
                val tvMap = mutableMapOf<String, MutableList<TV>>()
                var currentTV: TV? = null

                for (line in lines) {
                    val trimmedLine = line.trim()
                    if (trimmedLine.isEmpty()) continue

                    // Log.d(TAG, "str2Channels: Processing line: ${trimmedLine.take(500)}")

                    if (trimmedLine.startsWith("#EXTM3U")) {
                        val epgIndex = trimmedLine.indexOf("x-tvg-url=\"")
                        if (epgIndex != -1) {
                            val endIndex = trimmedLine.indexOf("\"", epgIndex + 11)
                            if (endIndex != -1) epgUrl = trimmedLine.substring(epgIndex + 11, endIndex)
                        }
                    } else if (trimmedLine.startsWith("#EXTINF")) {
                        if (currentTV != null && currentTV.uris.isNotEmpty()) {
                            val key = (currentTV.group + currentTV.name).ifEmpty { currentTV.title }
                            tvMap.computeIfAbsent(key) { mutableListOf() }.add(currentTV)
                        }
                        currentTV = TV()
                        val info = trimmedLine.split(",", limit = 2)
                        if (info.size < 2) {
                            Log.w(TAG, "str2Channels: Invalid #EXTINF line: $trimmedLine")
                            currentTV = null
                            continue
                        }
                        currentTV = currentTV.copy(title = info.last().trim())

                        val extinf = info.first()
                        val nameStart = extinf.indexOf("tvg-name=\"") + 10
                        val nameEnd = extinf.indexOf("\"", nameStart)
                        currentTV = currentTV.copy(
                            name = if (nameStart > 9 && nameEnd > nameStart) {
                                extinf.substring(nameStart, nameEnd)
                            } else {
                                currentTV.title
                            }
                        )

                        val logoStart = extinf.indexOf("tvg-logo=\"") + 10
                        val logoEnd = extinf.indexOf("\"", logoStart)
                        currentTV = currentTV.copy(
                            logo = if (logoStart > 9 && logoEnd > logoStart) {
                                extinf.substring(logoStart, logoEnd)
                            } else {
                                ""
                            }
                        )

                        val numStart = extinf.indexOf("tvg-chno=\"") + 10
                        val numEnd = extinf.indexOf("\"", numStart)
                        currentTV = currentTV.copy(
                            number = if (numStart > 9 && numEnd > numStart) {
                                extinf.substring(numStart, numEnd).toIntOrNull() ?: -1
                            } else {
                                -1
                            }
                        )

                        val groupStart = extinf.indexOf("group-title=\"") + 13
                        val groupEnd = extinf.indexOf("\"", groupStart)
                        currentTV = currentTV.copy(
                            group = if (groupStart > 12 && groupEnd > groupStart) {
                                extinf.substring(groupStart, groupEnd)
                            } else {
                                ""
                            }
                        )
                    } else if (trimmedLine.startsWith("#EXTVLCOPT:http-")) {
                        if (currentTV != null) {
                            val keyValue = trimmedLine.substringAfter("#EXTVLCOPT:http-").split("=", limit = 2)
                            if (keyValue.size == 2) {
                                currentTV = currentTV.copy(
                                    headers = (currentTV.headers ?: emptyMap()).toMutableMap().apply {
                                        this[keyValue[0]] = keyValue[1]
                                    }
                                )
                            }
                        }
                    } else if (!trimmedLine.startsWith("#") && currentTV != null) {
                        currentTV = currentTV.copy(
                            uris = currentTV.uris.toMutableList().apply { add(trimmedLine) }
                        )
                    }
                }

                if (currentTV != null && currentTV.uris.isNotEmpty()) {
                    val key = (currentTV.group + currentTV.name).ifEmpty { currentTV.title }
                    tvMap.computeIfAbsent(key) { mutableListOf() }.add(currentTV)
                }

                val tvList = tvMap.values.map { tvs ->
                    val uris = tvs.flatMap { it.uris }.distinct()
                    TV(
                        id = -1,
                        name = tvs[0].name,
                        title = tvs[0].title,
                        description = null,
                        logo = tvs[0].logo,
                        image = null,
                        uris = uris,
                        videoIndex = 0,
                        headers = tvs[0].headers,
                        group = tvs[0].group,
                        sourceType = SourceType.UNKNOWN,
                        number = tvs[0].number,
                        child = emptyList()
                    )
                }.filter { it.uris.isNotEmpty() }
                Log.d(TAG, "str2Channels: Generated TV list size=${tvList.size}")
                tvList
            }
            else -> {
                val lines = string.split("\n", "\r\n", "\r").filter { it.isNotBlank() }
                Log.d(TAG, "str2Channels: Parsing other format, line count=${lines.size}")
                val tvMap = mutableMapOf<String, TV>()
                var group = ""

                for (line in lines) {
                    val trimmedLine = line.trim()
                    if (trimmedLine.isEmpty()) continue
                    if (trimmedLine.contains("#genre#")) {
                        group = trimmedLine.split(',', limit = 2)[0].trim()
                    } else if (trimmedLine.contains(",")) {
                        val arr = trimmedLine.split(',', limit = 2)
                        val title = arr[0].trim()
                        val uri = arr[1].trim()
                        val key = group + title
                        tvMap.compute(key) { _, existing ->
                            existing?.copy(uris = existing.uris.toMutableList().apply { add(uri) })
                                ?: TV(
                                    id = -1,
                                    name = "",
                                    title = title,
                                    description = null,
                                    logo = "",
                                    image = null,
                                    uris = listOf(uri),
                                    videoIndex = 0,
                                    headers = emptyMap(),
                                    group = group,
                                    sourceType = SourceType.UNKNOWN,
                                    number = -1,
                                    child = emptyList()
                                )
                        }
                    }
                }
                val tvList = tvMap.values.toList()
                Log.d(TAG, "str2Channels: Generated TV list size=${tvList.size}")
                tvList
            }
        }

        if (list.isEmpty()) {
            Log.w(TAG, "str2Channels: Parsed TV list is empty")
            return false
        }

        //if (!isStableSourcePlaying) {
            groupModel.setTVListModelList(listOf(
                TVListModel("我的收藏", 0),
                TVListModel("全部頻道", 1)
            ))
        //}

        groupModel.tvGroupValue.forEach { it.initTVList() }

        val listModelNew = mutableListOf<TVModel>()
        var id = 0
        list.forEach { tv ->
            val group = tv.group.ifEmpty { "未知" }
            val tvModel = TVModel(tv).apply {
                tv.id = id
                setLike(SP.getLike(id))
                setGroupIndex(2)
                listIndex = listModelNew.size
            }
            listModelNew.add(tvModel)
            val existingGroup = groupModel.tvGroupValue.find { model -> model.getName() == group }
            if (existingGroup != null) {
                existingGroup.addTVModel(tvModel)
            } else {
                val newGroup = TVListModel(group, groupModel.tvGroupValue.size)
                newGroup.addTVModel(tvModel)
                groupModel.addTVListModel(newGroup)
            }
            id++
        }

        listModel = listModelNew
        groupModel.tvGroupValue[1].setTVListModel(listModel)

        if (currentTvTitle != null) {
            val matchingTvModel = listModelNew.firstOrNull { it.tv.title == currentTvTitle }
            if (matchingTvModel != null) {
                groupModel.setCurrent(matchingTvModel)
                Log.d(TAG, "str2Channels: Restored groupModel.current to: ${matchingTvModel.tv.title}")
            } else {
                Log.w(TAG, "str2Channels: No matching TVModel found for title: $currentTvTitle")
            }
        }

        try {
            val encodedString = SourceEncoder.encodeJsonSource(string)
            if (string != cacheChannels && encodedString != cacheChannels) {
                // Remove initPosition
            }
        } catch (e: Exception) {
            Log.e(TAG, "加密字符串失敗: ${e.message}")
        }

        groupModel.setChange()
        viewModelScope.launch(Dispatchers.IO) { preloadLogo() }
        Log.d(TAG, "str2Channels: Updated listModel size=${listModel.size}")

        // 新增：确保 groupModel.current 非空，恢复旧代码的隐式播放
        if (groupModel.getCurrent() == null && listModel.isNotEmpty()) {
            groupModel.setCurrent(listModel[0])
            Log.d(TAG, "str2Channels: Set default groupModel.current to: ${listModel[0].tv.title}")
        }

        return true
    }

    fun clearCacheChannels() {
        cacheChannels = ""
        Log.d(TAG, "clearCacheChannels: Cache cleared")
    }

    companion object {
        private const val TAG = "MainViewModel"
        const val CACHE_FILE_NAME = "codechannels.txt"
        const val CACHE_CODE_FILE = "cacheCode.txt"
        const val CACHE_EPG = "epg.xml"
        val DEFAULT_CHANNELS_FILE = R.raw.channels
    }
}