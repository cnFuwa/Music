package cn.primeessence.music

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val uri: Uri,
    val name: String,
    val baseName: String,
    val parentNames: List<String> = emptyList(),
) : Parcelable {
    val title: String get() = baseName
}
