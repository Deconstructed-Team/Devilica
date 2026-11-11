# Upstream

Devilica carries code from these projects, all LGPL-3.0. Their files keep their packages, so
upstream fixes can be carried over file by file. Record every sync here.

| Project | Source | Base commit | What Devilica takes |
|---|---|---|---|
| Celeritas | https://git.taumc.org/embeddedt/celeritas | `5cf5e9a89` (2026-11-05) | `common` -> `common/`; `forge122` -> `src/main/java/org/taumc/celeritas` |
| Angelica | https://github.com/GTNewHorizons/Angelica | `d7d2228a~1` (2026-03-03) for Iris and its GL layer; `3876218` (2026-11-06) for the other features | Iris shaders and its other features (dynamic lights, MCPatcher's connected textures, custom colours, skies, item textures and random mobs, block entity batching), not its fonts |
| CleanroomModTemplate | https://github.com/CleanroomMC/CleanroomModTemplate | `1863fbb` | the build |

## Celeritas `common`

- `common/src/main/java` = upstream `common/src/main/java` + `src/lwjglCommon/java` +
  `src/lwjgl3/java` + the classes `generateLWJGLAbstraction` writes
  (`build/generated/sources/lwjgl-abstraction/java/main`, GL constant tables). Cleanroom has
  LWJGL 3 natively, so the LWJGL 2 variant is gone.
- Compiled with Java 25 against the libraries Cleanroom 0.7 ships (fastutil 8.5.18, JOML 1.10.9,
  gson 2.14.0, ASM 9.10.1, log4j 2.26.0); nothing is bundled.
- Celeritas's rule stays: never take code from Sodium 0.6 or later, its license is incompatible.

## Celeritas `forge122`

Changes against upstream:

- `CeleritasVintage` is no longer a `@Mod`; `dev.feldsher.devilica.Devilica` is the mod and calls it.
  `CeleritasVintage.MODID` is `devilica`.
- Gone: `core/CeleritasLoadingPlugin` (mixins come from the jar manifest), the lwjgl3ify/RFB
  compat and the `org.lwjgl` -> `org.lwjgl3` relocation transformer.
- `mixins.celeritas.json` -> `mixins.devilica.json`, `celeritas_at.cfg` -> `devilica_at.cfg`.
- Mixins on members Forge or Cleanroom added (`FaceBakery.makeBakedQuad`, `SplashProgress$1.call`)
  are `remap = false`; `RenderItemMixin` names `renderQuads` only.
- Jabel's `@Desugar` removed (Java 25).
- Raster occlusion culling runs under a shader pack: the meshing task fills a
  `SectionVisibilityBuilder` (with occluder boxes) in place of vanilla's `VisGraph`, and the frustum's
  viewport carries its view-projection matrix. In `common`, the option is
  `use_raster_occlusion_culling_v2`, on by default.

## Angelica Iris

Iris comes from Angelica's last commit before "Target OpenGL 3.3 core profile" (#1412, `d7d2228a`).
From then on Angelica's Iris rewrites every gbuffers program for the core profile and feeds it from
Angelica's fixed-function emulation; Devilica keeps Cleanroom's compatibility context, where shader
packs' `#version 120` code runs as written, as it did in that Angelica.

- Angelica's GLStateManager owned all GL state, and an ASM redirector sent every `GL11` call in the
  game into it. Devilica has no redirector: its `GLStateManager` has Angelica's method names and
  goes through vanilla `GlStateManager`, and Iris's own raw state calls were changed to it.
- Gone: display list recording, Distant Horizons, SPIR-V/SDL GPU, RFB/lwjgl3ify loaders, GTNHLib
  tessellators, Angelica's config (now a Forge config with the fields Iris reads), Backhand.
- Macros: `IS_DEVILICA`/`DEVILICA_VERSION` added; `IS_ANGELICA` stays, with `ANGELICA_VERSION`
  fixed at 20000025 (2.0.0-alpha25, the base), since packs gate Angelica features by that version
  and read 1.7.10 from `MC_VERSION`. `IRIS_HAS_TRANSLUCENCY_SORTING` while Celeritas sorts
  translucent faces (Complementary's fancy glass needs it).
- Terrain: Iris's extended vertex is a 48-byte per-vertex `ChunkVertexLayout` on Celeritas's newer
  common; the block being meshed reaches the encoder through `BlockRenderContext.CURRENT`.
- `AdvancedShadowCullingFrustum` is a `ShadowSearchFrustum`, as in newer Angelica, so the shadow pass
  takes Celeritas's receiver-driven search; `SafeZoneCullingFrustum` opts out.
- Later non-core Iris fixes from newer Angelica are to be carried over one by one.
