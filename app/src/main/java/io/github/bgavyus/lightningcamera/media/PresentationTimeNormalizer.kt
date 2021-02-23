package io.github.bgavyus.lightningcamera.media

class PresentationTimeNormalizer : SamplesProcessor {
    companion object {
        private const val microsInUnit = 1_000_000
        private const val playbackFps = 5
    }

    private val generator = generateSequence(0L) { it + microsInUnit / playbackFps }.iterator()

    override fun process(sample: Sample) {
        sample.info.presentationTimeUs = generator.next()
    }
}
