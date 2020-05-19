#pragma version(1)
#pragma rs java_package_name(io.github.bgavyus.splash.graphics.detection)
#pragma rs_fp_relaxed
#pragma rs reduce(rate) accumulator(addDistance) combiner(sum)

static void addDistance(float *accum, uchar4 a, uchar4 b) {
    uchar4 d = max(a, b) - min(a, b);
    *accum += d.x + d.y + d.z;
}

static void sum(float *accum, const float *val) {
    *accum += *val;
}
