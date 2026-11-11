/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.feldsher.devilica.mixin.shaders;

import net.coderbot.iris.Iris;
import net.coderbot.iris.shaderpack.LanguageMap;
import net.coderbot.iris.shaderpack.ShaderPack;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The shader pack's own translations (its option names and screens), for keys the game's languages do not have: the
 * selected language first, then English.
 */
@Mixin(Locale.class)
public abstract class LocaleMixin {

    @Shadow
    Map<String, String> properties;

    @Unique
    private final List<String> devilica$languages = new ArrayList<>();

    @Inject(method = "loadLocaleDataFiles", at = @At("HEAD"))
    private void devilica$rememberLanguages(IResourceManager resourceManager, List<String> languages, CallbackInfo ci) {
        this.devilica$languages.clear();
        // The game lists English first and the selected language after it
        for (int i = languages.size() - 1; i >= 0; i--) {
            this.devilica$languages.add(languages.get(i));
        }
    }

    @Inject(method = "translateKeyPrivate", at = @At("HEAD"), cancellable = true)
    private void devilica$packTranslation(String key, CallbackInfoReturnable<String> cir) {
        final String translation = this.devilica$lookup(key);
        if (translation != null) {
            cir.setReturnValue(translation);
        }
    }

    @Inject(method = "hasKey", at = @At("HEAD"), cancellable = true)
    private void devilica$packHasKey(String key, CallbackInfoReturnable<Boolean> cir) {
        if (this.devilica$lookup(key) != null) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private String devilica$lookup(String key) {
        if (this.properties.containsKey(key)) {
            return null;
        }
        final ShaderPack pack = Iris.getCurrentPack().orElse(null);
        if (pack == null) {
            return null;
        }
        final LanguageMap languageMap = pack.getLanguageMap();
        for (String language : this.devilica$languages) {
            final Map<String, String> translations = languageMap.getTranslations(language);
            if (translations != null) {
                final String translation = translations.get(key);
                if (translation != null) {
                    return translation;
                }
            }
        }
        return null;
    }
}
