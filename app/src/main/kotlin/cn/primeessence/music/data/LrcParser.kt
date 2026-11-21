package cn.primeessence.music

import kotlin.math.max

data class LyricLine(val timeMs: Long, val text: String)

object LrcParser {
    private val lineRegex = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    fun parse(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        for (raw in content.lineSequence()) {
            val matches = lineRegex.findAll(raw).toList()
            if (matches.isEmpty()) continue
            val text = raw.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = min * 60_000 + sec * 1000 +
                    when (frac.length) {
                        0 -> 0
                        1 -> frac.toLong() * 100
                        2 -> frac.toLong() * 10
                        else -> frac.take(3).toLong()
                    }
                if (text.isNotEmpty()) lines += LyricLine(ms, text)
            }
        }
        if (lines.isEmpty()) {
            val plain = content.lines().firstOrNull { it.isNotBlank() }?.trim()
            if (plain != null) lines += LyricLine(0, plain)
        }
        return lines.sortedBy { it.timeMs }
    }


    fun indexAt(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var lo = 0
        var hi = lines.lastIndex
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) lo = mid + 1 else hi = mid - 1
        }
        return max(-1, lo - 1)
    }
}
