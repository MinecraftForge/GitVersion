/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gradleutils.GenerateActionsWorkflow
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.annotations.Nullable

import javax.inject.Inject

@CompileStatic
@PackageScope abstract class GitVersionExtensionImpl implements GitVersionExtensionInternal {
    private final GitVersionProblems problems
    private final Property<Output> gitversion

    protected abstract @Inject ObjectFactory getObjects()
    protected abstract @Inject ProviderFactory getProviders()

    @Inject
    GitVersionExtensionImpl(GitVersionPlugin plugin, ExtensionAware target, Directory projectDirectory) {
        this.problems = this.objects.newInstance(GitVersionProblems)
        this.gitversion = this.objects.property(Output)
                              .value(GitVersionValueSource.of(plugin, this.providers, projectDirectory))
                              .tap { disallowChanges(); finalizeValueOnRead() }

        if (target instanceof Project)
            target.afterEvaluate { this.finish(it) }
    }

    @CompileDynamic
    private void finish(Project project) {
        project.pluginManager.withPlugin('net.minecraftforge.gradleutils') {
            project.tasks.withType(GenerateActionsWorkflow).configureEach { task ->
                task.gitVersionPresent.set(true)
                task.branch.convention(this.providers.provider { this.info.branch })
                task.localPath.convention(this.providers.provider { this.projectPath })
                task.paths.convention(this.providers.provider { this.gitversion.get().subprojectPaths().collect { "!${it}/**".toString() } })
            }
        }
    }

    @Lazy GitVersionExtension.Info info = {
        this.gitversion.get().info()
    }()

    @Lazy @Nullable String url = {
        this.gitversion.get().url()
    }()

    @Lazy String tagPrefix = {
        this.gitversion.get().tagPrefix()
    }()

    @Lazy Collection<String> filters = {
        this.gitversion.get().filters()
    }()

    @Lazy Directory gitDir = {
        this.objects.directoryProperty().fileValue(new File(this.gitversion.get().gitDirPath())).get()
    }()

    @Lazy Directory root = {
        this.objects.directoryProperty().fileValue(new File(this.gitversion.get().rootPath())).get()
    }()

    @Lazy Directory project = {
        this.objects.directoryProperty().fileValue(new File(this.gitversion.get().projectPath())).get()
    }()
}
