# Changelog

## 0.1.6 - 2026-11-13

### Feature

- English and Ukrainian only, every menu text translated *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [e031183](https://github.com/Deconstructed-Team/Devilica/commit/e0311836fba4f929dd5d4d966cd784d89592ef75)]*
- ShadowNearPlane and shadowFarPlane of a shader pack *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [8af07ca](https://github.com/Deconstructed-Team/Devilica/commit/8af07cac5cc9f72fde2813a12a2df1281a05cd28)]*
- A pack's translucent entity and block entity programs *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [80f05f3](https://github.com/Deconstructed-Team/Devilica/commit/80f05f3c58e1bd53d47398cb6c6b52dff4f7d6f0)]*
- Block IDs by actual state where a pack tells states apart *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [30a01af](https://github.com/Deconstructed-Team/Devilica/commit/30a01af453cfd3a7d23bddda6f021d4212ff15db)]*
- Shader packs with 1.13+ block names in block.properties *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [85cb2f7](https://github.com/Deconstructed-Team/Devilica/commit/85cb2f7c81a5750095959690ca58d5aa1e1b215d)]*
- Gbuffers_particles and gbuffers_particles_translucent *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [a6173a8](https://github.com/Deconstructed-Team/Devilica/commit/a6173a81af57fff16e43f1f91940eab8ea661141)]*
- Gbuffers_lightning for bolts and the dragon's death beams *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [1b3bdcb](https://github.com/Deconstructed-Team/Devilica/commit/1b3bdcb93c563fa491e8b4def7d4b499976ec1d3)]*
- Shadow_water, separate solid and cutout terrain programs *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [b9b4a19](https://github.com/Deconstructed-Team/Devilica/commit/b9b4a19504b2d2b3ca5989ec1d8921bf6c63cc58)]*

### Bug Fix

- PBR maps no longer crash and animate with their block texture *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [d00d00b](https://github.com/Deconstructed-Team/Devilica/commit/d00d00bceefa388ac22d952be64260dc01aab01c)]*
- Compute shaders are supported on any OpenGL 4.3 context *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [9141015](https://github.com/Deconstructed-Team/Devilica/commit/914101543182230bda976745a6e1e4b6f8035062)]*
- A shadow pass that throws restores its GL state *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [bcf3735](https://github.com/Deconstructed-Team/Devilica/commit/bcf373574e9cdc276311a1e607f93cfb88d2277f)]*
- Previous-frame matrices are the last frame's in every program *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [e0309c2](https://github.com/Deconstructed-Team/Devilica/commit/e0309c2179920da8dc8393f113dfa8d2f8dd3433)]*
- Buffers with one clear colour are cleared in one pass *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [cd1bbb1](https://github.com/Deconstructed-Team/Devilica/commit/cd1bbb12cfe3c094b4529d45750acc6cc3009539)]*
- Shader menu labels follow a language change *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [d6a8bc7](https://github.com/Deconstructed-Team/Devilica/commit/d6a8bc7f26183be997ce63f1f2e489d6a9550111)]*
- PBR texture resources are closed after reading *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [f38f245](https://github.com/Deconstructed-Team/Devilica/commit/f38f245a6b82be8af76cb766257d86a81f40e4ec)]*
- A pack's texture over a built-in sampler keeps its own type *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [554a089](https://github.com/Deconstructed-Team/Devilica/commit/554a089e8571196cf6e019ac12c355ff28dbcaa6)]*
- Percent signs in shader pack texts, option clicks are heard *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [9660503](https://github.com/Deconstructed-Team/Devilica/commit/9660503e6759fb390a4182ad1d7573fa3e9a859d)]*
- Block.properties entries match in the file's order *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [36852cb](https://github.com/Deconstructed-Team/Devilica/commit/36852cb065a503492a212fec82509cbfa6bed317)]*
- Entity IDs of a pack resolve by registry name *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [8c7b827](https://github.com/Deconstructed-Team/Devilica/commit/8c7b827902cb8145f7c0c2033b666493207c0a53)]*
- Packs without block.properties get every vanilla block's ID *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [eab9e8c](https://github.com/Deconstructed-Team/Devilica/commit/eab9e8cd2f2464b1f915cad6aa1f0117f2589a6a)]*
- A dropped frame clears its special render condition too *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [2d12bdb](https://github.com/Deconstructed-Team/Devilica/commit/2d12bdb55591a7bc6b91c8e4e8d84073d7dc3e50)]*
- Iris fixes carried over from newer Angelica *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [93eb226](https://github.com/Deconstructed-Team/Devilica/commit/93eb22615317fcccb8bcf37550d4773fd25cfc25)]*
- Hand event posted once, in the pack's hand pass *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [21766af](https://github.com/Deconstructed-Team/Devilica/commit/21766aff712f4e56a96782b6dbb30911af0c01d1)]*
- Batched block entities hidden by other mods, null particles *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [f11e464](https://github.com/Deconstructed-Team/Devilica/commit/f11e464257a448d09916880af523f30beaefc66d)]*
- Malformed resource pack files no longer crash the game *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [864001e](https://github.com/Deconstructed-Team/Devilica/commit/864001e31a9471bc0336121c90c3e0f828f9b407)]*
- Dimension folders, per-tick uniforms, eye position, blend overrides *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [99a9be4](https://github.com/Deconstructed-Team/Devilica/commit/99a9be4fce84c74e629fa584b6e3e835b8398704)]*
- World empty after a render distance change under a shader pack *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [d593877](https://github.com/Deconstructed-Team/Devilica/commit/d59387752cb4d59b5425e7303741139e85174792)]*
- A renderer that throws no longer crashes every world joined after *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [8216576](https://github.com/Deconstructed-Team/Devilica/commit/8216576907d5cedcf40cddc7cb18e3c3f06f7e6f)]*
- Coloured light crash on light above 15, tint lost near sources *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [d191550](https://github.com/Deconstructed-Team/Devilica/commit/d1915501bdc9da7ae7bd6c78e353b16991c8bb0e)]*

### Performance

- Compute shaders are kept on disk with the other transforms *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [b57dfa1](https://github.com/Deconstructed-Team/Devilica/commit/b57dfa108d577125d557f681db672d4189d87cc5)]*
- State-following uniforms keep one listener each *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [7abb00f](https://github.com/Deconstructed-Team/Devilica/commit/7abb00fc5f72ca1020410fa5f5f626286741a986)]*
- Custom uniforms are uploaded only when their value changed *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [7bf5ff9](https://github.com/Deconstructed-Team/Devilica/commit/7bf5ff98ebf102328c911276196258e05144afad)]*
- No vector allocated per frame for cameraPositionInt, Fract *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [179166a](https://github.com/Deconstructed-Team/Devilica/commit/179166a7a3bc38a19db095f00eaab2fd63844f68)]*
- Only the transforms the program table asks for *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [b0f8270](https://github.com/Deconstructed-Team/Devilica/commit/b0f8270035de2990fb1b02a7cc3496bbb56e964f)]*
- NamespacedId keeps its hash, gbuffers_line is not transformed *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [e7b925f](https://github.com/Deconstructed-Team/Devilica/commit/e7b925fe634e6e60892f1a4bb147a78c959a1171)]*
- A pack's program sources are preprocessed in parallel *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [2486930](https://github.com/Deconstructed-Team/Devilica/commit/24869306585c9c144c5f09824922c6a0be492099)]*
- The start no longer waits for the shader transform warm-up *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [650b07f](https://github.com/Deconstructed-Team/Devilica/commit/650b07f35f10fa78df6d795a168f8ecbeaaaf71a)]*
- No vector allocated per texture bind for atlasSize, blendFunc *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [79276ba](https://github.com/Deconstructed-Team/Devilica/commit/79276ba8d771aa0858e653a629c65c00ba42768c)]*
- Transformed shaders kept on disk between runs *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [a0a0768](https://github.com/Deconstructed-Team/Devilica/commit/a0a0768f88bb298d1212ef7cbb3505d85d41d118)]*
- Shadow pass no longer re-runs the camera's search each frame *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [eaf49c1](https://github.com/Deconstructed-Team/Devilica/commit/eaf49c1de11f9895c2958d0b1c530c49a3744245)]*
- Pipeline kept between dimensions with the same programs *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [0a546bc](https://github.com/Deconstructed-Team/Devilica/commit/0a546bcda6dcc86954f4856f3973fc61755d9bd5)]*
- Vanilla's idle chunk builders no longer hold a gigabyte *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [807a192](https://github.com/Deconstructed-Team/Devilica/commit/807a1927588455e13b05bd1ea8ba032b25125a7e)]*

### Documentation

- Upstream notes for the transform set and small fixes *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [f4e567b](https://github.com/Deconstructed-Team/Devilica/commit/f4e567b85e2b020401c9776368f3580696e45522)]*
- Upstream notes for modern block.properties *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [082d5cf](https://github.com/Deconstructed-Team/Devilica/commit/082d5cfc6d1f7562c2b63f0bcb007439d469a9f9)]*
- Upstream notes for the program files carried over *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [2de2a6e](https://github.com/Deconstructed-Team/Devilica/commit/2de2a6e988b25a02523152e51decc632e62f8cb1)]*
- Upstream notes for the audit fixes *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [d6d16cb](https://github.com/Deconstructed-Team/Devilica/commit/d6d16cbda50737304d2bcd6a5786f65711fa965f)]*

**Full Changelog**: https://github.com/Deconstructed-Team/Devilica/compare/0.1.5...0.1.6

## 0.1.5 - 2026-11-12

### Bug Fix

- Shadow pass crash on a player's name tag after joining; no cascade *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [ac11345](https://github.com/Deconstructed-Team/Devilica/commit/ac1134574552a0a5a32ac01c459e833705da725a)]*
- Lumenized's bloom tests against the game's depth texture *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [d5079b1](https://github.com/Deconstructed-Team/Devilica/commit/d5079b1a483956273e0c53a9f40977baf7c8e545)]*

**Full Changelog**: https://github.com/Deconstructed-Team/Devilica/compare/0.1.4...0.1.5

## 0.1.4 - 2026-11-12

### Bug Fix

- Shadow casters no longer drop out of the shadow map for a frame *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [a262648](https://github.com/Deconstructed-Team/Devilica/commit/a262648e2e72f17ad3ffb95469e5482bce6b0657)]*

**Full Changelog**: https://github.com/Deconstructed-Team/Devilica/compare/0.1.3...0.1.4

## 0.1.3 - 2026-11-11

### Bug Fix

- Java toolchains are downloaded where missing *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [a25bed3](https://github.com/Deconstructed-Team/Devilica/commit/a25bed309a6d4eb21dd893701066511428e911d8)]*

**Full Changelog**: https://github.com/Deconstructed-Team/Devilica/compare/0.1.2...0.1.3

## 0.1.2 - 2026-11-11

### Bug Fix

- Gradlew is executable *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [be1d2e9](https://github.com/Deconstructed-Team/Devilica/commit/be1d2e904e264968f4f0a7d4323fbe3669e4ffd1)]*

**Full Changelog**: https://github.com/Deconstructed-Team/Devilica/compare/0.1.1...0.1.2

## 0.1.1 - 2026-11-11

### Feature

- Devilica, rendering for Cleanroom 1.12.2 *[commit by [@romanfedyniak](https://github.com/romanfedyniak) in [6be6eab](https://github.com/Deconstructed-Team/Devilica/commit/6be6eab404a7ab547bb4a35b7cec483e1ec368f9)]*


