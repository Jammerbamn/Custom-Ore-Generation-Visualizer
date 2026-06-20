package com.example.cogveins;

final class BiomeGateEntry {
    String kind;
    String name;
    double weight;

    BiomeGateEntry(String kind, String name, double weight) {
        this.kind = kind == null || kind.trim().length() == 0 ? "Biome" : kind;
        this.name = name == null ? "" : name;
        this.weight = weight;
    }

    BiomeGateEntry copy() {
        return new BiomeGateEntry(kind, name, weight);
    }

    boolean isType() {
        return "BiomeType".equalsIgnoreCase(kind);
    }
}
