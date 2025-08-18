/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver.api;

import net.minecraftforge.gitver.internal.GitVersionImpl;
import net.minecraftforge.gitver.internal.GitVersionInternal;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * The heart of the GitVersion library. Information about how GitVersion operates can be found on the
 * <a href="https://github.com/MinecraftForge/GitVersion">path page</a>.
 */
public sealed interface GitVersion extends AutoCloseable permits GitVersionInternal {
    /* BUILDER */

    /**
     * Creates a new builder to build a GitVersion instance. The varying parameters and ephemeral availability warrant
     * this instead of using multiple {@code #of} methods.
     *
     * @return A new builder
     */
    static Builder builder() {
        return GitVersionInternal.builder();
    }

    /**
     * A builder for creating a {@link GitVersion} instance.
     * <p>
     * At a bare minimum, the root or project directory must be set. If the project directory is not set, it will
     * default to the root directory. If the root directory is not set, it will attempt to find the root directory
     * automatically from the project directory. If the git directory is not set, it will default to {@code .git} in the
     * root directory.
     */
    sealed interface Builder permits GitVersionInternal.Builder {
        /**
         * Sets the git directory for the GitVersion instance. Ideally, this should be located in the root directory.
         *
         * @param gitDir The git directory
         * @return This builder
         */
        Builder gitDir(@UnknownNullability File gitDir);

        /**
         * Sets the root directory for the GitVersion instance.
         *
         * @param root The root directory
         * @return This builder
         */
        Builder root(@UnknownNullability File root);

        /**
         * Sets the project directory for the GitVersion instance.
         *
         * @param project The project directory
         * @return This builder
         */
        Builder project(@UnknownNullability File project);

        Builder config(@UnknownNullability File config);

        /**
         * Sets the config to use for the GitVersion instance.
         *
         * @param config The config
         * @return This builder
         */
        Builder config(GitVersionConfig config);

        Builder strict(boolean strict);

        /**
         * Builds the GitVersion instance.
         *
         * @return The GitVersion instance
         * @throws IllegalArgumentException If neither the root nor project directory is set
         * @throws GitVersionException      If an error occurs during construction
         */
        GitVersion build();
    }


    /* VERSIONING */

    /**
     * Calculates a version number in the form
     * <code>{@link GitVersionImpl.Info#getTag() tag}.{@link GitVersionImpl.Info#getOffset() offset}</code>.
     * <p>
     * For example, if your current tag is {@code 1.0} and 5 commits have been made since the tagged commit, then the
     * resulting version number will be {@code 1.0.5}.
     *
     * @return The calculated version
     */
    String getTagOffset();

    /** @see #getTagOffsetBranch(Collection) */
    String getTagOffsetBranch();

    /** @see #getTagOffsetBranch(Collection) */
    String getTagOffsetBranch(@UnknownNullability String... allowedBranches);

    /**
     * Calculates a version number using {@link #getTagOffset()}. If the current branch is not included in the defined
     * list of allowed branches, it will be appended to the version number. It is important to note that any instances
     * of the forward-slash character ({@code /}) will be replaced with a hyphen ({@code -}).
     * <p>
     * For example, if your current tag is {@code 2.3}, 6 commits have been made since the tagged commit, and you are on
     * a branch named {@literal "feature/cool-stuff"}, then the resulting version number will be
     * {@code 2.3.6-feature-cool-stuff}.
     *
     * @param allowedBranches A list of allowed branches; the current branch is appended if not in this list
     * @return The calculated version
     * @see #getTagOffset()
     */
    String getTagOffsetBranch(@UnknownNullability Collection<String> allowedBranches);

    /** @see #getMCTagOffsetBranch(String, Collection) */
    String getMCTagOffsetBranch(@UnknownNullability String mcVersion);

    /** @see #getMCTagOffsetBranch(String, Collection) */
    String getMCTagOffsetBranch(String mcVersion, String... allowedBranches);

    /**
     * Calculates a version number using {@link #getTagOffsetBranch(String...)}, additionally prepending the given
     * Minecraft version.
     * <p>
     * For example, if your current tag is {@code 5.0}, 2 commits have been made since the tagged commit, you are on a
     * branch named {@literal "master"} which is included in the allowed branches, and the given Minecraft version is
     * {@code 1.21.4}, then the resulting version number will be {@code 1.21.4-5.0.2}.
     *
     * @param mcVersion       The minecraft version
     * @param allowedBranches A list of allowed branches
     * @return The calculated version
     */
    String getMCTagOffsetBranch(String mcVersion, Collection<String> allowedBranches);


    /* CHANGELOG */

