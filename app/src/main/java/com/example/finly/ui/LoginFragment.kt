package com.example.finly.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.finly.AuthActivity
import com.example.finly.MainActivity
import com.example.finly.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                saveUserSessions()
                navigateToMain()
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(requireContext(), "Forgot password features coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.tvSignup.setOnClickListener {
            (activity as? AuthActivity)?.navigateToSignUpTab()
        }
    }

    private fun validateInputs() : Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        } else {
            binding.tilEmail.error = null
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            return false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        } else {
            binding.tilPassword.error = null
        }

        val sharedPrefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPrefs.getString("user_email", "")
        val savedPassword = sharedPrefs.getString("user_password", "")

        if (savedEmail == null || savedPassword == null) {
            Toast.makeText(requireContext(), "No account found. Please sign up first.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email != savedEmail || password != savedPassword) {
            Toast.makeText(requireContext(), "Invalid email or password", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveUserSessions() {
        val sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("is_logged_in", true)
            putString("user_email", binding.etEmail.text.toString()).apply()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(requireContext(), MainActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}