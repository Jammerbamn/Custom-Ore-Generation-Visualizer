# Custom Ore Vein Visualizer

Standalone Java desktop application for visualizing Minecraft CustomOreGen-style ore veins without launching Minecraft.

The app uses Swing for the editor/browser UI and LWJGL/OpenGL for the 3D viewport. It targets Java 8-compatible source and bundles the LWJGL jars and Windows natives in `lib/`.

## Features

- Load CustomOreGen `.xml` files from the built-in file browser.
- Check multiple XML files and select top-level distributions from the dropdown.
- Render single veins, stacked veins, and region frequency views.
- Switch region view between 3D and 2D top-down.
- Rotate, zoom, and pan the viewport.
- Show axes, chunk grid lines, and major 32x32 chunk region lines.
- Optional ground cutoff grid.
- XML editor with add/remove parent veins, nested child veins, optional settings, ore weights, ore colors, biome gates, and editable XML preview.
- Ore dictionary and biome dictionary tabs with JSON save/load.
- Export render/context data for AI-assisted vein tuning.

The generation math is based on the CustomOreGen source, especially `MapGenOreDistribution`, `MapGenVeins`, `MapGenCloud`, and `PDist`.

## Run

```bat
build-lwjgl.bat
run-lwjgl.bat
```

For a non-UI generator smoke test:

```bat
build-core.bat
```

## Notes

- The viewer does not require Minecraft to launch.
- Biome gates are represented in XML/editor data, but the viewer does not simulate real biome maps yet.
- The frequency view samples region placement; low frequencies can need several regions before differences are visually obvious.

## Development Note

This project was developed with the help of AI-assisted coding tools.

The repository intentionally keeps the project simple:

- Java source lives under `src/main/java`.
- LWJGL jars and Windows natives are bundled in `lib/` so Windows users can build/run without Gradle or Maven.
- Generated classes in `build/`, IntelliJ project state in `.idea/`, and exported render reports are ignored by git.
