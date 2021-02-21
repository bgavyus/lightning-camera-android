package io.github.bgavyus.lightningcamera.media

class SamplesPipeline(private val stages: Iterable<SamplesProcessor>) : SamplesProcessor {
    override fun process(sample: Sample) {
        stages.forEach { it.process(sample) }
    }
}
