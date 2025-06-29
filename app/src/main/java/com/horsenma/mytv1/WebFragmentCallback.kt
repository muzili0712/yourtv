package com.horsenma.mytv1

interface WebFragmentCallback {
    fun onPlaybackStarted()
    fun onPlaybackStopped()
    fun onPlaybackError(error: String)
}