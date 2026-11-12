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
- Modern `block.properties`, from newer Angelica (#1626, #1635, #1631, #1696, #1815, the tokenizer of #1771, the
  liquid levels of #1831, the properties files of #1868): a pack that names blocks as 1.13+ does is read with a modern
  `MC_VERSION` (`modernFallbackMcVersion`, 260101) and its names go through `FlatteningMap`. What differs: the table is
  written for 1.12.2 and maps to block state properties, not metadata, so a pack's own state properties are matched
  against the 1.12.2 block (those it lacks match anything; nothing is waterlogged), and blocks 1.13+ added are left out.
  Angelica looks for a 1.7.10 section; here `IdMap` reads the file with the game's version and with 1.13's, and a pack
  with entries for 1.12.2 alone, or with no block name the flattening brought, is read exactly as before. The modern
  version is for `block`, `item` and `entity.properties` only: shader sources and `shaders.properties` keep 11202.
  Entity names get their 1.12.2 names, and the pre-1.11 ones `EntityIdHelper` looks up by, for such packs only.
  `LegacyIdMap` gives a pack without `block.properties` the block IDs as they are, OptiFine on 1.12.2 passes 16 bits.
  Not carried over: NBT matching (#1771, #2267; conditions are parsed and their entries skipped, Angelica matches them
  in its chunk mesher, and 1.12.2 has states and entity types for what 1.7.10 keeps in NBT).
- Block IDs by actual state. Part of a 1.12.2 block's state is not in its metadata: snow on grass, the plant of a double
  plant's top half, what a flower pot holds, a door's other half. Where a pack's entries give such states different
  IDs (`BlockMaterialMapping` keeps the ID of every state while it reads the entries), the chunk mesher asks the block
  for its actual state and looks the ID up by it; every other block goes by metadata as before, and so does a held
  block, whose metadata takes the ID of the state the metadata alone stands for. This replaces Angelica's
  `SNOWY_META_BIT` and holds for packs with a 1.12 section too.
- `block.properties` entries are matched in the file's order, the first one wins, as in OptiFine and Iris. The base
  (and newer Angelica) keep the entries in a hash map, so an overlap went to whichever ID hashed first.
- Per-buffer blend overrides: no `glIsEnabled` after each toggle (the cached flag is draw buffer 0's), and the
  game's blend state is put back for every draw buffer past vanilla's cache (the base's `setUnknownState`).
- Uniforms updated once a tick go by the total world time; `relativeEyePosition` is the camera as seen from the eye,
  the eye interpolated; `RenderTargets.resizeIfNeeded` re-attaches depth when the depth texture itself changed (render
  scale hands it two framebuffers); the alpha test is back on after the final pass.
- Carried over from newer Angelica: `a5d31cd1` and `117ff0ca` (uniforms that follow the phase, the bound texture, the
  blend function and fog; the notifiers are raised from Devilica's own state hooks), `1991dc58` (boolean expressions in
  `program.<name>.enabled`), `29f8d718` and `c7a0f3a2` (raw custom textures renamed to the pack's samplers, linear and
  clamped by default), `5ae4ab8c` (the built-in noise texture's filter), `cbd16b1a` (the shadow pass's lightmap
  input), `6f054a09` (PBR atlases released with the pack), the hand passes of `22c94799` (a held block's own layer,
  `finally`), `d1e6dc8c` (`markShadowGraphDirty`; here the camera's list is searched again while its build queue
  overflows).
- Forge's `RenderHandEvent` is posted once a frame, in the pack's solid hand pass under the hand's matrices; vanilla's
  `renderHand` does not post it again.
- Transformed shader sources are kept on disk (`cache/devilica/shaders`, `TransformDiskCache`), keyed by source,
  parameters, build and GL; `-Ddevilica.shaderDiskCache=false` turns it off.
- Program files newer Iris reads, from newer Angelica: `gbuffers_lightning` (#1787: the bolt and the dying dragon's
  beams, which are the entity `dragon_death_rays` to packs that name it, #1836), `shadow_water` (#1628),
  `gbuffers_particles` and `gbuffers_particles_translucent` (from #2017), and `gbuffers_terrain_solid`,
  `gbuffers_terrain_cutout`, `shadow_solid`, `shadow_cutout` for Celeritas's passes. A pack that ships none of them draws
  as before: their draws keep the pass they had (Celeritas's translucent shadow layer its program), where Angelica builds
  one from the fallback's source and sends lightning to `gbuffers_entities`. Translucent particles are vanilla's queue
  that leaves the depth buffer alone (`Particle.shouldDisableDepth`), not Angelica's split by particle; the death beams
  are not drawn a second time for depth. Not carried over: `gbuffers_terrain_cutout_mip`, which Angelica dropped.
- `gbuffers_entities_translucent` and `gbuffers_block_translucent` (the latter from newer Angelica). The base chose the
  entity one by comparing the wrong blend factors and never looked again when blending changed, so it was never used.
  Here both are for draws that blend with plain alpha in the second render pass of entities and block entities, the
  one after water and so after the pack's deferred passes, which is where Iris draws what these programs are written
  for (they fog themselves); in the first pass a blending draw keeps the opaque program. Angelica goes by blending
  alone, in both passes. The pass is matched again before the next draw after the game changed its blending
  (`BlendModeStorage.watchGameBlend`), not at every change, and only for a pack that ships one of the two programs.
- From newer Angelica's `BlendModeStorage`: a program's own blend mode (`blend.<program>=`) holds until the program
  goes; the base let the game's next blend change replace it at the following draw.
- Carried over from newer Angelica, second round:
  - `ffb4ca94` (#1612): `atlasSize`, `gtextureSize` and `blendFunc` no longer allocate a vector on every texture bind
    and blend change; the integer vector uniforms copy the value they are handed. The suppliers' types stay as they
    were, Angelica's move to JOML's read-only interfaces is not carried over.
  - `38b1d9a0` (#2190), the warm-up alone: the start no longer waits for the transform that loads the GLSL parser. Here
    it runs after the version hoisting is set up and past both caches, so the parser is loaded whatever is on disk.
    Not carried over: the index of PBR resources and the program set read ahead of the first world.
  - `38af64f1` (#2213), two parts: the sources of a program set go through the preprocessor on the transform threads
    while the set reads them (`ShaderPack.preprocessInParallel`; a source that fails is preprocessed again by the
    reader, so errors surface where they did), and `ConstDirectiveParser` looks only at lines that have `const` in them.
    Not carried over: the preprocessor's disk cache, files read in parallel, `IdMap` and the language map built in the
    background, programs kept across pipelines (`RetainedPrograms`), the pack parsed ahead while shaders are off.
  - `f78cfdd5` (#1828), the fixes of the shader menus: a pack's text with a percent sign in it is shown as written
    (the game's `String.format` put "Format error: " in front), an option without a comment gets no comment box with
    its key in it, the option widgets click, and a slider is applied only by the release of its own press. Not carried
    over: the new look (value colours, scrolling titles), the debug and unknown-pack toggles, the 1.7.10 list fixes.
  - `057ea803` (#1553), the one part that is not Distant Horizons: `const float shadowNearPlane` and `shadowFarPlane`
    set the planes of the shadow projection (0.05 and 256 without them, as before). A negative plane is the render
    distance, the game's own here.
  - `1772e9ea` (#2300), the sampler overrides alone: a pack's texture put over a built-in sampler is bound with its own
    texture type and without the built-in sampler's sampler object and notifier (`CustomTextureSamplerInterceptor`).
    The rest of that commit is sampler objects for terrain and PBR mipmaps, which this base does not have.
- Attribute transforms are submitted for the sources and input sets the program table will ask for and no more. The
  base transformed every program's source for three input sets, used or not (`gbuffers_line` among them, which nothing
  draws with here), and left the fourth set, a lightmap without a texture, to be transformed on the render thread while
  the passes were built. Small ones from newer Angelica: `NamespacedId` keeps its hash (`d876a203`), the unchanged-value
  returns of `BlendModeStorage`'s deferrals (`a07cecb8`). PBR image resources are closed after reading.
- Carried over from newer Angelica, third round (the parts of large Distant Horizons, SDL and refactoring commits that
  matter without them):
  - `057ea803` (#1553): `cameraPositionInt` and `cameraPositionFract` and their previous values no longer allocate a
    vector per program each frame; the integer vector uniform copies the value it is handed. Angelica's own version
    hands back the current position as the previous one, which is not carried over.
- Later non-core Iris fixes from newer Angelica are to be carried over one by one.
