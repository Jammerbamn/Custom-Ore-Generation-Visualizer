package com.example.cogveins;

import java.util.Random;

final class PDist {
    enum Type {
        CONSTANT,
        UNIFORM,
        NORMAL
    }

    final double mean;
    final double range;
    final Type type;

    PDist(double mean, double range) {
        this(mean, range, range == 0.0 ? Type.CONSTANT : Type.UNIFORM);
    }

    PDist(double mean, double range, Type type) {
        this.mean = mean;
        this.range = range;
        this.type = type;
    }

    double next(Random random) {
        if (type == Type.CONSTANT || range == 0.0) {
            return mean;
        }
        if (type == Type.NORMAL) {
            double value = random.nextGaussian() / 2.5;
            if (value < -1.0) {
                value = -1.0;
            } else if (value > 1.0) {
                value = 1.0;
            }
            return mean + value * range;
        }
        return mean + (random.nextDouble() * 2.0 - 1.0) * range;
    }

    int nextInt(Random random) {
        double value = next(random);
        int whole = (int) value;
        double fraction = value - whole;
        if (fraction > 0.0 && fraction > random.nextFloat()) {
            whole++;
        } else if (fraction < 0.0 && -fraction > random.nextFloat()) {
            whole--;
        }
        return whole;
    }

    double getMax() {
        return mean + Math.abs(range);
    }
}
