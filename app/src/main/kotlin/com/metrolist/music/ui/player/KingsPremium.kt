/**
 * Kings Play — based on Metrolist (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * "Premium player": calm white/glass buttons, thin slider, clean song titles,
 * smooth title transitions and a light tap feel on play/pause.
 */

package com.metrolist.music.ui.player

import androidx.datastore.preferences.core.booleanPreferencesKey

/** Saved setting: premium player look (on by default). */
val KingsPremiumPlayerKey = booleanPreferencesKey("kingsPremiumPlayer")

private val TITLE_JUNK = listOf(
    // (Official Video), [Official Music Video], (Official Audio), (Lyric Video), (Lyrics), (Visualizer) ...
    Regex(
        """\s*[\(\[]\s*(official\s*)?(music\s*|lyric(al)?\s*|audio\s*|hd\s*|4k\s*)?(video|audio|lyrics?|visuali[sz]er|mv|full\s*song|full\s*video)(\s*song)?\s*[\)\]]""",
        RegexOption.IGNORE_CASE,
    ),
    // (Official Video Song), (Official) ...
    Regex("""\s*[\(\[]\s*official[^\)\]]*[\)\]]""", RegexOption.IGNORE_CASE),
    // | 7clouds Hindi, | T-Series ... (anything after a pipe)
    Regex("""\s*\|.*$"""),
    // - Lyrics / - Official Video at the end
    Regex("""\s*[-–]\s*(official\s*)?(lyrics?|lyric\s*video|audio|video)\s*$""", RegexOption.IGNORE_CASE),
)

/** "Kal Ho Naa Ho - (Lyrics) | 7clouds Hindi" -> "Kal Ho Naa Ho" */
fun cleanSongTitle(title: String): String {
    var t = title
    TITLE_JUNK.forEach { t = t.replace(it, "") }
    t = t.trim().trimEnd('-', '–', '|', ':').trim()
    return t.ifBlank { title }
}
