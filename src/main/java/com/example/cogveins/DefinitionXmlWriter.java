package com.example.cogveins;

import java.util.List;
import java.util.Locale;

final class DefinitionXmlWriter {
    private DefinitionXmlWriter() {
    }

    static String write(VeinDefinition def) {
        return write(def, 0);
    }

    static String writeConfig(List<VeinDefinition> definitions) {
        return writeConfig(definitions, false);
    }

    static String writeConfigForSave(List<VeinDefinition> definitions) {
        return writeConfig(definitions, true);
    }

    private static String writeConfig(List<VeinDefinition> definitions, boolean normalizeOreWeights) {
        StringBuilder xml = new StringBuilder();
        for (VeinDefinition def : definitions) {
            if (xml.length() > 0) {
                xml.append("\n");
            }
            xml.append(write(def, 0, normalizeOreWeights));
        }
        return xml.toString();
    }

    private static String write(VeinDefinition def, int depth) {
        return write(def, depth, false);
    }

    private static String write(VeinDefinition def, int depth, boolean normalizeOreWeights) {
        String indent = indent(depth);
        String inner = indent(depth + 1);
        StringBuilder xml = new StringBuilder();
        boolean cloud = "Cloud".equalsIgnoreCase(def.distributionType);
        String tag = cloud ? "cloud" : "veins";
        xml.append(indent).append("<").append(tag).append(" name=\"").append(escape(def.nameForXml())).append("\"");
        if (!cloud) {
            xml.append(" branchtype=\"").append(escape((def.branchType == null ? "bezier" : def.branchType).toLowerCase(Locale.ENGLISH))).append("\"");
        }
        if (def.sourceSeed != null) {
            xml.append(" seed=\"").append(def.sourceSeed.longValue()).append("\"");
        }
        xml.append(">\n");
        if (cloud) {
            optionalSetting(xml, inner, def, "DistributionFrequency", def.distributionFrequency);
            optionalSetting(xml, inner, def, "ParentRangeLimit", def.motherlodeRangeLimit);
            optionalSetting(xml, inner, def, "CloudRadius", def.cloudRadius);
            optionalSetting(xml, inner, def, "CloudThickness", def.cloudThickness);
            optionalSetting(xml, inner, def, "CloudSizeNoise", def.cloudSizeNoise);
            optionalSetting(xml, inner, def, "CloudHeight", def.cloudHeight);
            optionalSetting(xml, inner, def, "CloudInclination", def.cloudInclination);
        } else {
            optionalSetting(xml, inner, def, "MotherlodeFrequency", def.distributionFrequency);
            optionalSetting(xml, inner, def, "MotherlodeRangeLimit", def.motherlodeRangeLimit);
            optionalSetting(xml, inner, def, "MotherlodeHeight", def.motherlodeHeight);
            optionalSetting(xml, inner, def, "MotherlodeSize", def.motherlodeSize);
            optionalSetting(xml, inner, def, "BranchFrequency", def.branchFrequency);
            optionalSetting(xml, inner, def, "BranchLength", def.branchLength);
            optionalSetting(xml, inner, def, "BranchInclination", def.branchInclination);
            optionalSetting(xml, inner, def, "BranchHeightLimit", def.branchHeightLimit);
            optionalSetting(xml, inner, def, "SegmentForkFrequency", def.segmentForkFrequency);
            optionalSetting(xml, inner, def, "SegmentForkLengthMult", def.segmentForkLengthMultiplier);
            optionalSetting(xml, inner, def, "SegmentLength", def.segmentLength);
            optionalSetting(xml, inner, def, "SegmentAngle", def.segmentAngle);
            optionalSetting(xml, inner, def, "SegmentPitch", def.segmentPitch);
            optionalSetting(xml, inner, def, "SegmentRadius", def.segmentRadius);
        }
        optionalSetting(xml, inner, def, "OreDensity", def.oreDensity);
        optionalSetting(xml, inner, def, "OreVolumeNoiseCutoff", def.oreVolumeNoiseCutoff);
        optionalSetting(xml, inner, def, "OreRadiusMult", def.oreRadiusMultiplier);
        xml.append("\n");
        xml.append(inner).append("<replacesore block=\"stone\" weight=\"1.0\"/>\n");
        if (def.replacesAir) {
            xml.append(inner).append("<replacesore block=\"air\" weight=\"1.0\"/>\n");
        }
        for (BiomeGateEntry biome : def.biomeGates) {
            if (biome.name == null || biome.name.trim().length() == 0 || biome.weight == 0.0) {
                continue;
            }
            xml.append(inner).append("<").append(biome.isType() ? "biometype" : "biome")
                    .append(" name=\"").append(escape(biome.name.trim())).append("\"");
            xml.append(" weight=\"").append(format(biome.weight)).append("\"");
            xml.append("/>\n");
        }
        def.ensureOreBlocks();
        double[] saveWeights = normalizeOreWeights ? saveSafeOreWeights(def) : null;
        for (int i = 0; i < def.oreBlocks.size(); i++) {
            OreBlockDefinition ore = def.oreBlocks.get(i);
            double weight = saveWeights == null ? ore.weight : saveWeights[i];
            if (ore.block != null && ore.block.trim().length() > 0 && weight > 0.0) {
                xml.append(inner).append("<oreblock block=\"").append(escape(ore.block.trim())).append("\" weight=\"")
                        .append(format(weight)).append("\"/>\n");
            }
        }
        for (VeinDefinition child : def.children) {
            xml.append("\n").append(write(child, depth + 1, normalizeOreWeights));
        }
        xml.append(indent).append("</").append(tag).append(">\n");
        return xml.toString();
    }

