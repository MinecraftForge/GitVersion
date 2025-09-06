/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import net.minecraftforge.gradleutils.shared.EnhancedProblems;

import javax.inject.Inject;

abstract class ChangelogProblems extends EnhancedProblems {
    @Inject
    public ChangelogProblems() {
        super(ChangelogPlugin.NAME, ChangelogPlugin.DISPLAY_NAME);
    }
}
