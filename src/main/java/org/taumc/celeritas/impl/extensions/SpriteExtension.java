package org.taumc.celeritas.impl.extensions;

public interface SpriteExtension {
    void celeritas$markActive();
    boolean celeritas$shouldUpdate();

    /** What {@link #celeritas$shouldUpdate} last answered: whether this tick animated the sprite */
    boolean celeritas$wasUpdated();
}
