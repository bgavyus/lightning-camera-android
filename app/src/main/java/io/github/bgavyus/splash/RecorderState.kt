package io.github.bgavyus.splash

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
