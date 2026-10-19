/**
 * Metrolist Project (C) 2026
 * Kings Play changes: loads a cover picture for each Explore card
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.MoodAndGenres
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class MoodAndGenresViewModel
@Inject
constructor() : ViewModel() {
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)

    /** Cover picture for each card, keyed by [coverKey]. */
    val covers = MutableStateFlow<Map<String, String>>(emptyMap())

    private val requested = mutableSetOf<String>()
    private val coverLimit = Semaphore(4) // load at most 4 covers at the same time

    init {
        viewModelScope.launch {
            YouTube
                .moodAndGenres()
                .onSuccess {
                    moodAndGenres.value = it
                }.onFailure {
                    reportException(it)
                }
        }
    }

    /** Called when a card appears on screen. Loads its cover once. */
    fun loadCover(item: MoodAndGenres.Item) {
        val key = coverKey(item)
        if (!requested.add(key)) return
        viewModelScope.launch {
            coverLimit.withPermit {
                YouTube
                    .browse(item.endpoint.browseId, item.endpoint.params)
                    .onSuccess { result ->
                        val thumbnail = result.items
                            .asSequence()
                            .flatMap { it.items.asSequence() }
                            .mapNotNull { it.thumbnail }
                            .firstOrNull()
                        if (thumbnail != null) {
                            covers.update { it + (key to thumbnail.resize(544, 544)) }
                        }
                    }
                // If it fails, the card simply keeps its color (no picture)
            }
        }
    }

    companion object {
        fun coverKey(item: MoodAndGenres.Item) = "${item.endpoint.browseId}|${item.endpoint.params}"
    }
}
