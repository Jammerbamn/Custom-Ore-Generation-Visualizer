package com.jammerbam.cogvisualizer;

import java.util.Arrays;

final class OreVolume {
    final int sizeX;
    final int sizeY;
    final int sizeZ;
    private final int[] ore;
    private int[] orePositions = new int[256];
    private int oreCount;

    OreVolume(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.ore = new int[sizeX * sizeY * sizeZ];
        Arrays.fill(this.ore, -1);
    }

    boolean inBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    void setOre(int x, int y, int z, int oreIndex) {
        if (inBounds(x, y, z)) {
            int packed = pack(x, y, z);
            if (ore[packed] >= 0) {
                return;
            }
            ore[packed] = oreIndex;
            addOrePosition(x, y, z);
            oreCount++;
        }
    }

    boolean isOre(int x, int y, int z) {
        return getOreIndex(x, y, z) >= 0;
    }

    int getOreIndex(int x, int y, int z) {
        return inBounds(x, y, z) ? ore[pack(x, y, z)] : -1;
    }

    int getOreCount() {
        return oreCount;
    }

    int getOrePosition(int index) {
        return orePositions[index];
    }

    int unpackX(int packed) {
        return packed / (sizeY * sizeZ);
    }

    int unpackY(int packed) {
        return (packed / sizeZ) % sizeY;
    }

    int unpackZ(int packed) {
        return packed % sizeZ;
    }

    int getOreIndexAtPosition(int packed) {
        return ore[packed];
    }

    private void addOrePosition(int x, int y, int z) {
        if (oreCount >= orePositions.length) {
            int[] expanded = new int[orePositions.length * 2];
            System.arraycopy(orePositions, 0, expanded, 0, orePositions.length);
            orePositions = expanded;
        }
        orePositions[oreCount] = pack(x, y, z);
    }

    private int pack(int x, int y, int z) {
        return (x * sizeY + y) * sizeZ + z;
    }
}
