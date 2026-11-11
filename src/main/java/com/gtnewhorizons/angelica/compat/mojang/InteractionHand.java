package com.gtnewhorizons.angelica.compat.mojang;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** Devilica: 1.12.2 has both hands; Angelica reaches the off hand through Backhand on 1.7.10. */
public enum InteractionHand {
    MAIN_HAND,
    OFF_HAND;

    public ItemStack getItemInHand(EntityPlayer player) {
        return this == OFF_HAND ? player.getHeldItemOffhand() : player.getHeldItemMainhand();
    }
}
