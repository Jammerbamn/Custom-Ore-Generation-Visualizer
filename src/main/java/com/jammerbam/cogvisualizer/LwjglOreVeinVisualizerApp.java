package com.jammerbam.cogvisualizer;

import java.awt.BorderLayout;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

public final class LwjglOreVeinVisualizerApp {
    private static final int CHUNK_SIZE = 16;
    private static final int SIDE_PANEL_WIDTH = 340;
    private static final int EDITOR_CONTENT_WIDTH = 300;
    private static final Color APP_BG = new Color(18, 20, 24);
    private static final Color PANEL_BG = new Color(23, 29, 37);
    private static final Color CONTROL_BG = new Color(31, 39, 49);
    private static final Color CONTROL_FG = new Color(230, 236, 245);
    private static final Color MUTED_FG = new Color(165, 176, 190);
    private static final Color BORDER_COLOR = new Color(48, 59, 72);
    private static final String PREF_EXPORT_FOLDER = "lwjglExportFolder";
    private static final String PREF_BIOME_DICTIONARY_FILE = "lwjglBiomeDictionaryFile";
    private static final String[] VEIN_SETTINGS = new String[] {
            "MotherlodeFrequency", "MotherlodeRangeLimit", "MotherlodeHeight", "MotherlodeSize",
            "BranchFrequency", "BranchLength", "BranchInclination", "BranchHeightLimit",
            "SegmentForkFrequency", "SegmentForkLengthMult", "SegmentLength", "SegmentAngle",
            "SegmentPitch", "SegmentRadius", "OreDensity", "OreRadiusMult"
    };
    private static final String[] CLOUD_SETTINGS = new String[] {
            "DistributionFrequency", "ParentRangeLimit", "CloudRadius", "CloudThickness",
            "CloudSizeNoise", "CloudHeight", "CloudInclination", "OreDensity",
            "OreVolumeNoiseCutoff", "OreRadiusMult"
    };
    private static final Map<String, String> SETTING_HELP = createSettingHelp();
    private static final String[] BIOME_GATE_KINDS = new String[] { "Biome", "BiomeType" };
    private static final String[] BIOME_CATEGORIES = new String[] {
            "River", "Jungle", "Lush", "Mushroom", "Hot", "Hills", "Dry", "Sparse",
            "Beach", "Cold", "Sandy", "Wasteland", "Nether", "Spooky", "Void", "Wet",
            "Magical", "Plains", "Mountain", "Ocean", "End", "Water", "Forest",
            "Coniferous", "Mesa", "Dead", "Swamp", "Savanna", "Snowy", "Dense", "Rare"
    };
    private static final String GROUP_CATEGORY = "Category";
    private static final String GROUP_MINECRAFT = "minecraft";
    private final List<VeinDefinition> loadedDefinitions = new ArrayList<VeinDefinition>();
    private final List<OreDictionaryEntry> oreDictionary = new ArrayList<OreDictionaryEntry>();
    private final List<BiomeDictionaryEntry> biomeDictionary = new ArrayList<BiomeDictionaryEntry>();
    private final JComboBox<VeinDefinition> distributionBox = new JComboBox<VeinDefinition>();
    private final JComboBox<String> modeBox = new JComboBox<String>(new String[] { "Single vein", "Region" });
    private final JComboBox<String> regionViewBox = new JComboBox<String>(new String[] { "3D", "2D top-down" });
    private final JComboBox<String> renderDetailBox = new JComboBox<String>(new String[] { "Auto", "Full cubes", "Fast points" });
    private final JTextField seedField = new JTextField(Long.toString(new Random().nextLong()), 18);
    private final JCheckBox stackAllBox = new JCheckBox("Stack all");
    private final JCheckBox groundGridBox = new JCheckBox("Ground grid");
    private final JTextField groundYField = new JTextField("70", 4);
    private final JTextField regionXField = new JTextField("1", 3);
    private final JTextField regionZField = new JTextField("1", 3);
    private final JLabel statusLabel = new JLabel("Ready");
    private final JTextArea statsArea = new JTextArea(9, 30);
    private final JTextArea xmlPreview = new JTextArea(22, 34);
    private final JPanel editorPanel = new JPanel(new BorderLayout());
    private final JPanel oreDictionaryPanel = new JPanel(new BorderLayout());
    private final JPanel biomeDictionaryPanel = new JPanel(new BorderLayout());
    private final OpenGlViewport viewport = new OpenGlViewport();
    private final DefaultListModel<File> fileListModel = new DefaultListModel<File>();
    private final JList<File> fileBrowser = new JList<File>(fileListModel);
    private final Set<File> checkedXmlFiles = new HashSet<File>();
    private final Map<VeinDefinition, Boolean> collapsedVeins = new IdentityHashMap<VeinDefinition, Boolean>();
    private final Set<File> dirtyXmlFiles = new HashSet<File>();
    private final Preferences preferences = Preferences.userNodeForPackage(LwjglOreVeinVisualizerApp.class);
    private File browserRoot = new File("C:\\Users\\Ethan\\curseforge\\minecraft\\Instances\\HBMHell Test\\config\\CustomOreGen");
    private File activeBiomeDictionaryFile;
    private JLabel browserRootLabel;
    private JScrollPane editorScrollPane;
    private SwingWorker<RenderScene, Void> renderWorker;
    private VeinDefinition selectedDefinition = VeinDefinition.sample();
    private boolean unsavedXmlDirty;
    private boolean rebuildingEditor;
    private boolean refreshingDistributionBox;
    private long renderRequest;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                installDarkDefaults();
                ToolTipManager.sharedInstance().setInitialDelay(1000);
                ToolTipManager.sharedInstance().setDismissDelay(20000);
                ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
                new LwjglOreVeinVisualizerApp().show();
            }
        });
    }

    private static Map<String, String> createSettingHelp() {
        Map<String, String> help = new HashMap<String, String>();
        help.put("MotherlodeFrequency", "Motherlodes per 16x16 chunk. Higher than 1 can create multiple motherlodes in one chunk. Also called DistributionFrequency.");
        help.put("DistributionFrequency", "Clouds per 16x16 chunk. COG samples this for each chunk; values above 1 can create multiple clouds in one chunk.");
        help.put("MotherlodeRangeLimit", "For child veins, limits horizontal distance from the parent motherlode/cloud/cluster. Ignored when the vein has no parent.");
        help.put("ParentRangeLimit", "For child clouds, limits horizontal distance from the nearest parent cloud, motherlode, or cluster. Height difference is ignored. Root clouds ignore this.");
        help.put("MotherlodeHeight", "Y-level where motherlodes generate. Range and normal type make heights vary around the average.");
        help.put("MotherlodeSize", "Motherlode radius in blocks/meters. Higher range makes round lodes more irregular.");
        help.put("CloudRadius", "Horizontal cloud radius in blocks/meters. COG samples separate X and Z radii from this setting, so range can make clouds oval.");
        help.put("CloudThickness", "Vertical cloud radius in blocks/meters. This controls how tall the ellipsoid cloud is before noise and ore radius multipliers are applied.");
        help.put("CloudSizeNoise", "Noise level added to the cloud surface. Zero makes a smooth ellipsoid; higher values carve bumps and irregular edges.");
        help.put("CloudHeight", "Y-level where cloud centers generate. Range and normal type vary the center height around the average.");
        help.put("CloudInclination", "Cloud tilt from the horizontal XZ plane in radians. Zero is level; PI/2 would stand the cloud nearly upright.");
        help.put("BranchFrequency", "Number of main branches generated from each motherlode.");
        help.put("BranchInclination", "Initial branch angle from the horizontal plane, in radians. Low values are flatter; high values climb or dive more.");
        help.put("BranchLength", "Total length of each branch path. Twisting branches may use length without traveling far from the lode.");
        help.put("BranchHeightLimit", "Maximum vertical distance branches can move above or below the motherlode before being cut off.");
        help.put("SegmentForkFrequency", "Chance that a branch forks at the end of a segment. Values above 1 can create multiple fork branches.");
        help.put("SegmentForkLengthMult", "Multiplier applied to remaining branch length for forks. Kept between 0 and 1 so forks are not longer than the parent branch.");
        help.put("SegmentLength", "Length of each straight branch segment before the branch turns or forks.");
        help.put("SegmentAngle", "How sharply each segment turns away from the previous one. Low is smooth; high is zig-zaggy.");
        help.put("SegmentPitch", "Vertical pitch change applied as branches continue or fork. Higher range creates more vertical waviness.");
        help.put("SegmentRadius", "Cross-section radius of branch segments. Very small radii can produce broken or sparse branches.");
        help.put("OreDensity", "Per-block placement density from 0 to 1. For clouds, this is applied after the block passes the ellipsoid and internal noise checks.");
        help.put("OreVolumeNoiseCutoff", "Internal cloud noise threshold. Zero ignores internal volume noise; higher values hollow and break up the cloud. At 1 almost nothing places.");
        help.put("OreRadiusMult", "Per-block multiplier for the maximum allowed radius. For clouds, range makes the cloud edge more ragged; for veins, it affects motherlode and branch thickness.");
        return help;
    }

    private LwjglOreVeinVisualizerApp() {
        restoreBrowserRootPreference();
        loadOreDictionary();
        loadBiomeDictionary();
        loadedDefinitions.add(selectedDefinition);
        distributionBox.addItem(selectedDefinition);
        updateXmlPreview();
    }

    private void show() {
        JFrame frame = new JFrame("CustomOreGen Vein Visualizer - LWJGL");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (confirmSaveXmlChanges(frame)) {
                    frame.dispose();
                    System.exit(0);
                }
            }
        });
        frame.setLayout(new BorderLayout());
        frame.add(createToolbar(frame), BorderLayout.NORTH);
        frame.add(viewport, BorderLayout.CENTER);
        frame.add(createFileBrowser(frame), BorderLayout.WEST);
        frame.add(createSidePanel(), BorderLayout.EAST);
        installShortcuts(frame);
        installTooltipViewportRepaint(frame);
        frame.setSize(1280, 820);
        frame.setLocationRelativeTo(null);
        applyDarkTheme(frame);
        frame.setVisible(true);
        refreshFileBrowser();
        regenerate();
    }

    private void installShortcuts(final JFrame frame) {
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save-selected-distribution");
        frame.getRootPane().getActionMap().put("save-selected-distribution", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                saveSelectedDistributionXml(frame);
            }
        });
    }

    private void installTooltipViewportRepaint(final JFrame frame) {
        final Timer tooltipRepaintTimer = new Timer(90, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                viewport.refreshNow();
            }
        });
        tooltipRepaintTimer.setRepeats(false);
        Toolkit.getDefaultToolkit().addAWTEventListener(new java.awt.event.AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (!(event instanceof MouseEvent)) {
                    return;
                }
                MouseEvent mouseEvent = (MouseEvent) event;
                int id = mouseEvent.getID();
                if (id != MouseEvent.MOUSE_MOVED && id != MouseEvent.MOUSE_EXITED
                        && id != MouseEvent.MOUSE_ENTERED) {
                    return;
                }
                Object source = mouseEvent.getSource();
                if (!(source instanceof Component) || source == viewport) {
                    return;
                }
                if (SwingUtilities.getWindowAncestor((Component) source) == frame) {
                    tooltipRepaintTimer.restart();
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    private JPanel createToolbar(final JFrame frame) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton loadXml = new JButton("Load XML");
        JButton exportRender = new JButton("Export Render");
        JButton randomSeed = new JButton("Random Seed");
        regionViewBox.setPreferredSize(new Dimension(105, 24));
        renderDetailBox.setPreferredSize(new Dimension(100, 24));
        regionXField.setPreferredSize(new Dimension(40, 24));
        regionZField.setPreferredSize(new Dimension(40, 24));
        ActionListener redraw = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                regenerate();
            }
        };
        modeBox.addActionListener(redraw);
        regionViewBox.addActionListener(redraw);
        renderDetailBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                viewport.setRenderOptions(String.valueOf(renderDetailBox.getSelectedItem()), isRegionMode());
                viewport.invalidateDisplayList();
            }
        });
        regionXField.getDocument().addDocumentListener(changeListener(new Runnable() {
            @Override
            public void run() {
                regenerate();
            }
        }));
        regionZField.getDocument().addDocumentListener(changeListener(new Runnable() {
            @Override
            public void run() {
                regenerate();
            }
        }));
        groundGridBox.setSelected(false);
        groundGridBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                regenerate();
            }
        });
        groundYField.getDocument().addDocumentListener(changeListener(new Runnable() {
            @Override
            public void run() {
                regenerate();
            }
        }));

        distributionBox.setPreferredSize(new Dimension(280, 26));
        distributionBox.setRenderer(new VeinDefinitionCellRenderer());
        distributionBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (refreshingDistributionBox) {
                    return;
                }
                VeinDefinition selected = (VeinDefinition) distributionBox.getSelectedItem();
                if (selected != null) {
                    selectedDefinition = selected;
                    rebuildEditor();
                    updateXmlPreview();
                    regenerate();
                }
            }
        });

        loadXml.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                loadXml(frame);
            }
        });
        exportRender.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                exportCurrentRender(frame);
            }
        });
        randomSeed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                seedField.setText(Long.toString(new Random().nextLong()));
                regenerate();
            }
        });
        stackAllBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                regenerate();
            }
        });

        toolbar.add(loadXml);
        toolbar.add(exportRender);
        toolbar.add(new JLabel("Distribution"));
        toolbar.add(distributionBox);
        toolbar.add(new JLabel("Seed"));
        toolbar.add(seedField);
        toolbar.add(randomSeed);
        toolbar.add(stackAllBox);
        toolbar.add(new JLabel("Mode"));
        toolbar.add(modeBox);
        toolbar.add(new JLabel("Region View"));
        toolbar.add(regionViewBox);
        toolbar.add(new JLabel("Detail"));
        toolbar.add(renderDetailBox);
        toolbar.add(new JLabel("Regions"));
        toolbar.add(regionXField);
        toolbar.add(new JLabel("x"));
        toolbar.add(regionZField);
        toolbar.add(groundGridBox);
        toolbar.add(new JLabel("Ground Y"));
        toolbar.add(groundYField);
        toolbar.add(statusLabel);
        return toolbar;
    }

    private JPanel createSidePanel() {
        statsArea.setEditable(false);
        statsArea.setBackground(new Color(24, 27, 32));
        statsArea.setForeground(new Color(230, 236, 245));
        statsArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, 0));
        panel.setMinimumSize(new Dimension(SIDE_PANEL_WIDTH, 0));

        xmlPreview.setBackground(new Color(24, 27, 32));
        xmlPreview.setForeground(new Color(230, 236, 245));
        xmlPreview.setCaretColor(Color.WHITE);
        xmlPreview.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        xmlPreview.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));

        JButton applyXml = new JButton("Apply XML Text");
        applyXml.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                applyXmlText();
            }
        });
        JPanel xmlPanel = new JPanel(new BorderLayout(6, 6));
        xmlPanel.add(new JScrollPane(xmlPreview), BorderLayout.CENTER);
        xmlPanel.add(applyXml, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Stats", new JScrollPane(statsArea));
        tabs.addTab("Editor", editorPanel);
        tabs.addTab("Ore Dictionary", oreDictionaryPanel);
        tabs.addTab("Biome Dictionary", biomeDictionaryPanel);
        tabs.addTab("XML", xmlPanel);
        panel.add(tabs, BorderLayout.CENTER);
        rebuildEditor();
        refreshOreDictionaryTab();
        refreshBiomeDictionaryTab();
        applyDarkTheme(panel);
        return panel;
    }

    private JPanel createFileBrowser(final JFrame frame) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setPreferredSize(new Dimension(300, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel header = new JPanel(new BorderLayout(6, 4));
        JLabel title = new JLabel("XML Browser");
        browserRootLabel = new JLabel(browserRoot.getName());
        JButton folder = new JButton("Folder");
        folder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                chooseBrowserRoot(frame);
            }
        });
        header.add(title, BorderLayout.NORTH);
        header.add(browserRootLabel, BorderLayout.CENTER);
        header.add(folder, BorderLayout.EAST);

        fileBrowser.setCellRenderer(new XmlFileCellRenderer());
        fileBrowser.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int index = fileBrowser.locationToIndex(event.getPoint());
                if (index < 0) {
                    return;
                }
                File file = fileListModel.get(index);
                Set<File> previousChecked = new HashSet<File>(checkedXmlFiles);
                if (checkedXmlFiles.contains(file)) {
                    checkedXmlFiles.remove(file);
                } else {
                    checkedXmlFiles.add(file);
                }
                fileBrowser.repaint();
                if (!loadCheckedXmlFiles()) {
                    checkedXmlFiles.clear();
                    checkedXmlFiles.addAll(previousChecked);
                    fileBrowser.repaint();
                }
            }
        });

        JButton loadChecked = new JButton("Load Checked XML");
        loadChecked.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                loadCheckedXmlFiles();
            }
        });
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refreshFileBrowser();
            }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(loadChecked);
        buttons.add(refresh);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(fileBrowser), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void restoreBrowserRootPreference() {
        String path = preferences.get("lwjglBrowserRoot", null);
        if (path != null) {
            File saved = new File(path);
            if (saved.isDirectory()) {
                browserRoot = saved;
            }
        }
    }

    private void loadOreDictionary() {
        oreDictionary.clear();
        String saved = preferences.get("lwjglOreDictionary", "");
        if (saved != null && saved.trim().length() > 0) {
            String[] entries = saved.split("\\n");
            for (String encoded : entries) {
                String[] parts = encoded.split("\\|", -1);
                if (parts.length >= 3 && parts[1].trim().length() > 0) {
                    oreDictionary.add(new OreDictionaryEntry(unescapePref(parts[0]), unescapePref(parts[1]), unescapePref(parts[2])));
                }
            }
        }
        if (oreDictionary.isEmpty()) {
            addDefaultOreDictionary();
            saveOreDictionary();
        }
    }

    private void loadBiomeDictionary() {
        biomeDictionary.clear();
        preferences.remove("lwjglBiomeDictionary");
        activeBiomeDictionaryFile = rememberedBiomeDictionaryFile();
        if (activeBiomeDictionaryFile == null) {
            activeBiomeDictionaryFile = defaultBiomeDictionaryFile();
        }
        if (activeBiomeDictionaryFile.isFile()) {
            try {
                biomeDictionary.addAll(parseBiomeDictionaryJson(new String(Files.readAllBytes(activeBiomeDictionaryFile.toPath()),
                        Charset.forName("UTF-8"))));
            } catch (Exception ex) {
                statusLabel.setText("Biome dictionary load failed: " + ex.getMessage());
            }
        }
        if (biomeDictionary.isEmpty()) {
            addDefaultBiomeDictionary();
        }
        saveBiomeDictionary();
    }

    private void addDefaultOreDictionary() {
        oreDictionary.add(new OreDictionaryEntry("Iron Ore", "minecraft:iron_ore", colorHex(colorForOreBlock("minecraft:iron_ore"))));
        oreDictionary.add(new OreDictionaryEntry("Gold Ore", "minecraft:gold_ore", colorHex(colorForOreBlock("minecraft:gold_ore"))));
        oreDictionary.add(new OreDictionaryEntry("Coal Ore", "minecraft:coal_ore", colorHex(colorForOreBlock("minecraft:coal_ore"))));
        oreDictionary.add(new OreDictionaryEntry("Diamond Ore", "minecraft:diamond_ore", colorHex(colorForOreBlock("minecraft:diamond_ore"))));
        oreDictionary.add(new OreDictionaryEntry("Redstone Ore", "minecraft:redstone_ore", colorHex(colorForOreBlock("minecraft:redstone_ore"))));
        oreDictionary.add(new OreDictionaryEntry("Lapis Ore", "minecraft:lapis_ore", colorHex(colorForOreBlock("minecraft:lapis_ore"))));
        oreDictionary.add(new OreDictionaryEntry("Emerald Ore", "minecraft:emerald_ore", colorHex(colorForOreBlock("minecraft:emerald_ore"))));
        oreDictionary.add(new OreDictionaryEntry("Quartz Ore", "minecraft:quartz_ore", colorHex(colorForOreBlock("minecraft:quartz_ore"))));
    }

    private void addDefaultBiomeDictionary() {
        for (String category : BIOME_CATEGORIES) {
            addBiomeDictionaryDefault(toTitleCase(category), "BiomeType", toTitleCase(category), GROUP_CATEGORY);
        }
        String[] vanillaBiomes = new String[] {
                "minecraft:beaches", "minecraft:birch_forest", "minecraft:birch_forest_hills",
                "minecraft:cold_beach", "minecraft:deep_ocean", "minecraft:desert",
                "minecraft:desert_hills", "minecraft:extreme_hills", "minecraft:extreme_hills_with_trees",
                "minecraft:forest", "minecraft:forest_hills", "minecraft:frozen_ocean",
                "minecraft:frozen_river", "minecraft:hell", "minecraft:ice_flats",
                "minecraft:ice_mountains", "minecraft:jungle", "minecraft:jungle_edge",
                "minecraft:jungle_hills", "minecraft:mesa", "minecraft:mesa_clear_rock",
                "minecraft:mesa_rock", "minecraft:mushroom_island", "minecraft:mushroom_island_shore",
                "minecraft:mutated_birch_forest", "minecraft:mutated_birch_forest_hills",
                "minecraft:mutated_desert", "minecraft:mutated_extreme_hills",
                "minecraft:mutated_extreme_hills_with_trees", "minecraft:mutated_forest",
                "minecraft:mutated_ice_flats", "minecraft:mutated_jungle", "minecraft:mutated_jungle_edge",
                "minecraft:mutated_mesa", "minecraft:mutated_mesa_clear_rock", "minecraft:mutated_mesa_rock",
                "minecraft:mutated_plains", "minecraft:mutated_redwood_taiga",
                "minecraft:mutated_redwood_taiga_hills", "minecraft:mutated_roofed_forest",
                "minecraft:mutated_savanna", "minecraft:mutated_savanna_rock",
                "minecraft:mutated_swampland", "minecraft:mutated_taiga",
                "minecraft:mutated_taiga_cold", "minecraft:ocean", "minecraft:plains",
                "minecraft:redwood_taiga", "minecraft:redwood_taiga_hills", "minecraft:river",
                "minecraft:roofed_forest", "minecraft:savanna", "minecraft:savanna_rock",
                "minecraft:sky", "minecraft:smaller_extreme_hills", "minecraft:stone_beach",
                "minecraft:swampland", "minecraft:taiga", "minecraft:taiga_cold",
                "minecraft:taiga_cold_hills", "minecraft:taiga_hills", "minecraft:void"
        };
        for (String biome : vanillaBiomes) {
            String namespace = namespaceFromRegistry(biome);
            String path = pathFromRegistry(biome);
            addBiomeDictionaryDefault(displayNameFromRegistry(biome), "Biome", path, namespace);
        }
    }

    private void addBiomeDictionaryDefault(String name, String kind, String matcher, String group) {
        for (BiomeDictionaryEntry entry : biomeDictionary) {
            if (entry.kind.equalsIgnoreCase(kind) && entry.matcher.equalsIgnoreCase(matcher)
                    && entry.group.equalsIgnoreCase(group)) {
                if (entry.group == null || entry.group.length() == 0) {
                    entry.group = group;
                }
                return;
            }
        }
        biomeDictionary.add(new BiomeDictionaryEntry(name, kind, matcher, group));
    }

    private void cleanBiomeDictionaryDefaults() {
        for (int i = biomeDictionary.size() - 1; i >= 0; i--) {
            BiomeDictionaryEntry entry = biomeDictionary.get(i);
            String label = entry.nameForEditor().trim();
            String normalized = label.toLowerCase(Locale.ENGLISH);
            if ("Biome".equalsIgnoreCase(entry.kind)
                    && ("all biomes".equals(normalized)
                    || "exclude ocean".equals(normalized)
                    || "exclude desert".equals(normalized)
                    || ".*".equals(entry.matcher))) {
                biomeDictionary.remove(i);
                continue;
            }
            if ("BiomeType".equalsIgnoreCase(entry.kind)) {
                if ("hot/sandy category".equals(normalized) || "hot/sandy".equals(normalized)) {
                    biomeDictionary.remove(i);
                    continue;
                }
                if (label.toLowerCase(Locale.ENGLISH).endsWith(" category")) {
                    entry.setEditorName(label.substring(0, label.length() - " Category".length()).trim());
                }
            }
        }
    }

    private void saveOreDictionary() {
        StringBuilder text = new StringBuilder();
        for (OreDictionaryEntry entry : oreDictionary) {
            if (entry.block == null || entry.block.trim().length() == 0) {
                continue;
            }
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(escapePref(entry.name)).append('|')
                    .append(escapePref(entry.block)).append('|')
                    .append(escapePref(entry.colorHex));
        }
        preferences.put("lwjglOreDictionary", text.toString());
    }

    private void saveBiomeDictionary() {
        if (activeBiomeDictionaryFile == null) {
            activeBiomeDictionaryFile = defaultBiomeDictionaryFile();
        }
        try {
            Files.write(activeBiomeDictionaryFile.toPath(), buildBiomeDictionaryJson().getBytes(Charset.forName("UTF-8")));
            preferences.put(PREF_BIOME_DICTIONARY_FILE, activeBiomeDictionaryFile.getAbsolutePath());
            File parent = activeBiomeDictionaryFile.getParentFile();
            if (parent != null) {
                preferences.put("lwjglOreDictionaryFolder", parent.getAbsolutePath());
            }
        } catch (Exception ex) {
            statusLabel.setText("Biome dictionary save failed: " + ex.getMessage());
        }
    }

    private String escapePref(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n");
    }

    private String unescapePref(String text) {
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escaped) {
                out.append(ch == 'p' ? '|' : ch == 'n' ? '\n' : ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else {
                out.append(ch);
            }
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }

    private void chooseBrowserRoot(JFrame frame) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (browserRoot.isDirectory()) {
            chooser.setCurrentDirectory(browserRoot);
        }
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        browserRoot = chooser.getSelectedFile();
        preferences.put("lwjglBrowserRoot", browserRoot.getAbsolutePath());
        refreshFileBrowser();
    }

    private void refreshFileBrowser() {
        Set<File> loadedFiles = loadedXmlSourceFiles();
        fileListModel.clear();
        checkedXmlFiles.retainAll(loadedFiles);
        if (browserRootLabel != null) {
            browserRootLabel.setText(browserRoot.getName());
        }
        if (!browserRoot.isDirectory()) {
            return;
        }
        File custom = new File(browserRoot, "modules\\custom");
        File defaults = new File(browserRoot, "modules\\default");
        if (custom.isDirectory()) {
            addXmlFiles(custom);
        }
        if (defaults.isDirectory()) {
            addXmlFiles(defaults);
        }
        addXmlFiles(browserRoot);
        checkedXmlFiles.addAll(loadedFilesInBrowser());
        fileBrowser.repaint();
    }

    private void syncCheckedXmlFilesToLoadedDefinitions() {
        checkedXmlFiles.clear();
        checkedXmlFiles.addAll(loadedFilesInBrowser());
        fileBrowser.repaint();
    }

    private Set<File> loadedXmlSourceFiles() {
        Set<File> files = new HashSet<File>();
        for (VeinDefinition def : loadedDefinitions) {
            collectSourceFiles(def, files);
        }
        return files;
    }

    private void collectSourceFiles(VeinDefinition def, Set<File> files) {
        if (def.sourceFile != null) {
            files.add(def.sourceFile);
        }
        for (VeinDefinition child : def.children) {
            collectSourceFiles(child, files);
        }
    }

    private List<File> loadedFilesInBrowser() {
        Set<File> loadedFiles = loadedXmlSourceFiles();
        List<File> present = new ArrayList<File>();
        for (int i = 0; i < fileListModel.size(); i++) {
            File file = fileListModel.get(i);
            if (loadedFiles.contains(file)) {
                present.add(file);
            }
        }
        return present;
    }

    private void addXmlFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        java.util.Arrays.sort(files);
        for (File file : files) {
            if (file.isDirectory()) {
                addXmlFiles(file);
            } else if (file.getName().toLowerCase().endsWith(".xml")
                    && !"CustomOreGen_Config.xml".equalsIgnoreCase(file.getName())
                    && !containsFile(file)) {
                fileListModel.addElement(file);
            }
        }
    }

    private boolean containsFile(File file) {
        for (int i = 0; i < fileListModel.size(); i++) {
            if (fileListModel.get(i).equals(file)) {
                return true;
            }
        }
        return false;
    }

    private boolean loadCheckedXmlFiles() {
        List<File> files = checkedXmlFilesInBrowserOrder();
        Set<File> targetFiles = new HashSet<File>(files);
        Set<File> unloading = loadedXmlSourceFiles();
        unloading.removeAll(targetFiles);
        boolean unloadUnsourced = !unsourcedDefinitions().isEmpty() && !files.isEmpty();
        if (!confirmSaveXmlUnload(fileBrowser, unloading, unloadUnsourced)) {
            return false;
        }
        if (files.isEmpty()) {
            if (!confirmSaveXmlUnload(fileBrowser, loadedXmlSourceFiles(), !unsourcedDefinitions().isEmpty())) {
                return false;
            }
            resetToSampleDefinition();
            statusLabel.setText("No XML files checked");
            return true;
        }
        loadXmlFiles(files);
        return true;
    }

    private void resetToSampleDefinition() {
        collapsedVeins.clear();
        loadedDefinitions.clear();
        clearXmlDirtyState();
        selectedDefinition = VeinDefinition.sample();
        loadedDefinitions.add(selectedDefinition);
        refreshDistributionBox();
        updateXmlPreview();
        rebuildEditor();
        regenerate();
    }

    private List<File> checkedXmlFilesInBrowserOrder() {
        List<File> files = new ArrayList<File>();
        for (int i = 0; i < fileListModel.size(); i++) {
            File file = fileListModel.get(i);
            if (checkedXmlFiles.contains(file)) {
                files.add(file);
            }
        }
        return files;
    }

    private void loadXml(JFrame frame) {
        if (!confirmSaveXmlChanges(frame)) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CustomOreGen XML", "xml"));
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        List<VeinDefinition> parsed = new ArrayList<VeinDefinition>();
        for (File file : chooser.getSelectedFiles()) {
            parsed.addAll(loadXmlFile(file));
        }
        replaceLoadedDefinitions(parsed, true);
    }

    private void loadXmlFiles(List<File> files) {
        List<VeinDefinition> parsed = new ArrayList<VeinDefinition>();
        for (File file : files) {
            List<VeinDefinition> existing = definitionsForSource(file);
            if (!existing.isEmpty()) {
                parsed.addAll(existing);
            } else {
                parsed.addAll(loadXmlFile(file));
            }
        }
        replaceLoadedDefinitions(parsed, false);
        dirtyXmlFiles.retainAll(new HashSet<File>(files));
    }

    private List<VeinDefinition> loadXmlFile(File file) {
        try {
            return CogXmlLoader.load(file);
        } catch (Exception ex) {
            statusLabel.setText("Could not load " + file.getName() + ": " + ex.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    private void replaceLoadedDefinitions(List<VeinDefinition> parsed) {
        replaceLoadedDefinitions(parsed, true);
    }

    private void replaceLoadedDefinitions(List<VeinDefinition> parsed, boolean clearDirty) {
        if (parsed.isEmpty()) {
            return;
        }
        VeinDefinition previousSelection = selectedDefinition;
        collapsedVeins.clear();
        loadedDefinitions.clear();
        loadedDefinitions.addAll(parsed);
        if (clearDirty) {
            clearXmlDirtyState();
        }
        selectedDefinition = parsed.contains(previousSelection) ? previousSelection : loadedDefinitions.get(0);
        syncCheckedXmlFilesToLoadedDefinitions();
        refreshDistributionBox();
        updateXmlPreview();
        rebuildEditor();
        regenerate();
    }

    private void refreshDistributionBox() {
        refreshingDistributionBox = true;
        try {
            distributionBox.removeAllItems();
            for (VeinDefinition def : loadedDefinitions) {
                distributionBox.addItem(def);
            }
            distributionBox.setSelectedItem(selectedDefinition);
        } finally {
            refreshingDistributionBox = false;
        }
        distributionBox.repaint();
    }

    private void addParentVein() {
        VeinDefinition created = VeinDefinition.sample();
        created.name = "New_Parent_Vein_" + (loadedDefinitions.size() + 1);
        int insertAt = loadedDefinitions.indexOf(selectedDefinition) + 1;
        if (insertAt <= 0 || insertAt > loadedDefinitions.size()) {
            loadedDefinitions.add(created);
        } else {
            loadedDefinitions.add(insertAt, created);
        }
        selectedDefinition = created;
        refreshDistributionBox();
        editorChanged();
        rebuildEditor();
    }

    private void removeParentVein(VeinDefinition target) {
        if (loadedDefinitions.size() <= 1) {
            return;
        }
        int oldIndex = loadedDefinitions.indexOf(target);
        loadedDefinitions.remove(target);
        if (loadedDefinitions.isEmpty()) {
            VeinDefinition created = VeinDefinition.sample();
            created.name = "New_Parent_Vein";
            loadedDefinitions.add(created);
        }
        int nextIndex = oldIndex < 0 ? 0 : Math.min(oldIndex, loadedDefinitions.size() - 1);
        selectedDefinition = loadedDefinitions.get(nextIndex);
        refreshDistributionBox();
    }

    private boolean removeChildFromRoots(VeinDefinition target) {
        for (VeinDefinition root : loadedDefinitions) {
            if (removeChild(root, target)) {
                return true;
            }
        }
        return false;
    }

    private boolean removeChild(VeinDefinition parent, VeinDefinition target) {
        if (parent.children.remove(target)) {
            return true;
        }
        for (VeinDefinition child : parent.children) {
            if (removeChild(child, target)) {
                return true;
            }
        }
        return false;
    }

    private void updateXmlPreview() {
        xmlPreview.setText(DefinitionXmlWriter.writeConfig(loadedDefinitions));
        xmlPreview.setCaretPosition(0);
    }

    private void applyXmlText() {
        try {
            List<VeinDefinition> parsed = CogXmlLoader.loadText(xmlPreview.getText(), "LWJGL XML editor");
            clearXmlDirtyState();
            replaceLoadedDefinitions(parsed, false);
            markSelectedXmlDirty();
            statusLabel.setText("Applied XML text");
        } catch (Exception ex) {
            statusLabel.setText("XML error: " + ex.getMessage());
        }
    }

    private void saveCurrentXml(boolean saveAs) {
        File file = saveAs ? null : selectedDefinition.sourceFile;
        if (file == null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CustomOreGen XML", "xml"));
            File initial = selectedDefinition.sourceFile != null ? selectedDefinition.sourceFile.getParentFile() : browserRoot;
            if (initial != null && initial.isDirectory()) {
                chooser.setCurrentDirectory(initial);
            }
            if (chooser.showSaveDialog(editorPanel) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".xml")) {
                file = new File(file.getParentFile(), file.getName() + ".xml");
            }
        }
        try {
            writeCurrentXml(file);
            statusLabel.setText("Saved " + file.getName());
            refreshFileBrowser();
        } catch (Exception ex) {
            statusLabel.setText("Save failed: " + ex.getMessage());
        }
    }

    private void saveSelectedDistributionXml(Component owner) {
        if (selectedDefinition == null) {
            statusLabel.setText("No selected distribution to save");
            return;
        }
        File file = selectedDefinition == null ? null : selectedDefinition.sourceFile;
        if (file == null) {
            file = chooseSaveXmlFile(owner, selectedDefinition == null ? "selected-distribution.xml"
                    : selectedDefinition.nameForXml() + ".xml");
            if (file == null) {
                return;
            }
        }
        try {
            List<VeinDefinition> single = java.util.Collections.singletonList(selectedDefinition);
            boolean hadUnsourced = selectedDefinition.sourceFile == null;
            Set<File> previousSources = sourceFilesForGroup(single);
            writeDefinitionsXml(file, single);
            setSourceFileRecursive(selectedDefinition, file);
            for (File previousSource : previousSources) {
                dirtyXmlFiles.remove(previousSource);
            }
            dirtyXmlFiles.remove(file);
            if (hadUnsourced) {
                unsavedXmlDirty = false;
            }
            updateXmlPreview();
            refreshFileBrowser();
            statusLabel.setText("Saved selected distribution " + selectedDefinition.nameForXml());
        } catch (Exception ex) {
            statusLabel.setText("Save failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(owner, "Save failed: " + ex.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void writeCurrentXml(File file) throws IOException {
        List<VeinDefinition> group = getCurrentXmlDefinitions();
        boolean hadUnsourced = groupContainsUnsourced(group);
        Set<File> previousSources = sourceFilesForGroup(group);
        writeDefinitionsXml(file, group);
        for (VeinDefinition def : group) {
            setSourceFileRecursive(def, file);
        }
        for (File previousSource : previousSources) {
            dirtyXmlFiles.remove(previousSource);
        }
        dirtyXmlFiles.remove(file);
        if (hadUnsourced) {
            unsavedXmlDirty = false;
        }
        updateXmlPreview();
    }

    private Set<File> sourceFilesForGroup(List<VeinDefinition> group) {
        Set<File> files = new HashSet<File>();
        for (VeinDefinition def : group) {
            if (def.sourceFile != null) {
                files.add(def.sourceFile);
            }
        }
        return files;
    }

    private void writeDefinitionsXml(File file, List<VeinDefinition> group) throws IOException {
        String xml = DefinitionXmlWriter.writeConfigForSave(group);
        Files.write(file.toPath(), xml.getBytes(Charset.forName("UTF-8")));
    }

    private void exportCurrentRender(Component owner) {
        long seed = parseSeed(seedField.getText());
        seedField.setText(Long.toString(seed));
        List<VeinDefinition> roots = copyActiveRoots();
        RenderScene scene = buildScene(roots, seed);
        List<VeinDefinition> renderedDefinitions = flattenDefinitions(roots);

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("AI Render Context JSON", "json"));
        chooser.setSelectedFile(new File(renderedDefinitions.size() == 1
                ? renderedDefinitions.get(0).nameForXml() + "-render-context.json"
                : "rendered-veins-render-context.json"));
        File initial = getExportInitialDirectory();
        if (initial != null) {
            chooser.setCurrentDirectory(initial);
        }
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".json")) {
            file = new File(file.getParentFile(), file.getName() + ".json");
        }
        try {
            Files.write(file.toPath(), buildRenderExport(roots, renderedDefinitions, scene, seed).getBytes(Charset.forName("UTF-8")));
            rememberExportFolder(file);
            statusLabel.setText("Exported " + file.getName());
        } catch (Exception ex) {
            statusLabel.setText("Export failed: " + ex.getMessage());
        }
    }

    private File getExportInitialDirectory() {
        String saved = preferences.get(PREF_EXPORT_FOLDER, null);
        if (saved != null) {
            File folder = new File(saved);
            if (folder.isDirectory()) {
                return folder;
            }
        }
        if (selectedDefinition.sourceFile != null && selectedDefinition.sourceFile.getParentFile() != null
                && selectedDefinition.sourceFile.getParentFile().isDirectory()) {
            return selectedDefinition.sourceFile.getParentFile();
        }
        return browserRoot != null && browserRoot.isDirectory() ? browserRoot : null;
    }

    private void rememberExportFolder(File file) {
        if (file == null) {
            return;
        }
        File folder = file.isDirectory() ? file : file.getParentFile();
        if (folder != null && folder.isDirectory()) {
            preferences.put(PREF_EXPORT_FOLDER, folder.getAbsolutePath());
        }
    }

    private String buildRenderExport(List<VeinDefinition> roots, List<VeinDefinition> renderedDefinitions,
                                     RenderScene scene, long seed) {
        StringBuilder text = new StringBuilder();
        text.append("{\n");
        appendJsonStringField(text, "schema", "cog-lwjgl-render-context-v1", 2, true);
        appendJsonStringField(text, "intendedUse", "Dense machine-readable context for AI-assisted Minecraft 1.12.2 CustomOreGen vein editing.", 2, true);
        boolean compactRegionExport = isRegionMode();
        appendJsonStringField(text, "coordinateSystem", compactRegionExport
                ? "Region exports are compact: singleVeinSamples describe local vein shape; renderedScene summarizes region placement without dumping every rendered voxel."
                : "definition voxels are local x/y/z block coordinates. renderedScene voxels are final OpenGL scene coordinates after stacking, child offsets, and voxel scaling.", 2, true);
        text.append("  \"view\": {\n");
        appendJsonStringField(text, "renderer", "LWJGL/OpenGL", 4, true);
        text.append("    \"worldSeed\": ").append(seed).append(",\n");
        appendJsonStringField(text, "mode", isRegionMode() ? "Region" : "Single vein", 4, true);
        appendJsonStringField(text, "regionView", isRegionTopDown() ? "2D top-down" : "3D", 4, true);
        int exportRegionsX = clampInt(parseEditorInt(regionXField.getText(), 1), 1, 8);
        int exportRegionsZ = clampInt(parseEditorInt(regionZField.getText(), 1), 1, 8);
        int exportChunksX = exportRegionsX * 32;
        int exportChunksZ = exportRegionsZ * 32;
        appendJsonStringField(text, "viewDescription", isRegionMode()
                ? (isRegionTopDown() ? "Region top-down orthographic projection" : "Region 3D perspective")
                : "Single vein 3D perspective", 4, true);
        appendJsonStringField(text, "frequencyMath", "Checked against COG MapGenOreDistribution: world/chunk seed, distribution seed, two nextInt calls, then PDist integer sampling.", 4, true);
        text.append("    \"regionsX\": ").append(exportRegionsX).append(",\n");
        text.append("    \"regionsZ\": ").append(exportRegionsZ).append(",\n");
        text.append("    \"regionCount\": ").append(exportRegionsX * exportRegionsZ).append(",\n");
        text.append("    \"chunksX\": ").append(exportChunksX).append(",\n");
        text.append("    \"chunksZ\": ").append(exportChunksZ).append(",\n");
        text.append("    \"chunkCount\": ").append((long) exportChunksX * (long) exportChunksZ).append(",\n");
        text.append("    \"chunksPerRegion\": 32,\n");
        text.append("    \"chunkSizeBlocks\": ").append(CHUNK_SIZE).append(",\n");
        text.append("    \"stackAllVeins\": ").append(stackAllBox.isSelected()).append(",\n");
        text.append("    \"groundCutoffEnabled\": ").append(isGroundGridEnabled()).append(",\n");
        text.append("    \"groundCutoffYBlock\": ").append(groundY()).append(",\n");
        text.append("    \"selectedDefinition\": ");
        appendJsonString(text, selectedDefinition.nameForXml());
        text.append(",\n");
        text.append("    \"renderedRootCount\": ").append(roots.size()).append(",\n");
        text.append("    \"renderedDefinitionCountIncludingChildren\": ").append(renderedDefinitions.size()).append("\n");
        text.append("  },\n");
        text.append("  \"renderedScene\": {\n");
        text.append("    \"sizeX\": ").append(scene.sizeX).append(",\n");
        text.append("    \"sizeY\": ").append(scene.sizeY).append(",\n");
        text.append("    \"sizeZ\": ").append(scene.sizeZ).append(",\n");
        text.append("    \"topDownCamera\": ").append(scene.topDown).append(",\n");
        text.append("    \"voxelSizeOpenGlUnits\": ").append(format(OpenGlViewport.VOXEL_SIZE)).append(",\n");
        text.append("    \"renderedVoxelCount\": ").append(scene.voxelCount).append(",\n");
        text.append("    \"cpuSceneBuildMillis\": ").append(scene.buildMillis).append(",\n");
        text.append("    \"compactRegionExport\": ").append(compactRegionExport).append(",\n");
        appendJsonStringField(text, "statsText", scene.stats, 4, true);
        appendRenderedBreakdown(text, scene);
        if (compactRegionExport) {
            text.append(",\n");
            appendRegionPlacementSummary(text, scene);
        } else {
            text.append(",\n");
            appendRenderedSceneVoxels(text, scene);
        }
        text.append("\n  },\n");
        text.append(compactRegionExport ? "  \"singleVeinSamples\": [\n" : "  \"definitions\": [\n");
        for (int i = 0; i < renderedDefinitions.size(); i++) {
            long renderSeed = renderedDefinitions.get(i).sourceSeed == null
                    ? seed + i * 1000003L
                    : renderedDefinitions.get(i).sourceSeed.longValue();
            appendDefinitionExport(text, renderedDefinitions.get(i), i, renderSeed, !compactRegionExport);
            if (i < renderedDefinitions.size() - 1) {
                text.append(",");
            }
            text.append("\n");
        }
        text.append("  ],\n");
        appendJsonStringField(text, "exportedXml", DefinitionXmlWriter.writeConfig(roots), 2, false);
        text.append("}\n");
        return text.toString();
    }

    private void appendRenderedSceneVoxels(StringBuilder text, RenderScene scene) {
        text.append("    \"voxels\": [\n");
        for (int i = 0; i < scene.voxels.size(); i++) {
            Voxel voxel = scene.voxels.get(i);
            text.append("      {\"x\": ").append(format(voxel.x))
                    .append(", \"y\": ").append(format(voxel.y))
                    .append(", \"z\": ").append(format(voxel.z))
                    .append(", \"colorRgb01\": [").append(format(voxel.r)).append(", ")
                    .append(format(voxel.g)).append(", ").append(format(voxel.b)).append("]}");
            if (i < scene.voxels.size() - 1) {
                text.append(",");
            }
            text.append("\n");
        }
        text.append("    ]");
    }

    private void appendRegionPlacementSummary(StringBuilder text, RenderScene scene) {
        Map<String, int[]> summary = new HashMap<String, int[]>();
        for (DefinitionRenderStats stats : scene.definitionStats) {
            int[] values = summary.get(stats.name);
            if (values == null) {
                values = new int[] { 0, 0 };
                summary.put(stats.name, values);
            }
            values[0]++;
            values[1] += stats.total;
        }
        text.append("    \"regionPlacementSummary\": {\n");
        appendJsonStringField(text, "note", "Compact region export omits per-voxel renderedScene.voxels to keep files small for AI tools. Use singleVeinSamples for shape details and renderedBreakdown/regionPlacementSummary for region frequency.", 6, true);
        text.append("      \"instanceCountIncludingChildren\": ").append(scene.definitionStats.size()).append(",\n");
        text.append("      \"byDefinitionName\": [\n");
        int index = 0;
        for (Map.Entry<String, int[]> entry : summary.entrySet()) {
            text.append("        {\"name\": ");
            appendJsonString(text, entry.getKey());
            text.append(", \"instances\": ").append(entry.getValue()[0])
                    .append(", \"renderedBlocks\": ").append(entry.getValue()[1]).append("}");
            if (index++ < summary.size() - 1) {
                text.append(",");
            }
            text.append("\n");
        }
        text.append("      ]\n");
        text.append("    }");
    }

    private void appendRenderedBreakdown(StringBuilder text, RenderScene scene) {
        Map<String, Integer> totals = new HashMap<String, Integer>();
        for (DefinitionRenderStats stats : scene.definitionStats) {
            for (Map.Entry<String, Integer> entry : stats.oreCounts.entrySet()) {
                Integer count = totals.get(entry.getKey());
                totals.put(entry.getKey(), count == null ? entry.getValue() : count + entry.getValue());
            }
        }
        text.append("    \"renderedBreakdown\": {\n");
        text.append("      \"totalBlocks\": ").append(scene.voxelCount).append(",\n");
        text.append("      \"totalByOre\": {");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            if (!first) {
                text.append(", ");
            }
            appendJsonString(text, entry.getKey());
            text.append(": ").append(entry.getValue());
            first = false;
        }
        text.append("},\n");
        text.append("      \"byDefinition\": [\n");
        for (int i = 0; i < scene.definitionStats.size(); i++) {
            DefinitionRenderStats stats = scene.definitionStats.get(i);
            text.append("        {\"index\": ").append(i)
                    .append(", \"depth\": ").append(stats.depth)
                    .append(", \"role\": ");
            appendJsonString(text, stats.depth == 0 ? "mother" : "child");
            text.append(", \"name\": ");
            appendJsonString(text, stats.name);
            text.append(", \"totalBlocks\": ").append(stats.total)
                    .append(", \"byOre\": {");
            first = true;
            for (Map.Entry<String, Integer> entry : stats.oreCounts.entrySet()) {
                if (!first) {
                    text.append(", ");
                }
                appendJsonString(text, entry.getKey());
                text.append(": ").append(entry.getValue());
                first = false;
            }
            text.append("}}");
            if (i < scene.definitionStats.size() - 1) {
                text.append(",");
            }
            text.append("\n");
        }
        text.append("      ]\n");
        text.append("    }");
    }

    private void appendDefinitionExport(StringBuilder text, VeinDefinition def, int index, long seed, boolean includeLocalVoxels) {
        VeinGenerationReport report = new OreVeinGenerator(def).generateReport(seed);
        int[] oreCounts = countOres(report.volume, def, false, 70);
        int[] visibleOreCounts = countOres(report.volume, def, isGroundGridEnabled(), groundY());
        text.append("    {\n");
        text.append("      \"index\": ").append(index).append(",\n");
        appendJsonStringField(text, "name", def.nameForXml(), 6, true);
        appendJsonStringField(text, "sourceFile", def.sourceFile == null ? "" : def.sourceFile.getAbsolutePath(), 6, true);
        appendJsonStringField(text, "distributionType", isCloud(def) ? "Cloud" : "Veins", 6, true);
        appendJsonStringField(text, "branchType", def.branchType == null ? "Bezier" : def.branchType, 6, true);
        text.append("      \"sampleSeed\": ").append(seed).append(",\n");
        text.append("      \"volume\": {\"sizeX\": ").append(report.volume.sizeX)
                .append(", \"sizeY\": ").append(report.volume.sizeY)
                .append(", \"sizeZ\": ").append(report.volume.sizeZ).append("},\n");
        text.append("      \"expectedAverageOreBlocksPerVein\": ").append(format(def.getAverageOreCount())).append(",\n");
        text.append("      \"expectedOreBlocksPerChunk\": ").append(format(def.getExpectedOresPerChunk())).append(",\n");
        text.append("      \"children\": [");
        for (int i = 0; i < def.children.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            appendJsonString(text, def.children.get(i).nameForXml());
        }
        text.append("],\n");
        appendGenerationReportJson(text, report);
        text.append(",\n");
        appendOresJson(text, def, oreCounts, visibleOreCounts);
        text.append(",\n");
        appendSettingsJson(text, def);
        text.append(",\n");
        appendLayerStatsJson(text, report.volume, def, isGroundGridEnabled(), groundY());
        if (includeLocalVoxels) {
            text.append(",\n");
            appendLocalVoxelJson(text, report.volume, def, isGroundGridEnabled(), groundY());
        } else {
            text.append(",\n");
            appendCompactShapeJson(text, report.volume, def, isGroundGridEnabled(), groundY());
        }
        text.append("\n    }");
    }

    private void appendGenerationReportJson(StringBuilder text, VeinGenerationReport report) {
        text.append("      \"generationReport\": {\n");
        text.append("        \"oreBlockCountRaw\": ").append(report.volume.getOreCount()).append(",\n");
        text.append("        \"motherlodeCenter\": {\"x\": ").append(format(report.motherlodeCenter.x))
                .append(", \"y\": ").append(format(report.motherlodeCenter.y))
                .append(", \"z\": ").append(format(report.motherlodeCenter.z)).append("},\n");
        text.append("        \"motherlodeRadius\": ").append(format(report.motherlodeRadius)).append(",\n");
        text.append("        \"primaryBranches\": ").append(report.primaryBranches).append(",\n");
        text.append("        \"forkBranches\": ").append(report.forkBranches).append(",\n");
        text.append("        \"totalBranches\": ").append(report.totalBranches()).append(",\n");
        text.append("        \"branchSegments\": ").append(report.segments).append(",\n");
        text.append("        \"totalBranchPathLength\": ").append(format(report.totalBranchLength)).append(",\n");
        text.append("        \"segmentRadius\": {\"min\": ").append(format(report.minSegmentRadius == Double.POSITIVE_INFINITY ? 0.0 : report.minSegmentRadius))
                .append(", \"avg\": ").append(format(report.averageSegmentRadius()))
                .append(", \"max\": ").append(format(report.maxSegmentRadius)).append("},\n");
        if (report.hasOreBounds()) {
            text.append("        \"oreBounds\": {\"minX\": ").append(report.minOreX)
                    .append(", \"maxX\": ").append(report.maxOreX)
                    .append(", \"minY\": ").append(report.minOreY)
                    .append(", \"maxY\": ").append(report.maxOreY)
                    .append(", \"minZ\": ").append(report.minOreZ)
                    .append(", \"maxZ\": ").append(report.maxOreZ).append("},\n");
            text.append("        \"visibleSize\": {\"x\": ").append(report.widthX())
                    .append(", \"y\": ").append(report.heightY())
                    .append(", \"z\": ").append(report.depthZ()).append("},\n");
        } else {
            text.append("        \"oreBounds\": null,\n");
            text.append("        \"visibleSize\": {\"x\": 0, \"y\": 0, \"z\": 0},\n");
        }
        appendJsonStringField(text, "shapeSummary", describeShape(report), 8, false);
        text.append("      }");
    }

    private void appendOresJson(StringBuilder text, VeinDefinition def, int[] oreCounts, int[] visibleOreCounts) {
        text.append("      \"ores\": [\n");
        for (int i = 0; i < def.oreBlocks.size(); i++) {
            OreBlockDefinition ore = def.oreBlocks.get(i);
            Color color = colorForOre(ore, ore.block);
            text.append("        {\"oreIndex\": ").append(i).append(", \"block\": ");
            appendJsonString(text, ore.block);
            text.append(", \"weight01\": ").append(format(ore.weight));
            text.append(", \"weightPercent\": ").append(oreWeightPercent(ore));
            text.append(", \"displayColorHex\": ");
            appendJsonString(text, colorHex(color));
            text.append(", \"generatedBlockCountRaw\": ").append(i < oreCounts.length ? oreCounts[i] : 0);
            text.append(", \"renderedBlockCountAfterCutoff\": ").append(i < visibleOreCounts.length ? visibleOreCounts[i] : 0).append("}");
            if (i < def.oreBlocks.size() - 1) {
                text.append(",");
            }
            text.append("\n");
        }
        text.append("      ]");
    }

    private int[] countOres(OreVolume volume, VeinDefinition def, boolean groundEnabled, int groundLevel) {
        int[] counts = new int[Math.max(1, def.oreBlocks.size())];
        for (int i = 0; i < volume.getOreCount(); i++) {
            int packed = volume.getOrePosition(i);
            int y = volume.unpackY(packed);
            int oreIndex = volume.getOreIndexAtPosition(packed);
            if (oreIndex >= 0 && oreIndex < counts.length && !isCutOffByGround(def, y, groundEnabled, groundLevel)) {
                counts[oreIndex]++;
            }
        }
        return counts;
    }

    private void appendSettingsJson(StringBuilder text, VeinDefinition def) {
        text.append("      \"settings\": {\n");
        text.append("        \"enabled\": [");
        boolean first = true;
        for (String setting : settingsFor(def)) {
            if (def.enabledSettings.contains(setting) && settingAllowedFor(def, setting)) {
                if (!first) {
                    text.append(", ");
                }
                appendJsonString(text, setting);
                first = false;
            }
        }
        text.append("],\n");
        text.append("        \"values\": {\n");
        first = true;
        for (String setting : settingsFor(def)) {
            if (def.enabledSettings.contains(setting) && settingAllowedFor(def, setting)) {
                if (!first) {
                    text.append(",\n");
                }
                text.append("          ");
                appendJsonString(text, setting);
                text.append(": ");
                appendPDistJson(text, getSetting(def, setting));
                first = false;
            }
        }
        text.append("\n        }\n");
        text.append("      }");
    }

    private void appendPDistJson(StringBuilder text, PDist dist) {
        text.append("{\"avg\": ").append(format(dist.mean))
                .append(", \"range\": ").append(format(dist.range))
                .append(", \"type\": ");
        appendJsonString(text, dist.type.name().toLowerCase(java.util.Locale.ENGLISH));
        text.append("}");
    }

    private void appendLayerStatsJson(StringBuilder text, OreVolume volume, VeinDefinition def,
                                      boolean groundEnabled, int groundLevel) {
        text.append("      \"layerStatsY\": [\n");
        boolean firstLayer = true;
        int[][] layerCounts = new int[volume.sizeY][Math.max(1, def.oreBlocks.size())];
        int[] layerTotals = new int[volume.sizeY];
        for (int i = 0; i < volume.getOreCount(); i++) {
            int packed = volume.getOrePosition(i);
            int y = volume.unpackY(packed);
            int oreIndex = volume.getOreIndexAtPosition(packed);
            if (oreIndex >= 0 && oreIndex < layerCounts[y].length && !isCutOffByGround(def, y, groundEnabled, groundLevel)) {
                layerCounts[y][oreIndex]++;
                layerTotals[y]++;
            }
        }
        for (int y = 0; y < volume.sizeY; y++) {
            if (layerTotals[y] <= 0) {
                continue;
            }
            if (!firstLayer) {
                text.append(",\n");
            }
            text.append("        {\"y\": ").append(y).append(", \"total\": ").append(layerTotals[y]).append(", \"byOre\": [");
            for (int i = 0; i < layerCounts[y].length; i++) {
                if (i > 0) {
                    text.append(", ");
                }
                text.append(layerCounts[y][i]);
            }
            text.append("]}");
            firstLayer = false;
        }
        text.append("\n      ]");
    }

    private void appendLocalVoxelJson(StringBuilder text, OreVolume volume, VeinDefinition def,
                                      boolean groundEnabled, int groundLevel) {
        text.append("      \"localVoxels\": [\n");
        boolean first = true;
        for (int i = 0; i < volume.getOreCount(); i++) {
            int packed = volume.getOrePosition(i);
            int x = volume.unpackX(packed);
            int y = volume.unpackY(packed);
            int z = volume.unpackZ(packed);
            int oreIndex = volume.getOreIndexAtPosition(packed);
            if (oreIndex < 0 || isCutOffByGround(def, y, groundEnabled, groundLevel)) {
                continue;
            }
            if (!first) {
                text.append(",\n");
            }
            text.append("        [").append(x).append(",").append(y).append(",").append(z).append(",").append(oreIndex).append("]");
            first = false;
        }
        text.append("\n      ]");
    }

    private void appendCompactShapeJson(StringBuilder text, OreVolume volume, VeinDefinition def,
                                        boolean groundEnabled, int groundLevel) {
        int total = 0;
        int sampleLimit = 512;
        List<String> samples = new ArrayList<String>();
        for (int i = 0; i < volume.getOreCount(); i++) {
            int packed = volume.getOrePosition(i);
            int x = volume.unpackX(packed);
            int y = volume.unpackY(packed);
            int z = volume.unpackZ(packed);
            int oreIndex = volume.getOreIndexAtPosition(packed);
            if (oreIndex < 0 || isCutOffByGround(def, y, groundEnabled, groundLevel)) {
                continue;
            }
            total++;
            if (samples.size() < sampleLimit) {
                samples.add("[" + x + "," + y + "," + z + "," + oreIndex + "]");
            }
        }
        text.append("      \"compactShape\": {\n");
        appendJsonStringField(text, "note", "localVoxels omitted in compact region exports. voxelSamples contains the first visible local voxels as [x,y,z,oreIndex] examples.", 8, true);
        text.append("        \"visibleLocalVoxelCount\": ").append(total).append(",\n");
        text.append("        \"voxelSampleLimit\": ").append(sampleLimit).append(",\n");
        text.append("        \"voxelSamples\": [");
        for (int i = 0; i < samples.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(samples.get(i));
        }
        text.append("]\n");
        text.append("      }");
    }

    private String describeShape(VeinGenerationReport report) {
        if (!report.hasOreBounds()) {
            return "no ore blocks were placed for this sampled seed.";
        }
        String spread;
        if (report.widthX() > report.depthZ() * 1.4) {
            spread = "wider east-west than north-south";
        } else if (report.depthZ() > report.widthX() * 1.4) {
            spread = "wider north-south than east-west";
        } else {
            spread = "roughly balanced horizontally";
        }
        String vertical = report.heightY() > Math.max(report.widthX(), report.depthZ()) * 0.5
                ? "with noticeable vertical spread"
                : "with mostly horizontal spread";
        return report.totalBranches() + " total branches across " + report.segments
                + " segments, " + spread + " and " + vertical + ".";
    }

    private void appendJsonStringField(StringBuilder text, String name, String value, int indent, boolean comma) {
        for (int i = 0; i < indent; i++) {
            text.append(' ');
        }
        appendJsonString(text, name);
        text.append(": ");
        appendJsonString(text, value);
        if (comma) {
            text.append(",");
        }
        text.append("\n");
    }

    private List<VeinDefinition> getCurrentXmlDefinitions() {
        File source = selectedDefinition.sourceFile;
        if (source == null) {
            return new ArrayList<VeinDefinition>(loadedDefinitions);
        }
        List<VeinDefinition> group = new ArrayList<VeinDefinition>();
        for (VeinDefinition def : loadedDefinitions) {
            if (sameSource(def.sourceFile, source)) {
                group.add(def);
            }
        }
        return group.isEmpty() ? java.util.Collections.singletonList(selectedDefinition) : group;
    }

    private boolean sameSource(File a, File b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.equals(b);
    }

    private void setSourceFileRecursive(VeinDefinition def, File file) {
        def.sourceFile = file;
        for (VeinDefinition child : def.children) {
            setSourceFileRecursive(child, file);
        }
    }

    private boolean hasUnsavedXmlChanges() {
        return unsavedXmlDirty || !dirtyXmlFiles.isEmpty();
    }

    private void clearXmlDirtyState() {
        dirtyXmlFiles.clear();
        unsavedXmlDirty = false;
    }

    private void markSelectedXmlDirty() {
        File source = selectedDefinition == null ? null : selectedDefinition.sourceFile;
        if (source == null) {
            unsavedXmlDirty = true;
        } else {
            dirtyXmlFiles.add(source);
        }
    }

    private boolean confirmSaveXmlChanges(Component owner) {
        if (!hasUnsavedXmlChanges()) {
            return true;
        }
        Object[] options = new Object[] { "Save", "Discard", "Cancel" };
        int choice = JOptionPane.showOptionDialog(owner,
                "Save changes to the edited XML before continuing?",
                "Unsaved XML Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == 0) {
            return saveAllXmlChanges(owner);
        }
        if (choice == 1) {
            clearXmlDirtyState();
            return true;
        }
        return false;
    }

    private boolean confirmSaveXmlUnload(Component owner, Set<File> unloadingFiles, boolean unloadUnsourced) {
        Set<File> dirtyUnloading = new HashSet<File>(dirtyXmlFiles);
        dirtyUnloading.retainAll(unloadingFiles);
        boolean dirtyUnsourced = unloadUnsourced && unsavedXmlDirty;
        if (dirtyUnloading.isEmpty() && !dirtyUnsourced) {
            return true;
        }
        Object[] options = new Object[] { "Save", "Discard", "Cancel" };
        int choice = JOptionPane.showOptionDialog(owner,
                "Save changes before unloading the selected XML?",
                "Unsaved XML Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == 0) {
            return saveXmlChanges(owner, dirtyUnloading, dirtyUnsourced);
        }
        if (choice == 1) {
            dirtyXmlFiles.removeAll(dirtyUnloading);
            if (dirtyUnsourced) {
                unsavedXmlDirty = false;
            }
            return true;
        }
        return false;
    }

    private boolean saveAllXmlChanges(Component owner) {
        return saveXmlChanges(owner, new HashSet<File>(dirtyXmlFiles), unsavedXmlDirty);
    }

    private boolean saveXmlChanges(Component owner, Set<File> filesToSave, boolean saveUnsourced) {
        try {
            for (File file : new HashSet<File>(filesToSave)) {
                if (file == null) {
                    continue;
                }
                List<VeinDefinition> group = definitionsForSource(file);
                if (!group.isEmpty()) {
                    writeDefinitionsXml(file, group);
                    dirtyXmlFiles.remove(file);
                }
            }
            if (saveUnsourced) {
                List<VeinDefinition> unsourced = unsourcedDefinitions();
                if (!unsourced.isEmpty()) {
                    File file = chooseSaveXmlFile(owner, "unsaved-cog-veins.xml");
                    if (file == null) {
                        return false;
                    }
                    writeDefinitionsXml(file, unsourced);
                    for (VeinDefinition def : unsourced) {
                        setSourceFileRecursive(def, file);
                    }
                }
                unsavedXmlDirty = false;
            }
            updateXmlPreview();
            refreshFileBrowser();
            statusLabel.setText("Saved XML changes");
            return true;
        } catch (Exception ex) {
            statusLabel.setText("Save failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(owner, "Save failed: " + ex.getMessage(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private File chooseSaveXmlFile(Component owner, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CustomOreGen XML", "xml"));
        File initial = selectedDefinition != null && selectedDefinition.sourceFile != null
                ? selectedDefinition.sourceFile.getParentFile() : browserRoot;
        if (initial != null && initial.isDirectory()) {
            chooser.setCurrentDirectory(initial);
        }
        chooser.setSelectedFile(new File(defaultName));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xml")) {
            file = new File(file.getParentFile(), file.getName() + ".xml");
        }
        return file;
    }

    private List<VeinDefinition> definitionsForSource(File source) {
        List<VeinDefinition> group = new ArrayList<VeinDefinition>();
        for (VeinDefinition def : loadedDefinitions) {
            if (sameSource(def.sourceFile, source)) {
                group.add(def);
            }
        }
        return group;
    }

    private List<VeinDefinition> unsourcedDefinitions() {
        List<VeinDefinition> group = new ArrayList<VeinDefinition>();
        for (VeinDefinition def : loadedDefinitions) {
            if (def.sourceFile == null) {
                group.add(def);
            }
        }
        return group;
    }

    private boolean groupContainsUnsourced(List<VeinDefinition> group) {
        for (VeinDefinition def : group) {
            if (def.sourceFile == null) {
                return true;
            }
        }
        return false;
    }

    private void rebuildEditor() {
        if (editorPanel == null) {
            return;
        }
        final int scrollY = editorScrollPane == null ? 0 : editorScrollPane.getVerticalScrollBar().getValue();
        rebuildingEditor = true;
        editorPanel.removeAll();
        JPanel form = new EditorFormPanel();
        form.setLayout(new javax.swing.BoxLayout(form, javax.swing.BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("XML Maker");
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        form.add(title);

        JPanel actions = new JPanel();
        actions.setLayout(new javax.swing.BoxLayout(actions, javax.swing.BoxLayout.Y_AXIS));
        actions.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JPanel xmlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton saveXml = new JButton("Save XML");
        JButton saveNew = new JButton("Save New");
        JButton newXml = new JButton("New XML");
        JButton addParent = new JButton("Add Parent Vein");
        addParent.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        saveXml.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                saveCurrentXml(false);
            }
        });
        saveNew.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                saveCurrentXml(true);
            }
        });
        newXml.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!confirmSaveXmlChanges(editorPanel)) {
                    return;
                }
                VeinDefinition created = VeinDefinition.sample();
                created.name = "New_Parent_Vein";
                collapsedVeins.clear();
                loadedDefinitions.clear();
                clearXmlDirtyState();
                loadedDefinitions.add(created);
                selectedDefinition = created;
                refreshDistributionBox();
                editorChanged();
                rebuildEditor();
            }
        });
        addParent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addParentVein();
            }
        });
        xmlRow.add(saveXml);
        xmlRow.add(saveNew);
        xmlRow.add(newXml);
        JPanel parentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        parentRow.add(addParent);
        actions.add(xmlRow);
        actions.add(parentRow);
        form.add(actions);

        form.add(createVeinEditorPane(selectedDefinition, 0, loadedDefinitions.size() > 1));
        rebuildingEditor = false;
        editorScrollPane = new JScrollPane(form);
        editorPanel.add(editorScrollPane, BorderLayout.CENTER);
        applyDarkTheme(editorPanel);
        editorPanel.revalidate();
        editorPanel.repaint();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (editorScrollPane != null) {
                    editorScrollPane.getVerticalScrollBar().setValue(scrollY);
                }
            }
        });
    }

    private void refreshOreDictionaryTab() {
        oreDictionaryPanel.removeAll();
        oreDictionaryPanel.add(createOreDictionaryPanel(oreDictionaryPanel), BorderLayout.CENTER);
        applyDarkTheme(oreDictionaryPanel);
        oreDictionaryPanel.revalidate();
        oreDictionaryPanel.repaint();
    }

    private void refreshBiomeDictionaryTab() {
        biomeDictionaryPanel.removeAll();
        biomeDictionaryPanel.add(createBiomeDictionaryPanel(biomeDictionaryPanel), BorderLayout.CENTER);
        applyDarkTheme(biomeDictionaryPanel);
        biomeDictionaryPanel.revalidate();
        biomeDictionaryPanel.repaint();
    }

    private JPanel createOreDictionaryPanel(final Component owner) {
        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JLabel title = new JLabel("Ore Dictionary");
        content.add(title);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton saveDictionary = new JButton("Save Dictionary");
        JButton loadDictionary = new JButton("Load Dictionary");
        saveDictionary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                saveOreDictionaryFile(owner);
            }
        });
        loadDictionary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (loadOreDictionaryFile(owner)) {
                    refreshOreDictionaryTab();
                    rebuildEditor();
                    regenerate();
                }
            }
        });
        actions.add(saveDictionary);
        actions.add(loadDictionary);
        content.add(actions);
        for (int i = 0; i < oreDictionary.size(); i++) {
            final int index = i;
            final OreDictionaryEntry entry = oreDictionary.get(i);
            final JTextField name = textField(entry.name, 10);
            final JTextField block = textField(entry.block, 16);
            JButton color = new JButton("  ");
            color.setPreferredSize(new Dimension(34, 22));
            color.setBackground(colorForDictionaryEntry(entry));
            color.setOpaque(true);
            JButton remove = new JButton("-");
            name.getDocument().addDocumentListener(changeListener(new Runnable() {
                @Override public void run() {
                    entry.name = name.getText().trim();
                    saveOreDictionary();
                    rebuildEditor();
                }
            }));
            block.getDocument().addDocumentListener(changeListener(new Runnable() {
                @Override public void run() {
                    entry.block = block.getText().trim();
                    saveOreDictionary();
                    rebuildEditor();
                    regenerate();
                }
            }));
            color.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    Color chosen = JColorChooser.showDialog(owner, "Ore Color", colorForDictionaryEntry(entry));
                    if (chosen != null) {
                        entry.colorHex = colorHex(chosen);
                        saveOreDictionary();
                        refreshOreDictionaryTab();
                        rebuildEditor();
                        regenerate();
                    }
                }
            });
            remove.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    oreDictionary.remove(index);
                    saveOreDictionary();
                    refreshOreDictionaryTab();
                    rebuildEditor();
                    regenerate();
                }
            });
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            row.add(color);
            row.add(new JLabel("Name"));
            row.add(name);
            row.add(new JLabel("Block"));
            row.add(block);
            row.add(remove);
            content.add(row);
        }
        JButton add = new JButton("Add Dictionary Ore");
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                oreDictionary.add(new OreDictionaryEntry("New Ore", "", colorHex(colorForOreBlock(""))));
                saveOreDictionary();
                refreshOreDictionaryTab();
                rebuildEditor();
            }
        });
        content.add(add);
        return new JPanel(new BorderLayout()) {{
            add(new JScrollPane(content), BorderLayout.CENTER);
        }};
    }

    private JPanel createBiomeDictionaryPanel(final Component owner) {
        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel("Biome Dictionary");
        title.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        title.setToolTipText("Reusable COG biome gates. Biome entries use regex against biome registry names. BiomeType entries use Forge BiomeDictionary categories.");
        content.add(title);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actions.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JButton saveDictionary = new JButton("Save Dictionary");
        JButton loadDictionary = new JButton("Load Dictionary");
        saveDictionary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                saveBiomeDictionaryFile(owner);
            }
        });
        loadDictionary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (loadBiomeDictionaryFile(owner)) {
                    refreshBiomeDictionaryTab();
                    rebuildEditor();
                }
            }
        });
        actions.add(saveDictionary);
        actions.add(loadDictionary);
        actions.setMaximumSize(actions.getPreferredSize());
        content.add(actions);
        addBiomeDictionarySection(content, "Biome Categories", GROUP_CATEGORY, "Add Category", "BiomeType");
        List<String> namespaces = biomeNamespaces();
        for (String namespace : namespaces) {
            addBiomeDictionarySection(content, namespace, namespace, "Add " + namespace + " Biome", "Biome");
        }
        final JTextField namespace = compactTextField("", 12);
        JButton addNamespace = new JButton("Add Namespace Section");
        addNamespace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String value = normalizeNamespace(namespace.getText());
                if (value.length() == 0) {
                    return;
                }
                biomeDictionary.add(new BiomeDictionaryEntry("New " + value + " Biome", "Biome", "", value));
                saveBiomeDictionary();
                refreshBiomeDictionaryTab();
                rebuildEditor();
            }
        });
        JPanel namespaceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        namespaceRow.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        namespaceRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        namespaceRow.add(new JLabel("Namespace"));
        namespaceRow.add(namespace);
        namespaceRow.add(addNamespace);
        namespaceRow.setMaximumSize(namespaceRow.getPreferredSize());
        content.add(namespaceRow);
        return new JPanel(new BorderLayout()) {{
            JScrollPane scroll = new JScrollPane(content);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(scroll, BorderLayout.CENTER);
        }};
    }

    private void addBiomeDictionarySection(JPanel content, String titleText, final String group,
                                           String addText, final String defaultKind) {
        JLabel section = new JLabel(titleText);
        section.setBorder(BorderFactory.createEmptyBorder(10, 0, 2, 0));
        section.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        section.setToolTipText(GROUP_CATEGORY.equals(group)
                ? "Forge BiomeDictionary categories. These save to COG as <BiomeType name=\"...\"/>."
                : "Specific biome registry names. These save to COG as <Biome name=\"namespace:path\"/>.");
        content.add(section);
        for (int i = 0; i < biomeDictionary.size(); i++) {
            final int index = i;
            final BiomeDictionaryEntry entry = biomeDictionary.get(i);
            if (!group.equals(entry.group)) {
                continue;
            }
            final JTextField name = compactTextField(entry.nameForEditor(), 16);
            JButton remove = new JButton("-");
            remove.setMargin(new java.awt.Insets(0, 0, 0, 0));
            remove.setPreferredSize(new Dimension(24, 22));
            remove.setMinimumSize(new Dimension(24, 22));
            remove.setMaximumSize(new Dimension(24, 22));
            name.getDocument().addDocumentListener(changeListener(new Runnable() {
                @Override public void run() {
                    entry.setEditorName(name.getText());
                    saveBiomeDictionary();
                    rebuildEditor();
                }
            }));
            remove.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    biomeDictionary.remove(index);
                    saveBiomeDictionary();
                    refreshBiomeDictionaryTab();
                    rebuildEditor();
                }
            });
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            row.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            row.add(new JLabel(GROUP_CATEGORY.equals(group) ? "Category" : "Biome"));
            row.add(name);
            row.add(remove);
            row.setMaximumSize(row.getPreferredSize());
            content.add(row);
        }
        JButton add = new JButton(addText);
        add.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        add.setMaximumSize(add.getPreferredSize());
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String label = GROUP_CATEGORY.equals(group) ? "New Category" : "New Biome";
                biomeDictionary.add(new BiomeDictionaryEntry(label, defaultKind, label, group));
                saveBiomeDictionary();
                refreshBiomeDictionaryTab();
                rebuildEditor();
            }
        });
        content.add(add);
    }

    private JTextField compactTextField(String text, int columns) {
        JTextField field = new JTextField(text, columns);
        Dimension size = field.getPreferredSize();
        field.setPreferredSize(size);
        field.setMinimumSize(size);
        field.setMaximumSize(size);
        return field;
    }

    private void saveOreDictionaryFile(Component owner) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Ore Dictionary JSON", "json"));
        chooser.setSelectedFile(new File("ore-dictionary.json"));
        File initial = getDictionaryInitialDirectory();
        if (initial != null) {
            chooser.setCurrentDirectory(initial);
        }
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".json")) {
            file = new File(file.getParentFile(), file.getName() + ".json");
        }
        try {
            Files.write(file.toPath(), buildOreDictionaryJson().getBytes(Charset.forName("UTF-8")));
            preferences.put("lwjglOreDictionaryFolder", file.getParentFile().getAbsolutePath());
            statusLabel.setText("Saved ore dictionary " + file.getName());
        } catch (Exception ex) {
            statusLabel.setText("Dictionary save failed: " + ex.getMessage());
        }
    }

    private boolean loadOreDictionaryFile(Component owner) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Ore Dictionary JSON", "json"));
        File initial = getDictionaryInitialDirectory();
        if (initial != null) {
            chooser.setCurrentDirectory(initial);
        }
        if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        File file = chooser.getSelectedFile();
        try {
            String text = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
            List<OreDictionaryEntry> loaded = parseOreDictionaryJson(text);
            if (loaded.isEmpty()) {
                statusLabel.setText("Dictionary file contained no ores");
                return false;
            }
            oreDictionary.clear();
            oreDictionary.addAll(loaded);
            saveOreDictionary();
            preferences.put("lwjglOreDictionaryFolder", file.getParentFile().getAbsolutePath());
            statusLabel.setText("Loaded ore dictionary " + file.getName());
            return true;
        } catch (Exception ex) {
            statusLabel.setText("Dictionary load failed: " + ex.getMessage());
            return false;
        }
    }

    private void saveBiomeDictionaryFile(Component owner) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Biome Dictionary JSON", "json"));
        chooser.setSelectedFile(new File("biome-dictionary.json"));
        File initial = getDictionaryInitialDirectory();
        if (initial != null) {
            chooser.setCurrentDirectory(initial);
        }
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".json")) {
            file = new File(file.getParentFile(), file.getName() + ".json");
        }
        try {
            activeBiomeDictionaryFile = file;
            Files.write(file.toPath(), buildBiomeDictionaryJson().getBytes(Charset.forName("UTF-8")));
            preferences.put(PREF_BIOME_DICTIONARY_FILE, file.getAbsolutePath());
            preferences.put("lwjglOreDictionaryFolder", file.getParentFile().getAbsolutePath());
            statusLabel.setText("Saved biome dictionary " + file.getName());
        } catch (Exception ex) {
            statusLabel.setText("Biome dictionary save failed: " + ex.getMessage());
        }
    }

    private boolean loadBiomeDictionaryFile(Component owner) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Biome Dictionary JSON", "json"));
        File initial = getDictionaryInitialDirectory();
        if (initial != null) {
            chooser.setCurrentDirectory(initial);
        }
        if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        File file = chooser.getSelectedFile();
        try {
            String text = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
            List<BiomeDictionaryEntry> loaded = parseBiomeDictionaryJson(text);
            if (loaded.isEmpty()) {
                statusLabel.setText("Dictionary file contained no biomes");
                return false;
            }
            biomeDictionary.clear();
            biomeDictionary.addAll(loaded);
            activeBiomeDictionaryFile = file;
            preferences.put(PREF_BIOME_DICTIONARY_FILE, file.getAbsolutePath());
            preferences.put("lwjglOreDictionaryFolder", file.getParentFile().getAbsolutePath());
            statusLabel.setText("Loaded biome dictionary " + file.getName());
            return true;
        } catch (Exception ex) {
            statusLabel.setText("Biome dictionary load failed: " + ex.getMessage());
            return false;
        }
    }

    private File getDictionaryInitialDirectory() {
        if (activeBiomeDictionaryFile != null && activeBiomeDictionaryFile.getParentFile() != null
                && activeBiomeDictionaryFile.getParentFile().isDirectory()) {
            return activeBiomeDictionaryFile.getParentFile();
        }
        String saved = preferences.get("lwjglOreDictionaryFolder", null);
        if (saved != null) {
            File folder = new File(saved);
            if (folder.isDirectory()) {
                return folder;
            }
        }
        return browserRoot != null && browserRoot.isDirectory() ? browserRoot : null;
    }

    private File rememberedBiomeDictionaryFile() {
        String saved = preferences.get(PREF_BIOME_DICTIONARY_FILE, null);
        if (saved != null && saved.trim().length() > 0) {
            File file = new File(saved);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private File defaultBiomeDictionaryFile() {
        return new File("biome-dictionary.json").getAbsoluteFile();
    }

    private String buildOreDictionaryJson() {
        StringBuilder text = new StringBuilder();
        text.append("{\n");
        text.append("  \"schema\": \"cog-viewer-ore-dictionary-v1\",\n");
        text.append("  \"ores\": [\n");
        boolean first = true;
        for (OreDictionaryEntry entry : oreDictionary) {
            if (entry.block == null || entry.block.trim().length() == 0) {
                continue;
            }
            if (!first) {
                text.append(",\n");
            }
            text.append("    {\"name\": ");
            appendJsonString(text, entry.name);
            text.append(", \"block\": ");
            appendJsonString(text, entry.block);
            text.append(", \"color\": ");
            appendJsonString(text, entry.colorHex);
            text.append("}");
            first = false;
        }
        text.append("\n  ]\n");
        text.append("}\n");
        return text.toString();
    }

    private List<OreDictionaryEntry> parseOreDictionaryJson(String text) {
        List<OreDictionaryEntry> entries = new ArrayList<OreDictionaryEntry>();
        Pattern objectPattern = Pattern.compile("\\{([^{}]*)\\}");
        Matcher objectMatcher = objectPattern.matcher(text == null ? "" : text);
        while (objectMatcher.find()) {
            String object = objectMatcher.group(1);
            String block = jsonField(object, "block");
            if (block == null || block.trim().length() == 0) {
                continue;
            }
            String name = jsonField(object, "name");
            String color = jsonField(object, "color");
            entries.add(new OreDictionaryEntry(name == null ? block : name, block, color == null ? "" : color));
        }
        return entries;
    }

    private String buildBiomeDictionaryJson() {
        StringBuilder text = new StringBuilder();
        text.append("{\n");
        text.append("  \"schema\": \"cog-viewer-biome-dictionary-v1\",\n");
        text.append("  \"biomes\": [\n");
        boolean first = true;
        for (BiomeDictionaryEntry entry : biomeDictionary) {
            if (entry.matcher == null || entry.matcher.trim().length() == 0) {
                continue;
            }
            if (!first) {
                text.append(",\n");
            }
            text.append("    {\"name\": ");
            appendJsonString(text, entry.name);
            text.append(", \"kind\": ");
            appendJsonString(text, entry.kind);
            text.append(", \"matcher\": ");
            appendJsonString(text, entry.matcher);
            text.append(", \"group\": ");
            appendJsonString(text, entry.group);
            text.append("}");
            first = false;
        }
        text.append("\n  ]\n");
        text.append("}\n");
        return text.toString();
    }

    private List<BiomeDictionaryEntry> parseBiomeDictionaryJson(String text) {
        List<BiomeDictionaryEntry> entries = new ArrayList<BiomeDictionaryEntry>();
        Pattern objectPattern = Pattern.compile("\\{([^{}]*)\\}");
        Matcher objectMatcher = objectPattern.matcher(text == null ? "" : text);
        while (objectMatcher.find()) {
            String object = objectMatcher.group(1);
            String matcher = jsonField(object, "matcher");
            if (matcher == null || matcher.trim().length() == 0) {
                continue;
            }
            String name = jsonField(object, "name");
            String kind = jsonField(object, "kind");
            String group = jsonField(object, "group");
            entries.add(new BiomeDictionaryEntry(name == null ? matcher : name,
                    kind == null ? "Biome" : kind, matcher,
                    group == null ? biomeGroupFor(kind, matcher) : group));
        }
        return entries;
    }

    private String jsonField(String object, String field) {
        Pattern fieldPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher matcher = fieldPattern.matcher(object);
        return matcher.find() ? unescapeJson(matcher.group(1)) : null;
    }

    private static String biomeGroupFor(String kind, String matcher) {
        if ("BiomeType".equalsIgnoreCase(kind)) {
            return GROUP_CATEGORY;
        }
        return namespaceFromRegistry(matcher);
    }

    private List<String> biomeNamespaces() {
        Set<String> namespaces = new HashSet<String>();
        for (BiomeDictionaryEntry entry : biomeDictionary) {
            if ("Biome".equalsIgnoreCase(entry.kind)
                    && entry.group != null
                    && entry.group.trim().length() > 0
                    && !GROUP_CATEGORY.equalsIgnoreCase(entry.group)) {
                namespaces.add(entry.group);
            }
        }
        List<String> sorted = new ArrayList<String>(namespaces);
        Collections.sort(sorted);
        return sorted;
    }

    private static String normalizeNamespace(String text) {
        String value = text == null ? "" : text.trim().toLowerCase(Locale.ENGLISH);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '.') {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String namespaceFromRegistry(String registryName) {
        String value = registryName == null ? "" : registryName.trim();
        int colon = value.indexOf(':');
        if (colon > 0) {
            return normalizeNamespace(value.substring(0, colon));
        }
        return GROUP_MINECRAFT;
    }

    private static String pathFromRegistry(String registryName) {
        String value = registryName == null ? "" : registryName.trim();
        int colon = value.indexOf(':');
        return colon >= 0 && colon + 1 < value.length() ? value.substring(colon + 1) : value;
    }

    private static String biomePathFromLabel(String text) {
        String value = text == null ? "" : text.trim().toLowerCase(Locale.ENGLISH);
        StringBuilder out = new StringBuilder();
        boolean underscorePending = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                if (underscorePending && out.length() > 0) {
                    out.append('_');
                }
                out.append(ch);
                underscorePending = false;
            } else if (ch == '_' || ch == ' ' || ch == '-') {
                underscorePending = out.length() > 0;
            }
        }
        return out.toString();
    }

    private static String toTitleCase(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ENGLISH).replace('_', ' ');
        StringBuilder out = new StringBuilder();
        boolean upperNext = true;
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if (ch == ' ' || ch == '-') {
                upperNext = true;
                out.append(ch);
            } else if (upperNext) {
                out.append(Character.toUpperCase(ch));
                upperNext = false;
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String displayNameFromRegistry(String registryName) {
        return toTitleCase(pathFromRegistry(registryName));
    }

    private String unescapeJson(String value) {
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped) {
                if (c == '\\') {
                    escaped = true;
                } else {
                    out.append(c);
                }
                continue;
            }
            if (c == 'n') out.append('\n');
            else if (c == 'r') out.append('\r');
            else if (c == 't') out.append('\t');
            else out.append(c);
            escaped = false;
        }
        if (escaped) {
            out.append('\\');
        }
        return out.toString();
    }

    private void appendJsonString(StringBuilder text, String value) {
        text.append('"');
        if (value != null) {
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                if (ch == '"' || ch == '\\') {
                    text.append('\\').append(ch);
                } else if (ch == '\n') {
                    text.append("\\n");
                } else if (ch == '\r') {
                    text.append("\\r");
                } else if (ch == '\t') {
                    text.append("\\t");
                } else {
                    text.append(ch);
                }
            }
        }
        text.append('"');
    }

    private JPanel createVeinEditorPane(final VeinDefinition target, final int depth, boolean removable) {
        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        final boolean collapsed = isCollapsed(target);
        final String role = depth == 0 ? "Mother Vein: " : "Child Vein: ";
        content.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(role + target.nameForXml() + "  freq " + format(target.distributionFrequency.mean)),
                BorderFactory.createEmptyBorder(6, depth == 0 ? 6 : 14, 6, 6)));

        JPanel header = new JPanel(new BorderLayout(4, 0));
        JButton toggle = new JButton(collapsed ? "+" : "-");
        toggle.setMargin(new java.awt.Insets(0, 0, 0, 0));
        toggle.setPreferredSize(new Dimension(24, 22));
        toggle.setToolTipText(collapsed ? "Expand this vein." : "Collapse this vein.");
        JLabel headerLabel = new JLabel(target.nameForXml() + "  (" + (isCloud(target) ? "Cloud" : "Veins") + ")");
        header.add(toggle, BorderLayout.WEST);
        header.add(headerLabel, BorderLayout.CENTER);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
        header.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        toggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                collapsedVeins.put(target, Boolean.valueOf(!isCollapsed(target)));
                rebuildEditor();
            }
        });
        content.add(header);
        if (collapsed) {
            content.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            return content;
        }

        if (removable) {
            JButton remove = new JButton(depth == 0 ? "Remove Parent" : "Remove");
            remove.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            remove.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (depth == 0) {
                        removeParentVein(target);
                    } else {
                        removeChildFromRoots(target);
                    }
                    editorChanged();
                    rebuildEditor();
                }
            });
            content.add(remove);
        }

        JTextField name = textField(target.nameForXml(), 18);
        content.add(editorRow("Name", name));
        name.getDocument().addDocumentListener(changeListener(new Runnable() {
            @Override
            public void run() {
                target.name = name.getText();
                refreshDistributionBox();
                editorChanged();
            }
        }));

        final JComboBox<String> type = new JComboBox<String>(new String[] { "Veins", "Cloud" });
        type.setSelectedItem(target.distributionType);
        type.setToolTipText("Distribution element type. Veins creates a motherlode with branches. Cloud creates a branchless blob/cloud-style distribution.");
        content.add(editorRow("Distribution Type", type));
        type.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                target.distributionType = (String) type.getSelectedItem();
                if ("Cloud".equals(target.distributionType)) {
                    target.enableCloudDefaults();
                } else {
                    target.enableDefaultSettings();
                }
                editorChanged();
                rebuildEditor();
            }
        });

        if (!"Cloud".equals(target.distributionType)) {
            final JComboBox<String> branchType = new JComboBox<String>(new String[] { "Bezier", "Ellipsoid" });
            branchType.setSelectedItem(target.branchType == null || target.branchType.length() == 0 ? "Bezier" : target.branchType);
            branchType.setToolTipText("COG branchType. Bezier creates smoother curved tubes; Ellipsoid uses older independent oval segments.");
            content.add(editorRow("Branch Type", branchType));
            branchType.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    target.branchType = (String) branchType.getSelectedItem();
                    editorChanged();
                }
            });
        }

        content.add(createSettingsEditor(target));

        addBiomeGateEditor(content, target);
        addOreEditor(content, target);
        content.add(createNestedVeinTree(target, depth));
        content.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return content;
    }

    private boolean isCollapsed(VeinDefinition def) {
        Boolean collapsed = collapsedVeins.get(def);
        return collapsed != null && collapsed.booleanValue();
    }

    private void addPDistRow(JPanel form, String label, final PDist dist, final DistSetter setter) {
        final JTextField mean = textField(format(dist.mean), 4);
        final JTextField range = textField(format(dist.range), 4);
        JPanel line = new JPanel(new GridLayout(1, 4, 4, 0));
        line.add(new JLabel("Avg"));
        line.add(mean);
        line.add(new JLabel("Range"));
        line.add(range);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, line.getPreferredSize().height));
        Runnable update = new Runnable() {
            @Override
            public void run() {
                setter.set(new PDist(parseDouble(mean.getText(), dist.mean), parseDouble(range.getText(), dist.range),
                        dist.type));
                editorChanged();
            }
        };
        mean.getDocument().addDocumentListener(changeListener(update));
        range.getDocument().addDocumentListener(changeListener(update));
        form.add(editorRow(label, line));
    }

    private JPanel createSettingsEditor(final VeinDefinition target) {
        JPanel box = new JPanel();
        box.setLayout(new javax.swing.BoxLayout(box, javax.swing.BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        box.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Settings");
        title.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        box.add(title);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        addSettingsGridCell(grid, new JLabel("Setting"), 0, 0, 1.0);
        addSettingsGridCell(grid, new JLabel("Avg"), 1, 0, 0.0);
        addSettingsGridCell(grid, new JLabel("Range"), 2, 0, 0.0);
        addSettingsGridCell(grid, new JLabel(""), 3, 0, 0.0);
        int row = 1;
        for (String setting : settingsFor(target)) {
            if (target.enabledSettings.contains(setting) && settingAllowedFor(target, setting)) {
                addDistGridRow(grid, row++, target, setting, getSetting(target, setting));
            }
        }
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, grid.getPreferredSize().height));
        box.add(grid);

        final JComboBox<String> addSetting = new JComboBox<String>();
        addSetting.setPreferredSize(new Dimension(120, 24));
        addSetting.setMinimumSize(new Dimension(70, 24));
        addSetting.setToolTipText("Choose a setting to add to this vein or cloud.");
        addSetting.setRenderer(new ListCellRenderer<String>() {
            private final JLabel label = new JLabel();

            @Override
            public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                label.setOpaque(true);
                label.setText(value == null ? "" : value);
                label.setToolTipText(value == null ? null : tooltipForSetting(value));
                label.setBackground(isSelected ? new Color(54, 72, 96) : CONTROL_BG);
                label.setForeground(CONTROL_FG);
                label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
                return label;
            }
        });
        for (String setting : settingsFor(target)) {
            if (!target.enabledSettings.contains(setting) && settingAllowedFor(target, setting)) {
                addSetting.addItem(setting);
            }
        }
        JButton add = new JButton("Add");
        add.setMargin(new java.awt.Insets(1, 8, 1, 8));
        add.setPreferredSize(new Dimension(48, 24));
        add.setMinimumSize(new Dimension(48, 24));
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String setting = (String) addSetting.getSelectedItem();
                if (setting != null) {
                    target.enabledSettings.add(setting);
                    editorChanged();
                    rebuildEditor();
                }
            }
        });
        JPanel addRow = new JPanel(new BorderLayout(4, 0));
        addRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        addRow.add(addSetting, BorderLayout.CENTER);
        addRow.add(add, BorderLayout.EAST);
        addRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        addRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        box.add(addRow);
        return box;
    }

    private void addDistGridRow(JPanel grid, int row, final VeinDefinition target, final String name, final PDist dist) {
        JLabel label = new JLabel(name);
        label.setToolTipText(tooltipForSetting(name));
        label.setPreferredSize(new Dimension(118, label.getPreferredSize().height));
        final JTextField avg = compactNumberField(format(dist.mean));
        final JTextField range = compactNumberField(format(dist.range));
        Runnable update = new Runnable() {
            @Override
            public void run() {
                Double avgValue = parseEditorDouble(avg.getText());
                Double rangeValue = parseEditorDouble(range.getText());
                if (avgValue == null || rangeValue == null) {
                    return;
                }
                PDist.Type type = rangeValue.doubleValue() == 0.0 ? PDist.Type.CONSTANT
                        : dist.type == PDist.Type.CONSTANT ? PDist.Type.UNIFORM : dist.type;
                setSetting(target, name, new PDist(avgValue.doubleValue(), rangeValue.doubleValue(), type));
                editorChanged();
            }
        };
        avg.getDocument().addDocumentListener(changeListener(update));
        range.getDocument().addDocumentListener(changeListener(update));
        JButton remove = new JButton("-");
        remove.setMargin(new java.awt.Insets(0, 0, 0, 0));
        remove.setPreferredSize(new Dimension(24, 22));
        remove.setMinimumSize(new Dimension(24, 22));
        remove.setMaximumSize(new Dimension(24, 22));
        remove.setToolTipText("Remove " + name + " from this vein.");
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                target.enabledSettings.remove(name);
                editorChanged();
                rebuildEditor();
            }
        });
        addSettingsGridCell(grid, label, 0, row, 1.0);
        addSettingsGridCell(grid, avg, 1, row, 0.0);
        addSettingsGridCell(grid, range, 2, row, 0.0);
        addSettingsGridCell(grid, remove, 3, row, 0.0);
    }

    private void addSettingsGridCell(JPanel grid, Component component, int x, int y, double weightX) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = weightX;
        gbc.fill = weightX > 0.0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(2, 2, 2, 2);
        grid.add(component, gbc);
    }

    private JTextField compactNumberField(String text) {
        JTextField field = new JTextField(text, 4);
        Dimension size = new Dimension(46, 22);
        field.setPreferredSize(size);
        field.setMinimumSize(size);
        field.setMaximumSize(size);
        return field;
    }

    private void addBiomeGateEditor(JPanel form, final VeinDefinition target) {
        JPanel box = new JPanel();
        box.setLayout(new javax.swing.BoxLayout(box, javax.swing.BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        box.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel("Biome Gates");
        title.setToolTipText("COG biome descriptor entries. Biome uses a regex against biome registry names. BiomeType uses Forge BiomeDictionary categories. Negative weight excludes matches.");
        box.add(title);
        if (target.biomeGates.isEmpty()) {
            JLabel empty = new JLabel("No biome gate; COG default is all biomes.");
            empty.setForeground(MUTED_FG);
            box.add(empty);
        }
        for (int i = 0; i < target.biomeGates.size(); i++) {
            final int index = i;
            final BiomeGateEntry gate = target.biomeGates.get(i);
            final JComboBox<BiomeDictionaryEntry> dictionary = createBiomeDictionaryCombo(gate);
            final JComboBox<String> kind = new JComboBox<String>(BIOME_GATE_KINDS);
            kind.setSelectedItem(gate.isType() ? "BiomeType" : "Biome");
            final JTextField name = textField(gate.name, 9);
            final JTextField weight = compactNumberField(format(gate.weight));
            JButton remove = new JButton("Remove");
            dictionary.setPreferredSize(new Dimension(205, 24));
            dictionary.setMaximumSize(new Dimension(205, 24));
            kind.setPreferredSize(new Dimension(92, 24));
            kind.setMaximumSize(new Dimension(92, 24));
            name.setToolTipText("COG biome gate name. Biome uses a registry-name regex; BiomeType uses a Forge biome category.");
            weight.setToolTipText("COG biome gate weight. Positive values include matching biomes; negative values exclude matching biomes.");
            remove.setToolTipText("Remove this biome gate from the selected vein.");
            dictionary.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    BiomeDictionaryEntry selected = (BiomeDictionaryEntry) dictionary.getSelectedItem();
                    if (selected != null) {
                        gate.kind = selected.kind;
                        gate.name = selected.cogName();
                        editorChanged();
                        rebuildEditor();
                    }
                }
            });
            kind.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    gate.kind = (String) kind.getSelectedItem();
                    editorChanged();
                }
            });
            name.getDocument().addDocumentListener(changeListener(new Runnable() {
                @Override public void run() {
                    gate.name = name.getText().trim();
                    editorChanged();
                }
            }));
            weight.getDocument().addDocumentListener(changeListener(new Runnable() {
                @Override public void run() {
                    gate.weight = parseDouble(weight.getText(), gate.weight);
                    editorChanged();
                }
            }));
            remove.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    target.biomeGates.remove(index);
                    editorChanged();
                    rebuildEditor();
                }
            });
            JPanel gateBox = new JPanel();
            gateBox.setLayout(new javax.swing.BoxLayout(gateBox, javax.swing.BoxLayout.Y_AXIS));
            gateBox.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            gateBox.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            JPanel pickRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            pickRow.add(new JLabel("Biome"));
            pickRow.add(dictionary);
            pickRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            JPanel valueRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            valueRow.add(new JLabel("Type"));
            valueRow.add(kind);
            valueRow.add(new JLabel("Name"));
            valueRow.add(name);
            valueRow.add(new JLabel("Weight"));
            valueRow.add(weight);
            valueRow.add(remove);
            valueRow.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            gateBox.add(pickRow);
            gateBox.add(valueRow);
            gateBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, gateBox.getPreferredSize().height));
            box.add(gateBox);
        }
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton add = new JButton("Add Biome Gate");
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                target.biomeGates.add(new BiomeGateEntry("Biome", "", 1.0));
                editorChanged();
                rebuildEditor();
            }
        });
        addRow.add(add);
        box.add(addRow);
        form.add(box);
    }

    private void addOreEditor(JPanel form, final VeinDefinition target) {
        target.ensureOreBlocks();
        final JLabel summary = new JLabel(oreWeightSummary(target));
        summary.setBorder(BorderFactory.createEmptyBorder(10, 0, 3, 0));
        summary.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        form.add(summary);
        for (int i = 0; i < target.oreBlocks.size(); i++) {
            final int index = i;
            final OreBlockDefinition ore = target.oreBlocks.get(i);
            final JLabel blockLabel = new JLabel(oreBlockDisplayName(ore));
            final JComboBox<OreDictionaryEntry> dictionary = createOreDictionaryCombo(ore.block);
            final JLabel percent = new JLabel(oreWeightPercent(ore) + "%");
            final JSlider weight = new JSlider(0, 100, oreWeightPercent(ore));
            final boolean[] correctingSlider = new boolean[] { false };
            weight.setMajorTickSpacing(25);
            weight.setMinorTickSpacing(5);
            weight.setPaintTicks(true);
            weight.setSnapToTicks(false);
            weight.setPreferredSize(new Dimension(150, 34));
            weight.setMaximumSize(new Dimension(150, 34));
            JButton color = new JButton("  ");
            color.setPreferredSize(new Dimension(34, 22));
            color.setBackground(colorForOre(ore, ore.block));
            color.setOpaque(true);
            JButton remove = new JButton("-");
            dictionary.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    OreDictionaryEntry selected = (OreDictionaryEntry) dictionary.getSelectedItem();
                    if (selected != null) {
                        ore.block = selected.block;
                        ore.colorHex = selected.colorHex;
                        blockLabel.setText(oreBlockDisplayName(ore));
                        color.setBackground(colorForOre(ore, ore.block));
                        if (index == 0 && ore.block.length() > 0) {
                            target.oreBlockName = ore.block;
                        }
                        editorChanged();
                    }
                }
            });
            color.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    Color chosen = JColorChooser.showDialog(editorPanel, "Ore Color", colorForOre(ore, ore.block));
                    if (chosen != null) {
                        ore.colorHex = colorHex(chosen);
                        updateDictionaryColorForBlock(ore.block, ore.colorHex);
                        color.setBackground(chosen);
                        editorChanged();
                    }
                }
            });
            remove.setEnabled(target.oreBlocks.size() > 1);
            remove.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    target.oreBlocks.remove(index);
                    editorChanged();
                    rebuildEditor();
                }
            });
            weight.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent event) {
                    if (correctingSlider[0]) {
                        return;
                    }
                    int percentValue = weight.getValue();
                    int maxAllowed = Math.max(0, 100 - totalOreWeightPercentExcept(target, index));
                    if (percentValue > maxAllowed) {
                        percentValue = maxAllowed;
                        if (!weight.getValueIsAdjusting()) {
                            correctingSlider[0] = true;
                            weight.setValue(percentValue);
                            correctingSlider[0] = false;
                        }
                    }
                    ore.weight = percentValue / 100.0;
                    percent.setText(percentValue + "%");
                    summary.setText(oreWeightSummary(target));
                    updateXmlPreview();
                    if (!weight.getValueIsAdjusting()) {
                        regenerate();
                    }
                }
            });
            JPanel oreBox = new JPanel();
            oreBox.setLayout(new javax.swing.BoxLayout(oreBox, javax.swing.BoxLayout.Y_AXIS));
            oreBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            oreBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 78, 92)),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            JPanel blockLine = new JPanel(new BorderLayout(4, 0));
            blockLine.add(color, BorderLayout.WEST);
            blockLine.add(dictionary, BorderLayout.CENTER);
            blockLine.add(blockLabel, BorderLayout.SOUTH);
            JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            tools.setMaximumSize(new Dimension(Integer.MAX_VALUE, tools.getPreferredSize().height));
            tools.add(percent);
            tools.add(weight);
            tools.add(remove);
            oreBox.add(blockLine);
            oreBox.add(tools);
            form.add(editorRow("Ore " + (i + 1), oreBox));
        }
        JButton addOre = new JButton("Add Ore");
        addOre.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                target.oreBlocks.add(new OreBlockDefinition("", 0.0));
                editorChanged();
                rebuildEditor();
            }
        });
        form.add(editorRow("", addOre));
    }

    private JPanel createNestedVeinTree(final VeinDefinition root, final int depth) {
        JPanel box = new JPanel();
        box.setLayout(new javax.swing.BoxLayout(box, javax.swing.BoxLayout.Y_AXIS));
        addSectionHeader(box, "Nested Veins");
        JButton addChild = new JButton("Add Child Vein");
        addChild.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        addChild.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                VeinDefinition child = VeinDefinition.sample();
                child.name = root.nameForXml() + "_Child";
                child.distributionFrequency = new PDist(1.0, 0.0);
                child.enabledSettings.clear();
                child.enabledSettings.add("MotherlodeFrequency");
                child.enabledSettings.add("MotherlodeRangeLimit");
                child.enabledSettings.add("MotherlodeHeight");
                child.enabledSettings.add("MotherlodeSize");
                child.enabledSettings.add("BranchFrequency");
                child.enabledSettings.add("BranchLength");
                child.enabledSettings.add("SegmentRadius");
                root.children.add(child);
                editorChanged();
                rebuildEditor();
            }
        });
        box.add(addChild);
        if (root.children.isEmpty()) {
            JLabel none = new JLabel("None");
            none.setForeground(Color.GRAY);
            box.add(none);
            box.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            return box;
        }
        for (VeinDefinition child : root.children) {
            box.add(createVeinEditorPane(child, depth + 1, true));
        }
        box.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return box;
    }

    private JPanel editorRow(String label, java.awt.Component field) {
        JPanel row = new JPanel();
        row.setLayout(new javax.swing.BoxLayout(row, javax.swing.BoxLayout.Y_AXIS));
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 5, 0));
        if (label != null && label.length() > 0) {
            row.add(new JLabel(label));
        }
        row.add(field);
        row.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void addSectionHeader(JPanel form, String text) {
        JLabel heading = new JLabel(text);
        heading.setBorder(BorderFactory.createEmptyBorder(10, 0, 3, 0));
        heading.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        form.add(heading);
    }

    private JTextField textField(String text, int columns) {
        JTextField field = new JTextField(text, columns);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        return field;
    }

    private DocumentListener changeListener(final Runnable runnable) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { run(); }
            @Override public void removeUpdate(DocumentEvent e) { run(); }
            @Override public void changedUpdate(DocumentEvent e) { run(); }
            private void run() {
                if (!rebuildingEditor) {
                    runnable.run();
                }
            }
        };
    }

    private void editorChanged() {
        if (rebuildingEditor) {
            return;
        }
        markSelectedXmlDirty();
        updateXmlPreview();
        regenerate();
    }

    private String oreWeightSummary(VeinDefinition def) {
        int sum = 0;
        for (OreBlockDefinition ore : def.oreBlocks) {
            sum += oreWeightPercent(ore);
        }
        return "Weighted Ores (" + sum + "%)";
    }

    private int oreWeightPercent(OreBlockDefinition ore) {
        return (int) Math.round(clamp(ore.weight, 0.0, 1.0) * 100.0);
    }

    private int totalOreWeightPercentExcept(VeinDefinition def, int exceptIndex) {
        int total = 0;
        for (int i = 0; i < def.oreBlocks.size(); i++) {
            if (i != exceptIndex) {
                total += oreWeightPercent(def.oreBlocks.get(i));
            }
        }
        return total;
    }

    private void regenerate() {
        final long requestId = ++renderRequest;
        final long seed = parseSeed(seedField.getText());
        seedField.setText(Long.toString(seed));
        final List<VeinDefinition> roots = copyActiveRoots();
        statusLabel.setText("Rendering...");
        if (renderWorker != null) {
            renderWorker.cancel(true);
        }
        renderWorker = new SwingWorker<RenderScene, Void>() {
            @Override
            protected RenderScene doInBackground() {
                return buildScene(roots, seed);
            }

            @Override
            protected void done() {
                if (requestId != renderRequest || isCancelled()) {
                    return;
                }
                try {
                    RenderScene scene = get();
                    viewport.setRenderOptions(String.valueOf(renderDetailBox.getSelectedItem()), isRegionMode());
                    viewport.setScene(scene);
                    statsArea.setText(scene.stats);
                    statusLabel.setText("Rendered " + scene.voxelCount + " blocks (" + viewport.activeRenderDetailName() + ")");
                } catch (Exception ex) {
                    statusLabel.setText("Render failed: " + ex.getMessage());
                }
            }
        };
        renderWorker.execute();
    }

    private List<VeinDefinition> copyActiveRoots() {
        List<VeinDefinition> roots = stackAllBox.isSelected()
                ? loadedDefinitions
                : java.util.Collections.singletonList(selectedDefinition);
        List<VeinDefinition> copies = new ArrayList<VeinDefinition>();
        for (VeinDefinition def : roots) {
            copies.add(def.copy());
        }
        return copies;
    }

    private boolean isRegionMode() {
        return "Region".equals(modeBox.getSelectedItem());
    }

    private boolean isRegionTopDown() {
        return "2D top-down".equals(regionViewBox.getSelectedItem());
    }

    private RenderScene buildScene(List<VeinDefinition> roots, long seed) {
        long started = System.nanoTime();
        if (isRegionMode()) {
            return buildRegionScene(roots, seed);
        }
        List<VeinDefinition> flat = flattenDefinitions(roots);
        boolean groundEnabled = isGroundGridEnabled();
        int groundLevel = groundY();
        int sceneSizeX = 0;
        int sceneSizeY = 0;
        int sceneSizeZ = 0;
        for (VeinDefinition def : flat) {
            def.fitVolumeToVeinSize(16);
            sceneSizeX = Math.max(sceneSizeX, def.sizeX);
            sceneSizeY = Math.max(sceneSizeY, def.sizeY);
            sceneSizeZ = Math.max(sceneSizeZ, def.sizeZ);
        }

        List<Voxel> voxels = new ArrayList<Voxel>();
        Map<String, Integer> oreCounts = new HashMap<String, Integer>();
        List<DefinitionRenderStats> definitionStats = new ArrayList<DefinitionRenderStats>();
        int[] veinIndex = new int[] { 0 };
        for (VeinDefinition root : roots) {
            buildDefinitionTree(root, seed, veinIndex, null, voxels, oreCounts, definitionStats, 0, sceneSizeY, groundEnabled, groundLevel);
        }

        StringBuilder stats = new StringBuilder();
        stats.append("Mode: LWJGL/OpenGL voxel viewer\n");
        stats.append("View: Single vein 3D\n");
        stats.append("Seed: ").append(seed).append("\n");
        stats.append("Distributions: ").append(roots.size()).append("\n");
        stats.append("Scene: ").append(sceneSizeX).append(" x ").append(sceneSizeY).append(" x ").append(sceneSizeZ).append("\n");
        stats.append("Ground cutoff: ").append(groundEnabled ? "Y=" + groundLevel : "off").append("\n");
        stats.append("Total ore blocks: ").append(voxels.size()).append("\n");
        long buildMillis = (System.nanoTime() - started) / 1000000L;
        stats.append("CPU scene build: ").append(buildMillis).append(" ms\n");
        stats.append("Total composition:\n");
        for (Map.Entry<String, Integer> entry : oreCounts.entrySet()) {
            stats.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        stats.append("\nPer vein:\n");
        for (DefinitionRenderStats definitionStat : definitionStats) {
            for (int i = 0; i < definitionStat.depth; i++) {
                stats.append("  ");
            }
            stats.append(definitionStat.depth == 0 ? "Mother: " : "Child: ")
                    .append(definitionStat.name)
                    .append(" - ")
                    .append(definitionStat.total)
                    .append(" blocks\n");
            for (Map.Entry<String, Integer> entry : definitionStat.oreCounts.entrySet()) {
                for (int i = 0; i < definitionStat.depth; i++) {
                    stats.append("  ");
                }
                stats.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        return new RenderScene(voxels, definitionStats, sceneSizeX, sceneSizeY, sceneSizeZ, stats.toString(), groundEnabled, groundLevel, false, buildMillis);
    }

    private int buildDefinitionTree(VeinDefinition def, long baseSeed, int[] veinIndex, PlacedVein parent,
                                    List<Voxel> voxels, Map<String, Integer> oreCounts,
                                    List<DefinitionRenderStats> definitionStats, int depth, int sceneSizeY,
                                    boolean groundEnabled, int groundLevel) {
        int currentIndex = veinIndex[0]++;
        long renderSeed = def.sourceSeed == null ? baseSeed + currentIndex * 1000003L : def.sourceSeed.longValue();
        VeinGenerationReport report = new OreVeinGenerator(def).generateReport(renderSeed);
        if (isStructureRejectedByGround(def, report, groundEnabled, groundLevel)) {
            return 0;
        }
        Random placementRandom = new Random(renderSeed ^ 0x6d2b79f5L);

        double offsetX = 0.0;
        double offsetZ = 0.0;
        if (parent != null) {
            double range = Math.max(0.0, def.motherlodeRangeLimit.next(placementRandom));
            double radius = Math.sqrt(placementRandom.nextDouble()) * range;
            double angle = placementRandom.nextDouble() * Math.PI * 2.0;
            double targetMotherX = parent.motherSceneX + Math.cos(angle) * radius;
            double targetMotherZ = parent.motherSceneZ + Math.sin(angle) * radius;
            offsetX = targetMotherX - localSceneX(def, report.motherlodeCenter.x);
            offsetZ = targetMotherZ - localSceneZ(def, report.motherlodeCenter.z);
        }

        DefinitionRenderStats currentStats = new DefinitionRenderStats(def.nameForXml(), depth);
        definitionStats.add(currentStats);
        int total = appendVoxels(report.volume, def, offsetX, offsetZ, voxels, oreCounts, currentStats, sceneSizeY, groundEnabled, groundLevel);
        PlacedVein placed = new PlacedVein(
                localSceneX(def, report.motherlodeCenter.x) + offsetX,
                localSceneZ(def, report.motherlodeCenter.z) + offsetZ);
        for (VeinDefinition child : def.children) {
            total += buildDefinitionTree(child, baseSeed, veinIndex, placed, voxels, oreCounts, definitionStats, depth + 1, sceneSizeY, groundEnabled, groundLevel);
        }
        return total;
    }

    private int appendVoxels(OreVolume volume, VeinDefinition def, double offsetX, double offsetZ,
                             List<Voxel> voxels, Map<String, Integer> oreCounts, DefinitionRenderStats definitionStats, int sceneSizeY,
                             boolean groundEnabled, int groundLevel) {
        def.ensureOreBlocks();
        OreRenderPalette palette = paletteFor(def);
        if (voxels instanceof ArrayList) {
            ((ArrayList<Voxel>) voxels).ensureCapacity(voxels.size() + volume.getOreCount());
        }
        int added = 0;
        for (int i = 0; i < volume.getOreCount(); i++) {
            int packed = volume.getOrePosition(i);
            int x = volume.unpackX(packed);
            int y = volume.unpackY(packed);
            int z = volume.unpackZ(packed);
            int oreIndex = volume.getOreIndexAtPosition(packed);
            if (oreIndex < 0 || isCutOffByGround(def, y, groundEnabled, groundLevel)) {
                continue;
            }
            int paletteIndex = palette.index(oreIndex);
            String block = palette.blocks[paletteIndex];
            float sx = (float) ((localSceneX(def, x) + offsetX) * OpenGlViewport.VOXEL_SIZE);
            float sy = (float) ((sceneSizeY / 2.0 - y) * OpenGlViewport.VOXEL_SIZE);
            float sz = (float) ((localSceneZ(def, z) + offsetZ) * OpenGlViewport.VOXEL_SIZE);
            voxels.add(new Voxel(sx, sy, sz, palette.r[paletteIndex], palette.g[paletteIndex], palette.b[paletteIndex]));
            Integer count = oreCounts.get(block);
            oreCounts.put(block, count == null ? 1 : count + 1);
            definitionStats.add(block);
            added++;
        }
        return added;
    }

    private RenderScene buildRegionScene(List<VeinDefinition> roots, long seed) {
        long started = System.nanoTime();
        boolean topDown = isRegionTopDown();
        boolean groundEnabled = isGroundGridEnabled();
        int groundLevel = groundY();
        int regionsX = clampInt(parseEditorInt(regionXField.getText(), 1), 1, 8);
        int regionsZ = clampInt(parseEditorInt(regionZField.getText(), 1), 1, 8);
        int chunksX = regionsX * 32;
        int chunksZ = regionsZ * 32;
        int blocksX = chunksX * CHUNK_SIZE;
        int blocksZ = chunksZ * CHUNK_SIZE;

        List<VeinDefinition> flat = flattenDefinitions(roots);
        int sceneSizeY = topDown ? 16 : 64;
        for (VeinDefinition def : flat) {
            def.fitVolumeToVeinSize(CHUNK_SIZE);
            sceneSizeY = Math.max(sceneSizeY, def.sizeY);
        }
        if (topDown) {
            sceneSizeY = 16;
        }

        final List<RegionInstance> regionInstances = new ArrayList<RegionInstance>();
        Map<VeinDefinition, Integer> sampledByRoot = new IdentityHashMap<VeinDefinition, Integer>();

        for (int defIndex = 0; defIndex < roots.size(); defIndex++) {
            VeinDefinition root = roots.get(defIndex);
            int rootSampled = 0;
            for (int chunkX = 0; chunkX < chunksX; chunkX++) {
                for (int chunkZ = 0; chunkZ < chunksZ; chunkZ++) {
                    Random random = cogStructureGroupRandom(root, seed, chunkX, chunkZ);
                    int count = cogStructureCount(root, random);
                    rootSampled += count;
                    for (int i = 0; i < count; i++) {
                        long structureSeed = random.nextLong();
                        Random structureRandom = new Random(structureSeed);
                        double anchorX = (structureRandom.nextFloat() + chunkX) * CHUNK_SIZE;
                        double anchorZ = (structureRandom.nextFloat() + chunkZ) * CHUNK_SIZE;
                        regionInstances.add(new RegionInstance(root, defIndex, structureSeed, anchorX, anchorZ));
                    }
                }
            }
            sampledByRoot.put(root, rootSampled);
        }

        final int finalBlocksX = blocksX;
        final int finalBlocksZ = blocksZ;
        final int finalSceneSizeY = sceneSizeY;
        final boolean finalTopDown = topDown;
        final boolean finalGroundEnabled = groundEnabled;
        final int finalGroundLevel = groundLevel;
        int workerThreads = regionWorkerCount(regionInstances.size());
        List<Voxel> voxels = new ArrayList<Voxel>();
        Map<String, Integer> totalOreCounts = new HashMap<String, Integer>();
        List<DefinitionRenderStats> definitionStats = new ArrayList<DefinitionRenderStats>();
        int sampledStructures = regionInstances.size();
        int totalInstances = 0;

        if (!regionInstances.isEmpty()) {
            ExecutorService executor = Executors.newFixedThreadPool(workerThreads);
            CompletionService<RegionBuildResult> completion = new ExecutorCompletionService<RegionBuildResult>(executor);
            try {
                for (final RegionInstance instance : regionInstances) {
                    completion.submit(new Callable<RegionBuildResult>() {
                        @Override
                        public RegionBuildResult call() {
                            RegionBuildResult result = new RegionBuildResult();
                            result.instances = appendRegionDefinitionTree(instance.root, instance.defIndex, instance.structureSeed,
                                    instance.anchorX, instance.anchorZ, result.voxels, result.oreCounts, result.definitionStats,
                                    0, finalBlocksX, finalBlocksZ, finalSceneSizeY, finalTopDown, true, finalGroundEnabled, finalGroundLevel);
                            return result;
                        }
                    });
                }

                for (int i = 0; i < regionInstances.size(); i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Region render cancelled");
                    }
                    RegionBuildResult result = completion.take().get();
                    mergeRegionBuildResult(result, voxels, totalOreCounts, definitionStats);
                    totalInstances += result.instances;
                }
            } catch (InterruptedException ex) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                throw new RuntimeException("Region render cancelled", ex);
            } catch (ExecutionException ex) {
                executor.shutdownNow();
                Throwable cause = ex.getCause();
                throw new RuntimeException(cause == null ? ex : cause);
            } finally {
                executor.shutdownNow();
            }
        }

        double expectedStructures = expectedFrequencyStructures(roots, chunksX, chunksZ);
        StringBuilder stats = new StringBuilder();
        stats.append("Mode: Region ").append(topDown ? "2D top-down" : "3D").append("\n");
        stats.append("View: ").append(topDown ? "top-down orthographic projection" : "3D perspective").append("\n");
        stats.append("Seed: ").append(seed).append("\n");
        stats.append("Regions: ").append(regionsX).append(" x ").append(regionsZ).append("\n");
        stats.append("Chunks: ").append(chunksX).append(" x ").append(chunksZ)
                .append(" (").append((long) chunksX * (long) chunksZ).append(" total)\n");
        stats.append("Chunk size: ").append(CHUNK_SIZE).append(" x ").append(CHUNK_SIZE).append(" blocks\n");
        stats.append("COG frequency math: MapGenOreDistribution-compatible chunk seeding\n");
        stats.append("Region worker threads: ").append(workerThreads).append("\n");
        stats.append("Expected structures: ").append(format(expectedStructures)).append("\n");
        stats.append("Sampled structures: ").append(sampledStructures).append("\n");
        stats.append("Per distribution frequency:\n");
        for (VeinDefinition root : roots) {
            double expectedForRoot = Math.max(0.0, root.distributionFrequency.mean) * (double) chunksX * (double) chunksZ;
            Integer sampledForRoot = sampledByRoot.get(root);
            stats.append("  ").append(root.nameForXml())
                    .append(": freq/chunk ").append(format(root.distributionFrequency.mean))
                    .append(", expected ").append(format(expectedForRoot))
                    .append(", sampled ").append(sampledForRoot == null ? 0 : sampledForRoot.intValue())
                    .append("\n");
        }
        stats.append("Rendered vein instances including children: ").append(totalInstances).append("\n");
        stats.append("Ground cutoff: ").append(groundEnabled ? "Y=" + groundLevel : "off").append("\n");
        stats.append("Total ore blocks: ").append(voxels.size()).append("\n");
        long buildMillis = (System.nanoTime() - started) / 1000000L;
        stats.append("CPU scene build: ").append(buildMillis).append(" ms\n");
        stats.append("Total composition:\n");
        for (Map.Entry<String, Integer> entry : totalOreCounts.entrySet()) {
            stats.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        stats.append("\nPer vein instance:\n");
        for (DefinitionRenderStats definitionStat : definitionStats) {
            for (int i = 0; i < definitionStat.depth; i++) {
                stats.append("  ");
            }
            stats.append(definitionStat.depth == 0 ? "Mother: " : "Child: ")
                    .append(definitionStat.name)
                    .append(" - ")
                    .append(definitionStat.total)
                    .append(" blocks\n");
        }

        return new RenderScene(voxels, definitionStats, blocksX, sceneSizeY, blocksZ, stats.toString(), groundEnabled, groundLevel, topDown, buildMillis);
    }

    private int regionWorkerCount(int taskCount) {
        if (taskCount <= 0) {
            return 0;
        }
        int processors = Runtime.getRuntime().availableProcessors();
        int usable = processors <= 2 ? processors : processors - 1;
        return Math.max(1, Math.min(taskCount, usable));
    }

    private void mergeRegionBuildResult(RegionBuildResult result, List<Voxel> voxels,
                                        Map<String, Integer> totalOreCounts,
                                        List<DefinitionRenderStats> definitionStats) {
        if (voxels instanceof ArrayList) {
            ((ArrayList<Voxel>) voxels).ensureCapacity(voxels.size() + result.voxels.size());
        }
        voxels.addAll(result.voxels);
        definitionStats.addAll(result.definitionStats);
        for (Map.Entry<String, Integer> entry : result.oreCounts.entrySet()) {
            Integer count = totalOreCounts.get(entry.getKey());
            totalOreCounts.put(entry.getKey(), count == null ? entry.getValue() : count + entry.getValue());
        }
    }

    private int appendRegionDefinitionTree(VeinDefinition def, int defIndex, long structureSeed, double anchorX, double anchorZ,
                                           List<Voxel> voxels, Map<String, Integer> totalOreCounts,
                                           List<DefinitionRenderStats> definitionStats, int depth,
                                           int blocksX, int blocksZ, int sceneSizeY, boolean topDown,
                                           boolean afterCogPosition, boolean groundEnabled, int groundLevel) {
        VeinGenerationReport report = afterCogPosition
                ? new OreVeinGenerator(def).generateReportForCogPosition(structureSeed)
                : new OreVeinGenerator(def).generateReport(structureSeed);
        if (isStructureRejectedByGround(def, report, groundEnabled, groundLevel)) {
            return 0;
        }
        DefinitionRenderStats currentStats = new DefinitionRenderStats(def.nameForXml(), depth);
        definitionStats.add(currentStats);
        appendRegionVolume(report.volume, def, anchorX, anchorZ, voxels, totalOreCounts, currentStats,
                blocksX, blocksZ, sceneSizeY, topDown, groundEnabled, groundLevel);
        double motherWorldX = reportMotherWorldX(report, def, anchorX);
        double motherWorldZ = reportMotherWorldZ(report, def, anchorZ);
        int total = 1;
        for (VeinDefinition child : def.children) {
            FrequencyPlacement placement = childFrequencyPlacement(child, structureSeed, motherWorldX, motherWorldZ);
            total += appendRegionDefinitionTree(child, defIndex + 1, placement.seed, placement.anchorX, placement.anchorZ,
                    voxels, totalOreCounts, definitionStats, depth + 1, blocksX, blocksZ, sceneSizeY,
                    topDown, false, groundEnabled, groundLevel);
        }
        return total;
    }

    private int appendRegionVolume(OreVolume volume, VeinDefinition def, double anchorX, double anchorZ,
                                   List<Voxel> voxels, Map<String, Integer> totalOreCounts,
                                   DefinitionRenderStats currentStats, int blocksX, int blocksZ, int sceneSizeY,
                                   boolean topDown, boolean groundEnabled, int groundLevel) {
        def.ensureOreBlocks();
        OreRenderPalette palette = paletteFor(def);
        if (voxels instanceof ArrayList) {
            ((ArrayList<Voxel>) voxels).ensureCapacity(voxels.size() + volume.getOreCount());
        }
        int added = 0;
        for (int i = 0; i < volume.getOreCount(); i++) {
            int packed = volume.getOrePosition(i);
            int x = volume.unpackX(packed);
            int y = volume.unpackY(packed);
            int z = volume.unpackZ(packed);
            int oreIndex = volume.getOreIndexAtPosition(packed);
            if (oreIndex < 0 || isCutOffByGround(def, y, groundEnabled, groundLevel)) {
                continue;
            }
            double worldX = anchorX + x - def.motherlodeX;
            double worldZ = anchorZ + z - def.motherlodeZ;
            if (worldX < 0.0 || worldZ < 0.0 || worldX >= blocksX || worldZ >= blocksZ) {
                continue;
            }
            int paletteIndex = palette.index(oreIndex);
            String block = palette.blocks[paletteIndex];
            float sx = (float) ((worldX - blocksX / 2.0) * OpenGlViewport.VOXEL_SIZE);
            float sy = (float) (((topDown ? sceneSizeY / 2.0 : sceneSizeY / 2.0 - y)) * OpenGlViewport.VOXEL_SIZE);
            float sz = (float) ((worldZ - blocksZ / 2.0) * OpenGlViewport.VOXEL_SIZE);
            voxels.add(new Voxel(sx, sy, sz, palette.r[paletteIndex], palette.g[paletteIndex], palette.b[paletteIndex]));
            Integer count = totalOreCounts.get(block);
            totalOreCounts.put(block, count == null ? 1 : count + 1);
            currentStats.add(block);
            added++;
        }
        return added;
    }

    private OreRenderPalette paletteFor(VeinDefinition def) {
        int count = Math.max(1, def.oreBlocks.size());
        String[] blocks = new String[count];
        float[] r = new float[count];
        float[] g = new float[count];
        float[] b = new float[count];
        for (int i = 0; i < count; i++) {
            OreBlockDefinition ore = i < def.oreBlocks.size() ? def.oreBlocks.get(i) : null;
            String block = ore == null ? def.oreBlockName : ore.block;
            Color color = colorForOre(ore, block);
            blocks[i] = block == null || block.length() == 0 ? "(unknown)" : block;
            r[i] = color.getRed() / 255.0f;
            g[i] = color.getGreen() / 255.0f;
            b[i] = color.getBlue() / 255.0f;
        }
        return new OreRenderPalette(blocks, r, g, b);
    }

    private List<VeinDefinition> flattenDefinitions(List<VeinDefinition> roots) {
        List<VeinDefinition> flattened = new ArrayList<VeinDefinition>();
        for (VeinDefinition root : roots) {
            addWithChildren(flattened, root);
        }
        return flattened;
    }

    private void addWithChildren(List<VeinDefinition> flattened, VeinDefinition def) {
        flattened.add(def);
        for (VeinDefinition child : def.children) {
            addWithChildren(flattened, child);
        }
    }

    private Random cogStructureGroupRandom(VeinDefinition def, long worldSeed, int chunkX, int chunkZ) {
        long distributionSeed = def.sourceSeed == null ? def.nameForXml().hashCode() : def.sourceSeed.longValue();
        Random random = new Random(worldSeed);
        long xSeed = random.nextLong();
        long zSeed = random.nextLong();
        xSeed >>= 3;
        zSeed >>= 3;
        random.setSeed((long) chunkX * xSeed ^ (long) chunkZ * zSeed ^ worldSeed);
        random.setSeed((long) random.nextInt() ^ distributionSeed);
        random.nextInt();
        random.nextInt();
        return random;
    }

    private int cogStructureCount(VeinDefinition def, Random random) {
        if (def.distributionFrequency.getMax() >= 1.0) {
            return Math.max(0, def.distributionFrequency.nextInt(random));
        }
        return def.distributionFrequency.nextInt(random) == 1 ? 1 : 0;
    }

    private double expectedFrequencyStructures(List<VeinDefinition> definitions, int chunksX, int chunksZ) {
        double chunks = (double) chunksX * (double) chunksZ;
        double total = 0.0;
        for (VeinDefinition def : definitions) {
            total += Math.max(0.0, def.distributionFrequency.mean) * chunks;
        }
        return total;
    }

    private FrequencyPlacement childFrequencyPlacement(VeinDefinition child, long parentSeed,
                                                       double parentMotherX, double parentMotherZ) {
        long childSeed = parentSeed ^ ((long) child.nameForXml().hashCode() << 32) ^ 0x4f1bbcdcL;
        Random placementRandom = new Random(childSeed);
        double range = Math.max(0.0, child.motherlodeRangeLimit.next(placementRandom));
        double radius = Math.sqrt(placementRandom.nextDouble()) * range;
        double angle = placementRandom.nextDouble() * Math.PI * 2.0;
        return new FrequencyPlacement(childSeed, parentMotherX + Math.cos(angle) * radius,
                parentMotherZ + Math.sin(angle) * radius);
    }

    private double reportMotherWorldX(VeinGenerationReport report, VeinDefinition def, double anchorX) {
        return anchorX + report.motherlodeCenter.x - def.motherlodeX;
    }

    private double reportMotherWorldZ(VeinGenerationReport report, VeinDefinition def, double anchorZ) {
        return anchorZ + report.motherlodeCenter.z - def.motherlodeZ;
    }

    private VeinDefinition findParentOf(VeinDefinition child) {
        for (VeinDefinition root : loadedDefinitions) {
            VeinDefinition parent = findParentOf(root, child);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    private VeinDefinition findParentOf(VeinDefinition current, VeinDefinition child) {
        for (VeinDefinition nested : current.children) {
            if (nested == child) {
                return current;
            }
            VeinDefinition parent = findParentOf(nested, child);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    private double localSceneX(VeinDefinition def, double x) {
        return x - def.sizeX / 2.0;
    }

    private double localSceneZ(VeinDefinition def, double z) {
        return z - def.sizeZ / 2.0;
    }

    private long parseSeed(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (Exception ignored) {
            return text == null ? 0L : text.hashCode();
        }
    }

    private double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int parseEditorInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Double parseEditorDouble(String text) {
        try {
            return Double.valueOf(Double.parseDouble(text.trim()));
        } catch (Exception ignored) {
            return null;
        }
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int groundY() {
        return clampInt(parseEditorInt(groundYField == null ? null : groundYField.getText(), 70), 0, 255);
    }

    private boolean isGroundGridEnabled() {
        return groundGridBox != null && groundGridBox.isSelected();
    }

    private boolean isCutOffByGround(VeinDefinition def, int y, boolean groundEnabled, int groundLevel) {
        return groundEnabled && def != null && !def.replacesAir && y > groundLevel;
    }

    private boolean isStructureRejectedByGround(VeinDefinition def, VeinGenerationReport report,
                                                boolean groundEnabled, int groundLevel) {
        return groundEnabled
                && def != null
                && !def.replacesAir
                && report != null
                && report.motherlodeCenter != null
                && report.motherlodeCenter.y > groundLevel;
    }

    private boolean isCloud(VeinDefinition def) {
        return def != null && "Cloud".equals(def.distributionType);
    }

    private boolean settingAllowedFor(VeinDefinition def, String setting) {
        String[] settings = settingsFor(def);
        for (String allowed : settings) {
            if (allowed.equals(setting)) {
                return true;
            }
        }
        return false;
    }

    private String[] settingsFor(VeinDefinition def) {
        return isCloud(def) ? CLOUD_SETTINGS : VEIN_SETTINGS;
    }

    private PDist getSetting(VeinDefinition target, String name) {
        if ("DistributionFrequency".equals(name)) return target.distributionFrequency;
        if ("ParentRangeLimit".equals(name)) return target.motherlodeRangeLimit;
        if ("CloudRadius".equals(name)) return target.cloudRadius;
        if ("CloudThickness".equals(name)) return target.cloudThickness;
        if ("CloudSizeNoise".equals(name)) return target.cloudSizeNoise;
        if ("CloudHeight".equals(name)) return target.cloudHeight;
        if ("CloudInclination".equals(name)) return target.cloudInclination;
        if ("MotherlodeFrequency".equals(name)) return target.distributionFrequency;
        if ("MotherlodeRangeLimit".equals(name)) return target.motherlodeRangeLimit;
        if ("MotherlodeHeight".equals(name)) return target.motherlodeHeight;
        if ("MotherlodeSize".equals(name)) return target.motherlodeSize;
        if ("BranchFrequency".equals(name)) return target.branchFrequency;
        if ("BranchLength".equals(name)) return target.branchLength;
        if ("BranchInclination".equals(name)) return target.branchInclination;
        if ("BranchHeightLimit".equals(name)) return target.branchHeightLimit;
        if ("SegmentForkFrequency".equals(name)) return target.segmentForkFrequency;
        if ("SegmentForkLengthMult".equals(name)) return target.segmentForkLengthMultiplier;
        if ("SegmentLength".equals(name)) return target.segmentLength;
        if ("SegmentAngle".equals(name)) return target.segmentAngle;
        if ("SegmentPitch".equals(name)) return target.segmentPitch;
        if ("SegmentRadius".equals(name)) return target.segmentRadius;
        if ("OreDensity".equals(name)) return target.oreDensity;
        if ("OreVolumeNoiseCutoff".equals(name)) return target.oreVolumeNoiseCutoff;
        if ("OreRadiusMult".equals(name)) return target.oreRadiusMultiplier;
        return new PDist(0.0, 0.0);
    }

    private void setSetting(VeinDefinition target, String name, PDist value) {
        if ("MotherlodeFrequency".equals(name) || "DistributionFrequency".equals(name)) target.distributionFrequency = value;
        else if ("MotherlodeRangeLimit".equals(name) || "ParentRangeLimit".equals(name)) target.motherlodeRangeLimit = value;
        else if ("CloudRadius".equals(name)) target.cloudRadius = value;
        else if ("CloudThickness".equals(name)) target.cloudThickness = value;
        else if ("CloudSizeNoise".equals(name)) target.cloudSizeNoise = value;
        else if ("CloudHeight".equals(name)) target.cloudHeight = value;
        else if ("CloudInclination".equals(name)) target.cloudInclination = value;
        else if ("MotherlodeHeight".equals(name)) target.motherlodeHeight = value;
        else if ("MotherlodeSize".equals(name)) target.motherlodeSize = value;
        else if ("BranchFrequency".equals(name)) target.branchFrequency = value;
        else if ("BranchLength".equals(name)) target.branchLength = value;
        else if ("BranchInclination".equals(name)) target.branchInclination = value;
        else if ("BranchHeightLimit".equals(name)) target.branchHeightLimit = value;
        else if ("SegmentForkFrequency".equals(name)) target.segmentForkFrequency = value;
        else if ("SegmentForkLengthMult".equals(name)) target.segmentForkLengthMultiplier = value;
        else if ("SegmentLength".equals(name)) target.segmentLength = value;
        else if ("SegmentAngle".equals(name)) target.segmentAngle = value;
        else if ("SegmentPitch".equals(name)) target.segmentPitch = value;
        else if ("SegmentRadius".equals(name)) target.segmentRadius = value;
        else if ("OreDensity".equals(name)) target.oreDensity = value;
        else if ("OreVolumeNoiseCutoff".equals(name)) target.oreVolumeNoiseCutoff = value;
        else if ("OreRadiusMult".equals(name)) target.oreRadiusMultiplier = value;
    }

    private String tooltipForSetting(String setting) {
        String text = SETTING_HELP.get(setting);
        return text == null ? setting : "<html><b>" + setting + "</b><br><br>" + text + "</html>";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.US, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String colorHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private JComboBox<OreDictionaryEntry> createOreDictionaryCombo(String block) {
        JComboBox<OreDictionaryEntry> combo = new JComboBox<OreDictionaryEntry>();
        for (OreDictionaryEntry entry : oreDictionary) {
            combo.addItem(entry);
        }
        selectDictionaryEntry(combo, block);
        return combo;
    }

    private void selectDictionaryEntry(JComboBox<OreDictionaryEntry> combo, String block) {
        if (block == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            OreDictionaryEntry entry = combo.getItemAt(i);
            if (entry != null && block.equalsIgnoreCase(entry.block)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(-1);
    }

    private String oreBlockDisplayName(OreBlockDefinition ore) {
        return ore == null || ore.block == null || ore.block.trim().length() == 0 ? "(no block id)" : ore.block;
    }

    private Color colorForDictionaryEntry(OreDictionaryEntry entry) {
        if (entry != null && entry.colorHex != null && entry.colorHex.length() > 0) {
            try {
                return Color.decode(entry.colorHex);
            } catch (Exception ignored) {
            }
        }
        return colorForOreBlock(entry == null ? "" : entry.block);
    }

    private void updateDictionaryColorForBlock(String block, String colorHex) {
        if (block == null || colorHex == null) {
            return;
        }
        for (OreDictionaryEntry entry : oreDictionary) {
            if (block.equalsIgnoreCase(entry.block)) {
                entry.colorHex = colorHex;
                saveOreDictionary();
                return;
            }
        }
    }

    private JComboBox<BiomeDictionaryEntry> createBiomeDictionaryCombo(BiomeGateEntry gate) {
        JComboBox<BiomeDictionaryEntry> combo = new JComboBox<BiomeDictionaryEntry>();
        for (BiomeDictionaryEntry entry : biomeDictionary) {
            combo.addItem(entry);
        }
        selectBiomeDictionaryEntry(combo, gate);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        combo.setToolTipText("Choose a saved biome gate. Biome entries are regexes; BiomeType entries are Forge biome categories.");
        return combo;
    }

    private void selectBiomeDictionaryEntry(JComboBox<BiomeDictionaryEntry> combo, BiomeGateEntry gate) {
        if (gate == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            BiomeDictionaryEntry entry = combo.getItemAt(i);
            if (entry != null && entry.kind.equalsIgnoreCase(gate.kind) && entry.matcher.equalsIgnoreCase(gate.name)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(-1);
    }

    private Color colorForOre(OreBlockDefinition ore, String block) {
        if (ore != null && ore.colorHex != null && ore.colorHex.length() > 0) {
            try {
                return Color.decode(ore.colorHex);
            } catch (Exception ignored) {
            }
        }
        for (OreDictionaryEntry entry : oreDictionary) {
            if (block != null && block.equalsIgnoreCase(entry.block)) {
                return colorForDictionaryEntry(entry);
            }
        }
        return colorForOreBlock(block);
    }

    private Color colorForOreBlock(String block) {
        String lower = block == null ? "" : block.toLowerCase();
        if (lower.contains("diamond")) return new Color(70, 230, 230);
        if (lower.contains("emerald")) return new Color(60, 220, 110);
        if (lower.contains("gold")) return new Color(245, 195, 60);
        if (lower.contains("redstone")) return new Color(230, 50, 50);
        if (lower.contains("lapis")) return new Color(55, 90, 220);
        if (lower.contains("coal")) return new Color(45, 45, 45);
        if (lower.contains("copper")) return new Color(210, 110, 60);
        if (lower.contains("uranium")) return new Color(115, 230, 70);
        if (lower.contains("iron")) return new Color(205, 170, 135);
        return new Color(180, 180, 190);
    }

    private static void installDarkDefaults() {
        UIManager.put("Panel.background", PANEL_BG);
        UIManager.put("Label.foreground", CONTROL_FG);
        UIManager.put("Button.background", CONTROL_BG);
        UIManager.put("Button.foreground", CONTROL_FG);
        UIManager.put("CheckBox.background", PANEL_BG);
        UIManager.put("CheckBox.foreground", CONTROL_FG);
        UIManager.put("ComboBox.background", CONTROL_BG);
        UIManager.put("ComboBox.foreground", CONTROL_FG);
        UIManager.put("TextField.background", CONTROL_BG);
        UIManager.put("TextField.foreground", CONTROL_FG);
        UIManager.put("TextField.caretForeground", CONTROL_FG);
        UIManager.put("TextArea.background", CONTROL_BG);
        UIManager.put("TextArea.foreground", CONTROL_FG);
        UIManager.put("TabbedPane.background", PANEL_BG);
        UIManager.put("TabbedPane.foreground", CONTROL_FG);
        UIManager.put("TabbedPane.selected", CONTROL_BG);
        UIManager.put("List.background", CONTROL_BG);
        UIManager.put("List.foreground", CONTROL_FG);
        UIManager.put("ScrollPane.background", PANEL_BG);
        UIManager.put("Viewport.background", PANEL_BG);
    }

    private static void applyDarkTheme(Component component) {
        if (component == null) {
            return;
        }
        if (component instanceof JPanel) {
            component.setBackground(PANEL_BG);
            component.setForeground(CONTROL_FG);
        } else if (component instanceof JLabel) {
            component.setForeground(CONTROL_FG);
        } else if (component instanceof JTextField) {
            JTextField field = (JTextField) component;
            field.setBackground(CONTROL_BG);
            field.setForeground(CONTROL_FG);
            field.setCaretColor(CONTROL_FG);
            field.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        } else if (component instanceof JTextArea) {
            JTextArea area = (JTextArea) component;
            area.setBackground(CONTROL_BG);
            area.setForeground(CONTROL_FG);
            area.setCaretColor(CONTROL_FG);
            area.setSelectionColor(new Color(57, 89, 132));
        } else if (component instanceof JCheckBox) {
            component.setBackground(PANEL_BG);
            component.setForeground(CONTROL_FG);
        } else if (component instanceof JComboBox) {
            component.setBackground(CONTROL_BG);
            component.setForeground(CONTROL_FG);
        } else if (component instanceof JSlider) {
            component.setBackground(PANEL_BG);
            component.setForeground(CONTROL_FG);
        } else if (component instanceof JList) {
            component.setBackground(CONTROL_BG);
            component.setForeground(CONTROL_FG);
        } else if (component instanceof JTabbedPane) {
            component.setBackground(PANEL_BG);
            component.setForeground(CONTROL_FG);
        } else if (component instanceof JScrollPane) {
            component.setBackground(PANEL_BG);
            ((JScrollPane) component).getViewport().setBackground(PANEL_BG);
        } else if (component instanceof JButton) {
            JButton button = (JButton) component;
            String text = button.getText();
            if (!"  ".equals(text) && !"...".equals(text)) {
                button.setBackground(CONTROL_BG);
                button.setForeground(CONTROL_FG);
            }
        }
        if (component instanceof javax.swing.JComponent) {
            javax.swing.JComponent jComponent = (javax.swing.JComponent) component;
            if (jComponent.getBorder() instanceof TitledBorder) {
                TitledBorder border = (TitledBorder) jComponent.getBorder();
                border.setTitleColor(CONTROL_FG);
                border.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
            }
        }
        if (component instanceof java.awt.Container) {
            Component[] children = ((java.awt.Container) component).getComponents();
            for (Component child : children) {
                applyDarkTheme(child);
            }
        }
    }

    private interface DistSetter {
        void set(PDist value);
    }

    private static final class OreDictionaryEntry {
        String name;
        String block;
        String colorHex;

        OreDictionaryEntry(String name, String block, String colorHex) {
            this.name = name == null ? "" : name;
            this.block = block == null ? "" : block;
            this.colorHex = colorHex == null ? "" : colorHex;
        }

        @Override
        public String toString() {
            return name == null || name.trim().length() == 0 ? block : name;
        }
    }

    private static final class BiomeDictionaryEntry {
        String name;
        String kind;
        String matcher;
        String group;

        BiomeDictionaryEntry(String name, String kind, String matcher) {
            this(name, kind, matcher, biomeGroupFor(kind, matcher));
        }

        BiomeDictionaryEntry(String name, String kind, String matcher, String group) {
            this.name = name == null ? "" : name;
            this.kind = "BiomeType".equalsIgnoreCase(kind) ? "BiomeType" : "Biome";
            String value = matcher == null ? "" : matcher.trim();
            if ("Biome".equals(this.kind) && value.indexOf(':') >= 0) {
                this.group = namespaceFromRegistry(value);
                this.matcher = displayNameFromRegistry(value);
            } else {
                this.matcher = value.length() == 0 ? this.name : value;
                this.group = "BiomeType".equals(this.kind) ? GROUP_CATEGORY : normalizeNamespace(group);
            }
            if (this.group == null || this.group.length() == 0) {
                this.group = biomeGroupFor(this.kind, this.matcher);
            }
            if (GROUP_CATEGORY.equals(group)) {
                this.group = GROUP_CATEGORY;
            } else if (GROUP_MINECRAFT.equalsIgnoreCase(this.group)) {
                this.group = GROUP_MINECRAFT;
            }
        }

        String cogName() {
            if ("BiomeType".equals(kind)) {
                return nameForEditor();
            }
            String path = biomePathFromLabel(nameForEditor());
            if (path.length() == 0) {
                return "";
            }
            String namespace = group == null || group.trim().length() == 0 ? GROUP_MINECRAFT : group.trim();
            if (namespace.length() == 0) {
                return path;
            } else {
                return namespace + ":" + path;
            }
        }

        String nameForEditor() {
            if (name != null && name.trim().length() > 0) {
                return name;
            }
            return matcher == null ? "" : matcher;
        }

        void setEditorName(String text) {
            this.name = text == null ? "" : text.trim();
            this.matcher = this.name;
        }

        @Override
        public String toString() {
            String label = nameForEditor();
            return label + " [" + ("BiomeType".equals(kind) ? "category" : group) + "]";
        }
    }

    private static final class OpenGlViewport extends AWTGLCanvas {
        static final float VOXEL_SIZE = 4.0f;
        private static final int AUTO_POINT_LOD_VOXELS = 600000;
        private static final int RENDER_BATCH_BLOCKS = 128;
        private RenderScene scene = RenderScene.empty();
        private final List<RenderBatch> renderBatches = new ArrayList<RenderBatch>();
        private boolean displayListDirty = true;
        private float rotateX = 28.0f;
        private float rotateY = -38.0f;
        private float zoom = -760.0f;
        private float panX;
        private float panY;
        private int lastX;
        private int lastY;
        private int lastButton;
        private boolean cameraInitialized;
        private boolean lastTopDown;
        private String renderDetail = "Auto";
        private boolean regionMode;
        private boolean lastPointLod;

        OpenGlViewport() {
            super(defaultData());
            setBackground(new Color(18, 20, 24));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    lastX = event.getX();
                    lastY = event.getY();
                    lastButton = event.getButton();
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent event) {
                    int dx = event.getX() - lastX;
                    int dy = event.getY() - lastY;
                    if (scene.topDown) {
                        panX += dx;
                        panY -= dy;
                    } else if (lastButton == MouseEvent.BUTTON2 || lastButton == MouseEvent.BUTTON3) {
                        panX -= dx;
                        panY -= dy;
                    } else {
                        rotateY += dx * 0.45f;
                        rotateX += dy * 0.45f;
                        rotateX = clampFloat(rotateX, -85.0f, 85.0f);
                    }
                    lastX = event.getX();
                    lastY = event.getY();
                    render();
                }
            });
            addMouseWheelListener(new java.awt.event.MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent event) {
                    zoom -= event.getWheelRotation() * 45.0f;
                    if (zoom > -80.0f) zoom = -80.0f;
                    if (zoom < -5000.0f) zoom = -5000.0f;
                    render();
                }
            });
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    render();
                }

                @Override
                public void componentShown(ComponentEvent event) {
                    render();
                }
            });
        }

        private static GLData defaultData() {
            GLData data = new GLData();
            data.samples = 2;
            data.swapInterval = 1;
            return data;
        }

        void invalidateDisplayList() {
            render();
        }

        void refreshNow() {
            render();
        }

        void setRenderOptions(String renderDetail, boolean regionMode) {
            this.renderDetail = renderDetail == null ? "Auto" : renderDetail;
            this.regionMode = regionMode;
        }

        String activeRenderDetailName() {
            return lastPointLod ? "fast points" : "full cubes";
        }

        void setScene(RenderScene scene) {
            RenderScene nextScene = scene == null ? RenderScene.empty() : scene;
            boolean topDownChanged = cameraInitialized && lastTopDown != nextScene.topDown;
            this.scene = nextScene;
            this.displayListDirty = true;
            if (!cameraInitialized || topDownChanged) {
                fitCameraToScene();
            }
            cameraInitialized = true;
            lastTopDown = this.scene.topDown;
            render();
        }

        private void fitCameraToScene() {
            panX = 0.0f;
            panY = 0.0f;
            if (scene.topDown) {
                zoom = -Math.max(650.0f, Math.max(scene.sizeX, scene.sizeZ) * VOXEL_SIZE);
            } else {
                rotateX = clampFloat(rotateX, -85.0f, 85.0f);
                zoom = -Math.max(650.0f, Math.max(scene.sizeX, scene.sizeZ) * VOXEL_SIZE * 1.25f);
            }
        }

        private static float clampFloat(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        @Override
        public void initGL() {
            GL.createCapabilities();
            GL11.glClearColor(18.0f / 255.0f, 20.0f / 255.0f, 24.0f / 255.0f, 1.0f);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);
        }

        @Override
        public void paintGL() {
            GL11.glViewport(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight()));
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            setupProjection();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            if (scene.topDown) {
                GL11.glTranslatef(panX, panY, -1000.0f);
                GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
            } else {
                GL11.glTranslatef(panX, panY, zoom);
                GL11.glScalef(1.0f, -1.0f, 1.0f);
                GL11.glRotatef(rotateX, 1.0f, 0.0f, 0.0f);
                GL11.glRotatef(rotateY, 0.0f, 1.0f, 0.0f);
            }
            GL11.glFrontFace(scene.topDown ? GL11.GL_CCW : GL11.GL_CW);
            drawGuides();
            if (displayListDirty) {
                rebuildRenderBatches();
            }
            drawRenderBatches();
            swapBuffers();
        }

        private void setupProjection() {
            float aspect = getHeight() == 0 ? 1.0f : (float) getWidth() / (float) getHeight();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            if (scene.topDown) {
                float base = Math.max(128.0f, Math.max(scene.sizeX, scene.sizeZ) * VOXEL_SIZE * 0.56f);
                float scale = Math.max(0.15f, Math.min(8.0f, -zoom / Math.max(1.0f, Math.max(scene.sizeX, scene.sizeZ) * VOXEL_SIZE)));
                float halfHeight = base * scale;
                float halfWidth = halfHeight * aspect;
                GL11.glOrtho(-halfWidth, halfWidth, -halfHeight, halfHeight, 1.0f, 4000.0f);
            } else {
                perspective(55.0f, aspect, 1.0f, 12000.0f);
            }
        }

        private void perspective(float fovY, float aspect, float near, float far) {
            float ymax = (float) (near * Math.tan(Math.toRadians(fovY / 2.0f)));
            float xmax = ymax * aspect;
            GL11.glFrustum(-xmax, xmax, -ymax, ymax, near, far);
        }

        private void rebuildRenderBatches() {
            deleteRenderBatches();
            Map<Long, List<Voxel>> buckets = new HashMap<Long, List<Voxel>>();
            for (Voxel voxel : scene.voxels) {
                int bx = (int) Math.floor((voxel.x / VOXEL_SIZE + scene.sizeX / 2.0f) / RENDER_BATCH_BLOCKS);
                int bz = (int) Math.floor((voxel.z / VOXEL_SIZE + scene.sizeZ / 2.0f) / RENDER_BATCH_BLOCKS);
                long key = (((long) bx) << 32) ^ (bz & 0xffffffffL);
                List<Voxel> bucket = buckets.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<Voxel>();
                    buckets.put(key, bucket);
                }
                bucket.add(voxel);
            }
            for (List<Voxel> bucket : buckets.values()) {
                renderBatches.add(createRenderBatch(bucket));
            }
            displayListDirty = false;
        }

        private RenderBatch createRenderBatch(List<Voxel> voxels) {
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;
            for (Voxel voxel : voxels) {
                minX = Math.min(minX, voxel.x);
                minY = Math.min(minY, voxel.y);
                minZ = Math.min(minZ, voxel.z);
                maxX = Math.max(maxX, voxel.x);
                maxY = Math.max(maxY, voxel.y);
                maxZ = Math.max(maxZ, voxel.z);
            }
            float centerX = (minX + maxX) * 0.5f;
            float centerY = (minY + maxY) * 0.5f;
            float centerZ = (minZ + maxZ) * 0.5f;
            float dx = maxX - centerX + VOXEL_SIZE;
            float dy = maxY - centerY + VOXEL_SIZE;
            float dz = maxZ - centerZ + VOXEL_SIZE;
            float radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            int cubeList = GL11.glGenLists(1);
            GL11.glNewList(cubeList, GL11.GL_COMPILE);
            GL11.glBegin(GL11.GL_TRIANGLES);
            for (Voxel voxel : voxels) {
                emitCube(voxel.x, voxel.y, voxel.z, VOXEL_SIZE * 0.44f, voxel.r, voxel.g, voxel.b);
            }
            GL11.glEnd();
            GL11.glEndList();
            int pointList = GL11.glGenLists(1);
            GL11.glNewList(pointList, GL11.GL_COMPILE);
            GL11.glBegin(GL11.GL_POINTS);
            for (Voxel voxel : voxels) {
                GL11.glColor3f(voxel.r, voxel.g, voxel.b);
                GL11.glVertex3f(voxel.x, voxel.y, voxel.z);
            }
            GL11.glEnd();
            GL11.glEndList();
            return new RenderBatch(cubeList, pointList, centerX, centerY, centerZ, radius, voxels.size());
        }

        private void drawRenderBatches() {
            boolean pointLod = usePointLod();
            lastPointLod = pointLod;
            if (pointLod) {
                GL11.glDisable(GL11.GL_CULL_FACE);
                GL11.glPointSize(pointLodSize());
            }
            ViewCuller culler = createViewCuller();
            for (RenderBatch batch : renderBatches) {
                if (!scene.topDown && !isBatchVisible(culler, batch)) {
                    continue;
                }
                GL11.glCallList(pointLod ? batch.pointList : batch.cubeList);
            }
            if (pointLod) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
        }

        private void deleteRenderBatches() {
            for (RenderBatch batch : renderBatches) {
                if (batch.cubeList != 0) {
                    GL11.glDeleteLists(batch.cubeList, 1);
                }
                if (batch.pointList != 0) {
                    GL11.glDeleteLists(batch.pointList, 1);
                }
            }
            renderBatches.clear();
        }

        private boolean usePointLod() {
            if (scene.topDown) {
                return false;
            }
            if ("Fast points".equals(renderDetail)) {
                return true;
            }
            return "Auto".equals(renderDetail)
                    && regionMode
                    && scene.voxelCount >= AUTO_POINT_LOD_VOXELS
                    && isZoomedOutForPointLod();
        }

        private float pointLodSize() {
            return scene.voxelCount >= 600000 ? 2.0f : 3.0f;
        }

        private boolean isZoomedOutForPointLod() {
            float sceneSpan = Math.max(scene.sizeX, scene.sizeZ) * VOXEL_SIZE;
            return -zoom >= Math.max(1200.0f, sceneSpan * 0.55f);
        }

        private ViewCuller createViewCuller() {
            double yaw = Math.toRadians(rotateY);
            double pitch = Math.toRadians(rotateX);
            float aspect = getHeight() == 0 ? 1.0f : (float) getWidth() / (float) getHeight();
            return new ViewCuller(Math.cos(yaw), Math.sin(yaw), Math.cos(pitch), Math.sin(pitch), aspect);
        }

        private boolean isBatchVisible(ViewCuller culler, RenderBatch batch) {
            double rx = batch.centerX * culler.cosY + batch.centerZ * culler.sinY;
            double rz = -batch.centerX * culler.sinY + batch.centerZ * culler.cosY;
            double ry = batch.centerY * culler.cosX - rz * culler.sinX;
            double rz2 = batch.centerY * culler.sinX + rz * culler.cosX;
            double eyeX = rx + panX;
            double eyeY = -ry + panY;
            double eyeZ = rz2 + zoom;
            if (eyeZ > batch.radius || eyeZ < -12000.0 - batch.radius) {
                return false;
            }
            double distance = Math.max(1.0, -eyeZ);
            double halfY = distance * culler.tanHalfFov;
            double halfX = halfY * culler.aspect;
            double margin = batch.radius + Math.max(VOXEL_SIZE * 8.0, distance * 0.03);
            return eyeX >= -halfX - margin && eyeX <= halfX + margin
                    && eyeY >= -halfY - margin && eyeY <= halfY + margin;
        }

        private void drawGuides() {
            float halfX = scene.sizeX * VOXEL_SIZE / 2.0f;
            float halfY = scene.sizeY * VOXEL_SIZE / 2.0f;
            float halfZ = scene.sizeZ * VOXEL_SIZE / 2.0f;
            GL11.glBegin(GL11.GL_LINES);
            if (!scene.topDown) {
                GL11.glColor3f(0.9f, 0.2f, 0.2f);
                GL11.glVertex3f(-halfX, halfY, -halfZ);
                GL11.glVertex3f(halfX, halfY, -halfZ);
                GL11.glColor3f(0.2f, 0.85f, 0.35f);
                GL11.glVertex3f(-halfX, halfY, -halfZ);
                GL11.glVertex3f(-halfX, -halfY, -halfZ);
                GL11.glColor3f(0.25f, 0.45f, 0.95f);
                GL11.glVertex3f(-halfX, halfY, -halfZ);
                GL11.glVertex3f(-halfX, halfY, halfZ);
            }
            GL11.glColor3f(0.18f, 0.22f, 0.28f);
            for (int x = 0; x <= scene.sizeX; x += CHUNK_SIZE) {
                float sx = (x - scene.sizeX / 2.0f) * VOXEL_SIZE;
                GL11.glVertex3f(sx, halfY, -halfZ);
                GL11.glVertex3f(sx, halfY, halfZ);
            }
            for (int z = 0; z <= scene.sizeZ; z += CHUNK_SIZE) {
                float sz = (z - scene.sizeZ / 2.0f) * VOXEL_SIZE;
                GL11.glVertex3f(-halfX, halfY, sz);
                GL11.glVertex3f(halfX, halfY, sz);
            }
            GL11.glEnd();

            int regionStep = CHUNK_SIZE * 32;
            float majorOffset = scene.topDown ? 2.0f : 1.35f;
            GL11.glLineWidth(2.5f);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glColor3f(0.62f, 0.70f, 0.82f);
            for (int x = 0; x <= scene.sizeX; x += regionStep) {
                float sx = (x - scene.sizeX / 2.0f) * VOXEL_SIZE;
                emitMajorGridLineX(sx, halfY, halfZ, majorOffset);
            }
            for (int z = 0; z <= scene.sizeZ; z += regionStep) {
                float sz = (z - scene.sizeZ / 2.0f) * VOXEL_SIZE;
                emitMajorGridLineZ(halfX, halfY, sz, majorOffset);
            }
            GL11.glEnd();
            GL11.glLineWidth(1.0f);

            GL11.glBegin(GL11.GL_LINES);
            if (scene.groundEnabled) {
                float groundY = (scene.sizeY / 2.0f - Math.min(scene.groundLevel, scene.sizeY)) * VOXEL_SIZE;
                GL11.glColor3f(0.95f, 0.72f, 0.18f);
                for (int x = 0; x <= scene.sizeX; x += 16) {
                    float sx = (x - scene.sizeX / 2.0f) * VOXEL_SIZE;
                    GL11.glVertex3f(sx, groundY, -halfZ);
                    GL11.glVertex3f(sx, groundY, halfZ);
                }
                for (int z = 0; z <= scene.sizeZ; z += 16) {
                    float sz = (z - scene.sizeZ / 2.0f) * VOXEL_SIZE;
                    GL11.glVertex3f(-halfX, groundY, sz);
                    GL11.glVertex3f(halfX, groundY, sz);
                }
            }
            GL11.glEnd();
        }

        private void emitMajorGridLineX(float sx, float y, float halfZ, float offset) {
            for (int i = -2; i <= 2; i++) {
                float x = sx + i * offset;
                GL11.glVertex3f(x, y, -halfZ);
                GL11.glVertex3f(x, y, halfZ);
            }
        }

        private void emitMajorGridLineZ(float halfX, float y, float sz, float offset) {
            for (int i = -2; i <= 2; i++) {
                float z = sz + i * offset;
                GL11.glVertex3f(-halfX, y, z);
                GL11.glVertex3f(halfX, y, z);
            }
        }

        private void emitCube(float x, float y, float z, float h, float r, float g, float b) {
            face(r, g, b, 1.00f, x + h, y - h, z - h, x + h, y + h, z - h, x + h, y + h, z + h, x + h, y - h, z + h);
            face(r, g, b, 0.62f, x - h, y - h, z + h, x - h, y + h, z + h, x - h, y + h, z - h, x - h, y - h, z - h);
            face(r, g, b, 0.72f, x - h, y - h, z - h, x + h, y - h, z - h, x + h, y - h, z + h, x - h, y - h, z + h);
            face(r, g, b, 1.18f, x - h, y + h, z + h, x + h, y + h, z + h, x + h, y + h, z - h, x - h, y + h, z - h);
            face(r, g, b, 0.92f, x + h, y - h, z + h, x + h, y + h, z + h, x - h, y + h, z + h, x - h, y - h, z + h);
            face(r, g, b, 0.52f, x - h, y - h, z - h, x - h, y + h, z - h, x + h, y + h, z - h, x + h, y - h, z - h);
        }

        private void face(float r, float g, float b, float multiplier,
                          float ax, float ay, float az,
                          float bx, float by, float bz,
                          float cx, float cy, float cz,
                          float dx, float dy, float dz) {
            shade(r, g, b, multiplier);
            GL11.glVertex3f(ax, ay, az);
            GL11.glVertex3f(bx, by, bz);
            GL11.glVertex3f(cx, cy, cz);
            GL11.glVertex3f(ax, ay, az);
            GL11.glVertex3f(cx, cy, cz);
            GL11.glVertex3f(dx, dy, dz);
        }

        private void shade(float r, float g, float b, float multiplier) {
            GL11.glColor3f(Math.min(1.0f, r * multiplier), Math.min(1.0f, g * multiplier), Math.min(1.0f, b * multiplier));
        }
    }

    private static final class RenderScene {
        final List<Voxel> voxels;
        final List<DefinitionRenderStats> definitionStats;
        final int sizeX;
        final int sizeY;
        final int sizeZ;
        final int voxelCount;
        final String stats;
        final boolean groundEnabled;
        final int groundLevel;
        final boolean topDown;
        final long buildMillis;

        RenderScene(List<Voxel> voxels, int sizeX, int sizeY, int sizeZ, String stats) {
            this(voxels, new ArrayList<DefinitionRenderStats>(), sizeX, sizeY, sizeZ, stats, false, 70);
        }

        RenderScene(List<Voxel> voxels, List<DefinitionRenderStats> definitionStats,
                    int sizeX, int sizeY, int sizeZ, String stats, boolean groundEnabled, int groundLevel) {
            this(voxels, definitionStats, sizeX, sizeY, sizeZ, stats, groundEnabled, groundLevel, false);
        }

        RenderScene(List<Voxel> voxels, List<DefinitionRenderStats> definitionStats,
                    int sizeX, int sizeY, int sizeZ, String stats, boolean groundEnabled, int groundLevel,
                    boolean topDown) {
            this(voxels, definitionStats, sizeX, sizeY, sizeZ, stats, groundEnabled, groundLevel, topDown, 0L);
        }

        RenderScene(List<Voxel> voxels, List<DefinitionRenderStats> definitionStats,
                    int sizeX, int sizeY, int sizeZ, String stats, boolean groundEnabled, int groundLevel,
                    boolean topDown, long buildMillis) {
            this.voxels = voxels;
            this.definitionStats = definitionStats == null ? new ArrayList<DefinitionRenderStats>() : definitionStats;
            this.sizeX = Math.max(16, sizeX);
            this.sizeY = Math.max(16, sizeY);
            this.sizeZ = Math.max(16, sizeZ);
            this.voxelCount = voxels.size();
            this.stats = stats;
            this.groundEnabled = groundEnabled;
            this.groundLevel = groundLevel;
            this.topDown = topDown;
            this.buildMillis = buildMillis;
        }

        static RenderScene empty() {
            return new RenderScene(new ArrayList<Voxel>(), 128, 80, 128, "No render yet");
        }
    }

    private static final class DefinitionRenderStats {
        final String name;
        final int depth;
        final Map<String, Integer> oreCounts = new HashMap<String, Integer>();
        int total;

        DefinitionRenderStats(String name, int depth) {
            this.name = name == null ? "" : name;
            this.depth = depth;
        }

        void add(String block) {
            String key = block == null || block.length() == 0 ? "(unknown)" : block;
            Integer count = oreCounts.get(key);
            oreCounts.put(key, count == null ? 1 : count + 1);
            total++;
        }
    }

    private static final class Voxel {
        final float x;
        final float y;
        final float z;
        final float r;
        final float g;
        final float b;

        Voxel(float x, float y, float z, float r, float g, float b) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static final class RenderBatch {
        final int cubeList;
        final int pointList;
        final float centerX;
        final float centerY;
        final float centerZ;
        final float radius;
        final int voxelCount;

        RenderBatch(int cubeList, int pointList, float centerX, float centerY, float centerZ, float radius, int voxelCount) {
            this.cubeList = cubeList;
            this.pointList = pointList;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radius = radius;
            this.voxelCount = voxelCount;
        }
    }

    private static final class ViewCuller {
        final double cosY;
        final double sinY;
        final double cosX;
        final double sinX;
        final double aspect;
        final double tanHalfFov;

        ViewCuller(double cosY, double sinY, double cosX, double sinX, double aspect) {
            this.cosY = cosY;
            this.sinY = sinY;
            this.cosX = cosX;
            this.sinX = sinX;
            this.aspect = aspect;
            this.tanHalfFov = Math.tan(Math.toRadians(55.0 / 2.0));
        }
    }

    private static final class OreRenderPalette {
        final String[] blocks;
        final float[] r;
        final float[] g;
        final float[] b;

        OreRenderPalette(String[] blocks, float[] r, float[] g, float[] b) {
            this.blocks = blocks;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        int index(int oreIndex) {
            return oreIndex >= 0 && oreIndex < blocks.length ? oreIndex : 0;
        }
    }

    private static final class PlacedVein {
        final double motherSceneX;
        final double motherSceneZ;

        PlacedVein(double motherSceneX, double motherSceneZ) {
            this.motherSceneX = motherSceneX;
            this.motherSceneZ = motherSceneZ;
        }
    }

    private static final class FrequencyPlacement {
        final long seed;
        final double anchorX;
        final double anchorZ;

        FrequencyPlacement(long seed, double anchorX, double anchorZ) {
            this.seed = seed;
            this.anchorX = anchorX;
            this.anchorZ = anchorZ;
        }
    }

    private static final class RegionInstance {
        final VeinDefinition root;
        final int defIndex;
        final long structureSeed;
        final double anchorX;
        final double anchorZ;

        RegionInstance(VeinDefinition root, int defIndex, long structureSeed, double anchorX, double anchorZ) {
            this.root = root;
            this.defIndex = defIndex;
            this.structureSeed = structureSeed;
            this.anchorX = anchorX;
            this.anchorZ = anchorZ;
        }
    }

    private static final class RegionBuildResult {
        final List<Voxel> voxels = new ArrayList<Voxel>();
        final Map<String, Integer> oreCounts = new HashMap<String, Integer>();
        final List<DefinitionRenderStats> definitionStats = new ArrayList<DefinitionRenderStats>();
        int instances;
    }

    private final class VeinDefinitionCellRenderer extends JLabel implements ListCellRenderer<VeinDefinition> {
        VeinDefinitionCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends VeinDefinition> list, VeinDefinition value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            setText(value == null ? "" : value.nameForXml());
            setToolTipText(value == null || value.sourceFile == null ? null : value.sourceFile.getAbsolutePath());
            if (isSelected) {
                setBackground(new Color(54, 72, 96));
                setForeground(Color.WHITE);
            } else {
                setBackground(CONTROL_BG);
                setForeground(CONTROL_FG);
            }
            return this;
        }
    }

    private final class XmlFileCellRenderer extends JCheckBox implements ListCellRenderer<File> {
        XmlFileCellRenderer() {
            setOpaque(true);
        }

        @Override
        public java.awt.Component getListCellRendererComponent(JList<? extends File> list, File value, int index,
                                                               boolean isSelected, boolean cellHasFocus) {
            setSelected(value != null && checkedXmlFiles.contains(value));
            setText(value == null ? "" : value.getName());
            setToolTipText(value == null ? null : value.getAbsolutePath());
            if (isSelected) {
                setBackground(new Color(54, 72, 96));
                setForeground(Color.WHITE);
            } else {
                setBackground(new Color(24, 27, 32));
                setForeground(new Color(230, 236, 245));
            }
            return this;
        }
    }

    private static final class EditorFormPanel extends JPanel implements Scrollable {
        @Override
        public void doLayout() {
            int width = Math.max(1, getWidth());
            int y = 0;
            for (Component child : getComponents()) {
                if (!child.isVisible()) {
                    continue;
                }
                Dimension preferred = child.getPreferredSize();
                child.setBounds(0, y, width, preferred.height);
                y += preferred.height;
            }
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            int width = EDITOR_CONTENT_WIDTH;
            int height = 0;
            for (Component child : getComponents()) {
                if (!child.isVisible()) {
                    continue;
                }
                Dimension preferred = child.getPreferredSize();
                width = Math.max(width, preferred.width);
                height += preferred.height;
            }
            return new Dimension(width, height);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(60, orientation == SwingConstants.VERTICAL ? visibleRect.height - 40 : visibleRect.width - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
