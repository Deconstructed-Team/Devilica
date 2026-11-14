/*
 * Copyright (c) 2026 Feldsher
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.coderbot.iris.gl.state;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Runs a uniform's update listener with the uniform's own program bound. A value can change while another program is
 * bound (a mod's own shader, or none after a mod unbound its own): the upload would land there while the uniform kept
 * the new value as uploaded, and its program would keep the old one, since an unchanged value is never uploaded again.
 */
public final class ProgramBoundNotifier implements ValueUpdateNotifier {
    private final ValueUpdateNotifier notifier;
    private final int program;
    private Runnable listener;
    private Runnable bound;

    public ProgramBoundNotifier(ValueUpdateNotifier notifier, int program) {
        this.notifier = notifier;
        this.program = program;
    }

    @Override
    public void setListener(Runnable listener) {
        if (listener == null) {
            notifier.setListener(null);
            return;
        }
        if (listener != this.listener) {
            this.listener = listener;
            this.bound = () -> run(listener);
        }
        notifier.setListener(bound);
    }

    private void run(Runnable listener) {
        final int current = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (current == program) {
            listener.run();
            return;
        }
        GL20.glUseProgram(program);
        listener.run();
        GL20.glUseProgram(current);
    }
}
