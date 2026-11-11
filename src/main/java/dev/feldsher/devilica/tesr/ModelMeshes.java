/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.tesr;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelLargeChest;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Vanilla's block entity models posed as their renderers pose them, as raw vertices of {@link BlockEntityBatcher#FORMAT}
 * with the block entity's render origin at zero and no light yet: closed chests and signs.
 */
final class ModelMeshes {
    private static final float SCALE = 0.0625F;

    private static final ModelChest SINGLE = new ModelChest();
    private static final ModelChest LARGE = new ModelLargeChest();
    private static final ModelSign SIGN = new ModelSign();
    private static final Int2ObjectMap<int[]> MESHES = new Int2ObjectOpenHashMap<>();

    private ModelMeshes() {
    }

    /**
     * @param meta  the chest's metadata, its facing
     * @param shift the double chest's extra move: 0 none, 1 east (facing north with a chest east), 2 north (facing
     *              east with a chest south)
     */
    static int[] chest(boolean large, int meta, int shift) {
        final int key = (large ? 1 : 0) | meta << 1 | shift << 4;
        int[] mesh = MESHES.get(key);
        if (mesh == null) {
            mesh = buildChest(large ? LARGE : SINGLE, meta, shift);
            MESHES.put(key, mesh);
        }
        return mesh;
    }

    /** A sign's board, and its stick when it stands, as {@code TileEntitySignRenderer} poses them */
    static int[] sign(boolean standing, int meta) {
        final int key = 1 << 8 | (standing ? 1 << 7 : 0) | meta;
        int[] mesh = MESHES.get(key);
        if (mesh == null) {
            final Matrix4f matrix = signPose(standing, meta).scale(0.6666667F, -0.6666667F, -0.6666667F);
            mesh = build(matrix, standing ? new ModelRenderer[]{SIGN.signBoard, SIGN.signStick} : new ModelRenderer[]{SIGN.signBoard});
            MESHES.put(key, mesh);
        }
        return mesh;
    }

    /** Where {@code TileEntitySignRenderer} stands a sign before it draws the model and the text */
    static Matrix4f signPose(boolean standing, int meta) {
        final Matrix4f matrix = new Matrix4f().translate(0.5F, 0.5F, 0.5F);
        if (standing) {
            return matrix.rotateY((float) Math.toRadians(-(meta * 360) / 16.0F));
        }
        final float angle = switch (meta) {
            case 2 -> 180.0F;
            case 4 -> 90.0F;
            case 5 -> -90.0F;
            default -> 0.0F;
        };
        return matrix.rotateY((float) Math.toRadians(-angle)).translate(0.0F, -0.3125F, -0.4375F);
    }

    private static int[] buildChest(ModelChest model, int meta, int shift) {
        final int angle = switch (meta) {
            case 2 -> 180;
            case 4 -> 90;
            case 5 -> -90;
            default -> 0;
        };
        final Matrix4f chest = new Matrix4f().translate(0.0F, 1.0F, 1.0F).scale(1.0F, -1.0F, -1.0F).translate(0.5F, 0.5F, 0.5F);
        if (shift == 1) {
            chest.translate(1.0F, 0.0F, 0.0F);
        } else if (shift == 2) {
            chest.translate(0.0F, 0.0F, -1.0F);
        }
        chest.rotateY((float) Math.toRadians(angle)).translate(-0.5F, -0.5F, -0.5F);
        return build(chest, model.chestLid, model.chestKnob, model.chestBelow);
    }

    // ModelRenderer.render for parts at rest (no rotation), each with its offset and rotation point
    private static int[] build(Matrix4f pose, ModelRenderer... parts) {
        final IntArrayList out = new IntArrayList();
        for (ModelRenderer part : parts) {
            final Matrix4f matrix = new Matrix4f(pose)
                .translate(part.offsetX, part.offsetY, part.offsetZ)
                .translate(part.rotationPointX * SCALE, part.rotationPointY * SCALE, part.rotationPointZ * SCALE);
            for (ModelBox box : part.cubeList) {
                for (TexturedQuad quad : box.quadList) {
                    addQuad(out, quad, matrix);
                }
            }
        }
        return out.toIntArray();
    }

    // TexturedQuad.draw's normal, turned with the pose
    private static void addQuad(IntArrayList out, TexturedQuad quad, Matrix4f matrix) {
        final PositionTextureVertex[] vertices = quad.vertexPositions;
        final Vec3d a = vertices[1].vector3D.subtractReverse(vertices[0].vector3D);
        final Vec3d b = vertices[1].vector3D.subtractReverse(vertices[2].vector3D);
        Vec3d normal = b.crossProduct(a).normalize();
        if (quad.invertNormal) {
            normal = normal.scale(-1.0D);
        }
        final Vector3f turned = matrix.transformDirection(new Vector3f((float) normal.x, (float) normal.y, (float) normal.z)).normalize();
        final int packedNormal = (byte) (turned.x * 127.0F) & 0xFF | ((byte) (turned.y * 127.0F) & 0xFF) << 8 | ((byte) (turned.z * 127.0F) & 0xFF) << 16;

        final Vector3f position = new Vector3f();
        for (PositionTextureVertex vertex : vertices) {
            matrix.transformPosition((float) vertex.vector3D.x * SCALE, (float) vertex.vector3D.y * SCALE, (float) vertex.vector3D.z * SCALE, position);
            out.add(Float.floatToRawIntBits(position.x));
            out.add(Float.floatToRawIntBits(position.y));
            out.add(Float.floatToRawIntBits(position.z));
            out.add(0xFFFFFFFF);
            out.add(Float.floatToRawIntBits(vertex.texturePositionX));
            out.add(Float.floatToRawIntBits(vertex.texturePositionY));
            out.add(0);
            out.add(packedNormal);
        }
    }
}
