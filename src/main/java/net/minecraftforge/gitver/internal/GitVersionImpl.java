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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private final String tagPrefix;
    private final List<String> filters;
    private final List<File> subprojects;

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

        this.tagPrefix = this.makeTagPrefix(projectConfig.getTagPrefix());
        this.filters = this.makeFilters(projectConfig.getFilters());
        this.subprojects = this.makeSubprojects(config.getAllProjects());
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


    /* SUBPROJECTS */

    @Override
    public @Unmodifiable Collection<File> getSubprojects() {
        return this.subprojects;
    }

    private @Unmodifiable List<File> makeSubprojects(Collection<GitVersionConfig.Project> projects) {
        var ret = new ArrayList<File>(projects.size());
        for (var project : projects) {
            var file = new File(this.root, project.getPath()).getAbsoluteFile();
            var path = GitUtils.getRelativePath(this.project, file);
            if (path.startsWith("../") || path.equals("..")) continue;

            ret.add(file);
        }
        return Collections.unmodifiableList(ret);
    }

    /** The default implementation of {@link CommitCountProvider}, ignoring subprojects */
    private int getSubprojectCommitCount(Git git, String tag) {
        var excludePaths = this.getSubprojectPaths();
        if (this.localPath.isEmpty() && excludePaths.isEmpty()) return -1;

        var includePaths = !this.localPath.isEmpty() ? Collections.singleton(this.localPath) : Set.<String>of();
        try {
            int count = GitUtils.countCommits(git, tag, this.tagPrefix, includePaths, excludePaths);
            return Math.max(count, 0);
        } catch (GitAPIException | IOException e) {
            throw new GitVersionExceptionInternal("Failed to count commits with the following parameters: Tag %s, Include Paths [%s], Exclude Paths [%s]".formatted(tag, String.join(", ", includePaths), String.join(", ", excludePaths)));
        }
    }

    private final Lazy<List<String>> subprojectPathsFromRoot = Lazy.of(() -> this.makeSubprojectPaths(true));
    private final Lazy<List<String>> subprojectPathsFromProject = Lazy.of(() -> this.makeSubprojectPaths(false));

    private List<String> makeSubprojectPaths(boolean fromRoot) {
        return this.getSubprojects()
                   .stream()
                   .map(dir -> GitUtils.getRelativePath(fromRoot ? this.getRoot() : this.getProject(), dir))
                   .filter(Predicate.not(String::isBlank))
                   .toList();
    }

    @Override
    public Collection<String> getSubprojectPaths(boolean fromRoot) {
        return fromRoot ? this.subprojectPathsFromRoot.get() : this.subprojectPathsFromProject.get();
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

        @Override
        public @Unmodifiable Collection<File> getSubprojects() {
            throw new GitVersionExceptionInternal("Cannot get subprojects from an empty repository");
        }

        @Override
        public Collection<String> getSubprojectPaths(boolean fromRoot) {
            throw new GitVersionExceptionInternal("Cannot get subprojects from an empty repository");
        }

        @Override
        public void close() { }
    }
}