    private static void setting(StringBuilder xml, String indent, String name, PDist dist) {
        xml.append(indent).append("<setting name=\"").append(name.toLowerCase(Locale.ENGLISH)).append("\" avg=\"").append(format(dist.mean)).append("\"");
        if (dist.range != 0.0) {
            xml.append(" range=\"").append(format(dist.range)).append("\"");
        }
        if (dist.type == PDist.Type.NORMAL) {
            xml.append(" type=\"normal\"");
        }
        xml.append("/>\n");
    }

    private static void optionalSetting(StringBuilder xml, String indent, VeinDefinition def, String name, PDist dist) {
        if (def.enabledSettings.contains(name)) {
            setting(xml, indent, name, dist);
        }
    }

    private static double[] saveSafeOreWeights(VeinDefinition def) {
        double[] weights = new double[def.oreBlocks.size()];
        int validCount = 0;
        int totalUnits = 0;
        for (int i = 0; i < def.oreBlocks.size(); i++) {
            OreBlockDefinition ore = def.oreBlocks.get(i);
            if (ore.block == null || ore.block.trim().length() == 0) {
                continue;
            }
            validCount++;
            int units = clampUnits((int) Math.round(Math.max(0.0, ore.weight) * 1000000.0));
            weights[i] = units;
            totalUnits += units;
        }
        if (validCount <= 0) {
            return weights;
        }
        if (totalUnits < 1000000) {
            int missing = 1000000 - totalUnits;
            int each = missing / validCount;
            int remainder = missing % validCount;
            for (int i = 0; i < def.oreBlocks.size(); i++) {
                OreBlockDefinition ore = def.oreBlocks.get(i);
                if (ore.block == null || ore.block.trim().length() == 0) {
                    continue;
                }
                weights[i] += each;
                if (remainder > 0) {
                    weights[i] += 1;
                    remainder--;
                }
            }
        } else if (totalUnits > 1000000) {
            int assigned = 0;
            int lastValid = -1;
            for (int i = 0; i < def.oreBlocks.size(); i++) {
                OreBlockDefinition ore = def.oreBlocks.get(i);
                if (ore.block != null && ore.block.trim().length() > 0) {
                    lastValid = i;
                }
            }
            for (int i = 0; i < def.oreBlocks.size(); i++) {
                OreBlockDefinition ore = def.oreBlocks.get(i);
                if (ore.block == null || ore.block.trim().length() == 0) {
                    continue;
                }
                if (i == lastValid) {
                    weights[i] = 1000000 - assigned;
                } else {
                    weights[i] = clampUnits((int) Math.round(weights[i] / totalUnits * 1000000.0));
                    assigned += (int) weights[i];
                }
            }
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] = weights[i] / 1000000.0;
        }
        return weights;
    }

    private static int clampUnits(int value) {
        return Math.max(0, Math.min(1000000, value));
    }

    private static String indent(int depth) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            text.append("  ");
        }
        return text.toString();
    }

    private static String format(double value) {
        String text = String.format(Locale.US, "%.6f", value);
        while (text.indexOf('.') >= 0 && text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
