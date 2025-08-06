/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver.api;

import net.minecraftforge.gitver.internal.GitVersionConfigInternal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.tomlj.Toml;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * The configuration for GitVersion. This is used to determine the version of a project based on the declared
 * subprojects and their respective filters.
 *
 * @see Project
 */
public sealed interface GitVersionConfig permits GitVersionConfigInternal {
    /**
     * Gets the project at the given path.
     *
     * @param path The path
     * @return The project, or {@code null} if not found
     */
    @Nullable Project getProject(@Nullable String path);

    /** @return All projects, including the root project. */
    Collection<Project> getAllProjects();

    /**
     * Validates this configuration by ensuring that all declared subprojects exist from the given root.
     *
     * @param root The root to validate from
     * @throws IllegalArgumentException If a subproject path does not exist
     */
    void validate(@UnknownNullability File root) throws IllegalArgumentException;

    /** @return Any errors made during the config TOML parsing */
    List<? extends RuntimeException> errors();

    /**
     * Attempts to parse the given config file into a {@link GitVersionConfig} using {@linkplain Toml TOMLJ}. If it
     * cannot be parsed or read, an empty config is returned. Other errors will still throw an exception, however.
     *
     * @param config The config file
     * @return The parsed config, or an empty config if the file does not exist
     */
    static GitVersionConfig parse(@UnknownNullability File config) {
        return GitVersionConfigInternal.parse(config);
    }

    sealed interface Project permits GitVersionConfigInternal.Project {
        String getPath();

        String getTagPrefix();

        String[] getFilters();
    }
}
