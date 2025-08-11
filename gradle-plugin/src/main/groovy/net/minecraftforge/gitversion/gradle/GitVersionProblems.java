/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import net.minecraftforge.gradleutils.shared.EnhancedProblems;
import org.gradle.api.problems.Severity;

import javax.inject.Inject;

abstract class GitVersionProblems extends EnhancedProblems {
    @Inject
    public GitVersionProblems() {
        super(GitVersionPlugin.NAME, GitVersionPlugin.DISPLAY_NAME);
    }

    RuntimeException pomUtilsGitVersionNoUrl(Exception e) {
        return this.getReporter().throwing(e, id("pomutils-missing-url", "Cannot add POM remote details without URL"), spec -> spec
            .details("""
                Cannot add POM remote details using `gradleutils.pom.addRemoteDetails` without the URL.
                The containing Git repository may not have a remote.""")
            .severity(Severity.ERROR)
            .stackLocation()
            .solution("Check if the project's containing Git repository has a remote.")
            .solution("Manually add the remote URL in `addRemoteDetails`.")
            .solution(HELP_MESSAGE));
    }
}
