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
    // S•S Alive: pure black canvas with restrained midnight-blue surfaces.
    val background = Color.rgb(0, 0, 0)
    val surface = Color.rgb(7, 13, 25)
    val surfaceRaised = Color.rgb(11, 21, 39)
    val blue = Color.rgb(91, 145, 220)
    val blueSoft = Color.rgb(18, 39, 68)
    val text = Color.rgb(240, 244, 250)
    val muted = Color.rgb(137, 151, 172)
    val border = Color.rgb(24, 43, 68)
    val success = Color.rgb(105, 177, 153)

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

    fun menuTile(
        context: Context,
        title: String,
        description: String,
        onClick: () -> Unit
    ): MaterialCardView = MaterialCardView(context).apply {
        radius = dp(context, 18).toFloat()
        cardElevation = 0f
        setCardBackgroundColor(surface)
        strokeColor = border
        strokeWidth = dp(context, 1)
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }

        val line = row(context)
        line.setPadding(dp(context, 18), dp(context, 17), dp(context, 16), dp(context, 17))
        val words = column(context)
        words.addView(heading(context, title, 17f))
        words.addView(gap(context, 3))
        words.addView(body(context, description, 12f))
        line.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
        val arrow = TextView(context).apply {
            text = "›"
            textSize = 30f
            setTextColor(blue)
            gravity = Gravity.CENTER
        }
        line.addView(arrow, LinearLayout.LayoutParams(dp(context, 28), -2))
        addView(line, ViewGroup.LayoutParams(-1, -2))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(context, 11)
        }
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