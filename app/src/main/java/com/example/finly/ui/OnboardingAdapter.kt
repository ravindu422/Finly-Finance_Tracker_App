package com.example.finly.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.finly.R

class OnboardingAdapter(private val context: Context) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> (){

    private val layouts = listOf(
        R.layout.onboarding_page_1,
        R.layout.onboarding_page_2
    )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OnboardingViewHolder {
        return OnboardingViewHolder(
            LayoutInflater.from(context).inflate(
                layouts[viewType], parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        //no need implementation
    }
    override fun getItemCount(): Int = layouts.size

    override fun getItemViewType(position: Int): Int = position

    class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view)
}