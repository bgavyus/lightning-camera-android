#pragma version(1)
#pragma rs java_package_name(io.github.bgavyus.splash.detection)
#pragma rs_fp_relaxed
#pragma rs reduce(detected) accumulator(bitwiseOr) outconverter(isMax)

static void bitwiseOr(uchar4 *accum, uchar4 val) {
    *accum |= val;
}

static void isMax(uchar *result, const uchar4 *accum) {
    *result = accum->x == 255
           && accum->y == 255
           && accum->z == 255;
}
