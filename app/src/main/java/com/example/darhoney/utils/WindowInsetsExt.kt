package com.example.darhoney.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applyStatusBarPadding(extraDp: Int = 8) {
    val basePaddingTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        val extraPx = (extraDp * view.resources.displayMetrics.density).toInt()
        view.setPadding(
            view.paddingLeft,
            basePaddingTop + statusBars.top + extraPx,
            view.paddingRight,
            view.paddingBottom
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applyNavigationBarPadding() {
    val basePaddingBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            basePaddingBottom + navBars.bottom
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
