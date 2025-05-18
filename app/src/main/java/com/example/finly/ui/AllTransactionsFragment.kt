package com.example.finly.ui

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finly.R
import com.example.finly.databinding.FragmentAllTransactionsBinding
import com.example.finly.models.Transaction
import com.example.finly.models.TransactionType
import com.example.finly.viewmodels.TransactionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AllTransactionsFragment : Fragment() {
    private var _binding: FragmentAllTransactionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var transactionAdapter: AllTransactionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionViewModel = ViewModelProvider(requireActivity())[TransactionViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        loadAllTransactions()
    }

    private fun setupToolbar() {
        binding.toolbar.navigationIcon = null
        view?.findViewById<TextView>(R.id.tv_toolbar_title)?.text = getString(R.string.all_transactions)

        view?.findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = AllTransactionsAdapter(
            onDeleteClicked = { transaction ->
                showDeleteConfirmationDialog(transaction)
            },
            onEditClicked = { transaction ->
                showEditTransactionDialog(transaction)
            }
        )

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = transactionAdapter.currentList[position]
                showDeleteConfirmationDialog(transaction)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                // Skip drawing if the swipe distance is zero
                if (dX == 0f) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    return
                }

                // Define colors - use more modern colors
                val isNightMode = (requireContext().resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                val deleteColor = if (isNightMode) {
                    Color.parseColor("#CF6679") // Material Design dark theme error color
                } else {
                    Color.parseColor("#F44336") // Material Design red 500 - brighter for modern look
                }

                // Set up the background with rounded corners on the left side
                val background = GradientDrawable()
                background.shape = GradientDrawable.RECTANGLE
                background.setColor(deleteColor)

                // Apply corner radius only to left side (if swiping right-to-left)
                if (dX < 0) {
                    background.cornerRadii = floatArrayOf(
                        0f, 0f,         // top-left
                        24f, 24f,       // top-right
                        24f, 24f,       // bottom-right
                        0f, 0f          // bottom-left
                    )
                }

                // Draw the background
                if (dX < 0) {
                    val backgroundLeft = itemView.right + dX.toInt()
                    background.setBounds(
                        backgroundLeft,
                        itemView.top + 8,  
                        itemView.right,
                        itemView.bottom - 8
                    )
                }


                background.draw(c)


                val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
                deleteIcon?.setTint(Color.WHITE)


                val iconSize = itemView.height / 4
                val iconMargin = (itemView.height - iconSize) / 2


                val swipeFraction = Math.min(1f, Math.abs(dX) / (itemView.width / 2f))
                val rotationAngle = swipeFraction * 30f
                val scaleFactor = 0.8f + (swipeFraction * 0.2f)

                if (dX < 0) {
                    val iconLeft = itemView.right - iconMargin - iconSize
                    val iconRight = itemView.right - iconMargin
                    val iconTop = itemView.top + (itemView.height - iconSize) / 2
                    val iconBottom = iconTop + iconSize

                    c.save()

                    val iconCenterX = (iconLeft + iconRight) / 2f
                    val iconCenterY = (iconTop + iconBottom) / 2f

                    c.translate(iconCenterX, iconCenterY)
                    c.rotate(rotationAngle)
                    c.scale(scaleFactor, scaleFactor)
                    c.translate(-iconCenterX, -iconCenterY)

                    deleteIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    deleteIcon?.draw(c)

                    c.restore()


                    val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 30f
                        typeface = Typeface.create("poppins_medium", Typeface.NORMAL)
                        textAlign = Paint.Align.RIGHT
                        isAntiAlias = true
                        isDither = true
                        isFilterBitmap = true
                        isSubpixelText = true
                    }

                    val textY = (itemView.top + itemView.height / 2 + (textPaint.textSize / 4)).toInt()
                    val textX = iconLeft - 30

                    c.drawText("Delete", textX.toFloat(), textY.toFloat(), textPaint)
                }

                val alpha = 1.0f - Math.abs(dX) / itemView.width.toFloat()
                itemView.alpha = alpha
                itemView.translationX = dX
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.rvTransactions)

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            setHasFixedSize(true)
        }
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        val dialog = AddTransactionDialog.newInstance(transaction)
        dialog.show(parentFragmentManager, "EditTransactionDialog")
    }

    private fun setupFilterChips() {
        binding.chipAll.isChecked = true
        updateChipAppearance(binding.chipAll.id)

        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val checkedChipId = checkedIds.first()
            updateChipAppearance(checkedChipId)

            when (checkedChipId) {
                R.id.chip_all -> loadAllTransactions()
                R.id.chip_income -> loadFilteredTransactions(TransactionType.INCOME)
                R.id.chip_expense -> loadFilteredTransactions(TransactionType.EXPENSE)
            }
        }
    }

    private fun updateChipAppearance(selectedChipId: Int) {
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val allChip = binding.chipAll
        val incomeChip = binding.chipIncome
        val expenseChip = binding.chipExpense

        allChip.setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT))
        incomeChip.setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT))
        expenseChip.setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT))

        val unselectedTextColor = if (isDarkMode)
            ColorUtils.setAlphaComponent(Color.WHITE, 179)
        else
            ColorUtils.setAlphaComponent(Color.BLACK, 179)

        allChip.setTextColor(unselectedTextColor)
        incomeChip.setTextColor(unselectedTextColor)
        expenseChip.setTextColor(unselectedTextColor)

        val selectedChip = when (selectedChipId) {
            R.id.chip_all -> allChip
            R.id.chip_income -> incomeChip
            R.id.chip_expense -> expenseChip
            else -> allChip
        }

        val accentColor = ContextCompat.getColor(requireContext(), R.color.accent)
        selectedChip.setChipBackgroundColor(ColorStateList.valueOf(accentColor))

        selectedChip.setTextColor(Color.WHITE)
    }

    private fun loadAllTransactions() {
        transactionViewModel.getAllTransactions().observe(viewLifecycleOwner) { transactions ->
            updateTransactionsList(transactions)
        }
    }

    private fun loadFilteredTransactions(type: TransactionType) {
        transactionViewModel.getTransactionsByType(type).observe(viewLifecycleOwner) { transactions ->
            updateTransactionsList(transactions)
        }
    }

    private fun updateTransactionsList(transactions: List<Transaction>) {
        val sortedTransactions = transactions.sortedByDescending { it.date }

        transactionAdapter.submitList(sortedTransactions)

        if (transactions.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvTransactions.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvTransactions.visibility = View.VISIBLE
        }
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        // Check if we're in dark mode
        val isNightMode = (requireContext().resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Create the dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                transactionViewModel.deleteTransaction(transaction)
                Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                transactionAdapter.notifyDataSetChanged()
            }
            .setCancelable(false)
            .create()

        // Show the dialog first
        dialog.show()

        // Then customize button colors after the dialog is shown
        if (isNightMode) {
            // In dark mode, use white for the buttons
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.WHITE)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.WHITE)
        } else {
            // In light mode, use your app's accent color for a branded look
            val accentColor = ContextCompat.getColor(requireContext(), R.color.accent)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(accentColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(accentColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}