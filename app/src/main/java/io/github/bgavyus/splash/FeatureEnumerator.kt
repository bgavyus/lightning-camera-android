package io.github.bgavyus.splash

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.RecommendedStreamConfigurationMap
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.util.Printer

object FeatureEnumerator {
	fun cameraDevices(cameraManager: CameraManager, printer: Printer) {
		for (cameraId in cameraManager.cameraIdList) {
			val cc = cameraManager.getCameraCharacteristics(cameraId)
			cc.availableCaptureRequestKeys
			cc.availableCaptureResultKeys
			cc.availablePhysicalCameraRequestKeys
			cc.availableSessionKeys
			cc.keysNeedingPermission
			cc.getRecommendedStreamConfigurationMap(RecommendedStreamConfigurationMap.USECASE_PREVIEW)
			cc.physicalCameraIds

			CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO

			for (key in cc.keys) {
				val value = cc[key]
			}
		}
	}

	fun camcoderProfiles(cameraManager: CameraManager, printer: Printer) {
		data class Quality(val speed: String, val name: String, val id: Int)

		val qualities = listOf(
			Quality("Normal", "Low"  , CamcorderProfile.QUALITY_LOW),
			Quality("Normal", "High" , CamcorderProfile.QUALITY_HIGH),
			Quality("Normal", "QCIF" , CamcorderProfile.QUALITY_QCIF),
			Quality("Normal", "CIF"  , CamcorderProfile.QUALITY_CIF),
			Quality("Normal", "480p" , CamcorderProfile.QUALITY_480P),
			Quality("Normal", "720p" , CamcorderProfile.QUALITY_720P),
			Quality("Normal", "1080p", CamcorderProfile.QUALITY_1080P),
			Quality("Normal", "QVGA" , CamcorderProfile.QUALITY_QVGA),
			Quality("Normal", "2160p", CamcorderProfile.QUALITY_2160P),

			Quality("Time Lapse", "Low"  , CamcorderProfile.QUALITY_TIME_LAPSE_LOW),
			Quality("Time Lapse", "High" , CamcorderProfile.QUALITY_TIME_LAPSE_HIGH),
			Quality("Time Lapse", "QCIF" , CamcorderProfile.QUALITY_TIME_LAPSE_QCIF),
			Quality("Time Lapse", "CIF"  , CamcorderProfile.QUALITY_TIME_LAPSE_CIF),
			Quality("Time Lapse", "480p" , CamcorderProfile.QUALITY_TIME_LAPSE_480P),
			Quality("Time Lapse", "720p" , CamcorderProfile.QUALITY_TIME_LAPSE_720P),
			Quality("Time Lapse", "1080p", CamcorderProfile.QUALITY_TIME_LAPSE_1080P),
			Quality("Time Lapse", "QVGA" , CamcorderProfile.QUALITY_TIME_LAPSE_QVGA),
			Quality("Time Lapse", "2160p", CamcorderProfile.QUALITY_TIME_LAPSE_2160P),

			Quality("High", "Low"  , CamcorderProfile.QUALITY_HIGH_SPEED_LOW),
			Quality("High", "High" , CamcorderProfile.QUALITY_HIGH_SPEED_HIGH),
			Quality("High", "480p" , CamcorderProfile.QUALITY_HIGH_SPEED_480P),
			Quality("High", "720p" , CamcorderProfile.QUALITY_HIGH_SPEED_720P),
			Quality("High", "1080p", CamcorderProfile.QUALITY_HIGH_SPEED_1080P),
			Quality("High", "2160p", CamcorderProfile.QUALITY_HIGH_SPEED_2160P)
		)

		val outputFormats = mapOf(
			MediaRecorder.OutputFormat.DEFAULT to "Default",
			MediaRecorder.OutputFormat.THREE_GPP to "3GPP",
			MediaRecorder.OutputFormat.MPEG_4 to "MPEG-4",
			MediaRecorder.OutputFormat.AMR_NB to "AMR-NB",
			MediaRecorder.OutputFormat.AMR_WB to "AMR-WB",
			MediaRecorder.OutputFormat.AAC_ADTS to "AAC-ADTS",
			MediaRecorder.OutputFormat.MPEG_2_TS to "MPEG-2-TS",
			MediaRecorder.OutputFormat.WEBM to "WEBM",
			MediaRecorder.OutputFormat.OGG to "OGG"
		)

		val videoEncoders = mapOf(
			MediaRecorder.VideoEncoder.DEFAULT to "Default",
			MediaRecorder.VideoEncoder.H263 to "H263",
			MediaRecorder.VideoEncoder.H264 to "H264",
			MediaRecorder.VideoEncoder.MPEG_4_SP to "MPEG-4-SP",
			MediaRecorder.VideoEncoder.VP8 to "VP8",
			MediaRecorder.VideoEncoder.HEVC to "HEVC"
		)

		val audioEncoders = mapOf(
			MediaRecorder.AudioEncoder.DEFAULT to "Default",
			MediaRecorder.AudioEncoder.AMR_NB to "AMR-NB",
			MediaRecorder.AudioEncoder.AMR_WB to "AMR-WB",
			MediaRecorder.AudioEncoder.AAC to "AAC",
			MediaRecorder.AudioEncoder.HE_AAC to "HE-AAC",
			MediaRecorder.AudioEncoder.AAC_ELD to "AAC-ELD",
			MediaRecorder.AudioEncoder.VORBIS to "Ogg Vorbis",
			MediaRecorder.AudioEncoder.OPUS to "Opus"
		)

		printer.println(listOf(
			"Camera ID",
			"Speed",
			"Quality",
			"Duration",
			"File Format",
			"Video Codec",
			"Video Bit Rate",
			"Video Frame Rate",
			"Video Frame Width",
			"Video Frame Height",
			"Audio Codec",
			"Audio Bit Rate",
			"Audio Sample Rate",
			"Audio Channels"
		).joinToString(","))

		for (cameraId in cameraManager.cameraIdList.map(String::toInt)) {
			for (quality in qualities) {
				try {
					val profile = CamcorderProfile.get(cameraId, quality.id)

					printer.println(listOf(
						cameraId,
						quality.speed,
						quality.name,
						profile.duration,
						outputFormats[profile.fileFormat],
						videoEncoders[profile.videoCodec],
						profile.videoBitRate,
						profile.videoFrameRate,
						profile.videoFrameWidth,
						profile.videoFrameHeight,
						audioEncoders[profile.audioCodec],
						profile.audioBitRate,
						profile.audioSampleRate,
						profile.audioChannels
					).joinToString(","))
				}

				catch (ex: RuntimeException) { }
			}
		}
	}
}
