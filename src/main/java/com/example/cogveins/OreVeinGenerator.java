package com.example.cogveins;

import java.util.Random;

final class OreVeinGenerator {
    private final VeinDefinition def;
    private final double[] cumulativeOreWeights;
    private final double cappedOreWeightTotal;

    OreVeinGenerator(VeinDefinition def) {
        this.def = def;
        this.def.ensureOreBlocks();
        this.cumulativeOreWeights = new double[this.def.oreBlocks.size()];
        double total = 0.0;
        for (int i = 0; i < this.def.oreBlocks.size(); i++) {
            total += Math.max(0.0, this.def.oreBlocks.get(i).weight);
            this.cumulativeOreWeights[i] = total;
        }
        this.cappedOreWeightTotal = Math.min(1.0, total);
    }

    OreVolume generate(long seed) {
        return generateReport(seed).volume;
    }

    VeinGenerationReport generateReport(long seed) {
        Random random = new Random(seed);
        return generateReport(random);
    }

    OreVolume generateAfterCogPosition(Random random) {
        random.nextFloat();
        random.nextFloat();
        return generateReport(random).volume;
    }

    VeinGenerationReport generateReportForCogPosition(long structureSeed) {
        Random random = new Random(structureSeed);
        random.nextFloat();
        random.nextFloat();
        return generateReport(random);
    }

    private VeinGenerationReport generateReport(Random random) {
        OreVolume volume = new OreVolume(def.sizeX, def.sizeY, def.sizeZ);
        VeinGenerationReport report = new VeinGenerationReport(volume);

        if ("Cloud".equals(def.distributionType)) {
            generateCloud(volume, report, random);
            updateOreBounds(report);
            return report;
        }

        Vec3 motherlode = new Vec3(def.motherlodeX, clamp(def.motherlodeHeight.next(random), 0, def.sizeY - 1), def.motherlodeZ);
        double motherlodeRadius = Math.max(0.5, def.motherlodeSize.next(random));
        report.motherlodeCenter = motherlode;
        report.motherlodeRadius = motherlodeRadius;
        fillSphere(volume, motherlode, motherlodeRadius, random);

        int branches = Math.max(1, def.branchFrequency.nextInt(random));
        report.primaryBranches = branches;
        for (int i = 0; i < branches; i++) {
            Random branchRandom = new Random(random.nextLong());
            double yaw = branchRandom.nextDouble() * Math.PI * 2.0;
            double pitch = -def.branchInclination.next(branchRandom);
            double length = Math.max(4.0, def.branchLength.next(branchRandom));
            generateBranch(volume, report, motherlode, yaw, pitch, length, motherlode.y, 0, branchRandom);
        }

        updateOreBounds(report);
        return report;
    }

