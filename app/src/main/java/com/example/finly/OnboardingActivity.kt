package com.example.finly

import android.content.Context
import android.content.Intent
import android.icu.text.Transliterator.Position
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.util.TypedValueCompat.dpToPx
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.viewpager2.widget.ViewPager2
import com.example.finly.databinding.ActivityOnboardingBinding
import com.example.finly.ui.OnboardingAdapter

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var onboardingAdapter : OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupOnboarding()
        setupIndicators()
        setupButtons()
    }

    private fun setupOnboarding() {
        onboardingAdapter = OnboardingAdapter(this)
        binding.onboardingViewpager.adapter = onboardingAdapter

        binding.onboardingViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)

                if (position == onboardingAdapter.itemCount - 1) {
                    binding.btnNext.text = "Get Started"
                    binding.btnSkip.visibility = View.GONE
                } else {
                    binding.btnNext.text = "Next"
                    binding.btnSkip.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupIndicators() {
        binding.indicatorsContainer.removeAllViews()

        for (i in 0 until onboardingAdapter.itemCount) {
            val indicator = View(this).apply {
                id = View.generateViewId()

                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(24),
                    dpToPx(4)
                ).apply {
                    marginStart = dpToPx(4)
                    marginEnd = dpToPx(4)
                }

                setBackgroundColor(
                    ContextCompat.getColor(
                        this@OnboardingActivity,
                        if (i == 0) R.color.secondary else R.color.background_secondary
                    )
                )
            }
            binding.indicatorsContainer.addView(indicator)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setCurrentIndicator(position: Int) {
        for (i in 0 until binding.indicatorsContainer.childCount) {
            val indicator = binding.indicatorsContainer.getChildAt(i) as View

            indicator.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    if (i == position) R.color.secondary else R.color.background_secondary
                )
            )
        }
    }

    private fun setupButtons() {
        binding.btnSkip.setOnClickListener {
            navigateToAuth()
        }
        binding.btnNext.setOnClickListener {
            val currentPosition = binding.onboardingViewpager.currentItem
            if (currentPosition < onboardingAdapter.itemCount - 1) {
                binding.onboardingViewpager.currentItem = currentPosition + 1
            } else {
                navigateToAuth()
            }
        }
    }

    private fun navigateToAuth() {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("onboarding_completed", true).apply()

        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}