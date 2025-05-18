package com.example.finly

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.finly.databinding.ActivityAuthBinding
import com.example.finly.ui.AuthPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
    }

    private fun setupViewPager() {
        val authAdapter = AuthPagerAdapter(this)
        binding.viewPager.adapter = authAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) {
            tab, position -> tab.text = if (position == 0) "Login" else "Sign Up"
        }.attach()
    }

    fun navigateToSignUpTab() {
        binding.viewPager.currentItem = 1
    }
    fun navigateToLoginTab() {
        binding.viewPager.currentItem = 0
    }
}