    private void generateCloud(OreVolume volume, VeinGenerationReport report, Random random) {
        double centerY = clamp(def.cloudHeight.next(random), 1.0, def.sizeY - 2.0);
        Vec3 center = new Vec3(def.motherlodeX, centerY, def.motherlodeZ);
        double rotation = random.nextDouble() * Math.PI * 2.0;
        double inclination = def.cloudInclination.next(random);
        double thickness = Math.max(0.5, def.cloudThickness.next(random));
        double radiusX = Math.max(0.5, def.cloudRadius.next(random));
        double radiusZ = Math.max(0.5, def.cloudRadius.next(random));
        double sizeNoise = Math.abs(def.cloudSizeNoise.next(random));
        double maxRadiusMult = Math.max(0.0, def.oreRadiusMultiplier.getMax());
        double reach = Math.max(radiusX, Math.max(radiusZ, thickness)) * (1.0 + sizeNoise * 2.0) * Math.max(0.1, maxRadiusMult);

        report.motherlodeCenter = center;
        report.motherlodeRadius = Math.max(radiusX, Math.max(radiusZ, thickness));

        int minX = (int) Math.floor(center.x - reach - 1);
        int maxX = (int) Math.ceil(center.x + reach + 1);
        int minY = (int) Math.floor(center.y - reach - 1);
        int maxY = (int) Math.ceil(center.y + reach + 1);
        int minZ = (int) Math.floor(center.z - reach - 1);
        int maxZ = (int) Math.ceil(center.z + reach + 1);

        double cosRot = Math.cos(-rotation);
        double sinRot = Math.sin(-rotation);
        double cosTilt = Math.cos(-inclination);
        double sinTilt = Math.sin(-inclination);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double dx = x + 0.5 - center.x;
                    double dy = y + 0.5 - center.y;
                    double dz = z + 0.5 - center.z;

                    double rx = dx * cosRot - dz * sinRot;
                    double rz = dx * sinRot + dz * cosRot;
                    double ty = dy * cosTilt - rx * sinTilt;
                    double tx = dy * sinTilt + rx * cosTilt;

                    double nx = tx / radiusX;
                    double ny = ty / thickness;
                    double nz = rz / radiusZ;
                    double r2 = nx * nx + ny * ny + nz * nz;
                    if (r2 > Math.max(0.01, maxRadiusMult * maxRadiusMult) * (1.0 + sizeNoise * 2.0) * (1.0 + sizeNoise * 2.0)) {
                        continue;
                    }

                    double radiusMult = Math.max(0.0, def.oreRadiusMultiplier.next(random));
                    double surfaceMult = 1.0 + sizeNoise * signedNoise(nx, ny, nz, 0x51a7L);
                    if (surfaceMult <= 0.0) {
                        continue;
                    }
                    double effectiveR = Math.sqrt(r2) / surfaceMult;
                    if (effectiveR > radiusMult) {
                        continue;
                    }

                    double cutoff = clamp(def.oreVolumeNoiseCutoff.next(random), 0.0, 1.0);
                    double volumeNoise = (signedNoise(nx * 2.0, ny * 2.0, nz * 2.0, 0x9e37L) + 1.0) * 0.5;
                    if (cutoff > 0.0 && volumeNoise < cutoff) {
                        continue;
                    }
                    tryPlaceOre(volume, x, y, z, random);
                }
            }
        }
    }

    private void generateBranch(OreVolume volume, VeinGenerationReport report, Vec3 start, double yaw, double pitch, double length,
                                double motherY, int depth, Random random) {
        Vec3 position = start;
        double remaining = length;
        double maxY = motherY + Math.max(1.0, def.branchHeightLimit.next(random));
        double minY = motherY - Math.max(1.0, def.branchHeightLimit.next(random));

        while (remaining > 0.0) {
            double segmentLength = Math.max(1.0, Math.min(remaining, def.segmentLength.next(random)));
            double radius = Math.max(0.15, def.segmentRadius.next(random));
            Vec3 direction = Vec3.fromYawPitch(yaw, pitch);
            Vec3 next = position.add(direction.scale(segmentLength));

            if ("Ellipsoid".equalsIgnoreCase(def.branchType)) {
                fillEllipsoidSegment(volume, position, next, radius, random);
            } else {
                fillTube(volume, position, next, radius, random);
            }
            report.segments++;
            report.totalBranchLength += segmentLength;
            report.minSegmentRadius = Math.min(report.minSegmentRadius, radius);
            report.maxSegmentRadius = Math.max(report.maxSegmentRadius, radius);
            report.totalSegmentRadius += radius;

            remaining -= segmentLength;
            position = next;
            if (position.y < minY || position.y > maxY || !volume.inBounds((int) position.x, (int) position.y, (int) position.z)) {
                return;
            }

            if (depth < 3) {
                int forks = def.segmentForkFrequency.nextInt(random);
                for (int i = 0; i < forks; i++) {
                    Random forkRandom = new Random(random.nextLong());
                    double forkYaw = yaw + (forkRandom.nextDouble() * 2.0 - 1.0) * Math.PI;
                    double forkPitch = pitch + def.segmentPitch.next(forkRandom) + (forkRandom.nextDouble() - 0.5) * 0.4;
                    double forkLength = remaining * clamp(def.segmentForkLengthMultiplier.next(forkRandom), 0.1, 1.0);
                    report.forkBranches++;
                    generateBranch(volume, report, position, forkYaw, forkPitch, forkLength, motherY, depth + 1, forkRandom);
                }
            }

            yaw += signed(def.segmentAngle.next(random), random);
            pitch = clamp(pitch + def.segmentPitch.next(random), -1.2, 1.2);
        }
    }

    private void fillSphere(OreVolume volume, Vec3 center, double radius, Random random) {
        int minX = (int) Math.floor(center.x - radius - 1);
        int maxX = (int) Math.ceil(center.x + radius + 1);
        int minY = (int) Math.floor(center.y - radius - 1);
        int maxY = (int) Math.ceil(center.y + radius + 1);
        int minZ = (int) Math.floor(center.z - radius - 1);
        int maxZ = (int) Math.ceil(center.z + radius + 1);
        double radiusSquared = radius * radius;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double dx = x + 0.5 - center.x;
                    double dy = y + 0.5 - center.y;
                    double dz = z + 0.5 - center.z;
                    if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                        tryPlaceOre(volume, x, y, z, random);
                    }
                }
            }
        }
    }

    private void fillTube(OreVolume volume, Vec3 a, Vec3 b, double radius, Random random) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        double lenSquared = dx * dx + dy * dy + dz * dz;
        double maxRadius = Math.max(0.2, radius * Math.max(0.1, def.oreRadiusMultiplier.next(random)));
        double radiusSquared = maxRadius * maxRadius;

        int minX = (int) Math.floor(Math.min(a.x, b.x) - maxRadius - 1);
        int maxX = (int) Math.ceil(Math.max(a.x, b.x) + maxRadius + 1);
        int minY = (int) Math.floor(Math.min(a.y, b.y) - maxRadius - 1);
        int maxY = (int) Math.ceil(Math.max(a.y, b.y) + maxRadius + 1);
        int minZ = (int) Math.floor(Math.min(a.z, b.z) - maxRadius - 1);
        int maxZ = (int) Math.ceil(Math.max(a.z, b.z) + maxRadius + 1);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double px = x + 0.5 - a.x;
                    double py = y + 0.5 - a.y;
                    double pz = z + 0.5 - a.z;
                    double t = lenSquared == 0.0 ? 0.0 : clamp((px * dx + py * dy + pz * dz) / lenSquared, 0.0, 1.0);
                    double cx = a.x + dx * t;
                    double cy = a.y + dy * t;
                    double cz = a.z + dz * t;
                    double ox = x + 0.5 - cx;
                    double oy = y + 0.5 - cy;
                    double oz = z + 0.5 - cz;
                    if (ox * ox + oy * oy + oz * oz <= radiusSquared) {
                        tryPlaceOre(volume, x, y, z, random);
                    }
                }
            }
        }
    }

    private void fillEllipsoidSegment(OreVolume volume, Vec3 a, Vec3 b, double radius, Random random) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0.0) {
            fillSphere(volume, a, radius, random);
            return;
        }
        double ux = dx / len;
        double uy = dy / len;
        double uz = dz / len;
        double cx = (a.x + b.x) * 0.5;
        double cy = (a.y + b.y) * 0.5;
        double cz = (a.z + b.z) * 0.5;
        double axialRadius = Math.max(0.5, len * 0.5);
        double radialRadius = Math.max(0.2, radius * Math.max(0.1, def.oreRadiusMultiplier.next(random)));

        int minX = (int) Math.floor(Math.min(a.x, b.x) - radialRadius - 1);
        int maxX = (int) Math.ceil(Math.max(a.x, b.x) + radialRadius + 1);
        int minY = (int) Math.floor(Math.min(a.y, b.y) - radialRadius - 1);
        int maxY = (int) Math.ceil(Math.max(a.y, b.y) + radialRadius + 1);
        int minZ = (int) Math.floor(Math.min(a.z, b.z) - radialRadius - 1);
        int maxZ = (int) Math.ceil(Math.max(a.z, b.z) + radialRadius + 1);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double px = x + 0.5 - cx;
                    double py = y + 0.5 - cy;
                    double pz = z + 0.5 - cz;
                    double axial = px * ux + py * uy + pz * uz;
                    double radialX = px - axial * ux;
                    double radialY = py - axial * uy;
                    double radialZ = pz - axial * uz;
                    double radialSquared = radialX * radialX + radialY * radialY + radialZ * radialZ;
                    double ellipsoid = (axial * axial) / (axialRadius * axialRadius)
                            + radialSquared / (radialRadius * radialRadius);
                    if (ellipsoid <= 1.0) {
                        tryPlaceOre(volume, x, y, z, random);
                    }
                }
            }
        }
    }

    private void tryPlaceOre(OreVolume volume, int x, int y, int z, Random random) {
        double density = clamp(def.oreDensity.next(random), 0.0, 1.0);
        if (random.nextDouble() <= density) {
            int oreIndex = chooseOreIndex(random);
            if (oreIndex >= 0) {
                volume.setOre(x, y, z, oreIndex);
            }
        }
    }

    private int chooseOreIndex(Random random) {
        if (cappedOreWeightTotal <= 0.0) {
            return -1;
        }
        double pick = random.nextDouble();
        if (pick >= cappedOreWeightTotal) {
            return -1;
        }
        for (int i = 0; i < cumulativeOreWeights.length; i++) {
            if (pick <= cumulativeOreWeights[i]) {
                return i;
            }
        }
        return Math.max(0, cumulativeOreWeights.length - 1);
    }

    private void updateOreBounds(VeinGenerationReport report) {
        OreVolume volume = report.volume;
        for (int i = 0; i < volume.getOreCount(); i++) {
            int packed = volume.getOrePosition(i);
            int x = volume.unpackX(packed);
            int y = volume.unpackY(packed);
            int z = volume.unpackZ(packed);
            report.minOreX = Math.min(report.minOreX, x);
            report.minOreY = Math.min(report.minOreY, y);
            report.minOreZ = Math.min(report.minOreZ, z);
            report.maxOreX = Math.max(report.maxOreX, x);
            report.maxOreY = Math.max(report.maxOreY, y);
            report.maxOreZ = Math.max(report.maxOreZ, z);
        }
        if (report.minSegmentRadius == Double.POSITIVE_INFINITY) {
            report.minSegmentRadius = 0.0;
        }
    }

    private static double signed(double value, Random random) {
        return random.nextBoolean() ? value : -value;
    }

    private static double signedNoise(double x, double y, double z, long salt) {
        long xi = Math.round(x * 37.0);
        long yi = Math.round(y * 37.0);
        long zi = Math.round(z * 37.0);
        long n = xi * 73428767L ^ yi * 912931L ^ zi * 42317861L ^ salt;
        n = (n ^ (n >>> 13)) * 1274126177L;
        n ^= n >>> 16;
        return ((n & 0xFFFFFF) / (double) 0x7FFFFF) - 1.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
