package com.jammerbam.cogvisualizer;

import java.io.File;
import java.util.List;

public final class GeneratorSmokeTest {
    private GeneratorSmokeTest() {
    }

    public static void main(String[] args) {
        VeinDefinition definition = VeinDefinition.sample();
        OreVolume volumeA = new OreVeinGenerator(definition).generate(12345L);
        OreVolume volumeB = new OreVeinGenerator(definition).generate(67890L);

        if (volumeA.getOreCount() <= 0) {
            throw new IllegalStateException("Expected seed 12345 to generate ore blocks");
        }
        if (volumeB.getOreCount() <= 0) {
            throw new IllegalStateException("Expected seed 67890 to generate ore blocks");
        }
        if (volumeA.getOreCount() == volumeB.getOreCount()) {
            throw new IllegalStateException("Expected different seeds to produce different ore counts");
        }
        VeinDefinition floorVein = VeinDefinition.sample();
        floorVein.motherlodeHeight = new PDist(1.0, 0.0);
        VeinGenerationReport floorReport = new OreVeinGenerator(floorVein).generateReport(12345L);
        if (Math.round(floorReport.motherlodeCenter.y) != 1L) {
            throw new IllegalStateException("Expected motherlode height 1 to remain at Y=1");
        }
        VeinDefinition partialWeights = VeinDefinition.sample();
        partialWeights.oreBlocks.clear();
        partialWeights.oreBlocks.add(new OreBlockDefinition("minecraft:iron_ore", 0.6));
        partialWeights.oreBlocks.add(new OreBlockDefinition("minecraft:gold_ore", 0.2));
        String savedXml = DefinitionXmlWriter.writeConfigForSave(java.util.Collections.singletonList(partialWeights));
        if (savedXml.indexOf("weight=\"0.7\"") < 0 || savedXml.indexOf("weight=\"0.3\"") < 0) {
            throw new IllegalStateException("Expected save XML to distribute missing ore weight to 100%");
        }

        System.out.println("Smoke test OK");
        System.out.println("Seed 12345 ore blocks: " + volumeA.getOreCount());
        System.out.println("Seed 67890 ore blocks: " + volumeB.getOreCount());

        try {
            String cloudXml = "<cloud name=\"SmokeCloud\">"
                    + "<setting name=\"distributionfrequency\" avg=\"0.001\"/>"
                    + "<setting name=\"cloudradius\" avg=\"20\" range=\"5\"/>"
                    + "<setting name=\"cloudthickness\" avg=\"10\" range=\"3\"/>"
                    + "<setting name=\"cloudsizenoise\" avg=\"0.2\"/>"
                    + "<setting name=\"cloudheight\" avg=\"32\" range=\"6\" type=\"normal\"/>"
                    + "<setting name=\"orevolumenoisecutoff\" avg=\"0.35\"/>"
                    + "<setting name=\"oredensity\" avg=\"0.35\"/>"
                    + "<oreblock block=\"minecraft:gold_ore\" weight=\"1\"/>"
                    + "</cloud>";
            VeinDefinition cloud = CogXmlLoader.loadText(cloudXml, "cloud-smoke.xml").get(0);
            cloud.fitVolumeToVeinSize(16);
            OreVolume cloudVolume = new OreVeinGenerator(cloud).generate(24680L);
            if (!"Cloud".equals(cloud.distributionType) || cloudVolume.getOreCount() <= 0) {
                throw new IllegalStateException("Expected cloud XML to load and generate ore blocks");
            }
            String written = DefinitionXmlWriter.write(cloud);
            if (written.indexOf("cloudradius") < 0 || written.indexOf("motherlodesize") >= 0) {
                throw new IllegalStateException("Expected cloud XML writer to use cloud settings");
            }
            System.out.println("Cloud XML smoke OK");
            System.out.println("Cloud ore blocks: " + cloudVolume.getOreCount());
        } catch (Exception ex) {
            throw new RuntimeException("Cloud XML smoke test failed", ex);
        }

        File sample = new File("C:\\Users\\Ethan\\curseforge\\minecraft\\Instances\\HBMHell Test\\config\\CustomOreGen\\modules\\custom\\PorphyryCuAu.xml");
        if (sample.isFile()) {
            try {
                List<VeinDefinition> definitions = CogXmlLoader.load(sample);
                if (definitions.isEmpty()) {
                    throw new IllegalStateException("Expected XML loader to find at least one <Veins> distribution");
                }
                OreVolume xmlVolume = new OreVeinGenerator(definitions.get(0)).generate(12345L);
                if (xmlVolume.getOreCount() <= 0) {
                    throw new IllegalStateException("Expected loaded XML distribution to generate ore blocks");
                }
                System.out.println("XML loader OK");
                System.out.println("Loaded distributions: " + definitions.size());
                System.out.println("First XML distribution: " + definitions.get(0).name);
                System.out.println("First XML ore blocks: " + xmlVolume.getOreCount());
            } catch (Exception ex) {
                throw new RuntimeException("XML loader smoke test failed", ex);
            }
        }

        File master = new File("C:\\Users\\Ethan\\curseforge\\minecraft\\Instances\\HBMHell Test\\config\\CustomOreGen\\CustomOreGen_Config.xml");
        if (master.isFile()) {
            try {
                List<VeinDefinition> definitions = CogXmlLoader.load(master);
                if (definitions.isEmpty()) {
                    throw new IllegalStateException("Expected master config imports to provide <Veins> distributions");
                }
                System.out.println("Master config import OK");
                System.out.println("Master/imported distributions: " + definitions.size());
            } catch (Exception ex) {
                throw new RuntimeException("Master config XML smoke test failed", ex);
            }
        }
    }
}