    /**
     * Attempts to generate a changelog using the parameters given to this GitVersion.
     * <p>
     * If {@linkplain Builder#strict(boolean) strict mode} is disabled, this method will return an empty string instead
     * of throwing an exception.
     *
     * @param start     The tag or commit hash to start the changelog from, or {@code null} to start from the current
     * @param url       The URL to the repository, or {@code null} to attempt to use the
     *                  {@linkplain #getUrl() auto-calculated URL} (if available)
     * @param plainText Whether to generate the changelog in plain text ({@code} false to use Markdown formatting)
     * @return The generated changelog
     * @throws GitVersionException If changelog fails to generate (in {@linkplain Builder#strict(boolean) strict mode})
     */
    String generateChangelog(@Nullable String start, @Nullable String url, boolean plainText) throws GitVersionException;


    /* INFO */

    /**
     * Gets {@link Info info} of the current state of the Git repository. This is used to generate the version string,
     * and is exposed here to allow access to other parts of the version (or general) information.
     * <p>
     * This object is lazily recalculated whenever one of the values of this version object change, such as the tag
     * prefix or match filters. As such, it is recommended not to cache the result of this method.
     *
     * @return Information about the current state of the Git repository
     * @see Info
     */
    Info getInfo() throws GitVersionException;

    /**
     * Gets the URL of the Git repository. This is used to generate the changelog, and is exposed here to allow access
     * to it for other purposes, such as setting links in a Maven POM.
     *
     * @return The URL of the Git repository, or {@code null} if there is none
     */
    @Nullable String getUrl();

    /**
     * Represents information about a git repository. This can be used to access other information when the standard
     * versioning methods in {@link GitVersion} do not suffice.
     */
    @NotNullByDefault
    sealed interface Info extends Serializable permits GitVersionInternal.Info {
        /** @return The current tag as described by the Git repository using the applied filters */
        String getTag();

        /**
         * @return The commit offset from the {@linkplain #getTag() current tag} to {@code HEAD} using the applied
         * filters
         */
        String getOffset();

        /** @return The hash as described by the Git repository */
        String getHash();

        /**
         * @return The current branch
         * @see #getBranch(boolean)
         */
        String getBranch();

        /**
         * Gets the current branch, optionally formatted to be more version string friendly. This is done by replacing
         * any forward-slashes ({@code /}) with hyphens ({@code -}). If the branch is a pull request branch, it is
         * formatted as {@code pr} followed by the pull request number.
         *
         * @param versionFriendly Whether to format the branch to make it version string friendly
         * @return The current branch
         */
        String getBranch(boolean versionFriendly);

        /**
         * @return The long {@code HEAD} commit hash
         * @see #getAbbreviatedId()
         */
        String getCommit();

        /**
         * @return The short {@code HEAD} commit hash
         * @see #getCommit()
         */
        String getAbbreviatedId();
    }


    /* FILTERING */

    /** @return The tag prefix used when filtering tags */
    String getTagPrefix();

    /** @return The filters used when filtering tags (excluding the {@linkplain #getTagPrefix() tag prefix}) */
    @Unmodifiable Collection<String> getFilters();


    /* FILE SYSTEM */

    /** @return The {@code .git} directory containing the Git repository */
    File getGitDir();

    /** @return The root directory of the Git repository, ideally containing the {@linkplain #getGitDir() git directory} */
    File getRoot();

    /** @return The path directory, which may or may not also be the {@linkplain #getRoot() root} */
    File getProject();

    /**
     * Gets the relative {@linkplain #getProject() path} path string. This could be an empty string if the path
     * directory is the same as the {@linkplain #getRoot() root}.
     * <p>
     * This path string is calculated using the
     * {@linkplain java.nio.file.Path#relativize(java.nio.file.Path) relative path} from the root.
     *
     * @return The relative path string
     */
    String getProjectPath();

    String getRelativePath(File file);

    /**
     * Gets the relative path string of a given file.
     * <p>
     * This path string is calculated using the
     * {@linkplain java.nio.file.Path#relativize(java.nio.file.Path) relative path} from either the
     * {@linkplain #getRoot() root} or the {@linkplain #getProject() path}, depending on the {@code fromRoot} boolean
     * parameter.
     *
     * @param fromRoot Whether to get the relative path from the root ({@code true}) or the path ({@code false})
     * @param file     The file to get the relative path to
     * @return The relative path string
     */
    String getRelativePath(boolean fromRoot, File file);


    /* MANUAL PATHS */

    @Unmodifiable Collection<File> getIncludes();

    @Unmodifiable Collection<File> getExcludes();


    /* SUBPROJECTS */

    /**
     * Gets the subprojects declared relative to this project. Subprojects are ignored from the version number and
     * changelog generation.
     *
     * @return The declared subprojects of this path.
     */
    @Unmodifiable Collection<File> getSubprojects();


    /* REPOSITORY */

    /** Closes this GitVersion instance along with its Git repository (if it was opened). */
    @Override
    void close();


    /* SERIALIZATION */

    String toJson();

    interface Output extends Serializable {
        Info info();
        @Nullable String url();

        @Nullable String gitDirPath();
        @Nullable String rootPath();
        @Nullable String projectPath();

        List<String> includePaths();
        List<String> excludePaths();
        @Nullable String tagPrefix();
        List<String> filters();
        List<String> subprojectPaths();
    }
}
