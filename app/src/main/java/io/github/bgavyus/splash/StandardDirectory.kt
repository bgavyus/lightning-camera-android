package io.github.bgavyus.splash

import android.os.Environment

enum class StandardDirectory(val value: String) {
	Music(Environment.DIRECTORY_MUSIC),
	Podcasts(Environment.DIRECTORY_PODCASTS),
	Ringtones(Environment.DIRECTORY_RINGTONES),
	Alarms(Environment.DIRECTORY_ALARMS),
	Notifications(Environment.DIRECTORY_NOTIFICATIONS),
	Pictures(Environment.DIRECTORY_PICTURES),
	Movies(Environment.DIRECTORY_MOVIES),
	Downloads(Environment.DIRECTORY_DOWNLOADS),
	Dcim(Environment.DIRECTORY_DCIM),
	Documents(Environment.DIRECTORY_DOCUMENTS),
	Screenshots(Environment.DIRECTORY_SCREENSHOTS),
	Audiobooks(Environment.DIRECTORY_AUDIOBOOKS)
}
