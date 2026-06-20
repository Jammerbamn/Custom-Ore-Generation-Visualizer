package com.example.cogveins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

final class CogXmlLoader {
    private static final Pattern NUMBER = Pattern.compile("[-+]?0x[0-9a-fA-F]+|[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?");

    private CogXmlLoader() {
    }

    static List<VeinDefinition> load(File file) throws Exception {
        List<VeinDefinition> definitions = new ArrayList<VeinDefinition>();
        loadInto(file, definitions, 0);
        return definitions;
    }

    static List<VeinDefinition> loadText(String xml, String sourceName) throws Exception {
        List<VeinDefinition> definitions = new ArrayList<VeinDefinition>();
        Document document = parseLooseXml(xml);
        collectVeins(document.getDocumentElement(), definitions, sourceName, null);
        return definitions;
    }

    private static void loadInto(File file, List<VeinDefinition> definitions, int depth) throws Exception {
        if (depth > 8) {
            return;
        }

        Document document = parseLooseXml(readText(file));
        collectVeins(document.getDocumentElement(), definitions, file.getName(), file);
        collectImports(file, document.getDocumentElement(), definitions, depth);
    }

    private static void collectImports(File source, Element root, List<VeinDefinition> definitions, int depth) throws Exception {
        NodeList imports = root.getElementsByTagName("Import");
        File base = source.getParentFile();
        for (int i = 0; i < imports.getLength(); i++) {
            Element element = (Element) imports.item(i);
            String path = element.getAttribute("file");
            if (path == null || path.length() == 0) {
                continue;
            }
            File imported = new File(path);
            if (!imported.isAbsolute()) {
                imported = new File(base, path.replace('/', File.separatorChar));
            }
            if (imported.isFile()) {
                loadInto(imported, definitions, depth + 1);
            }
        }
    }

