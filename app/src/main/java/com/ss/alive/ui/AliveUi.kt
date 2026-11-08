package com.ss.alive.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import android.content.Context
import android.view.ViewGroup

object AliveUi {
    val background = Color.rgb(8, 13, 24)
    val surface = Color.rgb(17, 26, 43)
    val surfaceRaised = Color.rgb(23, 35, 56)
    val blue = Color.rgb(77, 145, 235)
    val blueSoft = Color.rgb(37, 68, 108)
    val text = Color.rgb(239, 244, 252)
    val muted = Color.rgb(157, 174, 198)
    val border = Color.rgb(43, 59, 82)
    val success = Color.rgb(111, 196, 157)

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun page(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(context, 20))
        setBackgroundColor(background)
    }

    fun scrollPage(context: Context): Pair<android.widget.ScrollView, LinearLayout> {
        val content = page(context)
        val scroll = android.widget.ScrollView(context).apply {
            fillViewport = true
            setBackgroundColor(background)
            isVerticalScrollBarEnabled = false
            addView(content, ViewGroup.LayoutParams(-1, -2))
        }
        return scroll to content
    }

    fun heading(context: Context, value: String, size: Float = 26f): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(text)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

    fun body(context: Context, value: String, size: Float = 14f): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(muted)
            setLineSpacing(dp(context, 2).toFloat(), 1f)
        }

    fun label(context: Context, value: String): TextView =
        TextView(context).apply {
            text = value.uppercase()
            textSize = 12f
            letterSpacing = 0.08f
            setTextColor(muted)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

    fun card(context: Context, content: LinearLayout, raised: Boolean = false): MaterialCardView =
        MaterialCardView(context).apply {
            radius = dp(context, 18).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(if (raised) surfaceRaised else surface)
            strokeColor = border
            strokeWidth = dp(context, 1)
            setContentPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
            addView(content, ViewGroup.LayoutParams(-1, -2))
            val params = LinearLayout.LayoutParams(-1, -2)
            params.bottomMargin = dp(context, 14)
            layoutParams = params
        }

    fun column(context: Context, padding: Int = 0): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        if (padding > 0) setPadding(dp(context, padding))
    }

    fun row(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun button(context: Context, value: String, primary: Boolean = true, onClick: () -> Unit): MaterialButton =
        MaterialButton(context).apply {
            text = value
            isAllCaps = false
            textSize = 14f
            cornerRadius = dp(context, 13)
            insetTop = 0
            insetBottom = 0
            minimumHeight = dp(context, 48)
            setTextColor(if (primary) Color.WHITE else text)
            backgroundTintList = ColorStateList.valueOf(if (primary) blue else surfaceRaised)
            if (!primary) strokeColor = ColorStateList.valueOf(border)
            if (!primary) strokeWidth = dp(context, 1)
            setOnClickListener { onClick() }
        }

    fun gap(context: Context, height: Int = 10): View =
        View(context).apply { layoutParams = LinearLayout.LayoutParams(1, dp(context, height)) }

    fun addButtonRow(context: Context, parent: LinearLayout, buttons: List<MaterialButton>) {
        val row = row(context)
        buttons.forEachIndexed { index, button ->
            val params = LinearLayout.LayoutParams(0, dp(context, 48), 1f)
            if (index > 0) params.marginStart = dp(context, 8)
            row.addView(button, params)
        }
        parent.addView(row)
    }
}
