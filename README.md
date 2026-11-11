# Devilica

A rendering mod for Minecraft 1.12.2 on [Cleanroom](https://github.com/CleanroomMC/Cleanroom):
[Celeritas](https://git.taumc.org/embeddedt/celeritas) terrain, shader packs through Iris, and most of
what OptiFine does, carried over from [Angelica](https://github.com/GTNewHorizons/Angelica) and brought
to 1.12.2.

## Read this first

I made Devilica for my own modpack. It is developed and tested with that pack's mods and nothing else:
it works there, and that is the only setup I check. I don't plan to support other packs or mod sets,
so issues from other setups will likely go unanswered. You are welcome to use it, fork it or take code
from it under the license below.

## What it does

- Terrain through Celeritas: chunk meshes built on worker threads, multi-draw, translucent face
  sorting, graph and fog culling, and, under a shader pack, raster occlusion culling and a shadow pass
  that draws only what can cast a shadow on what the camera sees.
- Shader packs in OptiFine's format through Iris, run as Angelica runs them, in Cleanroom's OpenGL
  compatibility context. Checked with Complementary Reimagined and Unbound (with Euphoria Patches),
  BSL, Sildur's Vibrant and MakeUp Ultra Fast.
- Dynamic lights from held items and glowing or burning entities, with Celeritas Dynamic Lights'
  options.
- Coloured block light without a shader pack.
- Zoom, as Zume does it.
- Resource pack features of MCPatcher and OptiFine: connected textures, custom item textures, custom
  colours, custom skies, random mobs, emissive textures, natural textures, better grass and better
  snow.
- The detail and performance options of OptiFine and Celeritas Extra: clouds, fog, sky, particles,
  animations, entity render distance, leaves, an FPS overlay and more.
- Render scale, with AMD FidelityFX Super Resolution 1 upscaling.
- Chests, ender chests and signs drawn in batches.
- An API for mods that draw their own things in the world: [docs/API.md](docs/API.md).

Devilica replaces OptiFine, Celeritas, Celeritas Extra, Celeritas Dynamic Lights and Zume. Don't
install it together with any of them.

## Requirements

- Minecraft 1.12.2 on Cleanroom 0.7.14 or newer, with Java 25.
- The OpenGL compatibility context Cleanroom creates by default. Tested on Windows with OpenGL 4.6;
  not tested on macOS.

## Building

```bash
./gradlew build
```

The jar to install is `build/libs/devilica-<version>.jar` (not `-mcp`, `-sources` or `-javadoc`).

## Origin and license

Devilica is under the GNU Lesser General Public License v3 (see [LICENSE](LICENSE)), which adds its
permissions to the GNU General Public License v3 (see [COPYING](COPYING)). Most of its code
comes from other projects, changed for 1.12.2 and Cleanroom; their files keep their packages:

- [Celeritas](https://git.taumc.org/embeddedt/celeritas) by embeddedt and contributors, LGPL v3: the
  terrain renderer (`common/`, and `org.taumc.celeritas` from its 1.12.2 version). Celeritas descends
  from Sodium by JellySquid, from its versions under the LGPL.
- [Angelica](https://github.com/GTNewHorizons/Angelica) by GTNewHorizons and contributors, LGPL v3:
  its port of [Iris](https://github.com/IrisShaders/Iris) (`net.coderbot.iris`) and most of the
  features above.
- bitraster by grondag, Apache License 2.0, part of Celeritas: the raster occlusion culler.
- AMD FidelityFX Super Resolution 1, MIT License: the upscaler's reference headers.

The jar carries these licenses in `licenses/`. [docs/UPSTREAM.md](docs/UPSTREAM.md) lists the commits
the code was taken from and what was changed. Devilica isn't made, endorsed or supported by any of
these projects: don't report its problems to them.
