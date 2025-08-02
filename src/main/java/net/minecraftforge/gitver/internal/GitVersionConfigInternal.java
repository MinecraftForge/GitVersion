package net.minecraftforge.gitver.internal;

import net.minecraftforge.gitver.api.GitVersionConfig;
import org.jetbrains.annotations.UnknownNullability;

import java.io.File;
import java.io.IOException;

public non-sealed interface GitVersionConfigInternal extends GitVersionConfig {
    static GitVersionConfig parse(@UnknownNullability File config) {
        try {
            return GitVersionConfigImpl.parse(config);
        } catch (IOException e) {
            return GitVersionConfigImpl.EMPTY;
        }
    }

    non-sealed interface Project extends GitVersionConfig.Project {
        String getPath();

        String getTagPrefix();

        String[] getFilters();
    }
}
