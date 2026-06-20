package com.jammerbam.cogvisualizer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class VeinDefinition {
    String name = "Sample COG Layered Vein";
    String oreBlockName = "minecraft:iron_ore";
    String distributionType = "Veins";
    String branchType = "Bezier";
    final List<OreBlockDefinition> oreBlocks = new ArrayList<OreBlockDefinition>();
    final List<BiomeGateEntry> biomeGates = new ArrayList<BiomeGateEntry>();
    final List<VeinDefinition> children = new ArrayList<VeinDefinition>();
    final Set<String> enabledSettings = new HashSet<String>();
    Long sourceSeed;
    File sourceFile;
    boolean replacesAir;
    PDist distributionFrequency = new PDist(0.025, 0.0);
    PDist motherlodeRangeLimit = new PDist(32.0, 32.0, PDist.Type.NORMAL);

    int sizeX = 128;
    int sizeY = 80;
    int sizeZ = 128;

    double motherlodeX = 64.0;
    PDist motherlodeHeight = new PDist(36.0, 8.0, PDist.Type.NORMAL);
    double motherlodeZ = 64.0;
    PDist motherlodeSize = new PDist(4.0, 1.2, PDist.Type.UNIFORM);

    PDist branchFrequency = new PDist(4.0, 1.0, PDist.Type.UNIFORM);
    PDist branchInclination = new PDist(0.0, 0.35, PDist.Type.UNIFORM);
    PDist branchLength = new PDist(64.0, 20.0, PDist.Type.UNIFORM);
    PDist branchHeightLimit = new PDist(18.0, 0.0);

    PDist segmentForkFrequency = new PDist(0.22, 0.0);
    PDist segmentForkLengthMultiplier = new PDist(0.65, 0.18, PDist.Type.UNIFORM);
    PDist segmentLength = new PDist(9.0, 3.0, PDist.Type.UNIFORM);
    PDist segmentAngle = new PDist(0.38, 0.24, PDist.Type.UNIFORM);
    PDist segmentPitch = new PDist(0.0, 0.12, PDist.Type.UNIFORM);
    PDist segmentRadius = new PDist(1.15, 0.35, PDist.Type.UNIFORM);

    PDist oreDensity = new PDist(0.72, 0.0);
    PDist oreRadiusMultiplier = new PDist(1.0, 0.12, PDist.Type.UNIFORM);
    PDist cloudRadius = new PDist(25.0, 10.0, PDist.Type.UNIFORM);
    PDist cloudThickness = new PDist(14.0, 6.0, PDist.Type.UNIFORM);
    PDist cloudSizeNoise = new PDist(0.2, 0.0);
    PDist cloudHeight = new PDist(32.0, 16.0, PDist.Type.NORMAL);
    PDist cloudInclination = new PDist(0.0, 0.35, PDist.Type.UNIFORM);
    PDist oreVolumeNoiseCutoff = new PDist(0.5, 0.0);

    static VeinDefinition sample() {
        VeinDefinition def = new VeinDefinition();
        def.ensureOreBlocks();
        def.enableDefaultSettings();
        return def;
    }

    VeinDefinition copy() {
        VeinDefinition copy = new VeinDefinition();
        copy.name = name;
        copy.oreBlockName = oreBlockName;
        copy.distributionType = distributionType;
        copy.branchType = branchType;
        copy.sourceSeed = sourceSeed;
        copy.sourceFile = sourceFile;
        copy.replacesAir = replacesAir;
        copy.distributionFrequency = distributionFrequency;
        copy.motherlodeRangeLimit = motherlodeRangeLimit;
        copy.sizeX = sizeX;
        copy.sizeY = sizeY;
        copy.sizeZ = sizeZ;
        copy.motherlodeX = motherlodeX;
        copy.motherlodeHeight = motherlodeHeight;
        copy.motherlodeZ = motherlodeZ;
        copy.motherlodeSize = motherlodeSize;
        copy.branchFrequency = branchFrequency;
        copy.branchInclination = branchInclination;
        copy.branchLength = branchLength;
        copy.branchHeightLimit = branchHeightLimit;
        copy.segmentForkFrequency = segmentForkFrequency;
        copy.segmentForkLengthMultiplier = segmentForkLengthMultiplier;
        copy.segmentLength = segmentLength;
        copy.segmentAngle = segmentAngle;
        copy.segmentPitch = segmentPitch;
        copy.segmentRadius = segmentRadius;
        copy.oreDensity = oreDensity;
        copy.oreRadiusMultiplier = oreRadiusMultiplier;
        copy.cloudRadius = cloudRadius;
        copy.cloudThickness = cloudThickness;
        copy.cloudSizeNoise = cloudSizeNoise;
        copy.cloudHeight = cloudHeight;
        copy.cloudInclination = cloudInclination;
        copy.oreVolumeNoiseCutoff = oreVolumeNoiseCutoff;
        copy.enabledSettings.clear();
        copy.enabledSettings.addAll(enabledSettings);
        copy.oreBlocks.clear();
        for (OreBlockDefinition ore : oreBlocks) {
            OreBlockDefinition oreCopy = new OreBlockDefinition(ore.block, ore.weight);
            oreCopy.colorHex = ore.colorHex;
            copy.oreBlocks.add(oreCopy);
        }
        copy.biomeGates.clear();
        for (BiomeGateEntry gate : biomeGates) {
            copy.biomeGates.add(gate.copy());
        }
        copy.children.clear();
        for (VeinDefinition child : children) {
            copy.children.add(child.copy());
        }
        return copy;
    }

    String nameForXml() {
        int marker = name.indexOf("  [");
        return marker >= 0 ? name.substring(0, marker) : name;
    }

    void ensureOreBlocks() {
        if (oreBlocks.isEmpty()) {
            oreBlocks.add(new OreBlockDefinition(oreBlockName, 1.0));
        }
        if (!oreBlocks.isEmpty()) {
            oreBlockName = oreBlocks.get(0).block;
        }
    }

    void enableDefaultSettings() {
        enabledSettings.add("MotherlodeFrequency");
        enabledSettings.add("MotherlodeRangeLimit");
        enabledSettings.add("MotherlodeHeight");
        enabledSettings.add("MotherlodeSize");
        enabledSettings.add("BranchFrequency");
        enabledSettings.add("BranchLength");
        enabledSettings.add("BranchInclination");
        enabledSettings.add("BranchHeightLimit");
        enabledSettings.add("SegmentLength");
        enabledSettings.add("SegmentAngle");
        enabledSettings.add("SegmentRadius");
        enabledSettings.add("OreDensity");
        enabledSettings.add("OreRadiusMult");
    }

    void enableCloudDefaults() {
        enabledSettings.clear();
        enabledSettings.add("DistributionFrequency");
        enabledSettings.add("ParentRangeLimit");
        enabledSettings.add("CloudRadius");
        enabledSettings.add("CloudThickness");
        enabledSettings.add("CloudSizeNoise");
        enabledSettings.add("CloudHeight");
        enabledSettings.add("CloudInclination");
        enabledSettings.add("OreDensity");
        enabledSettings.add("OreVolumeNoiseCutoff");
        enabledSettings.add("OreRadiusMult");
        distributionFrequency = new PDist(0.001, 0.0);
        motherlodeRangeLimit = new PDist(32.0, 32.0, PDist.Type.NORMAL);
        oreDensity = new PDist(0.1, 0.0);
        oreRadiusMultiplier = new PDist(1.0, 0.1, PDist.Type.UNIFORM);
    }

    double getAverageOreCount() {
        if ("Cloud".equals(distributionType)) {
            double radius = cloudRadius.mean * oreRadiusMultiplier.mean;
            double thickness = cloudThickness.mean * oreRadiusMultiplier.mean;
            double volume = 4.0 * Math.PI * radius * radius * thickness / 3.0;
            double cutoff = Math.max(0.0, Math.min(1.0, oreVolumeNoiseCutoff.mean));
            return volume * oreDensity.mean * (1.0 - cutoff);
        }
        double motherlodeVolume = sphericalVolume(motherlodeSize.mean * oreRadiusMultiplier.mean);
        double branchVolume = cylindricalVolume(getAverageBranchLength(branchLength.mean), segmentRadius.mean * oreRadiusMultiplier.mean);
        return oreDensity.mean * (branchFrequency.mean * branchVolume + motherlodeVolume);
    }

    double getExpectedOresPerChunk() {
        return distributionFrequency.mean * getAverageOreCount();
    }

    int sampleStructureCountForChunk(Random random) {
        if (distributionFrequency.getMax() >= 1.0) {
            return Math.max(0, distributionFrequency.nextInt(random));
        }
        return distributionFrequency.nextInt(random) == 1 ? 1 : 0;
    }

    private double getAverageBranchLength(double length) {
        double avgBranchLength = 0.0;
        while (length > 0.0) {
            double segment = segmentLength.mean;
            if (segment <= 0.0) {
                return avgBranchLength;
            }
            if (segment > length) {
                segment = length;
            }
            avgBranchLength += segment;
            length -= segment;
            for (int forks = (int) Math.round(segmentForkFrequency.mean); forks > 0; forks--) {
                double forkLengthMultiplier = segmentForkLengthMultiplier.mean;
                avgBranchLength += getAverageBranchLength(length * (forkLengthMultiplier > 1.0 ? 1.0 : forkLengthMultiplier));
            }
        }
        return avgBranchLength;
    }

    private static double sphericalVolume(double radius) {
        return 4.0 * Math.PI * radius * radius * radius / 3.0;
    }

    private static double cylindricalVolume(double length, double radius) {
        return Math.PI * radius * radius * length;
    }

    void fitVolumeToVeinSize(int chunkSize) {
        if ("Cloud".equals(distributionType)) {
            double horizontalReach = maxOf(cloudRadius) * (1.0 + Math.max(0.0, cloudSizeNoise.getMax()) * 2.0)
                    * Math.max(0.0, oreRadiusMultiplier.getMax()) + chunkSize / 2.0;
            int horizontalBlocks = roundUpToChunk((int) Math.ceil(horizontalReach * 2.0), chunkSize);
            horizontalBlocks = clampInt(horizontalBlocks, chunkSize * 4, chunkSize * 32);
            sizeX = horizontalBlocks;
            sizeZ = horizontalBlocks;
            motherlodeX = sizeX / 2.0;
            motherlodeZ = sizeZ / 2.0;

            double verticalReach = maxOf(cloudHeight) + maxOf(cloudThickness) * (1.0 + Math.max(0.0, cloudSizeNoise.getMax()) * 2.0)
                    * Math.max(0.0, oreRadiusMultiplier.getMax()) + chunkSize / 2.0;
            int verticalBlocks = roundUpToChunk((int) Math.ceil(verticalReach), chunkSize);
            sizeY = clampInt(verticalBlocks, 64, 256);
            return;
        }
        double horizontalReach = maxOf(motherlodeSize) + maxOf(branchLength) + maxOf(segmentRadius) + chunkSize / 2.0;
        int horizontalBlocks = roundUpToChunk((int) Math.ceil(horizontalReach * 2.0), chunkSize);
        horizontalBlocks = clampInt(horizontalBlocks, chunkSize * 4, chunkSize * 32);
        sizeX = horizontalBlocks;
        sizeZ = horizontalBlocks;
        motherlodeX = sizeX / 2.0;
        motherlodeZ = sizeZ / 2.0;

        double verticalReach = maxOf(motherlodeHeight) + maxOf(branchHeightLimit) + maxOf(motherlodeSize) + chunkSize / 2.0;
        int verticalBlocks = roundUpToChunk((int) Math.ceil(verticalReach), chunkSize);
        sizeY = clampInt(verticalBlocks, 64, 256);
    }

    private static double maxOf(PDist dist) {
        return Math.max(0.0, dist.getMax());
    }

    private static int roundUpToChunk(int value, int chunkSize) {
        return ((value + chunkSize - 1) / chunkSize) * chunkSize;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
