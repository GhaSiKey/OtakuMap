package com.gaoshiqi.otakumap.search.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.gaoshiqi.otakumap.R
import com.gaoshiqi.otakumap.databinding.FragmentSearchFilterBinding
import com.gaoshiqi.otakumap.databinding.ItemRangeInputBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 搜索筛选面板 BottomSheet
 * 通过 Fragment Result API 将筛选结果传递给 SearchActivity
 */
class SearchFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentSearchFilterBinding? = null
    private val binding get() = _binding!!

    private val currentTags = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSortChips()
        setupTypeChips()
        setupNsfwChips()
        setupTagInput()
        setupDatePickers()
        setupRangeInputHints()
        setupButtons()

        val state = arguments?.getParcelable<SearchFilterState>(ARG_FILTER_STATE)
            ?: SearchFilterState.DEFAULT
        restoreState(state)
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet = requireView().parent as View
        val fixedHeight = (resources.displayMetrics.heightPixels * 0.6).toInt()
        bottomSheet.layoutParams.height = fixedHeight

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.peekHeight = fixedHeight
        behavior.maxHeight = fixedHeight
        behavior.isFitToContents = true
        behavior.isDraggable = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        // 拖动条区域下拉 → 关闭弹窗
        binding.dragHandleArea.setOnTouchListener { _, event ->
            // 将拖动条的触摸事件传递给 BottomSheet 让其处理拖拽关闭
            bottomSheet.onTouchEvent(event)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ==================== 初始化 UI ====================

    private fun setupSortChips() {
        SortOption.entries.forEach { option ->
            val chip = createChoiceChip(getString(option.labelResId))
            chip.tag = option
            binding.chipGroupSort.addView(chip)
        }
    }

    private fun setupTypeChips() {
        SubjectType.entries.forEach { type ->
            val chip = createFilterChip(getString(type.labelResId))
            chip.tag = type
            binding.chipGroupType.addView(chip)
        }
    }

    private fun setupNsfwChips() {
        NsfwOption.entries.forEach { option ->
            val chip = createChoiceChip(getString(option.labelResId))
            chip.tag = option
            binding.chipGroupNsfw.addView(chip)
        }
    }

    private fun setupTagInput() {
        binding.btnAddTag.setOnClickListener { addTag() }
        binding.etTag.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTag()
                true
            } else false
        }
    }

    private fun setupDatePickers() {
        binding.etDateStart.setOnClickListener {
            showDatePicker { date -> binding.etDateStart.setText(date) }
        }
        binding.etDateEnd.setOnClickListener {
            showDatePicker { date -> binding.etDateEnd.setText(date) }
        }
        // 长按清除日期
        binding.etDateStart.setOnLongClickListener {
            binding.etDateStart.text?.clear()
            true
        }
        binding.etDateEnd.setOnLongClickListener {
            binding.etDateEnd.text?.clear()
            true
        }
    }

    private fun setupRangeInputHints() {
        val ratingBinding = ItemRangeInputBinding.bind(binding.rangeRating.root)
        ratingBinding.tilMin.hint = getString(R.string.filter_rating_min)
        ratingBinding.tilMax.hint = getString(R.string.filter_rating_max)

        val ratingCountBinding = ItemRangeInputBinding.bind(binding.rangeRatingCount.root)
        ratingCountBinding.tilMin.hint = getString(R.string.filter_rating_count_min)
        ratingCountBinding.tilMax.hint = getString(R.string.filter_rating_count_max)
        ratingCountBinding.etMin.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        ratingCountBinding.etMax.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val rankBinding = ItemRangeInputBinding.bind(binding.rangeRank.root)
        rankBinding.tilMin.hint = getString(R.string.filter_rank_min)
        rankBinding.tilMax.hint = getString(R.string.filter_rank_max)
        rankBinding.etMin.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        rankBinding.etMax.inputType = android.text.InputType.TYPE_CLASS_NUMBER
    }

    private fun setupButtons() {
        binding.btnReset.setOnClickListener { resetToDefault() }
        binding.tvResetTop.setOnClickListener { resetToDefault() }
        binding.btnApply.setOnClickListener { applyFilter() }
    }

    // ==================== 状态回显 ====================

    private fun restoreState(state: SearchFilterState) {
        // 排序
        for (i in 0 until binding.chipGroupSort.childCount) {
            val chip = binding.chipGroupSort.getChildAt(i) as Chip
            if (chip.tag == state.sort) {
                chip.isChecked = true
                break
            }
        }

        // 类型
        for (i in 0 until binding.chipGroupType.childCount) {
            val chip = binding.chipGroupType.getChildAt(i) as Chip
            chip.isChecked = (chip.tag as SubjectType) in state.types
        }

        // 标签
        currentTags.clear()
        state.tags.forEach { addTagChip(it) }

        // 日期
        binding.etDateStart.setText(state.airDateStart)
        binding.etDateEnd.setText(state.airDateEnd)

        // 评分
        val ratingBinding = ItemRangeInputBinding.bind(binding.rangeRating.root)
        ratingBinding.etMin.setText(state.rating.min)
        ratingBinding.etMax.setText(state.rating.max)

        // 评分人数
        val ratingCountBinding = ItemRangeInputBinding.bind(binding.rangeRatingCount.root)
        ratingCountBinding.etMin.setText(state.ratingCount.min)
        ratingCountBinding.etMax.setText(state.ratingCount.max)

        // 排名
        val rankBinding = ItemRangeInputBinding.bind(binding.rangeRank.root)
        rankBinding.etMin.setText(state.rank.min)
        rankBinding.etMax.setText(state.rank.max)

        // NSFW
        for (i in 0 until binding.chipGroupNsfw.childCount) {
            val chip = binding.chipGroupNsfw.getChildAt(i) as Chip
            if (chip.tag == state.nsfw) {
                chip.isChecked = true
                break
            }
        }
    }

    // ==================== 状态收集 ====================

    private fun collectCurrentState(): SearchFilterState? {
        val sort = getSelectedSort() ?: SortOption.MATCH
        val types = getSelectedTypes()
        val rating = collectRange(binding.rangeRating.root)
        val ratingCount = collectRange(binding.rangeRatingCount.root)
        val rank = collectRange(binding.rangeRank.root)

        // 验证评分范围 0-10
        if (!validateRatingRange(rating)) return null

        return SearchFilterState(
            sort = sort,
            types = types,
            tags = currentTags.toList(),
            airDateStart = binding.etDateStart.text?.toString()?.trim() ?: "",
            airDateEnd = binding.etDateEnd.text?.toString()?.trim() ?: "",
            rating = rating,
            rank = rank,
            ratingCount = ratingCount,
            nsfw = getSelectedNsfw() ?: NsfwOption.NO_FILTER
        )
    }

    private fun getSelectedSort(): SortOption? {
        for (i in 0 until binding.chipGroupSort.childCount) {
            val chip = binding.chipGroupSort.getChildAt(i) as Chip
            if (chip.isChecked) return chip.tag as SortOption
        }
        return null
    }

    private fun getSelectedTypes(): Set<SubjectType> {
        val types = mutableSetOf<SubjectType>()
        for (i in 0 until binding.chipGroupType.childCount) {
            val chip = binding.chipGroupType.getChildAt(i) as Chip
            if (chip.isChecked) types.add(chip.tag as SubjectType)
        }
        return types
    }

    private fun getSelectedNsfw(): NsfwOption? {
        for (i in 0 until binding.chipGroupNsfw.childCount) {
            val chip = binding.chipGroupNsfw.getChildAt(i) as Chip
            if (chip.isChecked) return chip.tag as NsfwOption
        }
        return null
    }

    private fun collectRange(rangeView: View): RangeCondition {
        val rangeBinding = ItemRangeInputBinding.bind(rangeView)
        return RangeCondition(
            min = rangeBinding.etMin.text?.toString()?.trim() ?: "",
            max = rangeBinding.etMax.text?.toString()?.trim() ?: ""
        )
    }

    // ==================== 验证 ====================

    private fun validateRatingRange(rating: RangeCondition): Boolean {
        val minVal = rating.min.toDoubleOrNull()
        val maxVal = rating.max.toDoubleOrNull()

        if (minVal != null && (minVal < 0 || minVal > 10)) {
            Toast.makeText(requireContext(), getString(R.string.filter_rating_title) + ": 0~10", Toast.LENGTH_SHORT).show()
            return false
        }
        if (maxVal != null && (maxVal < 0 || maxVal > 10)) {
            Toast.makeText(requireContext(), getString(R.string.filter_rating_title) + ": 0~10", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // ==================== 标签管理 ====================

    private fun addTag() {
        val tag = binding.etTag.text?.toString()?.trim() ?: return
        if (tag.isBlank()) return

        if (currentTags.size >= MAX_TAGS) {
            Toast.makeText(requireContext(), getString(R.string.filter_tag_limit, MAX_TAGS), Toast.LENGTH_SHORT).show()
            return
        }
        if (tag.length > MAX_TAG_LENGTH) {
            Toast.makeText(requireContext(), getString(R.string.filter_tag_too_long, MAX_TAG_LENGTH), Toast.LENGTH_SHORT).show()
            return
        }
        if (tag in currentTags) {
            Toast.makeText(requireContext(), R.string.filter_tag_duplicate, Toast.LENGTH_SHORT).show()
            return
        }

        addTagChip(tag)
        binding.etTag.text?.clear()
    }

    private fun addTagChip(tag: String) {
        currentTags.add(tag)
        val chip = Chip(requireContext()).apply {
            text = tag
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                currentTags.remove(tag)
                binding.chipGroupTags.removeView(this)
            }
        }
        binding.chipGroupTags.addView(chip)
    }

    // ==================== 日期选择 ====================

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.filter_air_date_title))
            .build()

        picker.addOnPositiveButtonClickListener { timestamp ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            onDateSelected(dateFormat.format(Date(timestamp)))
        }
        picker.show(childFragmentManager, "date_picker")
    }

    // ==================== 操作 ====================

    private fun resetToDefault() {
        restoreState(SearchFilterState.DEFAULT)
        // 清除标签 ChipGroup
        binding.chipGroupTags.removeAllViews()
        currentTags.clear()
        // 清除所有 range input
        clearEditText(binding.rangeRating.root)
        clearEditText(binding.rangeRatingCount.root)
        clearEditText(binding.rangeRank.root)
    }

    private fun clearEditText(rangeView: View) {
        val rangeBinding = ItemRangeInputBinding.bind(rangeView)
        rangeBinding.etMin.text?.clear()
        rangeBinding.etMax.text?.clear()
    }

    private fun applyFilter() {
        val state = collectCurrentState() ?: return
        setFragmentResult(
            REQUEST_KEY,
            bundleOf(RESULT_FILTER_STATE to state)
        )
        dismiss()
    }

    // ==================== Chip 工厂 ====================

    private fun createStyledChip(text: String): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            checkedIcon = null
            chipBackgroundColor = resources.getColorStateList(R.color.chip_bg_color, null)
            setTextColor(resources.getColorStateList(R.color.chip_text_color, null))
            chipStrokeColor = resources.getColorStateList(R.color.chip_stroke_color, null)
            chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width)
        }
    }

    private fun createChoiceChip(text: String): Chip = createStyledChip(text)

    private fun createFilterChip(text: String): Chip = createStyledChip(text)

    companion object {
        const val TAG = "SearchFilterBottomSheet"
        const val REQUEST_KEY = "search_filter_result"
        const val RESULT_FILTER_STATE = "filter_state"
        private const val ARG_FILTER_STATE = "arg_filter_state"
        private const val MAX_TAGS = 10
        private const val MAX_TAG_LENGTH = 30

        fun newInstance(currentState: SearchFilterState): SearchFilterBottomSheet {
            return SearchFilterBottomSheet().apply {
                arguments = bundleOf(ARG_FILTER_STATE to currentState)
            }
        }
    }
}
