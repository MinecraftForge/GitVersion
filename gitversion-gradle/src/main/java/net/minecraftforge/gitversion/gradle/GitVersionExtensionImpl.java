/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import net.minecraftforge.gradleutils.GenerateActionsWorkflow;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

abstract class GitVersionExtensionImpl implements GitVersionExtensionInternal {
    private final GitVersionImpl instance;

    private boolean hasProject;

    private final GitVersionPlugin plugin;
    private final GitVersionProblems problems;

    protected abstract @Inject Project getGradleProject();

    protected abstract @Inject ObjectFactory getObjects();

    @Inject
    public GitVersionExtensionImpl(GitVersionPlugin plugin, ExtensionAware target, Directory projectDirectory) {
        this.plugin = plugin;
        this.problems = getObjects().newInstance(GitVersionProblems.class);
        this.instance = (GitVersionImpl) this.of(projectDirectory);

        if (target instanceof Project project) {
            project.getPluginManager().withPlugin("net.minecraftforge.gradleutils", appliedPlugin -> {
                try {
                    // GradleUtilsIntegration includes symbols from GradleUtils that may or may not be properly loaded in the classpath.
                    // Gradle plugin loading has a quirk where plugins cannot access the classpath of plugins loaded after itself.
                    // Keeping this code in another class prevents Gradle's class verifier from causing a crash when loading this plugin.
                    GradleUtilsIntegration.apply(appliedPlugin, project, this, this.instance);
                } catch (Throwable e) {
                    problems.reportGradleUtilsActionsWorkflowConfigureFailure(project, e);
                }
            });
        }
    }

    @Override
    public GitVersion of(ProjectDependency project) {
        return this.of(getGradleProject().project(project.getPath()));
    }

    @Override
    public GitVersion of(Directory directory) {
        return getObjects().newInstance(GitVersionImpl.class, this.plugin, directory);
    }

    /* IMPL DELEGATION */

    @Override
    public GitVersionImpl getInstance() {
        return this.instance;
    }
}
