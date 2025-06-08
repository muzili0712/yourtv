package com.horsenma.yourtv

import android.content.Context
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.app.Activity
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.horsenma.yourtv.databinding.MenuBinding
import com.horsenma.yourtv.models.TVListModel
import com.horsenma.yourtv.models.TVModel
import java.io.File


class MenuFragment : Fragment(), GroupAdapter.ItemListener, TVListAdapter.ItemListener {
    private var _binding: MenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var groupAdapter: GroupAdapter
    private lateinit var listAdapter: TVListAdapter

    private var groupWidth = 0
    private var listWidth = 0

    private val cachedSources = LinkedHashMap<String, String>()
    private lateinit var viewModel: MainViewModel
    private var currentTestCodeIndex: Int = 0 // 实际源索引
    private var displaySourceIndex: Int = 0 // 显示源索引
    private var lastSwitchSourceTime: Long = 0L
    private var lastUpdateTime: Long = 0L
    private var listenersBound: Boolean = false
    private var clickCount: Int = 0
    private var lastClickTime: Long = System.currentTimeMillis() // 初始化为当前时间

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireActivity()
        val application = context.applicationContext as YourTVApplication
        viewModel = ViewModelProvider(context)[MainViewModel::class.java]

        groupAdapter = GroupAdapter(context, binding.group, viewModel.groupModel)
        binding.group.adapter = groupAdapter
        binding.group.layoutManager = LinearLayoutManager(context)
        groupWidth = application.px2Px(binding.group.layoutParams.width)
        binding.group.layoutParams.width = if (SP.compactMenu) {
            groupWidth * 2 / 3
        } else {
            groupWidth
        }
        groupAdapter.setItemListener(this)

        listAdapter = TVListAdapter(context, binding.list, this)
        listAdapter.setItemListener(this)
        binding.list.adapter = listAdapter
        binding.list.layoutManager = LinearLayoutManager(context)
        listWidth = application.px2Px(binding.list.layoutParams.width)
        binding.list.layoutParams.width = if (SP.compactMenu) {
            listWidth * 4 / 5
        } else {
            listWidth
        }

