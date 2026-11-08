package com.ss.alive.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

object AliveUi {
    // Minimal dark palette: true black canvas, quiet charcoal surfaces, one clear accent.
    val background = Color.rgb(0, 0, 0)
    val surface = Color.rgb(16, 17, 19)
    val surfaceRaised = Color.rgb(25, 27, 31)
    val blue = Color.rgb(105, 160, 255)
    val blueSoft = Color.rgb(29, 48, 76)
    val text = Color.rgb(246, 247, 249)
    val muted = Color.rgb(157, 163, 174)
    val border = Color.rgb(39, 42, 48)
    val success = Color.rgb(115, 205, 157)

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun page(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(context, 20))
        setBackgroundColor(AliveUi.background)
    }

    fun scrollPage(context: Context): Pair<android.widget.ScrollView, LinearLayout> {
        val content = page(context)
        val scroll = android.widget.ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(AliveUi.background)
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(content, ViewGroup.LayoutParams(-1, -2))
        }
        return scroll to content
    }

    fun heading(context: Context, value: String, size: Float = 26f): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(AliveUi.text)
            gravity = Gravity.START
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

    fun body(context: Context, value: String, size: Float = 14f): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(AliveUi.muted)
            gravity = Gravity.START
            setLineSpacing(dp(context, 3).toFloat(), 1f)
        }

    fun label(context: Context, value: String): TextView =
        TextView(context).apply {
            text = value.uppercase()
            textSize = 11f
            letterSpacing = 0.12f
            setTextColor(AliveUi.blue)
            gravity = Gravity.START
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

    fun card(context: Context, content: LinearLayout, raised: Boolean = false): MaterialCardView =
        MaterialCardView(context).apply {
            radius = dp(context, 16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(if (raised) surfaceRaised else surface)
            strokeColor = border
            strokeWidth = dp(context, 1)
            setContentPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
            addView(content, ViewGroup.LayoutParams(-1, -2))
            val params = LinearLayout.LayoutParams(-1, -2)
            params.bottomMargin = dp(context, 12)
            layoutParams = params
        }

    fun column(context: Context, padding: Int = 0): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.START
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
            letterSpacing = 0f
            cornerRadius = dp(context, 12)
            insetTop = 0
            insetBottom = 0
            minimumHeight = dp(context, 48)
            setTextColor(if (primary) Color.rgb(8, 13, 22) else AliveUi.text)
            backgroundTintList = ColorStateList.valueOf(if (primary) blue else surfaceRaised)
            if (!primary) {
                strokeColor = ColorStateList.valueOf(border)
                strokeWidth = dp(context, 1)
            }
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