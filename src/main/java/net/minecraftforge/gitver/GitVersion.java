package net.minecraftforge.gitver;

import net.minecraftforge.util.git.GitUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.jgit.util.SystemReader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The heart of the GitVersion library. Information about how GitVersion operates can be found on the
 * <a href="https://github.com/MinecraftForge/GitVersion">project page</a>.
 */
public class GitVersion implements AutoCloseable {
    public static final String DEFAULT_IGNORE_FILE = ".gitversion.ignore";

    /** The root directory of the Git repository. */
    public final File root;
    /** The {@code .git} directory in the root directory. */
    public final File gitDir;
    /** The project directory in the Git repository (can be the same as the {@link #root}). */
    public final File project;
    /**
     * The relative path from the {@link #root} to the {@link #project} using
     * {@link GitUtils#getRelativePath(File, File)}.
     */
    public final String localPath;
    private final ActionableLazy<Map<File, String>> subprojects = ActionableLazy.of(this::calculateSubprojects);

    // Config
    private String tagPrefix;
    private final Set<String> matchFilters = new HashSet<>();
    private final Set<String> markerName = new HashSet<>();
    private final Set<String> ignoreName = new HashSet<>();
    private final Set<File> ignoredDirs = new HashSet<>();

    // Git
    private final ActionableLazy<GitInfo> info = ActionableLazy.of(() -> this.calculateInfo(this::getSubprojectCommitCount));
    private Git git;
    private SystemReader reader;
    private boolean closed = false;

    /**
     * Creates a new GitVersion instance with the given project directory and configuration.
     *
     * @param project The project directory
     * @param config  The config file to use
     * @throws IllegalArgumentException If the project directory is not within a Git repository
     */
    public GitVersion(File project, GitVersionConfig config) {
        this(project, config.markerFile, config.ignoreFile, config.ignoredDirs.stream().map(dir -> new File(project, dir)).toList());
        this.setTagPrefix(config.tagPrefix);
        this.matchFilters.addAll(config.matchFilters);
    }

    /**
     * Creates a new GitVersion instance with the given project directory and initial values.
     *
     * @param project     The project directory
     * @param markerName  The marker file name(s) to indicate the root of projects
     * @param ignoreName  The ignore file name(s) to indicate that a detected subproject should not be treated as such
     * @param ignoredDirs The directories to ignore from counting as a subproject
     * @throws IllegalArgumentException If the project directory is not within a Git repository
     */
    public GitVersion(File project, Iterable<String> markerName, Iterable<String> ignoreName, Iterable<File> ignoredDirs) {
        this.root = GitUtils.findGitRoot(project);
        this.gitDir = new File(this.root, ".git");
        if (!this.gitDir.exists())
            throw new IllegalArgumentException("Root directory is not a git repository!");

        this.project = project;
        if (this.project.compareTo(this.root) < 0)
            throw new IllegalArgumentException("Project directory must be (a subdirectory of) the root directory!");

        this.localPath = GitUtils.getRelativePath(this.root, this.project);

        markerName.forEach(this.markerName::add);
        ignoreName.forEach(this.ignoreName::add);
        ignoredDirs.forEach(this.ignoredDirs::add);
        this.setTagPrefix(this.localPath.replace("/", "-"));
    }

    /**
     * Gets a {@link GitInfo} object containing information about the current state of the Git repository. This object
     * is lazily recalculated whenever one of the values of this version object change, such as the tag prefix or match
     * filters.
     */
    public GitInfo getInfo() {
        return this.info.get();
    }


    /* VERSIONING */

    /**
     * Calculates a version number in the form
     * <code>{@link GitInfo#getTag() tag}.{@link GitInfo#getOffset() offset}</code>.
     * <p>
     * For example, if your current tag is {@code 1.0} and 5 commits have been made since the tagged commit, then the
     * resulting version number will be {@code 1.0.5}.
     *
     * @return The calculated version
     */
    public String getTagOffset() {
        var info = this.getInfo();
        return "%s.%s".formatted(info.tag, info.offset);
    }

