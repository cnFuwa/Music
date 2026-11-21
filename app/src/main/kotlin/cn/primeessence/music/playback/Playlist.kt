package cn.primeessence.music

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(uri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(baseName).build())
    .build()
