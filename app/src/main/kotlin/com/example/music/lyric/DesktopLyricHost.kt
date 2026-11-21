package cn.primeessence.music

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DesktopLyricHost @Inject constructor(@ApplicationContext private val context: Context) {

    private val view by lazy { DesktopLyricView(context) }

    fun show() = view.show()
    fun hide() = view.hide()
    fun setText(text: String?) = view.setText(text)
}