    private static void collectVeins(Element root, List<VeinDefinition> definitions, String sourceName, File sourceFile) {
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element) node;
            if ("Veins".equalsIgnoreCase(element.getTagName()) || "Cloud".equalsIgnoreCase(element.getTagName())) {
                definitions.add(toDefinition(element, sourceName, sourceFile));
            } else {
                collectVeins(element, definitions, sourceName, sourceFile);
            }
        }
    }

    private static VeinDefinition toDefinition(Element vein, String sourceName, File sourceFile) {
        VeinDefinition def = VeinDefinition.sample();
        def.distributionType = "Cloud".equalsIgnoreCase(vein.getTagName()) ? "Cloud" : "Veins";
        String name = vein.getAttribute("name");
        def.name = (name == null || name.length() == 0 ? "Unnamed " + def.distributionType : name) + "  [" + sourceName + "]";
        def.sourceFile = sourceFile;
        if ("Cloud".equalsIgnoreCase(def.distributionType)) {
            def.enableCloudDefaults();
        }
        String branchType = vein.getAttribute("branchType");
        if (branchType == null || branchType.trim().length() == 0) {
            branchType = vein.getAttribute("branchtype");
        }
        if (branchType != null && branchType.trim().length() > 0) {
            def.branchType = "ellipsoid".equalsIgnoreCase(branchType.trim()) ? "Ellipsoid" : "Bezier";
        }
        String seed = vein.getAttribute("seed");
        if (seed != null && seed.length() > 0) {
            def.sourceSeed = parseSeed(seed);
        }

        readOreBlocks(vein, def);
        readReplaces(vein, def);
        readBiomeGates(vein, def);

        NodeList settings = vein.getChildNodes();
        for (int i = 0; i < settings.getLength(); i++) {
            if (!(settings.item(i) instanceof Element)) {
                continue;
            }
            Element setting = (Element) settings.item(i);
            if (setting.getParentNode() != vein || !"Setting".equalsIgnoreCase(setting.getTagName())) {
                continue;
            }
            applySetting(def, setting);
        }

        NodeList children = vein.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && ("Veins".equalsIgnoreCase(((Element) node).getTagName()) || "Cloud".equalsIgnoreCase(((Element) node).getTagName()))) {
                def.children.add(toDefinition((Element) node, sourceName, sourceFile));
            }
        }

        return def;
    }

    private static void readOreBlocks(Element vein, VeinDefinition def) {
        def.oreBlocks.clear();
        NodeList nodes = vein.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (!(nodes.item(i) instanceof Element)) {
                continue;
            }
            Element ore = (Element) nodes.item(i);
            if (!"OreBlock".equalsIgnoreCase(ore.getTagName())) {
                continue;
            }
            String block = ore.getAttribute("block");
            if (block != null && block.length() > 0) {
                double weight = parseNumber(ore.getAttribute("weight"), 1.0);
                def.oreBlocks.add(new OreBlockDefinition(block, weight));
            }
        }
        def.ensureOreBlocks();
    }

    private static void readReplaces(Element vein, VeinDefinition def) {
        NodeList nodes = vein.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (!(nodes.item(i) instanceof Element)) {
                continue;
            }
            Element replace = (Element) nodes.item(i);
            String tag = replace.getTagName();
            if (!"Replaces".equalsIgnoreCase(tag) && !"ReplacesOre".equalsIgnoreCase(tag)) {
                continue;
            }
            String block = replace.getAttribute("block");
            if (block == null || block.length() == 0) {
                block = replace.getAttribute("name");
            }
            double weight = parseNumber(replace.getAttribute("weight"), 1.0);
            if (weight > 0.0 && isAirBlock(block)) {
                def.replacesAir = true;
            }
        }
    }

    private static void readBiomeGates(Element vein, VeinDefinition def) {
        def.biomeGates.clear();
        NodeList nodes = vein.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (!(nodes.item(i) instanceof Element)) {
                continue;
            }
            Element biome = (Element) nodes.item(i);
            String tag = biome.getTagName();
            if (!"Biome".equalsIgnoreCase(tag) && !"BiomeType".equalsIgnoreCase(tag)) {
                continue;
            }
            String name = biome.getAttribute("name");
            if (name == null || name.trim().length() == 0) {
                continue;
            }
            double weight = parseNumber(biome.getAttribute("weight"), 1.0);
            def.biomeGates.add(new BiomeGateEntry("BiomeType".equalsIgnoreCase(tag) ? "BiomeType" : "Biome", name, weight));
        }
    }

    private static boolean isAirBlock(String block) {
        if (block == null) {
            return false;
        }
        String normalized = block.trim().toLowerCase(Locale.ENGLISH);
        return "air".equals(normalized) || "minecraft:air".equals(normalized);
    }

    private static void applySetting(VeinDefinition def, Element setting) {
        String name = setting.getAttribute("name");
        PDist value = readPDist(setting);
        if ("DistributionFrequency".equalsIgnoreCase(name) || "MotherlodeFrequency".equalsIgnoreCase(name)) {
            def.enabledSettings.add("Cloud".equalsIgnoreCase(def.distributionType) ? "DistributionFrequency" : "MotherlodeFrequency");
            def.distributionFrequency = value;
        } else if ("ParentRangeLimit".equalsIgnoreCase(name) || "MotherlodeRangeLimit".equalsIgnoreCase(name)) {
            def.enabledSettings.add("Cloud".equalsIgnoreCase(def.distributionType) ? "ParentRangeLimit" : "MotherlodeRangeLimit");
            def.motherlodeRangeLimit = value;
        } else if ("CloudRadius".equalsIgnoreCase(name)) {
            def.enabledSettings.add("CloudRadius");
            def.cloudRadius = value;
        } else if ("CloudThickness".equalsIgnoreCase(name)) {
            def.enabledSettings.add("CloudThickness");
            def.cloudThickness = value;
        } else if ("CloudSizeNoise".equalsIgnoreCase(name)) {
            def.enabledSettings.add("CloudSizeNoise");
            def.cloudSizeNoise = value;
        } else if ("CloudHeight".equalsIgnoreCase(name)) {
            def.enabledSettings.add("CloudHeight");
            def.cloudHeight = value;
        } else if ("CloudInclination".equalsIgnoreCase(name)) {
            def.enabledSettings.add("CloudInclination");
            def.cloudInclination = value;
        } else if ("MotherlodeHeight".equalsIgnoreCase(name)) {
            def.enabledSettings.add("MotherlodeHeight");
            def.motherlodeHeight = value;
        } else if ("MotherlodeSize".equalsIgnoreCase(name)) {
            def.enabledSettings.add("MotherlodeSize");
            def.motherlodeSize = value;
        } else if ("BranchFrequency".equalsIgnoreCase(name)) {
            def.enabledSettings.add("BranchFrequency");
            def.branchFrequency = value;
        } else if ("BranchInclination".equalsIgnoreCase(name)) {
            def.enabledSettings.add("BranchInclination");
            def.branchInclination = value;
        } else if ("BranchLength".equalsIgnoreCase(name)) {
            def.enabledSettings.add("BranchLength");
            def.branchLength = value;
        } else if ("BranchHeightLimit".equalsIgnoreCase(name)) {
            def.enabledSettings.add("BranchHeightLimit");
            def.branchHeightLimit = value;
        } else if ("SegmentForkFrequency".equalsIgnoreCase(name)) {
            def.enabledSettings.add("SegmentForkFrequency");
            def.segmentForkFrequency = value;
        } else if ("SegmentForkLengthMult".equalsIgnoreCase(name)) {
            def.enabledSettings.add("SegmentForkLengthMult");
            def.segmentForkLengthMultiplier = value;
        } else if ("SegmentLength".equalsIgnoreCase(name)) {
            def.enabledSettings.add("SegmentLength");
            def.segmentLength = value;
        } else if ("SegmentAngle".equalsIgnoreCase(name)) {
            def.enabledSettings.add("SegmentAngle");
            def.segmentAngle = value;
        } else if ("SegmentPitch".equalsIgnoreCase(name)) {
            def.enabledSettings.add("SegmentPitch");
            def.segmentPitch = value;
        } else if ("SegmentRadius".equalsIgnoreCase(name)) {
            def.enabledSettings.add("SegmentRadius");
            def.segmentRadius = value;
        } else if ("OreDensity".equalsIgnoreCase(name)) {
            def.enabledSettings.add("OreDensity");
            def.oreDensity = value;
        } else if ("OreRadiusMult".equalsIgnoreCase(name)) {
            def.enabledSettings.add("OreRadiusMult");
            def.oreRadiusMultiplier = value;
        } else if ("OreVolumeNoiseCutoff".equalsIgnoreCase(name)) {
            def.enabledSettings.add("OreVolumeNoiseCutoff");
            def.oreVolumeNoiseCutoff = value;
        }
    }

    private static PDist readPDist(Element setting) {
        double avg = parseNumber(setting.getAttribute("avg"), 0.0);
        double range = parseNumber(setting.getAttribute("range"), 0.0);
        String typeName = setting.getAttribute("type").toLowerCase(Locale.ENGLISH);
        PDist.Type type = PDist.Type.UNIFORM;
        if (range == 0.0) {
            type = PDist.Type.CONSTANT;
        } else if ("normal".equals(typeName)) {
            type = PDist.Type.NORMAL;
        }
        return new PDist(avg, range, type);
    }

    private static long parseSeed(String text) {
        try {
            return Long.decode(text.trim()).longValue();
        } catch (RuntimeException ignored) {
            return text.hashCode();
        }
    }

    private static double parseNumber(String text, double fallback) {
        if (text == null) {
            return fallback;
        }
        Matcher matcher = NUMBER.matcher(text);
        if (!matcher.find()) {
            return fallback;
        }
        String number = matcher.group();
        try {
            if (number.toLowerCase(Locale.ENGLISH).contains("0x")) {
                return parseSeed(number);
            }
            return Double.parseDouble(number);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static Document parseLooseXml(String text) throws Exception {
        String cleaned = text.replaceFirst("^\\s*<\\?xml[^>]*>", "");
        String wrapped = "<CogViewerRoot>" + cleaned + "</CogViewerRoot>";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setExpandEntityReferences(false);
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(wrapped)));
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // Older Java XML parsers may not expose every hardening feature.
        }
    }

    private static String readText(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), Charset.forName("UTF-8"));
        } finally {
            in.close();
        }
    }
}
