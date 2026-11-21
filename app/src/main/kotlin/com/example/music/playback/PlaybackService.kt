package cn.primeessence.music

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var player: ExoPlayer
    private var session: MediaSession? = null

    private val modeGuard = object : Player.Listener {
        override fun onRepeatModeChanged(repeatMode: Int) {
            if (player.repeatMode != Player.REPEAT_MODE_ALL) player.repeatMode = Player.REPEAT_MODE_ALL
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (player.shuffleModeEnabled) player.shuffleModeEnabled = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.shuffleModeEnabled = false
        player.addListener(modeGuard)
        session = MediaSession.Builder(this, player).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        player.removeListener(modeGuard)
        session?.release()
        session = null
        super.onDestroy()
    }
}
