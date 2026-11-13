# Devilica API

For mods that draw their own things in the world (models, machines, effects) and want them to
work with Devilica's shaders, dynamic lights and culling. Package `dev.feldsher.devilica.api`;
everything is client-side.

```java
if (Loader.isModLoaded("devilica")) {
    DevilicaCompat.init(); // a class of yours that is the only one touching dev.feldsher.devilica.api
}
```

`DevilicaApi.VERSION` is the major version times 1000 plus additions within it (1004 now).
Within a major version nothing is removed or changed, only added.

## Shaders: `DevilicaApi.shaders()`

With a shader pack, the programs bound while the world draws are the pack's. Geometry drawn the
game's way (client-side arrays, `Tessellator`/`BufferBuilder`, display lists) goes through them
as it is; the pack reads position, colour, texture, lightmap and normal from it.

- `isShaderPackActive()`, `isShadowPass()`: during the shadow pass draws go into the shadow map;
  skip what should cast no shadow.
- `blockId(state)`, `entityId(name)`, `itemId(stack)`: the IDs the pack gives things in its
  `block.properties`, `entity.properties`, `item.properties` (`ShaderAccess.NO_ID` when none).
- `pushBlockEntityId` / `restoreBlockEntityId`, and the same for entities and items: what the pack
  sees as the thing being drawn. Devilica sets them for the game's own block entities, entities
  and held items; a mod drawing something else sets them around its draws.
  `currentBlockEntityId()` and `currentEntityId()` (1004) read them.
- `beginPhase(ShaderPhase.ENTITIES | BLOCK_ENTITIES)` / `endPhase(token)`: the pack's programs for
  entities or block entities, for draws made outside the pass the game draws those in. Nests.
- `rebindProgram()` (1004): after drawing with a program of your own while the world draws (an
  effect, a glow), binds the pack's program for what is being drawn again, with its framebuffer
  and its blend and alpha settings.
- `beginOwnPrograms()` / `endOwnPrograms()` (1004): around draws with programs of your own while
  the world draws. Meanwhile Devilica does not bind the pack's program on blend or texture changes,
  does not write the pack's uniforms into your programs, and lifts the pack's blend and alpha
  locks; at the end it binds the pack's program again. Nests.
- `attributeLocation(ShaderAttribute)`: the generic vertex attributes for the extra inputs packs
  read (`mc_Entity`, `mc_midTexCoord`, `at_tangent`, `at_midBlock`). Devilica sets neutral values
  before each block entity and entity; set them per vertex with `glVertexAttribPointer` or per
  draw with `glVertexAttrib*`.
- Textures bound with `GlStateManager.bindTexture` bring their normal and specular maps along
  (PBR resource packs); nothing to do.

```java
final ShaderAccess shaders = DevilicaApi.shaders();
final Object phase = shaders.beginPhase(ShaderPhase.BLOCK_ENTITIES);
final int previous = shaders.pushBlockEntityId(shaders.blockId(state));
try {
    drawModel();
} finally {
    shaders.restoreBlockEntityId(previous);
    shaders.endPhase(phase);
}
```

## Dynamic lights: `DevilicaApi.lights()`

Light that moves with entities. The terrain and the client world's `getCombinedLight` already
include it, so anything lit the game's way is lit by it.

- `isEnabled()`, `lightAt(x, y, z)` (0 to 15 with fractions), `withDynamicLight(pos, packed)`.
- `forEachLight(box, consumer)`: every light reaching into a box this frame, for lighting by hand.
- Items that should glow while held or dropped implement `api.light.DynamicLightItem`; items of
  blocks glow with their block's light without it.

## Coloured block light: `DevilicaApi.coloredLight()` (1003)

Without a shader pack, light sources can give their light a colour. It tints the terrain they
light; the brightness stays the game's, and daylight washes the colour out. Colours are
`0xRRGGBB`, `0xFFFFFF` being the game's own light (torches and glowstone keep it).

- `isEnabled()`: switched on and no shader pack in use.
- `setBlockColor(block, rgb)`, `setBlockColor(state, rgb)`, `getBlockColor(state)`. The player's
  `coloredLight.blockColors` table in `devilica-options.json` wins over these.
- `setProvider(ColoredLightProvider)`: a light engine with its own coloured light hands it over
  per block (`r << 8 | g << 4 | b`, levels 0 to 15, as Angelica's `BlockLightProvider` packs it);
  called on chunk builder threads with the build's copy of the world. Only the hue is used.

## Culling: `DevilicaApi.culling()`

- `isBoxVisible(box)`: whether a world box can be seen this frame (in view, and in a chunk section
  the camera can see past the terrain). Skip drawing what returns false.

## Block entity pass: `DevilicaApi.addBlockEntityPassListener` (1001)

`BlockEntityPassListener.begin()` and `end()` are called around the camera's block entity pass,
on the client thread. `addShadowBlockEntityPassListener` (1004) does the same for the shader
pack's shadow pass, with the pack's shadow program bound: a mod that holds its block entity draws
and draws them at `end()` gets them into the shadow map there. Everything the block entity renderers draw happens in
between, Devilica's batched chests and signs last, sign text after the boards. A font mod can hold
the text drawn in between and draw all of it at `end()`: Devilica draws sign text line by line
through the game's `FontRenderer`, each line under its own GL matrix.
