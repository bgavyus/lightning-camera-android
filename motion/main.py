import argparse

import tensorflow as tf

image_spec = tf.TensorSpec(shape=[None], dtype=tf.uint8)


class MotionModel(tf.Module):
    @tf.function(input_signature=[image_spec, image_spec])
    def __call__(self, current_image, last_image):
        float_images = [tf.image.convert_image_dtype(image, dtype=tf.float32) for image in [current_image, last_image]]
        maximum = tf.maximum(*float_images)
        minimum = tf.minimum(*float_images)
        subtract = tf.subtract(maximum, minimum)
        return tf.reduce_sum(subtract, keepdims=True)


def main():
    args = parse_args()
    model = MotionModel()
    concrete_func = model.__call__.get_concrete_function()
    converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_func], model)
    serialized_model = converter.convert()

    with open(args.output, 'wb') as model_file:
        model_file.write(serialized_model)


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('output')
    return parser.parse_args()


if __name__ == '__main__':
    main()
