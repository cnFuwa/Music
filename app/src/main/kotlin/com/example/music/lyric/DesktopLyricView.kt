package cn.primeessence.music

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView


class DesktopLyricView(context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val textView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 18f
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
        gravity = Gravity.CENTER
        setPadding(24, 12, 24, 12)
        setBackgroundColor(Color.argb(90, 0, 0, 0))
    }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = 120
    }

    var showing: Boolean = false
        private set

    fun show() {
        if (showing) return
        windowManager.addView(textView, params)
        showing = true
    }

    fun hide() {
        if (!showing) return
        runCatching { windowManager.removeView(textView) }
        showing = false
    }

    fun setText(text: String?) {
        textView.text = text?.takeIf { it.isNotBlank() } ?: "— 暂无歌词 —"
    }
}
