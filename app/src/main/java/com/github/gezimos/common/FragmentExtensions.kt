package com.github.gezimos.common

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Fragment.showLongToast(message: String) {
    // Use a null-safe context and always post toast display to the main thread
    context?.let { ctx ->
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
        }
    }
}

fun Fragment.showShortToast(message: String) {
    // Use a null-safe context and always post toast display to the main thread
    context?.let { ctx ->
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
        }
    }
}

@SuppressLint("ServiceCast")
fun Fragment.hideKeyboard() {
    val inputMethodManager =
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = activity?.currentFocus ?: View(requireContext()) // Use current focus or a new view
    if (inputMethodManager.isActive) { // Check if IME is active before hiding
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
