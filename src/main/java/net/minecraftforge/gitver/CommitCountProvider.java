/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver;

import net.minecraftforge.util.git.GitUtils;
import org.eclipse.jgit.api.Git;

import java.util.function.Supplier;

/**
 * A provider for the commit count of a given tag. This is done in GitVersion in
 * {@link GitVersion#getSubprojectCommitCount(Git, String)} by using
 * {@link GitUtils#countCommits(Git, String, Iterable, Iterable) GitUtils.countCommits(Git,
 * String, Iterable, Iterable)}.
 */
@FunctionalInterface
public interface CommitCountProvider {
    /**
     * Gets the commit count of the given tag.
     *
     * @param git The Git repository to count commits in
     * @param tag The tag to count commits from
     * @return The commit count
     */
    int get(Git git, String tag);

    /**
     * Gets the commit count of the given tag as a string. If the commit count cannot be retrieved or is {@code -1}, the
     * fallback value is used instead.
     *
     * @param git      The Git repository to count commits in
     * @param tag      The tag to count commits from
     * @param fallback The fallback value to use if the commit count cannot be retrieved
     * @return The commit count as a string, or the fallback value
     * @see #get(Git, String)
     */
    default String getAsString(Git git, String tag, Supplier<String> fallback) {
        try {
            int result = this.get(git, tag);
            if (result > 0) return Integer.toString(result);
        } catch (Exception ignored) { }

        return fallback.get();
    }
}
