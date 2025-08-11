/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import javax.inject.Inject;

abstract class GitVersionPlugin extends EnhancedPlugin<Project> {
    static final String NAME = "gitversion";
    static final String DISPLAY_NAME = "Git Version";

    static final Logger LOGGER = Logging.getLogger(GitVersionPlugin.class);

    @Inject
    public GitVersionPlugin() {
        super(NAME, DISPLAY_NAME);
    }

    @Override
    public void setup(Project project) {
        project.getExtensions().create(GitVersionExtension.NAME, GitVersionExtensionImpl.class, this, project);
    }
}
