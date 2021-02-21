package io.github.bgavyus.lightningcamera.utilities

@JvmInline
value class OptionSet(val mask: Int = 0) : Set<Int> {
    override val size get() = mask.countOneBits()
    override fun isEmpty() = mask == 0
    override fun contains(element: Int) = mask and element == element
    override fun containsAll(elements: Collection<Int>) = elements.fold(0, Int::or) in this

    override fun iterator() = generateSequence(1) { it shl 1 }
        .takeWhile { it <= mask }
        .filter(::contains)
        .iterator()
}
