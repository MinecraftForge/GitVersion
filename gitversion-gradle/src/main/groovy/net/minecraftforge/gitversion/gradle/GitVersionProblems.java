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
}
