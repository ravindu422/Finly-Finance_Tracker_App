package com.example.finly.ui

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finly.R
import com.example.finly.databinding.FragmentStatisticsBinding
import com.example.finly.models.CategorySpendingItem
import com.example.finly.models.Transaction
import com.example.finly.models.TransactionType
import com.example.finly.utils.formatAmount
import com.example.finly.viewmodels.CategoryViewModel
import com.example.finly.viewmodels.TransactionViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class StatisticsFragment: Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var categoryViewModel: CategoryViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionViewModel = ViewModelProvider(requireActivity())[TransactionViewModel::class.java]
        categoryViewModel = ViewModelProvider(requireActivity())[CategoryViewModel::class.java]

        binding.rvCategorySpending.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = CategorySpendingAdapter()
        }

        transactionViewModel.getAllTransactions().observe(viewLifecycleOwner) { transactions ->
            updatePieChart(transactions)
            updateCategoryList(transactions)
            updateLineChart(transactions)
            updateBalanceSummary(transactions)
        }

        setupChipAppearance()
    }

    private fun updateBalanceSummary(transactions: List<Transaction>) {

        val currentYearTransactions = transactions.filter {
            val calendar = Calendar.getInstance().apply { time = it.date }
            val currentCalendar = Calendar.getInstance()

            calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)
        }

        val income = currentYearTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val expenses = currentYearTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val balance = income - expenses

        val preferences = requireContext().getSharedPreferences(
            "user_prefs", Context.MODE_PRIVATE
        )
        val currencySymbol = when (preferences.getString("currency", "USD")) {
            "USD" -> "$"
            "EUR" -> "€"
            "JPY" -> "¥"
            "CAD" -> "$"
            "LKR" -> "Rs."
            else -> "$"
        }

        binding.tvIncome.text = "$currencySymbol${income.formatAmount()}"
        binding.tvExpenses.text = "$currencySymbol${expenses.formatAmount()}"

        val displayBalance = abs(balance)
        binding.tvCurrentBalance.text = "$currencySymbol${displayBalance.formatAmount()}"

        binding.tvBalanceTitle.text = "Annual Balance"

        if (balance < 0) {
            binding.tvCurrentBalance.setTextColor(Color.WHITE)
            binding.tvCurrentBalance.alpha = 0.9f

            binding.tvBalanceStatus.visibility = View.VISIBLE
            binding.tvBalanceStatus.text = "Expenses Exceed Income"
            binding.tvBalanceStatus.setTextColor(Color.WHITE)
            binding.tvBalanceStatus.alpha = 0.7f

            binding.ivBalanceWarning.visibility = View.VISIBLE
        } else {
            binding.tvCurrentBalance.setTextColor(Color.WHITE)
            binding.tvCurrentBalance.alpha = 1.0f
            binding.tvBalanceStatus.visibility = View.GONE
            binding.ivBalanceWarning.visibility = View.GONE
        }
    }

    private fun updatePieChart(transactions: List<Transaction>) {
        val currentMonthTransactions = transactions.filter {
            val calendar = Calendar.getInstance().apply { time = it.date }
            val currentCalendar = Calendar.getInstance()
            calendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                    calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                    it.type == TransactionType.EXPENSE
        }

        val categorySpending = categoryViewModel.getCategorySpending(currentMonthTransactions)
        val pieEntries = categorySpending.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val colors = ArrayList<Int>()
        val categories = categoryViewModel.getCategories()

        for (entry in pieEntries) {
            val categoryName = entry.label
            val category = categories.find { it.name == categoryName }
            if (category != null) {
                colors.add(category.color)
            } else {
                colors.add(Color.GRAY)
            }
        }

        val isNightMode = (requireContext().resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val textColor = TypedValue().apply {
            requireContext().theme.resolveAttribute(android.R.attr.textColorPrimary, this, true)
        }.data

        val dataSet = PieDataSet(pieEntries, "")
        dataSet.apply {
            this.colors = colors
            valueTextSize = 10f

            valueTextColor = if (isNightMode) Color.WHITE else Color.WHITE

            valueFormatter = PercentFormatter(binding.pieChart)
            yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
            sliceSpace = 3f
            selectionShift = 8f
            valueTypeface = Typeface.create("poppins_medium", Typeface.BOLD)
        }

        val pieData = PieData(dataSet)

        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false

            isDrawHoleEnabled = true
            holeRadius = 58f
            transparentCircleRadius = 60f

            setHoleColor(Color.TRANSPARENT)

            setCenterTextSize(12f)

            setCenterTextColor(textColor)
            setCenterTextTypeface(Typeface.create("poppins_medium", Typeface.BOLD))
            setCenterText("Monthly\nExpenses")

            legend.isEnabled = false

            setUsePercentValues(true)
            setExtraOffsets(16f, 8f, 0f, 8f)
            setDrawEntryLabels(false)
            isRotationEnabled = false
            setDrawMarkers(false)

            animateY(1000, Easing.EaseInOutQuad)

            invalidate()
        }

        createCustomLegend(categorySpending, colors, textColor)
    }

    private fun createCustomLegend(
        categorySpending: Map<String, Double>,
        colors: ArrayList<Int>,
        textColor: Int
    ) {
        val legendContainer = binding.legendContainer

        legendContainer.removeAllViews()

        val legendLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        val categories = categorySpending.keys.toList()
        for (i in categories.indices) {
            if (i < colors.size) {
                val category = categories[i]
                val color = colors[i]
                val amount = categorySpending[category] ?: 0.0

                val itemLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics
                        ).toInt()
                    }
                    gravity = Gravity.CENTER_VERTICAL
                }

                val colorIndicator = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics
                        ).toInt(),
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics
                        ).toInt()
                    ).apply {
                        marginEnd = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
                        ).toInt()
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(color)
                    }
                }

                val categoryText = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = category
                    setTextColor(textColor)
                    textSize = 8f
                    typeface = Typeface.create("poppins", Typeface.NORMAL)
                }

                itemLayout.addView(colorIndicator)
                itemLayout.addView(categoryText)

                legendLayout.addView(itemLayout)
            }
        }

        legendContainer.addView(legendLayout)
    }

    private fun updateCategoryList(transactions: List<Transaction>) {
        val currentMonthTransactions = transactions.filter {
            val calendar = Calendar.getInstance().apply { time = it.date }
            val currentCalendar = Calendar.getInstance()

            calendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                    calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                    it.type == TransactionType.EXPENSE
        }

        val categorySpending = categoryViewModel.getCategorySpending(currentMonthTransactions)
        val categories = categoryViewModel.getCategories()

        val totalExpenses = currentMonthTransactions.sumOf { it.amount }

        val latestTransactionDateByCategory = currentMonthTransactions
            .groupBy { it.category }
            .mapValues { (_, transactions) ->
                transactions.maxByOrNull { it.date }?.date ?: Date()
            }

        val categoryItems = categories.mapNotNull { category ->
            val amount = categorySpending[category.name] ?: 0.0
            if (amount > 0) {

                val percentOfBudget = if (totalExpenses > 0) {
                    (amount / totalExpenses) * 100
                } else {
                    0.0
                }

                val lasUpdated = latestTransactionDateByCategory[category.name] ?: Date()

                CategorySpendingItem(
                    category.name,
                    amount,
                    category.icon,
                    category.color
                )
            } else null
        }. sortedByDescending { it.amount }

        if (binding.rvCategorySpending.adapter == null) {
            binding.rvCategorySpending.adapter = CategorySpendingAdapter()
            binding.rvCategorySpending.layoutManager = LinearLayoutManager(requireContext())
        }

        (binding.rvCategorySpending.adapter as CategorySpendingAdapter).submitList(categoryItems)
    }

    private fun setupChipAppearance() {
        val incomeChip = binding.chipIncome
        val expenseChip = binding.chipExpense

        val isNightMode = (requireContext().resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val unselectedTextColor = if (isNightMode) {
            Color.parseColor("#E0E0E0")
        } else {
            Color.parseColor("#333333")
        }

        val textColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                Color.WHITE,
                unselectedTextColor
            )
        )

        incomeChip.setTextColor(textColorStateList)
        expenseChip.setTextColor(textColorStateList)

        val strokeColor = ContextCompat.getColor(requireContext(), R.color.primary)

        incomeChip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
        expenseChip.chipStrokeColor = ColorStateList.valueOf(strokeColor)

        incomeChip.chipStrokeWidth = 1f
        expenseChip.chipStrokeWidth = 1f
    }

    private fun updateLineChart(transactions: List<Transaction>) {
        val sixMonthsData = mutableListOf<Entry>()
        val incomeData = mutableListOf<Entry>()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -5)

        val months = mutableListOf<String>()
        val expensesByMonth = mutableListOf<Float>()
        val incomeByMonth = mutableListOf<Float>()

        for (i in 0 until 6) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)

            val monthTransactions = transactions.filter {
                val txCalendar = Calendar.getInstance().apply { time = it.date }
                txCalendar.get(Calendar.YEAR) == year &&
                        txCalendar.get(Calendar.MONTH) == month
            }

            val expenses = monthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
                .toFloat()

            val income = monthTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
                .toFloat()

            months.add(SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time))
            expensesByMonth.add(expenses)
            incomeByMonth.add(income)

            sixMonthsData.add(Entry(i.toFloat(), expenses))
            incomeData.add(Entry(i.toFloat(), income))

            calendar.add(Calendar.MONTH, 1)
        }

        val isNightMode = (requireContext().resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val themeTextColor = TypedValue().apply {
            requireContext().theme.resolveAttribute(android.R.attr.textColorPrimary, this, true)
        }.data

        val gridLineColor = if (isNightMode) {
            Color.parseColor("#22FFFFFF")
        } else {
            Color.parseColor("#22000000")
        }

        val expenseColor = Color.parseColor("#FF5252")
        val incomeColor = Color.parseColor("#4CAF50")

        val expenseDataSet = LineDataSet(sixMonthsData, "Expenses")
        expenseDataSet.apply {
            color = expenseColor
            lineWidth = 2.5f
            mode = LineDataSet.Mode.CUBIC_BEZIER

            setDrawCircles(true)
            circleRadius = 5f
            circleHoleRadius = 2.5f
            circleColors = listOf(expenseColor)

            setDrawFilled(true)
            fillFormatter = IFillFormatter { _, _ -> binding.lineChart.axisLeft.axisMinimum }
            fillAlpha = 50
            fillColor = expenseColor

            valueTextSize = 11f
            setValueTextColor(themeTextColor)
            valueTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

            valueFormatter = object : ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    return if ((entry?.y ?: 0f) > 0) entry?.y?.toInt()?.toString() ?: "0" else ""
                }
            }
        }

        val incomeDataSet = LineDataSet(incomeData, "Income")
        incomeDataSet.apply {
            color = incomeColor
            lineWidth = 2.5f
            mode = LineDataSet.Mode.CUBIC_BEZIER

            setDrawCircles(true)
            circleRadius = 5f
            circleHoleRadius = 2.5f
            circleColors = listOf(incomeColor)

            setDrawFilled(true)
            fillFormatter = IFillFormatter { _, _ -> binding.lineChart.axisLeft.axisMinimum }
            fillAlpha = 50
            fillColor = incomeColor

            valueTextSize = 11f
            setValueTextColor(themeTextColor)
            valueTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

            valueFormatter = object : ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    return if ((entry?.y ?: 0f) > 0) entry?.y?.toInt()?.toString() ?: "0" else ""
                }
            }
        }

        val lineData = LineData(expenseDataSet, incomeDataSet)

        binding.lineChart.apply {
            data = lineData

            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(months)
                granularity = 1f
                setTextColor(themeTextColor)
                textSize = 11f
                gridColor = gridLineColor
                gridLineWidth = 0.8f
                axisLineColor = themeTextColor
                axisLineWidth = 0.8f
                setDrawGridLines(true)
            }

            axisLeft.apply {
                setTextColor(themeTextColor)
                textSize = 11f
                gridColor = gridLineColor
                gridLineWidth = 0.8f
                axisLineColor = themeTextColor
                axisLineWidth = 0.8f
                setDrawGridLines(true)

                axisMinimum = 0f
                spaceTop = 15f
            }

            axisRight.isEnabled = false

            legend.apply {
                setTextColor(themeTextColor)
                textSize = 12f
                form = Legend.LegendForm.CIRCLE
                formSize = 8f
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.HORIZONTAL
                xEntrySpace = 10f
                yEntrySpace = 0f
            }

            val markerView = object : MarkerView(requireContext(), R.layout.custom_marker_view) {
                private val tvContent: TextView = findViewById(R.id.tvContent)

                override fun refreshContent(e: Entry?, highlight: Highlight?) {
                    e?.let {
                        val value = it.y.toInt()
                        val month = months[it.x.toInt()]
                        val isIncome = highlight?.dataSetIndex == 1
                        val type = if (isIncome) "Income" else "Expense"
                        tvContent.text = "$type\n$month: ${value.toCurrencyString()}"
                    }
                    super.refreshContent(e, highlight)
                }

                override fun getOffset(): MPPointF {
                    return MPPointF(-(width / 2f), -height.toFloat())
                }
            }
            marker = markerView

            setExtraOffsets(16f, 16f, 16f, 8f)

            animateX(1200, Easing.EaseInOutQuad)

            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisRight.setDrawGridLines(false)

            isDoubleTapToZoomEnabled = false
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            isHighlightPerTapEnabled = true

            invalidate()
        }

        binding.chipIncome.isChecked = true

        binding.chipIncome.setOnClickListener {
            if (binding.chipIncome.isChecked) {
                binding.chipExpense.isChecked = false

                val newData = LineData(incomeDataSet)
                binding.lineChart.data = newData
                binding.lineChart.animateX(300)
                binding.lineChart.invalidate()
            } else {
                if (!binding.chipExpense.isChecked) {
                    val fullData = LineData(expenseDataSet, incomeDataSet)
                    binding.lineChart.data = fullData
                    binding.lineChart.animateX(300)
                    binding.lineChart.invalidate()
                }
            }
        }

        binding.chipExpense.setOnClickListener {
            if (binding.chipExpense.isChecked) {
                binding.chipIncome.isChecked = false

                val newData = LineData(expenseDataSet)
                binding.lineChart.data = newData
                binding.lineChart.animateX(300)
                binding.lineChart.invalidate()
            } else {
                if (!binding.chipIncome.isChecked) {
                    val fullData = LineData(expenseDataSet, incomeDataSet)
                    binding.lineChart.data = fullData
                    binding.lineChart.animateX(300)
                    binding.lineChart.invalidate()
                }
            }
        }

        binding.chipGroupTrend.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                val fullData = LineData(expenseDataSet, incomeDataSet)
                binding.lineChart.data = fullData
                binding.lineChart.invalidate()
            }
        }
    }

    fun Int.toCurrencyString(): String {
        return "Rs.$this"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}