/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver.internal;

import net.minecraftforge.gitver.api.GitVersionConfig;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;

import java.io.File;
import java.io.IOException;

@NullUnmarked
public non-sealed interface GitVersionConfigInternal extends GitVersionConfig {
    static @NonNull GitVersionConfig parse(File config) {
        try {
            return GitVersionConfigImpl.parse(config);
        } catch (IOException e) {
            return GitVersionConfigImpl.EMPTY;
        }
    }

    non-sealed interface Project extends GitVersionConfig.Project { }
}
