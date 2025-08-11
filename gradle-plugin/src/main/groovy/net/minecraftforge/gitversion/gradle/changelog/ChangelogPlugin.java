/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import org.gradle.api.Project;

import javax.inject.Inject;

abstract class ChangelogPlugin extends EnhancedPlugin<Project> {
    static final String NAME = "changelog";
    static final String DISPLAY_NAME = "Git Changelog";

    @Inject
    public ChangelogPlugin() {
        super(NAME, DISPLAY_NAME);
    }

    @Override
    public void setup(Project project) {
        project.getExtensions().create(ChangelogExtension.NAME, ChangelogExtensionImpl.class, project);
    }
}
