package com.gaoshiqi.otakumap.homepage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.TestActivity
import com.gaoshiqi.otakumap.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // 连续点击检测
    private var clickCount = 0
    private var lastClickTime = 0L
    private val requiredClicks = 8
    private val clickTimeout = 1000L  // 1秒内连续点击有效

    // 开发者模式状态持久化
    private val prefs by lazy {
        requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    private var isDeveloperModeEnabled: Boolean
        get() = prefs.getBoolean("developer_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("developer_mode_enabled", value).apply()

    // 复用 Toast 实例，避免排队
    private var currentToast: Toast? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        // 设置版本号
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName
            binding.tvAppVersion.text = getString(R.string.settings_version_format, versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            binding.tvAppVersion.text = getString(R.string.settings_version_format, "Unknown")
        }

        // 根据持久化状态显示/隐藏开发者选项
        binding.cardDeveloperOptions.visibility =
            if (isDeveloperModeEnabled) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        binding.btnDeveloperOptions.setOnClickListener {
            val intent = Intent(requireContext(), TestActivity::class.java)
            startActivity(intent)
        }

        binding.btnAbout.setOnClickListener {
            handleAboutClick()
        }

        binding.btnFeedback.setOnClickListener {
            showToast(getString(R.string.settings_feedback_message))
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
            startActivity(intent)
        }
    }

    companion object {
        private const val PRIVACY_POLICY_URL = "https://github.com/GhaSiKey/BangumiAPP/blob/main/docs/PRIVACY_POLICY_EN.md"
    }

    private fun handleAboutClick() {
        val currentTime = System.currentTimeMillis()

        // 如果已经开启开发者模式，只显示关于信息
        if (isDeveloperModeEnabled) {
            showToast(getString(R.string.settings_about_message), Toast.LENGTH_LONG)
            return
        }

        // 超时重置计数
        if (currentTime - lastClickTime > clickTimeout) {
            clickCount = 0
        }

        lastClickTime = currentTime
        clickCount++

        val remaining = requiredClicks - clickCount

        when {
            remaining == 0 -> {
                // 成功进入开发者模式
                isDeveloperModeEnabled = true
                binding.cardDeveloperOptions.visibility = View.VISIBLE
                showToast(getString(R.string.developer_mode_enabled))
            }
            remaining <= 3 -> {
                // 提示剩余次数
                showToast(getString(R.string.developer_mode_remaining, remaining))
            }
            else -> {
                showToast(getString(R.string.settings_about_message), Toast.LENGTH_LONG)
            }
        }
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        currentToast?.cancel()
        currentToast = Toast.makeText(requireContext(), message, duration).also { it.show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}