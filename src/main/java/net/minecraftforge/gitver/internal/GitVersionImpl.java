/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver.internal;

import net.minecraftforge.gitver.api.GitVersion;
import net.minecraftforge.gitver.api.GitVersionConfig;
import net.minecraftforge.gitver.api.GitVersionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class GitVersionImpl implements GitVersionInternal {
    // Git
    private final boolean strict;
    private Git git;
    private final Lazy<Info> info = Lazy.of(() -> this.calculateInfo(this::getSubprojectCommitCount));
    private final Lazy<String> url = Lazy.of(this::calculateUrl);
    private boolean closed = false;

    // Filesystem
    public final File gitDir;
    public final File root;
    public final File project;
    public final String localPath;

    // Config
    // NOTE: These are not calculated lazily because they are used in both info gen and changelog gen
    private final List<File> includes;
    private final List<File> excludes;
    private final String tagPrefix;
    private final List<String> filters;
    private final List<File> subprojects;

    // Internal Config
    private final List<String> includesPaths;
    private final List<String> excludesPaths;
    private final List<String> subprojectPaths;

    private final Set<String> allIncludingPaths;
    private final Set<String> allExcludingPaths;

    public GitVersionImpl(File gitDir, File root, File project, GitVersionConfig config, boolean strict) {
        this.strict = strict;

        this.gitDir = gitDir;
        this.root = root;
        if (!this.gitDir.exists())
            throw new GitVersionExceptionInternal("Root directory is not a git repository");

        this.project = project;
        if (this.project.compareTo(this.root) < 0)
            throw new IllegalArgumentException("Project directory must be (a subdirectory of) the root directory");

        try {
            config.validate(this.root);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid configuration", e);
        }

        this.localPath = GitVersionInternal.super.getProjectPath();
        var projectConfig = Objects.requireNonNull(config.getProject(this.localPath));

        this.includesPaths = makePaths(this.includes = parsePaths(Arrays.asList(projectConfig.getIncludePaths()), false));
        this.excludesPaths = makePaths(this.excludes = parsePaths(Arrays.asList(projectConfig.getExcludePaths()), true));
        this.tagPrefix = this.makeTagPrefix(projectConfig.getTagPrefix());
        this.filters = this.makeFilters(projectConfig.getFilters());
        this.subprojectPaths = makePaths(this.subprojects = parsePaths(config.getAllProjects(), GitVersionConfig.Project::getPath, true));

        var allIncludingPaths = new HashSet<>(includesPaths);
        if (!this.localPath.isEmpty())
            allIncludingPaths.add(this.localPath);
        this.allIncludingPaths = allIncludingPaths;

        var allExcludingPaths = new HashSet<>(excludesPaths);
        allExcludingPaths.addAll(this.subprojectPaths);
        this.allExcludingPaths = allExcludingPaths;
    }


    /* CHANGELOG */

    @Override
    public String generateChangelog(@Nullable String start, @Nullable String url, boolean plainText) throws GitVersionException {
        try {
            this.open();

            RevCommit from;
            if (StringUtils.isEmptyOrNull(start)) {
                var mergeBase = GitUtils.getMergeBaseCommit(this.git);
                from = mergeBase != null ? mergeBase : GitUtils.getFirstCommitInRepository(this.git);
            } else {
                var tags = GitUtils.getTagToCommitMap(this.git);
                var commitHash = Util.orElse(tags.get(start), () -> start);
                from = GitUtils.getCommitFromId(this.git, ObjectId.fromString(commitHash));
            }

            if (from == null)
                throw new GitVersionExceptionInternal("Opened repository has no commits");

            var head = GitUtils.getHead(this.git);
            return GitChangelog.generateChangelogFromTo(this.git, Util.orElse(url, () -> GitUtils.buildProjectUrl(this.git)), plainText, from, head, this.tagPrefix, this.getSubprojectPaths());
        } catch (GitVersionException | GitAPIException | IOException e) {
            if (this.strict) throw new GitVersionExceptionInternal("Failed to generate the changelog", e);

            return "";
        }
    }


    /* INFO */

    @Override
    public GitVersion.Info getInfo() {
        return this.info.get();
    }

    @Override
    public @Nullable String getUrl() {
        return this.url.get();
    }

    /** @see #info */
    private Info calculateInfo(CommitCountProvider commitCountProvider) {
        try {
            this.open();

            var describedTag = Util.make(this.git.describe(), it -> {
                it.setTags(true);
                it.setLong(true);

                try {
                    it.setMatch(this.tagPrefix + "[[:digit:]]**");

                    for (String filter : this.filters) {
                        if (filter.startsWith("!"))
                            it.setExclude(filter.substring(1));
                        else
                            it.setMatch(filter);
                    }
                } catch (Exception e) {
                    Util.sneak(e);
                }
            }).call();

            if (describedTag == null)
                throw new RefNotFoundException("Tag not found! A valid tag must include a digit! Tag prefix: %s, Filters: %s".formatted(!this.tagPrefix.isEmpty() ? this.tagPrefix : "NONE!", String.join(", ", this.filters)));

            var desc = Util.rsplit(describedTag, "-", 2);

            var head = this.git.getRepository().exactRef(Constants.HEAD);
            var longBranch = Util.make(() -> {
                if (!head.isSymbolic()) return null;

                var target = head.getTarget();
                return target != null ? target.getName() : null;
            }); // matches Repository.getFullBranch() but returning null when on a detached HEAD

            var tag = desc[0].substring(Util.indexOf(desc[0], Character::isDigit, 0));

            var offset = commitCountProvider.getAsString(this.git, desc[0], desc[1], this.strict);
            var hash = desc[2];
            var branch = longBranch != null ? Repository.shortenRefName(longBranch) : null;
            var commit = ObjectId.toString(head.getObjectId());
            var abbreviatedId = head.getObjectId().abbreviate(8).name();

            return new Info(tag, offset, hash, branch, commit, abbreviatedId);
        } catch (Exception e) {
            if (this.strict) throw new GitVersionExceptionInternal("Failed to calculate version info", e);

            return Info.EMPTY;
        }
    }

    /** @see #url */
    private @Nullable String calculateUrl() {
        try {
            this.open();

            return GitUtils.buildProjectUrl(this.git);
        } catch (Exception e) {
            return null;
        }
    }

    /** @see GitVersion.Info */
    public record Info(
        String getTag,
        String getOffset,
        String getHash,
        String getBranch,
        String getCommit,
        String getAbbreviatedId
    ) implements GitVersionInternal.Info {
        private static final Info EMPTY = new Info("0.0", "0", "00000000", "master", "0000000000000000000000", "00000000");
    }


    /* FILTERING */

    @Override
    public String getTagPrefix() {
        return this.tagPrefix;
    }

    private String makeTagPrefix(String tagPrefix) {
        // String#isBlank in case a weird freak accident where the string has empty (space) characters
        if (StringUtils.isEmptyOrNull(tagPrefix) || tagPrefix.isBlank())
            return "";

        return !tagPrefix.endsWith("-") ? tagPrefix + "-" : tagPrefix;
    }

    @Override
    public @Unmodifiable Collection<String> getFilters() {
        return this.filters;
    }

    private @Unmodifiable List<String> makeFilters(String... filters) {
        var list = new ArrayList<String>(filters.length);
        for (var s : filters) {
            if (s.length() > (s.startsWith("!") ? 1 : 0))
                list.add(s);
        }
        return Collections.unmodifiableList(list);
    }


    /* FILE SYSTEM */

    @Override
    public File getGitDir() {
        return this.gitDir;
    }

    @Override
    public File getRoot() {
        return this.root;
    }

    @Override
    public File getProject() {
        return this.project;
    }

    @Override
    public String getProjectPath() {
        return this.localPath;
    }

    private @Unmodifiable List<File> parsePaths(Collection<String> paths, boolean removeParents) {
        return parsePaths(paths, Function.identity(), removeParents);
    }

    private @Unmodifiable <T> List<File> parsePaths(Collection<T> paths, Function<T, String> mapper, boolean removeParents) {
        var ret = new ArrayList<File>(paths.size());
        for (var o : paths) {
            var p = mapper.apply(o);

            var file = new File(this.root, p).getAbsoluteFile();

            if (removeParents) {
                var path = GitUtils.getRelativePath(this.project, file);
                if (path.isEmpty() || path.startsWith("../") || path.equals("..") || path.equals(".")) continue;
            }

            ret.add(file);
        }
        return Collections.unmodifiableList(ret);
    }

    private @Unmodifiable List<String> makePaths(Collection<? extends File> files) {
        var ret = new ArrayList<String>(files.size());
        for (var file : files) {
            var path = GitUtils.getRelativePath(this.getRoot(), file);
            if (path.isBlank()) continue;

            ret.add(path);
        }
        return Collections.unmodifiableList(ret);
    }


    /* MANUAL PATHS */

    @Override
    public @Unmodifiable Collection<File> getIncludes() {
        return this.includes;
    }

    @Override
    public @Unmodifiable Collection<File> getExcludes() {
        return this.excludes;
    }

    @Override
    public @Unmodifiable Collection<String> getIncludesPaths() {
        return this.includesPaths;
    }

    @Override
    public @Unmodifiable Collection<String> getExcludesPaths() {
        return this.excludesPaths;
    }


    /* SUBPROJECTS */

    @Override
    public @Unmodifiable Collection<File> getSubprojects() {
        return this.subprojects;
    }

    /** The default implementation of {@link CommitCountProvider}, ignoring subprojects */
    private int getSubprojectCommitCount(Git git, String tag) {
        if (this.localPath.isEmpty() && this.allExcludingPaths.isEmpty()) return -1;

        try {
            int count = GitUtils.countCommits(git, tag, this.tagPrefix, this.allIncludingPaths, this.allExcludingPaths);
            return Math.max(count, 0);
        } catch (GitAPIException | IOException e) {
            throw new GitVersionExceptionInternal("Failed to count commits with the following parameters: Tag %s, Include Paths [%s], Exclude Paths [%s]".formatted(tag, String.join(", ", this.allIncludingPaths), String.join(", ", this.allExcludingPaths)));
        }
    }

    @Override
    public @Unmodifiable Collection<String> getSubprojectPaths() {
        return this.subprojectPaths;
    }


    /* REPOSITORY */

    /** Opens the Git repository. */
    private void open() {
        if (this.git != null) return;
        if (this.closed) throw new GitVersionExceptionInternal("GitVersion is closed!");

        try {
            this.git = Git.open(this.gitDir);
        } catch (IOException e) {
            this.close();
            throw new GitVersionExceptionInternal("Failed to open Git repository", e);
        }
    }

    @Override
    public void close() {
        this.closed = true;
        if (this.git == null) return;

        this.git.close();
        this.git = null;
    }


    /* EMPTY */

    public static GitVersion empty(@Nullable File project) {
        return new Empty(project);
    }

    public record Empty(@Nullable File project) implements GitVersionInternal {
        @Override
        public String generateChangelog(@Nullable String start, @Nullable String url, boolean plainText) throws GitVersionException {
            throw new GitVersionExceptionInternal("Cannot generate a changelog without a repository");
        }

        @Override
        public Info getInfo() throws GitVersionException {
            return GitVersionImpl.Info.EMPTY;
        }

        @Override
        public @Nullable String getUrl() {
            return null;
        }

        @Override
        public String getTagPrefix() {
            throw new GitVersionExceptionInternal("Cannot get tag prefix from an empty repository");
        }

        @Override
        public @Unmodifiable Collection<String> getFilters() {
            throw new GitVersionExceptionInternal("Cannot get filters from an empty repository");
        }

        @Override
        public File getGitDir() {
            throw new GitVersionExceptionInternal("Cannot get git directory from an empty repository");
        }

        @Override
        public File getRoot() {
            throw new GitVersionExceptionInternal("Cannot get root directory from an empty repository");
        }

        @Override
        public File getProject() {
            if (this.project != null)
                return this.project;

            throw new GitVersionExceptionInternal("Cannot get project directory without a project");
        }

        @Override public @Unmodifiable Collection<File> getIncludes() {
            throw new GitVersionExceptionInternal("Cannot get include paths from an empty repository");
        }

        @Override public @Unmodifiable Collection<File> getExcludes() {
            throw new GitVersionExceptionInternal("Cannot get exclude paths from an empty repository");
        }

        @Override
        public @Unmodifiable Collection<String> getIncludesPaths() {
            throw new GitVersionExceptionInternal("Cannot get include paths from an empty repository");
        }

        @Override
        public @Unmodifiable Collection<String> getExcludesPaths() {
            throw new GitVersionExceptionInternal("Cannot get exclude paths from an empty repository");
        }

        @Override
        public @Unmodifiable Collection<File> getSubprojects() {
            throw new GitVersionExceptionInternal("Cannot get subprojects from an empty repository");
        }

        @Override
        public Collection<String> getSubprojectPaths() {
            throw new GitVersionExceptionInternal("Cannot get subprojects from an empty repository");
        }

        @Override
        public void close() { }
    }
}
