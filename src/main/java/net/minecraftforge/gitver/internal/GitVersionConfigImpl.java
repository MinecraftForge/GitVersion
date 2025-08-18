/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver.internal;

import net.minecraftforge.gitver.api.GitVersionConfig;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;
import org.tomlj.TomlVersion;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

@NotNullByDefault
public record GitVersionConfigImpl(
    Map<String, GitVersionConfig.Project> projects,
    @Override List<? extends RuntimeException> errors
) implements GitVersionConfigInternal {
    static final GitVersionConfig EMPTY = new Empty();

    @Override
    public @Nullable GitVersionConfig.Project getProject(@Nullable String path) {
        return this.projects.get(path);
    }

    @Override
    public Collection<GitVersionConfig.Project> getAllProjects() {
        return this.projects.values();
    }

    @Override
    public void validate(File root) {
        for (var path : this.projects.keySet()) {
            if (!path.isEmpty() && !new File(root, path).exists())
                throw new IllegalArgumentException("Subproject path '%s' does not exist. Specify it explicitly in the config if necessary.".formatted(path));
        }
    }

    public static GitVersionConfig parse(@UnknownNullability File config) throws IOException {
        if (config == null || !config.exists()) return EMPTY;

        var toml = Toml.parse(config.toPath(), TomlVersion.V1_0_0);
        var keys = toml.keySet();
        if (keys.contains(""))
            throw new IllegalArgumentException("Config file cannot have a table with an empty string ([\"\"]), use [root] instead.");

        var projects = new HashMap<String, GitVersionConfig.Project>(keys.size() + 1);
        projects.put("", ProjectImpl.parse(toml));
        for (var key : keys) {
            if ("root".equals(key) || !toml.isTable(key)) continue;

            //noinspection DataFlowIssue - checked by TomlTable#isTable
            var project = ProjectImpl.parse(key, toml.getTable(key));
            projects.put(project.getPath(), project);
        }

        return new GitVersionConfigImpl(projects, toml.errors());
    }

    public record ProjectImpl(
        @Override String getPath,
        @Override String[] getIncludePaths,
        @Override String[] getExcludePaths,
        @Override String getTagPrefix,
        @Override String[] getFilters
    ) implements GitVersionConfigInternal.Project {
        private static final ProjectImpl DEFAULT_ROOT = new ProjectImpl("", new String[0], new String[0], "", new String[0]);
        private static final List<GitVersionConfig.Project> DEFAULT_ROOT_LIST = List.of(DEFAULT_ROOT);

        private static String getString(TomlTable table, String key) {
            return Objects.requireNonNullElse(table.getString(key), "");
        }

        private static String getString(TomlTable table, String key, Supplier<String> orElse) {
            return Objects.requireNonNullElseGet(table.getString(key), orElse);
        }

        private static String[] getStringArray(TomlTable table, String key) {
            return table.getArrayOrEmpty(key).toList().stream().map(String.class::cast).filter(Predicate.not(String::isBlank)).toArray(String[]::new);
        }

        private static GitVersionConfig.Project parse(TomlParseResult toml) {
            var root = toml.getTableOrEmpty("root");
            if (root.isEmpty()) return DEFAULT_ROOT;

            var includePaths = getStringArray(root, "include");
            var excludePaths = getStringArray(root, "exclude");
            var tagPrefix = getString(root, "tag");
            var filters = getStringArray(root, "filters");
            return new ProjectImpl("", includePaths, excludePaths, tagPrefix, filters);
        }

        private static GitVersionConfig.Project parse(String key, TomlTable table) {
            var project = Objects.requireNonNullElse(table.getString("path"), key);
            var includePaths = getStringArray(table, "include");
            var excludePaths = getStringArray(table, "exclude");
            var tagPrefix = getString(table, "tag", () -> project.replace("/", "-"));
            var filters = getStringArray(table, "filters");
            return new ProjectImpl(project, includePaths, excludePaths, tagPrefix, filters);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof ProjectImpl p && Objects.equals(this.getPath, p.getPath);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.getPath);
        }
    }

    public static final class Empty implements GitVersionConfigInternal {
        private Empty() { }

        @Override
        public @Nullable Project getProject(@Nullable String path) {
            return path != null && path.isEmpty() ? ProjectImpl.DEFAULT_ROOT : null;
        }

        @Override
        public Collection<GitVersionConfig.Project> getAllProjects() {
            return ProjectImpl.DEFAULT_ROOT_LIST;
        }

        @Override
        public void validate(@UnknownNullability File root) { }

        @Override
        public List<? extends RuntimeException> errors() {
            return List.of();
        }
    }
}
