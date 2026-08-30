/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver.internal;

import net.minecraftforge.gitver.api.GitVersion;
import net.minecraftforge.gitver.api.GitVersionConfig;
import net.minecraftforge.gitver.api.GitVersionException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.util.Collection;
import java.util.List;

public non-sealed interface GitVersionInternal extends GitVersion {
    List<String> DEFAULT_ALLOWED_BRANCHES = List.of("master", "main", "HEAD");
    /* BUILDER */

    static GitVersion.Builder builder() {
        return new Builder();
    }

    @NullUnmarked
    final class Builder implements GitVersion.Builder {
        private @Nullable File gitDir;
        private @Nullable File root;
        private @Nullable File project;
        private @Nullable GitVersionConfig config;
        private @Nullable String commit;
        private boolean strict = true;

        private Builder() { }

        @Override
        public GitVersion.Builder gitDir(File gitDir) {
            this.gitDir = gitDir;
            return this;
        }

        @Override
        public GitVersion.Builder root(File root) {
            this.root = root != null ? root.getAbsoluteFile() : null;
            return this;
        }

        @Override
        public GitVersion.Builder project(File project) {
            this.project = project != null ? project.getAbsoluteFile() : null;
            return this;
        }

        @Override
        public GitVersion.Builder config(File config) {
            this.config = config != null ? GitVersionConfig.parse(config) : null;
            return this;
        }

        @Override
        public GitVersion.Builder config(@NonNull GitVersionConfig config) {
            this.config = config;
            return this;
        }

        @Override
        public GitVersion.Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        @Override
        public GitVersion.Builder commit(@Nullable String ref) {
        	this.commit = ref;
        	return this;
        }

        @Override
        public GitVersion build() {
            if (this.root == null && this.project == null)
                throw new IllegalArgumentException("Either the root or project directory must be set");

            if (this.root == null)
                this.root = findGitRoot(this.project);

            if (this.project == null)
                this.project = this.root;

            if (this.gitDir == null)
                this.gitDir = new File(this.root, ".git");

            if (this.config == null)
                this.config = GitVersionConfig.parse(new File(this.root, ".gitversion.toml"));

            try {
                if (this.project.compareTo(this.root) > 0 && this.config.getProject(GitUtils.getRelativePath(this.root, this.project)) == null) {
                    for (var project = this.project.getParentFile(); project.compareTo(this.root) >= 0; project = project.getParentFile()) {
                        if (this.config.getProject(GitUtils.getRelativePath(this.root, project)) != null) {
                            this.project = project;
                            break;
                        }
                    }
                }

                return new GitVersionImpl(this.gitDir, this.root, this.project, this.config, this.strict, this.commit);
            } catch (GitVersionException e) {
                if (!this.strict)
                    return GitVersionImpl.empty(this.project);

                throw e;
            }
        }
    }


    /* INFO */

    /**
     * Represents information about a git repository. This can be used to access other information when the standard
     * versioning methods in {@link GitVersion} do not suffice.
     */
    non-sealed interface Info extends GitVersion.Info {
        @Override
        default String getBranch(boolean versionFriendly) {
            return getBranch(this.getBranch(), versionFriendly);
        }

        static String getBranch(String branch, boolean versionFriendly) {
            if (!versionFriendly || branch.isBlank()) return branch;

            if (branch.startsWith("pulls/"))
                branch = "pr" + branch.substring(branch.lastIndexOf('/') + 1);
            return branch.replaceAll("[\\\\/]", "-");
        }
    }


    /* FILE SYSTEM */

    @Override
    default String getProjectPath() {
        return GitUtils.getRelativePath(this.getRoot(), this.getProject());
    }

    @Override
    default String getRelativePath(File file) {
        return this.getRelativePath(false, file);
    }

    @Override
    default String getRelativePath(boolean fromRoot, File file) {
        return GitUtils.getRelativePath(fromRoot ? this.getRoot() : this.getProject(), file);
    }

    private static File findGitRoot(File from) {
        for (var dir = from.getAbsoluteFile(); dir != null; dir = dir.getParentFile())
            if (isGitRoot(dir)) return dir;

        return from;
    }

    private static boolean isGitRoot(File dir) {
        return new File(dir, ".git").exists();
    }


    /* MANUAL PATHS */

    @Unmodifiable Collection<String> getIncludesPaths();

    @Unmodifiable Collection<String> getExcludesPaths();


    /* SUBPROJECTS */

    @Unmodifiable Collection<String> getSubprojectPaths();


    /* SERIALIZATION */

    default String toJson() {
        return Util.toJson(new Output(this));
    }

    record Output(
        GitVersion.Info info,
        @Nullable String url,

        @Nullable String gitDirPath,
        @Nullable String rootPath,
        @Nullable String projectPath,

        List<String> includePaths,
        List<String> excludePaths,
        @Nullable String tagPrefix,
        List<String> filters,
        List<String> subprojectPaths
    ) implements GitVersion.Output {
        public Output(
            GitVersion.Info info,
            @Nullable String url,

            @Nullable File gitDir,
            @Nullable File root,
            @Nullable File project,

            @Nullable Collection<String> includePaths,
            @Nullable Collection<String> excludePaths,
            @Nullable String tagPrefix,
            @Nullable Collection<String> filters,
            @Nullable Collection<String> subprojectPaths
        ) {
            this(
                info,
                url,

                gitDir != null ? gitDir.getAbsolutePath() : null,
                root != null ? root.getAbsolutePath() : null,
                project != null ? project.getAbsolutePath() : null,

                includePaths != null ? List.copyOf(includePaths) : List.of(),
                excludePaths != null ? List.copyOf(excludePaths) : List.of(),
                tagPrefix,
                filters != null ? List.copyOf(filters) : List.of(),
                subprojectPaths != null ? List.copyOf(subprojectPaths) : List.of()
            );
        }

        public Output(GitVersionInternal gitVersion) {
            this(
                gitVersion.getInfo(),
                gitVersion.getUrl(),

                Util.tryOrNull(gitVersion::getGitDir),
                Util.tryOrNull(gitVersion::getRoot),
                Util.tryOrNull(gitVersion::getProject),

                Util.tryOrNull(gitVersion::getIncludesPaths),
                Util.tryOrNull(gitVersion::getExcludesPaths),
                Util.tryOrNull(gitVersion::getTagPrefix),
                Util.tryOrNull(gitVersion::getFilters),
                Util.tryOrNull(gitVersion::getSubprojectPaths)
            );
        }
    }
}
