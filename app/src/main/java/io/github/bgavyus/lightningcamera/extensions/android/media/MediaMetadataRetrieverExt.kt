package io.github.bgavyus.lightningcamera.extensions.android.media

import android.media.MediaMetadataRetriever

inline operator fun <reified T> MediaMetadataRetriever.get(keyCode: Int): T {
    val value = requireExtractMetadata(keyCode)

    return when (T::class) {
        String::class -> value
        Int::class -> value.toInt()
        Long::class -> value.toLong()
        Float::class -> value.toFloat()
        Double::class -> value.toDouble()
        else -> throw IllegalArgumentException()
    } as T
}

fun MediaMetadataRetriever.requireExtractMetadata(keyCode: Int) =
    extractMetadata(keyCode) ?: throw RuntimeException()

@Suppress("unused")
fun MediaMetadataRetriever.toMap() = listOf(
    "cd_track_number",
    "album",
    "artist",
    "author",
    "composer",
    "date",
    "genre",
    "title",
    "year",
    "duration",
    "num_tracks",
    "writer",
    "mimetype",
    "albumartist",
    "disc_number",
    "compilation",
    "has_audio",
    "has_video",
    "video_width",
    "video_height",
    "bitrate",
    "timed_text_languages",
    "is_drm",
    "location",
    "video_rotation",
    "capture_framerate",
    "has_image",
    "image_count",
    "image_primary",
    "image_width",
    "image_height",
    "image_rotation",
    "video_frame_count",
    "exif_offset",
    "exif_length",
    "color_standard",
    "color_transfer",
    "color_range",
    "samplerate",
    "bits_per_sample",
)
    .mapIndexed { code, name -> name to extractMetadata(code) }
    .toMap()
