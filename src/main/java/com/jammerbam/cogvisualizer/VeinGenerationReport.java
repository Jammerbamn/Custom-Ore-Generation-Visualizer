package com.jammerbam.cogvisualizer;

final class VeinGenerationReport {
    final OreVolume volume;
    Vec3 motherlodeCenter;
    double motherlodeRadius;
    int primaryBranches;
    int forkBranches;
    int segments;
    double totalBranchLength;
    double minSegmentRadius = Double.POSITIVE_INFINITY;
    double maxSegmentRadius;
    double totalSegmentRadius;
    int minOreX = Integer.MAX_VALUE;
    int minOreY = Integer.MAX_VALUE;
    int minOreZ = Integer.MAX_VALUE;
    int maxOreX = Integer.MIN_VALUE;
    int maxOreY = Integer.MIN_VALUE;
    int maxOreZ = Integer.MIN_VALUE;

    VeinGenerationReport(OreVolume volume) {
        this.volume = volume;
    }

    int totalBranches() {
        return primaryBranches + forkBranches;
    }

    double averageSegmentRadius() {
        return segments == 0 ? 0.0 : totalSegmentRadius / segments;
    }

    boolean hasOreBounds() {
        return minOreX != Integer.MAX_VALUE;
    }

    int widthX() {
        return hasOreBounds() ? maxOreX - minOreX + 1 : 0;
    }

    int heightY() {
        return hasOreBounds() ? maxOreY - minOreY + 1 : 0;
    }

    int depthZ() {
        return hasOreBounds() ? maxOreZ - minOreZ + 1 : 0;
    }
}
