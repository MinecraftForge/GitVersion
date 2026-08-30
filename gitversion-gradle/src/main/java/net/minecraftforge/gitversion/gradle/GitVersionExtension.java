/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.project.IsolatedProject;

// NOTE: See GitVersion
public sealed interface GitVersionExtension extends GitVersion permits GitVersionExtensionInternal {
    String NAME = "gitversion";


    GitVersion of(ProjectDependency project);

    default GitVersion of(Project project) {
        return this.of(project.getIsolated());
    }

    default GitVersion of(IsolatedProject project) {
        return this.of(project.getProjectDirectory());
    }

    GitVersion of(Directory directory);
}
