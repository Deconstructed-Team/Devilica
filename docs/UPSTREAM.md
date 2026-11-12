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
- `RenderGlobalMixin` gives vanilla's idle `ChunkRenderDispatcher` one builder (it set aside 10 MB of buffers for
  each, up to 30% of the heap). The chunk renderer keeps the shadow pass's cached batches under a view of its own
  (`getRenderView`), as Celeritas's modern renderer does.

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
  takes Celeritas's receiver-driven search; `SafeZoneCullingFrustum` opts out. As in newer Angelica, the
  shadow pass joins the last frame's search before it sets the terrain frustum anew, and terrain and entities
  keep frustums of their own (the entity one was set to the entity shadow distance while the search still
  read it, and casters dropped out of the shadow map for a frame).
- The shadow pass sets up the entity and block entity view before it draws them (from newer Angelica; on the
  first frame in a world another player's name tag crashed), clears its flags in a `finally`, and a frame left
  unfinished by a crash is ended at the next `beginLevelRendering` instead of throwing on every frame after.
- A frame that throws midway is dropped (`ShaderHooks.abandonFrame`, at the next world frame and on leaving a world):
  the phase flags of `GbufferPrograms`, the pipeline's pass and its world/shadow flags, the hand and render scale flags.
  Block entity phases end in a `finally`.
- The shadow pass hands Celeritas the terrain pass's frame number (`EntityRenderer.frameCount`), not a counter of its
  own: the section manager orders builds by it.
- `PipelineManager` keeps the pipeline over a dimension change when the pack draws both dimensions with the same
  program set. `ShaderPack` resolves the vanilla dimensions by their ids too (`minecraft:the_nether`, from newer
  Angelica's dimension flattening) and draws dimensions the pack does not name with `world0` (newer Angelica, #2137).
- Per-buffer blend overrides: no `glIsEnabled` after each toggle (the cached flag is draw buffer 0's), and the
  game's blend state is put back for every draw buffer past vanilla's cache (the base's `setUnknownState`).
- Uniforms updated once a tick go by the total world time; `relativeEyePosition` is the camera as seen from the eye,
  the eye interpolated; `RenderTargets.resizeIfNeeded` re-attaches depth when the depth texture itself changed (render
  scale hands it two framebuffers); the alpha test is back on after the final pass.
- Program files newer Iris reads, from newer Angelica: `gbuffers_lightning` (#1787: the bolt and the dying dragon's
  beams, which are the entity `dragon_death_rays` to packs that name it, #1836), `shadow_water` (#1628),
  `gbuffers_particles` and `gbuffers_particles_translucent` (from #2017), and `gbuffers_terrain_solid`,
  `gbuffers_terrain_cutout`, `shadow_solid`, `shadow_cutout` for Celeritas's passes. A pack that ships none of them draws
  as before: their draws keep the pass they had (Celeritas's translucent shadow layer its program), where Angelica builds
  one from the fallback's source and sends lightning to `gbuffers_entities`. Translucent particles are vanilla's queue
  that leaves the depth buffer alone (`Particle.shouldDisableDepth`), not Angelica's split by particle; the death beams
  are not drawn a second time for depth. Not carried over: `gbuffers_block_translucent`, which needs the pass matched
  again on blend changes, and `gbuffers_terrain_cutout_mip`, which Angelica dropped.
- Later non-core Iris fixes from newer Angelica are to be carried over one by one.
