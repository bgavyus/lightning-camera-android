#pragma version(1)
#pragma rs java_package_name(io.github.bgavyus.splash.detection)
#pragma rs_fp_relaxed
#pragma rs reduce(intensity) accumulator(bitwiseOr)

static void bitwiseOr(uchar4 *accum, uchar4 val) {
    *accum |= val;
}