    private static final List<String> DEFAULT_ALLOWED_BRANCHES = List.of("master", "main", "HEAD");

    /** @see #getTagOffsetBranch(Collection) */
    public String getTagOffsetBranch() {
        return this.getTagOffsetBranch(DEFAULT_ALLOWED_BRANCHES);
    }

    /** @see #getTagOffsetBranch(Collection) */
    public String getTagOffsetBranch(String... allowedBranches) {
        return this.getTagOffsetBranch(Arrays.asList(allowedBranches));
    }

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
    public String getTagOffsetBranch(Collection<String> allowedBranches) {
        var version = this.getTagOffset();
        if (Util.isEmptyOrNull(allowedBranches)) return version;

        var branch = this.getInfo().getBranch(true);
        return StringUtils.isEmptyOrNull(branch) || allowedBranches.contains(branch) ? version : "%s-%s".formatted(version, branch);
    }

    /** @see #getMCTagOffsetBranch(String, Collection) */
    public String getMCTagOffsetBranch(String mcVersion) {
        var allowedBranches = new ArrayList<>(DEFAULT_ALLOWED_BRANCHES);
        allowedBranches.add(mcVersion);
        allowedBranches.add(mcVersion + ".0");
        allowedBranches.add(mcVersion + ".x");
        allowedBranches.add(Util.rsplit(mcVersion, ".", 1)[0] + ".x");

        return this.getMCTagOffsetBranch(mcVersion, allowedBranches);
    }

    /** @see #getMCTagOffsetBranch(String, Collection) */
    public String getMCTagOffsetBranch(String mcVersion, String... allowedBranches) {
        return this.getMCTagOffsetBranch(mcVersion, Arrays.asList(allowedBranches));
    }

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
    public String getMCTagOffsetBranch(String mcVersion, Collection<String> allowedBranches) {
        return "%s-%s".formatted(mcVersion, this.getTagOffsetBranch(allowedBranches));
    }

    /**
     * Creates a config from this version object's current state.
     *
     * @return The config
     */
    public GitVersionConfig makeConfig() {
        return new GitVersionConfig(
            this.tagPrefix,
            new ArrayList<>(this.matchFilters),
            new ArrayList<>(this.markerName),
            new ArrayList<>(this.ignoreName),
            this.ignoredDirs.stream().map(dir -> GitUtils.getRelativePath(this.project, dir)).collect(Collectors.toCollection(ArrayList::new))
        );
    }

    /**
     * Sets the tag prefix to use when filtering tags. This will queue the {@linkplain #getInfo() info} to be
     * recalculated the next time it is needed.
     *
     * @param tagPrefix The tag prefix
     */
    public void setTagPrefix(@Nullable String tagPrefix) {
        this.tagPrefix = StringUtils.isEmptyOrNull(tagPrefix) ? "" : tagPrefix;
        this.info.reset();
    }

    /**
     * Sets the match filters to use when filtering tags. This will queue the {@linkplain #getInfo() info} to be
     * recalculated the next time it is needed.
     *
     * @param matchFilters The match filters
     */
    public void setMatchFilters(String... matchFilters) {
        this.matchFilters.clear();
        this.addMatchFilters(matchFilters);
    }

    /**
     * Adds the given match filters to the existing filters to use. This will queue the {@linkplain #getInfo() info} to
     * be recalculated the next time it is needed.
     *
     * @param matchFilters The match filters
     */
    public void addMatchFilters(String... matchFilters) {
        this.matchFilters.addAll(Arrays.asList(matchFilters));
        this.info.reset();
    }

