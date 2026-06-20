package com.example.cogveins;

final class Vec3 {
    final double x;
    final double y;
    final double z;

    Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    Vec3 scale(double amount) {
        return new Vec3(x * amount, y * amount, z * amount);
    }

    double distanceSquared(Vec3 other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    static Vec3 fromYawPitch(double yaw, double pitch) {
        double cp = Math.cos(pitch);
        return new Vec3(Math.sin(yaw) * cp, Math.sin(pitch), Math.cos(yaw) * cp);
    }
}
