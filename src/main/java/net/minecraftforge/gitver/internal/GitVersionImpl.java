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
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public sealed class GitVersionImpl implements GitVersion permits GitVersionImpl.Empty {
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
    private String tagPrefix;
    private String[] filters;
    private final List<File> subprojects;

    // Unmodifiable views
    private final Lazy<List<String>> filtersView = Lazy.of(() -> this.filters.length == 0 ? Collections.emptyList() : List.of(this.filters));

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

        this.localPath = GitVersion.super.getProjectPath();
        var projectConfig = config.getProject(this.localPath);
        if (projectConfig == null)
            throw new IllegalArgumentException("Subproject '%s' is not configured in the git version config! An entry for it must exist.".formatted(this.localPath));

        this.tagPrefix = projectConfig.getTagPrefix();
        this.filters = projectConfig.getFilters();
        this.subprojects = this.calculateSubprojects(config.getAllProjects());
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
                    if (!this.tagPrefix.isEmpty())
                        it.setMatch(this.tagPrefix + "**");
                    else
                        it.setExclude("*-*");

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
                throw new RefNotFoundException("Tag not found! Tag prefix: %s, Filters: %s".formatted(this.tagPrefix, String.join(", ", this.filters)));

            var desc = Util.rsplit(describedTag, "-", 2);

            Ref head = this.git.getRepository().exactRef(Constants.HEAD);
            var longBranch = Util.make(() -> {
                if (!head.isSymbolic()) return null;

                var target = head.getTarget();
                return target != null ? target.getName() : null;
            }); // matches Repository.getFullBranch() but returning null when on a detached HEAD

            var ret = Info.builder();
            ret.tag = Util.make(() -> {
                var t = desc[0].substring(this.tagPrefix.length());
                return t
                    .substring(t.startsWith("-v") && t.length() > 2 ? 2 : 0)
                    .substring((t.indexOf('v') == 0 || t.indexOf('-') == 0) && t.length() > 1 && Character.isDigit(t.charAt(1)) ? 1 : 0);
            });

            ret.offset = commitCountProvider.getAsString(this.git, desc[0], desc[1], this.strict);
            ret.hash = desc[2];
            if (longBranch != null) ret.branch = Repository.shortenRefName(longBranch);
            ret.commit = ObjectId.toString(head.getObjectId());
            ret.abbreviatedId = head.getObjectId().abbreviate(8).name();
            ret.url = GitUtils.buildProjectUrl(this.git);

            return ret.build();
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
        String getAbbreviatedId,
        @Deprecated(forRemoval = true, since = "0.2") @Nullable String getUrl
    ) implements GitVersion.Info {
        private static final Info EMPTY = new Info("0.0", "0", "00000000", "master", "0000000000000000000000", "00000000", null);

        private static Builder builder() {
            return new Builder();
        }

        private static final class Builder {
            private String tag;
            private String offset;
            private String hash;
            private String branch;
            private String commit;
            private String abbreviatedId;
            @Deprecated(forRemoval = true, since = "0.2") private String url;

            private Info build() {
                return new Info(this.tag, this.offset, this.hash, this.branch, this.commit, this.abbreviatedId, this.url);
            }
        }
    }


    /* FILTERING */

    @Override
    public String getTagPrefix() {
        return this.tagPrefix;
    }

    @Override
    public void setTagPrefix(String tagPrefix) {
        this.tagPrefix = tagPrefix;
        this.info.reset();
    }

    @Override
    public @UnmodifiableView Collection<String> getFilters() {
        return this.filtersView.get();
    }

    @Override
    public void setFilters(String... filters) {
        this.filters = filters;
        this.filtersView.reset();
        this.info.reset();
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
    public @UnmodifiableView Collection<File> getSubprojects() {
        return this.subprojects;
    }

    private List<File> calculateSubprojects(Collection<GitVersionConfig.Project> projects) {
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

        var includePaths = !this.localPath.isEmpty() ? Collections.singleton(this.localPath) : Collections.<String>emptySet();
        try {
            int count = GitUtils.countCommits(git, tag, this.tagPrefix, includePaths, excludePaths);
            return Math.max(count, 0);
        } catch (GitAPIException | IOException e) {
            throw new GitVersionExceptionInternal("Failed to count commits with the following parameters: Tag %s, Include Paths [%s], Exclude Paths [%s]".formatted(tag, String.join(", ", includePaths), String.join(", ", excludePaths)));
        }
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

    public static GitVersion empty() {
        return new Empty();
    }

    public static GitVersion emptyFor(File project) {
        return project == null ? empty() : new Empty() {
            @Override
            public File getProject() {
                return project;
            }
        };
    }

    private GitVersionImpl() {
        this.strict = false;
        this.gitDir = null;
        this.root = null;
        this.project = null;
        this.localPath = "";
        this.tagPrefix = "";
        this.filters = new String[0];
        this.subprojects = Collections.emptyList();
    }

    static non-sealed class Empty extends GitVersionImpl {
        private Empty() { }

        @Override
        public String generateChangelog(@Nullable String start, @Nullable String url, boolean plainText) throws GitVersionException {
            throw new GitVersionExceptionInternal("Cannot generate a changelog without a repository");
        }

        @Override
        public GitVersion.Info getInfo() {
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
        public @UnmodifiableView Collection<String> getFilters() {
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
            throw new GitVersionExceptionInternal("Cannot get project directory without a project");
        }

        @Override
        public String getProjectPath() {
            throw new GitVersionExceptionInternal("Cannot get project path from root with an empty repository");
        }

        @Override
        public @UnmodifiableView Collection<File> getSubprojects() {
            throw new GitVersionExceptionInternal("Cannot get subprojects from an empty repository");
        }
    }
}