    /**
     * Sets the marker file names to use. This will queue the {@linkplain #getInfo() info} to be recalculated the next
     * time it is needed.
     *
     * @param markerName The marker file names
     */
    public void setMarkerName(String... markerName) {
        this.markerName.clear();
        this.markerName.addAll(Arrays.asList(markerName));

        this.subprojects.reset();
        this.info.reset();
    }

    /**
     * Sets the ignore file names to use. This will queue the {@linkplain #getInfo() info} to be recalculated the next
     * time it is needed.
     *
     * @param ignoreName The ignore file names
     */
    public void setIgnoreName(String... ignoreName) {
        this.ignoreName.clear();
        this.ignoreName.addAll(Arrays.asList(ignoreName));

        this.subprojects.reset();
        this.info.reset();
    }

    /**
     * Sets the directories to ignore. This will queue the {@linkplain #getInfo() info} to be recalculated the next
     * time it is needed.
     *
     * @param ignoreDir The ignored directories
     */
    public void setIgnoreDir(File... ignoreDir) {
        this.ignoredDirs.clear();
        this.ignoredDirs.addAll(Arrays.asList(ignoreDir));

        this.subprojects.reset();
        this.info.reset();
    }


    /* SUBPROJECTS */

    /**
     * Gets the subproject paths that have been detected in the project directory. These paths are relative to the
     * {@link #root} and are calculated using {@link GitUtils#getRelativePath(File, File)}. They will be lazily
     * recalculated whenever the marker file names, ignore file names, or ignored directories change.
     *
     * @return The subproject paths
     */
    public Collection<String> getSubprojectPaths() {
        return this.subprojects.get().values();
    }

    private Map<File, String> calculateSubprojects() {
        var subprojects = new HashMap<File, String>();

        FilenameFilter fileFilter = (dir, name) ->
            !dir.equals(this.project) // directory is not the project root
                && Util.contains(this.markerName, name) // marker file is present (typically build.gradle)
                && FileUtils.listFiles(dir, null, false).stream().map(File::getName).noneMatch(s -> Util.contains(this.ignoreName, s)); // ignore file is not present

        IOFileFilter dirFilter = DirectoryFileFilter.INSTANCE;
        for (var dir : this.ignoredDirs)
            dirFilter = FileFilterUtils.and(FileFilterUtils.asFileFilter(f -> !Objects.equals(dir, f)));

        for (var file : FileUtils.listFiles(this.project, FileFilterUtils.asFileFilter(fileFilter), dirFilter)) {
            var subproject = file.getParentFile();
            subprojects.put(subproject, GitUtils.getRelativePath(this.root, subproject));
        }

        return subprojects;
    }


    /* INFO */

    private GitInfo calculateInfo(CommitCountProvider commitCountProvider) {
        try {
            this.open();

            String[] desc;
            try {
                var describedTag = Util.make(this.git.describe(), it -> {
                    it.setTags(true);
                    it.setLong(true);

                    try {
                        if (!this.tagPrefix.isEmpty())
                            it.setMatch(this.tagPrefix + "**");
                        else
                            it.setExclude("*-*");

                        for (String filter : this.matchFilters)
                            it.setMatch(filter);
                    } catch (Exception e) {
                        Util.sneak(e);
                    }
                }).call();

                desc = Util.rsplit(describedTag, "-", 2);
            } catch (Exception e) {
                System.err.printf("ERROR: Failed to describe git info! Incorrect filters? Tag prefix: %s, glob filters: %s%n", this.tagPrefix, String.join(", ", this.matchFilters));
                e.printStackTrace(System.err);
                return GitInfo.EMPTY;
            }

            Ref head = this.git.getRepository().exactRef(Constants.HEAD);
            var longBranch = Util.make(() -> {
                if (!head.isSymbolic()) return null;

                var target = head.getTarget();
                return target != null ? target.getName() : null;
            }); // matches Repository.getFullBranch() but returning null when on a detached HEAD

            var ret = new GitInfo();
            var tag = Util.make(() -> {
                var t = desc[0].substring(this.tagPrefix.length());
                return t.substring((t.indexOf('v') == 0 || t.indexOf('-') == 0) && t.length() > 1 && Character.isDigit(t.charAt(1)) ? 1 : 0);
            });
            ret.tag = tag;

            ret.offset = commitCountProvider.getAsString(this.git, tag, () -> desc[1]);
            ret.hash = desc[2];
            if (longBranch != null) ret.branch = Repository.shortenRefName(longBranch);
            ret.commit = ObjectId.toString(head.getObjectId());
            ret.abbreviatedId = head.getObjectId().abbreviate(8).name();
            ret.url = GitUtils.buildProjectUrl(this.git);

            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return GitInfo.EMPTY;
        }
    }

