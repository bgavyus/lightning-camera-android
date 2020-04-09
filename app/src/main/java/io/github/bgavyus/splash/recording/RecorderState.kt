package io.github.bgavyus.splash.recording

enum class RecorderState {
    Initial,
    Initialized,
    DataSourceConfigured,
    Prepared,
    Recording,
    Released,
    Error,
    Paused
}
