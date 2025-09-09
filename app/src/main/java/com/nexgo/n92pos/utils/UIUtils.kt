package com.nexgo.n92pos.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView
import android.widget.LinearLayout

object UIUtils {
    
    fun showSuccessSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_LONG) {
        val snackbar = Snackbar.make(view, message, duration)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
        
        // Make text white and center it
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.textSize = 16f
        textView.maxLines = 10 // Allow up to 10 lines
        textView.isSingleLine = false // Allow multiple lines
        textView.ellipsize = null // Remove ellipsis
        
        // Make snackbar expand to show full message
        val layoutParams = snackbarView.layoutParams as? android.widget.FrameLayout.LayoutParams
        layoutParams?.let {
            it.width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            it.height = android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            snackbarView.layoutParams = it
        }
        
        snackbar.show()
    }
    
    fun showErrorSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_INDEFINITE) {
        val snackbar = Snackbar.make(view, message, duration)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.parseColor("#F44336")) // Red
        
        // Make text white and center it
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.textSize = 16f
        textView.maxLines = 10 // Allow up to 10 lines
        textView.isSingleLine = false // Allow multiple lines
        textView.ellipsize = null // Remove ellipsis
        
        // Make snackbar expand to show full message
        val layoutParams = snackbarView.layoutParams as? android.widget.FrameLayout.LayoutParams
        layoutParams?.let {
            it.width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            it.height = android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            snackbarView.layoutParams = it
        }
        
        // Add action to dismiss
        snackbar.setAction("DISMISS") { snackbar.dismiss() }
        snackbar.setActionTextColor(Color.WHITE)
        
        snackbar.show()
    }
    
    fun showWarningSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_LONG) {
        val snackbar = Snackbar.make(view, message, duration)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.parseColor("#FF9800")) // Orange
        
        // Make text white and center it
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.textSize = 16f
        textView.maxLines = 10 // Allow up to 10 lines
        textView.isSingleLine = false // Allow multiple lines
        textView.ellipsize = null // Remove ellipsis
        
        // Make snackbar expand to show full message
        val layoutParams = snackbarView.layoutParams as? android.widget.FrameLayout.LayoutParams
        layoutParams?.let {
            it.width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            it.height = android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            snackbarView.layoutParams = it
        }
        
        snackbar.show()
    }
    
    fun showInfoSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_LONG) {
        val snackbar = Snackbar.make(view, message, duration)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.parseColor("#2196F3")) // Blue
        
        // Make text white and center it
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        textView.textSize = 16f
        textView.maxLines = 10 // Allow up to 10 lines
        textView.isSingleLine = false // Allow multiple lines
        textView.ellipsize = null // Remove ellipsis
        
        // Make snackbar expand to show full message
        val layoutParams = snackbarView.layoutParams as? android.widget.FrameLayout.LayoutParams
        layoutParams?.let {
            it.width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            it.height = android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            snackbarView.layoutParams = it
        }
        
        snackbar.show()
    }
}