    /** The default implementation of GitVersion's commit counter, ignoring subprojects */
    protected final int getSubprojectCommitCount(Git git, String tag) {
        var excludePaths = this.getSubprojectPaths();
        if (this.localPath.isEmpty() && excludePaths.isEmpty()) return -1;

        var includePaths = !this.localPath.isEmpty() ? Collections.singleton(this.localPath) : Collections.<String>emptySet();
        try {
            int count = GitUtils.countCommits(git, tag, includePaths, excludePaths);
            if (count >= 0) return count;

            throw new GitVersionException("Couldn't find any commits with the following parameters: Tag %s, Include Paths [%s], Exclude Paths [%s]".formatted(tag, String.join(", ", includePaths), String.join(", ", excludePaths)));
        } catch (GitVersionException | GitAPIException | IOException e) {
            System.err.printf("WARNING: Failed to count commits for tag %s!%n", tag);
            e.printStackTrace(System.err);
            return -1;
        }
    }

    /* CHANGELOG */

    // TODO [GitVersion] Document
    public String generateChangelog(@Nullable String start, @Nullable String url, boolean plainText) {
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
                throw new GitVersionException("Opened repository has no commits");

            var head = GitUtils.getHead(this.git);
            return GitChangelog.generateChangelogFromTo(this.git, Util.orElse(url, () -> GitUtils.buildProjectUrl(this.git)), plainText, from, head, this.tagPrefix, this.getSubprojectPaths());
        } catch (GitVersionException | GitAPIException | IOException e) {
            throw new GitVersionException("Failed to generate the changelog", e);
        }
    }


    /* REPOSITORY */

    /**
     * Opens the Git repository.
     *
     * @see #open()
     */
    public void open() {
        this.open(false);
    }

    /**
     * Opens the Git repository.
     *
     * @param ignoreSystemConfig Whether to ignore the system configuration when opening and reading the repository
     */
    public void open(boolean ignoreSystemConfig) {
        if (this.git != null) return;
        if (this.closed) throw new IllegalStateException("GitVersion is closed!");

        try {
            if (ignoreSystemConfig) this.disableSystemConfig();

            this.git = Git.open(this.gitDir);
        } catch (IOException e) {
            this.closed = true;
            throw new GitVersionException("Failed to open Git repository", e);
        }
    }

    private void disableSystemConfig() {
        SystemReader.setInstance(new SystemReader.Delegate(this.reader = SystemReader.getInstance()) {
            @Override
            public FileBasedConfig openSystemConfig(Config parent, FS fs) {
                return new FileBasedConfig(parent, null, fs) {
                    @Override
                    public void load() { }

                    @Override
                    public boolean isOutdated() {
                        return false;
                    }
                };
            }
        });
    }

    /** Closes the Git repository. If the system config was disabled, the original reader will be restored. */
    @Override
    public void close() {
        if (this.git == null) return;

        this.git.close();
        if (this.reader != null)
            SystemReader.setInstance(this.reader);

        this.git = null;
        this.reader = null;
        this.closed = true;
    }

    /** @return {@code true} if this version's Git repository is closed. */
    public boolean isClosed() {
        return this.closed;
    }
}