        var lastClickTime = 0L
        binding.menu.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > 1000) {
                hideSelf()
                lastClickTime = currentTime
            }
            (activity as? MainActivity)?.menuActive()
        }

        groupAdapter.focusable(true)
        listAdapter.focusable(true)

        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.post { requestFocus() }

        // 根视图按键监听
        binding.root.isFocusable = true
        binding.root.isFocusableInTouchMode = true
        binding.group.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && isVisible && isAdded) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        val layoutManager = binding.group.layoutManager as? LinearLayoutManager
                        val firstVisiblePosition = layoutManager?.findFirstCompletelyVisibleItemPosition() ?: -1
                        if (firstVisiblePosition == 0) {
                            binding.sourceSwitcherPrev.requestFocus()
                            //Log.d(TAG, "Group: Key DPAD_UP pressed at top, focus to source_switcher_prev")
                            return@setOnKeyListener true
                        }
                        return@setOnKeyListener false
                    }
                }
            }
            false
        }

        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val mainActivity = activity as? MainActivity
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        mainActivity?.menuActive()
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        mainActivity?.menuActive()
                    }
                }
            }
        }

        val onTouchListener = View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.requestFocus()
                (activity as? MainActivity)?.menuActive()
                //Log.d(TAG, "Touch on ${v.id}, focus requested")
            }
            false
        }
        binding.group.addOnScrollListener(scrollListener)
        binding.list.addOnScrollListener(scrollListener)
        binding.menu.setOnTouchListener(onTouchListener)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            onVisible()
            view?.post { requestFocus() }
        } else {
            view?.post {
                groupAdapter.visible = false
                listAdapter.setVisible(false)
            }
        }
    }

    private fun getList(): TVListModel? {
        if (!this::viewModel.isInitialized) {
            return null
        }
        if (viewModel.groupModel.getCurrentList() == null) {
            viewModel.groupModel.setPosition(0)
        }
        return viewModel.groupModel.getCurrentList()
    }

    fun update() {
        view?.post {
            groupAdapter.changed()
            getList()?.let { listModel ->
                listAdapter.update(listModel)
                //Log.d(TAG, "MenuFragment: Updated list with ${listModel.tvList.value?.size} items")
            }
            if (isVisible) setupSourceSwitcher() // 仅可见时更新 UI
        }
    }

    fun updateSize() {
        view?.post {
            binding.group.layoutParams.width = if (SP.compactMenu) {
                groupWidth * 4 / 5 // 统一为 4/5，与 mytv1 一致
            } else {
                groupWidth
            }
            binding.list.layoutParams.width = if (SP.compactMenu) {
                listWidth * 4 / 5
            } else {
                listWidth
            }
        }
    }

    fun updateList(position: Int) {
        if (!this::viewModel.isInitialized) {
            return
        }
        viewModel.groupModel.setPosition(position)
        SP.positionGroup = position
        viewModel.groupModel.getCurrentList()?.let {
            listAdapter.update(it)
            listAdapter.toPosition(it.positionPlayingValue)
        }
    }

    private fun hideSelf() {
        if (!isAdded || activity == null || requireActivity().isFinishing) {
            //Log.w(TAG, "hideSelf: Fragment not added or Activity finishing, skipping")
            return
        }
        try {
            requireActivity().supportFragmentManager.beginTransaction()
                .hide(this)
                .commitAllowingStateLoss()
            //Log.d(TAG, "MenuFragment hidden")
        } catch (e: IllegalStateException) {
            //Log.e(TAG, "hideSelf: Failed to commit transaction", e)
        }
    }

    // GroupAdapter.ItemListener
    override fun onItemFocusChange(listTVModel: TVListModel, hasFocus: Boolean) {
        if (hasFocus) {
            listAdapter.update(listTVModel)
            (activity as? MainActivity)?.menuActive()
        }
    }

    // TVListAdapter.ItemListener
    override fun onItemFocusChange(tvModel: TVModel, hasFocus: Boolean) {
    }

    // GroupAdapter.ItemListener
    override fun onItemClicked(position: Int) {
        if (!this::viewModel.isInitialized) return
        updateList(position)
        groupAdapter.focusable(true)
        listAdapter.focusable(false)
        binding.group.requestFocus()
        groupAdapter.scrollToPositionAndSelect(position)
        (activity as? MainActivity)?.menuActive() // 重置计时器
        //Log.d(TAG, "MenuFragment: Group item clicked, focusing on position $position")
    }

    // TVListAdapter.ItemListener
    override fun onItemClicked(position: Int, type: String) {
        if (!this::viewModel.isInitialized) return
        viewModel.groupModel.setPositionPlaying()
        viewModel.groupModel.getCurrentList()?.let {
            it.setPosition(position)
            it.setPositionPlaying()
            it.getCurrent()?.setReady()
        }
        (activity as? MainActivity)?.menuActive() // 重置计时器
        hideSelf()
    }

    // GroupAdapter.ItemListener 的 onKey
    override fun onKey(keyCode: Int): Boolean {
        val mainActivity = activity as? MainActivity
        mainActivity?.menuActive()
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                return false
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (listAdapter.itemCount == 0) return true
                listAdapter.focusable(true)
                groupAdapter.focusable(false)
                binding.list.requestFocus()
                listAdapter.toPosition(viewModel.groupModel.getCurrentList()?.positionPlayingValue ?: 0)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                groupAdapter.focusable(true)
                listAdapter.focusable(false)
                binding.group.requestFocus()
                groupAdapter.scrollToPositionAndSelect(viewModel.groupModel.positionValue)
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                hideSelf()
                return true
            }
        }
        return false
    }

    // TVListAdapter.ItemListener 的 onKey
    override fun onKey(listAdapter: TVListAdapter, keyCode: Int): Boolean {
        (activity as? MainActivity)?.menuActive() // 重置计时器
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                groupAdapter.focusable(true)
                listAdapter.focusable(false)
                binding.group.requestFocus()
                groupAdapter.scrollToPositionAndSelect(viewModel.groupModel.positionValue)
                (activity as? MainActivity)?.menuActive()
                return true
            }
        }
        return false
    }

    fun onVisible() {
        if (viewModel.groupModel.tvGroupValue.size < 2 || viewModel.groupModel.getAllList()?.size() == 0) {
            //Log.w(TAG, "MenuFragment: No data available in group or list")
            return
        }
        //Log.d(TAG, "MenuFragment onVisible, calling setupSourceSwitcher")
        setupSourceSwitcher()
        val position = viewModel.groupModel.positionPlayingValue
        if (position != viewModel.groupModel.positionValue) {
            updateList(position)
        }
        viewModel.groupModel.getCurrentList()?.let {
            listAdapter.update(it)
            listAdapter.toPosition(it.positionPlayingValue)
        } ?: run {
            //Log.w(TAG, "MenuFragment: Current list is null, retrying")
            view?.postDelayed({ if (isAdded) onVisible() }, 1000)
        }
        (activity as MainActivity).menuActive()
    }

    private fun requestFocus() {
        binding.sourceSwitcherText.isFocusable = true
        binding.sourceSwitcherText.isFocusableInTouchMode = true
        binding.sourceSwitcherPrev.isFocusable = true
        binding.sourceSwitcherPrev.isFocusableInTouchMode = true
        binding.sourceSwitcherNext.isFocusable = true
        binding.sourceSwitcherNext.isFocusableInTouchMode = true
        binding.group.isFocusable = true
        binding.group.isFocusableInTouchMode = true

        binding.list.isFocusable = true
        binding.list.isFocusableInTouchMode = true
        if (listAdapter.itemCount > 0) {
            listAdapter.focusable(true)
            groupAdapter.focusable(false)
            binding.list.requestFocus()
            val position = viewModel.groupModel.getCurrentList()?.positionPlayingValue ?: 0
            listAdapter.toPosition(position)
            //Log.d(TAG, "Focus requested on list at position $position")
        } else {
            binding.group.isFocusable = true
            binding.group.isFocusableInTouchMode = true
            groupAdapter.focusable(true)
            listAdapter.focusable(false)
            binding.group.requestFocus()
            val groupPosition = viewModel.groupModel.positionValue
            groupAdapter.scrollToPositionAndSelect(groupPosition)
            //Log.d(TAG, "Focus requested on group at position $groupPosition")
        }
    }

    internal fun setupSourceSwitcher() {
        val context = context ?: return
        val prefs = context.getSharedPreferences("SourceCache", Context.MODE_PRIVATE)

        // 清理无效键
        with(prefs.edit()) {
            prefs.all.keys.filter { key ->
                if (key.startsWith("cache_") && !key.startsWith("cache_time_") && key.matches(Regex("^cache_.*\\.txt$"))) {
                    val filename = key.removePrefix("cache_")
                    val cacheFile = File(context.filesDir, "cache_$filename")
                    prefs.getString(key, null).isNullOrEmpty() || !cacheFile.exists()
                } else if (key.startsWith("cache_time_") || key.startsWith("url_")) {
                    val filename = key.removePrefix("cache_time_").removePrefix("url_")
                    !prefs.contains("cache_$filename")
                } else {
                    false
                }
            }.forEach { remove(it) }
            apply()
        }

        // 重建 cachedSources
        cachedSources.clear()
        // 添加默认源
        try {
            context.resources.openRawResource(R.raw.channels).use {
                cachedSources["default_channels.txt"] = "default_channels"
                Log.d(TAG, "Added to cachedSources: default_channels.txt")
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupSourceSwitcher: Failed to access R.raw.channels: ${e.message}")
        }

        // 在 MenuFragment.setupSourceSwitcher 中修改
        prefs.all.keys.filter { it.startsWith("cache_") && !it.startsWith("cache_time_") }
            .forEach { key ->
                val filename = key.removePrefix("cache_")
                val cachedContent = prefs.getString(key, null)
                val cacheFile = File(context.filesDir, "cache_$filename")
                if (cachedContent != null && cacheFile.exists() && cacheFile.readText() == cachedContent) {
                    val sourceName = prefs.getString("url_$filename", null)?.substringAfterLast("/")?.substringBeforeLast(".")?.takeIf { it.isNotBlank() }
                        ?: filename.substringBeforeLast(".").takeIf { it.isNotBlank() }
                        ?: "未知源"
                    cachedSources[filename] = sourceName
                    Log.d(TAG, "Added to cachedSources: $filename -> $sourceName")
                }
            }

        // 初始化索引
        val activeFilename = prefs.getString("active_source", "default_channels.txt") ?: "default_channels.txt"
        currentTestCodeIndex = cachedSources.keys.indexOfFirst { it == activeFilename }.coerceAtLeast(0)
        displaySourceIndex = currentTestCodeIndex

        // 更新 UI
        updateSourceSwitcher()
        Log.d(TAG, "Source switcher updated, cachedSources.size=${cachedSources.size}")
        viewModel.channelsOk.observe(viewLifecycleOwner) { isInitialized ->
            if (isInitialized) {
                updateSourceSwitcher()
                view?.post {
                    if (cachedSources.size >= 1) {
                        view?.findViewById<View>(R.id.source_switcher_container)?.visibility = View.VISIBLE
                        //Log.d(TAG, "Showing source switcher: cachedSources.size=${cachedSources.size}")
                    } else {
                        view?.findViewById<View>(R.id.source_switcher_container)?.visibility = View.GONE
                        //Log.d(TAG, "Hiding source switcher: cachedSources.size=${cachedSources.size}")
                    }
                }
            }
        }
    }

    private fun updateSourceSwitcher() {
        try {
            if (cachedSources.size > 1) {
                Log.d(TAG, "Showing source switcher: cachedSources.size=${cachedSources.size}")
                binding.sourceSwitcherContainer.visibility = View.VISIBLE
                val application = context?.applicationContext as YourTVApplication
                val groupActualWidth = binding.group.layoutParams.width + application.dp2Px(1)
                val listActualWidth = binding.list.layoutParams.width
                val totalWidth = groupActualWidth + listActualWidth
                binding.sourceSwitcherContainer.layoutParams.width = totalWidth
                binding.sourceSwitcherText.textSize = 16f
                updateSourceText()
                if (!listenersBound) {
                    Log.d(TAG, "Binding source switcher listeners")
                    binding.sourceSwitcherPrev.setOnClickListener {
                        updateDisplaySource(-1)
                        binding.sourceSwitcherPrev.requestFocus()
                        (activity as? MainActivity)?.menuActive()
                    }
                    binding.sourceSwitcherNext.setOnClickListener {
                        updateDisplaySource(1)
                        binding.sourceSwitcherNext.requestFocus()
                        (activity as? MainActivity)?.menuActive()
                    }
                    binding.sourceSwitcherText.setOnClickListener {
                        val currentTime = System.currentTimeMillis()
                        // 检查是否在 10 秒防抖期内
                        if (currentTime - lastSwitchSourceTime < 10000) {
                            //Log.d(TAG, "Switch source blocked: within 10s debounce period")
                            return@setOnClickListener
                        }
                        //Log.d(TAG, "Text clicked, clickCount=$clickCount, timeDiff=${currentTime - lastClickTime}")
                        if (currentTime - lastClickTime <= 500) {
                            clickCount += 1
                            // 支持 4 次连击或 2 次双击
                            if (clickCount >= 4) {
                                //Log.d(TAG, "Triggering switchSource(0) on 4 clicks")
                                switchSource(0)
                                clickCount = 0
                            } else if (clickCount == 2) {
                                view?.postDelayed({
                                    if (clickCount == 2) {
                                        //Log.d(TAG, "Triggering switchSource(0) on 2 double clicks")
                                        switchSource(0)
                                        clickCount = 0
                                    }
                                }, 500) // 500ms 后检查是否为双击
                            }
                        } else {
                            clickCount = 1
                        }
                        lastClickTime = currentTime
                        binding.sourceSwitcherText.requestFocus() // 点击时同步焦点
                        (activity as? MainActivity)?.menuActive()
                    }
                    // 在 setupSourceSwitcher() 后，调整按键监听
                    binding.sourceSwitcherPrev.setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    binding.sourceSwitcherText.requestFocus()
                                    (activity as? MainActivity)?.menuActive()
                                    //Log.d(TAG, "Focus moved to source_switcher_text from prev")
                                    return@setOnKeyListener true
                                }
                                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    // 不处理上下键，保持旧版焦点逻辑
                                    return@setOnKeyListener false
                                }
                            }
                        }
                        false
                    }
                    binding.sourceSwitcherText.setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    binding.sourceSwitcherPrev.requestFocus()
                                    (activity as? MainActivity)?.menuActive()
                                    //Log.d(TAG, "Focus moved to source_switcher_prev from text")
                                    return@setOnKeyListener true
                                }
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    binding.sourceSwitcherNext.requestFocus()
                                    (activity as? MainActivity)?.menuActive()
                                    //Log.d(TAG, "Focus moved to source_switcher_next from text")
                                    return@setOnKeyListener true
                                }
                                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    // 不处理上下键，保持旧版焦点逻辑
                                    return@setOnKeyListener false
                                }
                            }
                        }
                        false
                    }
                    binding.sourceSwitcherNext.setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    binding.sourceSwitcherText.requestFocus()
                                    (activity as? MainActivity)?.menuActive()
                                    //Log.d(TAG, "Focus moved to source_switcher_text from next")
                                    return@setOnKeyListener true
                                }
                                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    // 不处理上下键，保持旧版焦点逻辑
                                    return@setOnKeyListener false
                                }
                            }
                        }
                        false
                    }
                    listenersBound = true
                }
                binding.sourceSwitcherContainer.requestLayout()
            } else {
                //Log.d(TAG, "Hiding source switcher: cachedSources.size=${cachedSources.size}")
                binding.sourceSwitcherContainer.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateSourceSwitcher: ${e.message}", e)
            binding.sourceSwitcherContainer.visibility = View.GONE
        }
    }

    private fun updateDisplaySource(direction: Int) {
        if (cachedSources.isEmpty()) {
            setupSourceSwitcher()
            return
        }
        // 循环更新显示索引
        displaySourceIndex = (displaySourceIndex + direction).let { idx ->
            when {
                idx >= cachedSources.size -> 0
                idx < 0 -> cachedSources.size - 1
                else -> idx
            }
        }
        //Log.d(TAG, "Updated display source: direction=$direction, displaySourceIndex=$displaySourceIndex")
        updateSourceText()
    }

    private fun switchSource(direction: Int) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSwitchSourceTime < 10000) { // 10 秒防抖
            //Log.d(TAG, "Switch source blocked: within 10s debounce period, time since last switch=${currentTime - lastSwitchSourceTime}ms")
            context?.let {
                Toast.makeText(it, "请等待 10 秒后再试", Toast.LENGTH_SHORT).show()
            }
            return
        }
        lastSwitchSourceTime = currentTime
        val prefs = context?.getSharedPreferences("SourceCache", Context.MODE_PRIVATE) ?: return

        if (cachedSources.isEmpty()) {
            setupSourceSwitcher()
            return
        }

        // 实际切换时，使用 displaySourceIndex
        currentTestCodeIndex = if (direction == 0) displaySourceIndex else {
            (currentTestCodeIndex + direction).let { idx ->
                when {
                    idx >= cachedSources.size -> 0
                    idx < 0 -> cachedSources.size - 1
                    else -> idx
                }
            }
        }
        displaySourceIndex = currentTestCodeIndex // 同步显示索引
        val selectedFilename = cachedSources.keys.elementAt(currentTestCodeIndex)
        val selectedUrl = prefs.getString("url_$selectedFilename", "") ?: ""
        val selectedSourceName = cachedSources[selectedFilename] ?: "未知源"
        // 检查是否为当前源
        val activeFilename = prefs.getString("active_source", "default_channels.txt") ?: "default_channels.txt"
        if (selectedFilename == activeFilename) {
            Log.d(TAG, "Selected source is already active: $selectedFilename, skipping switch")
            context?.let {
                // 可选：显示提示（根据需求决定是否保留）
                Toast.makeText(it, "已是当前源: $selectedSourceName", Toast.LENGTH_SHORT).show()
            }
            return
        }

        lastSwitchSourceTime = currentTime
        Log.d(TAG, "Switching source: direction=$direction, index=$currentTestCodeIndex, filename=$selectedFilename, url=$selectedUrl")
        hideSelf()
        view?.post {
            if (selectedFilename == "default_channels.txt") {
                viewModel.reset(requireContext())
            } else {
                (activity as? MainActivity)?.switchSource(selectedFilename, selectedUrl)
            }
            prefs.edit().putString("active_source", selectedFilename).apply()
            updateSourceText()
            // 添加 Toast 提示切换成功
            context?.let {
                Toast.makeText(it, "已切换到 $selectedSourceName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSourceText() {
        val sourceName = cachedSources.values.elementAtOrNull(displaySourceIndex) ?: ""
        binding.sourceSwitcherText.text = sourceName
        //Log.d(TAG, "Updated source text: $sourceName")
    }

    override fun onAttach(context: Context) { // 注意：使用 Context（适配新版 Fragment）
        super.onAttach(context)
        // 添加 OnBackPressedCallback，处理返回键（可选）
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 可选：自定义返回键逻辑，例如隐藏 MenuFragment
                if (isVisible && isAdded) {
                    hideSelf()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "MenuFragment"
    }
}