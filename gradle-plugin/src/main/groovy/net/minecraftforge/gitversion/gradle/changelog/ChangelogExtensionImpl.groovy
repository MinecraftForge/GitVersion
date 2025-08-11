/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider

import javax.inject.Inject

/** The heart of the Changelog plugin. This extension is used to enable and partially configure the changelog generation task. */
@CompileStatic
@PackageScope([PackageScopeTarget.CLASS, PackageScopeTarget.FIELDS])
abstract class ChangelogExtensionImpl implements ChangelogExtensionInternal {
    public static final String NAME = 'changelog'

    private final Project project

    private final Property<Boolean> publishingAll
    private final Property<Boolean> isGenerating

    private @Lazy TaskProvider<GenerateChangelog> task = {
        this.isGenerating.set(true)
        Util.ensureAfterEvaluate(this.project) { this.finish(it) }

        ChangelogUtils.setupChangelogTask(this.project)
    }()

    protected abstract @Inject ObjectFactory getObjects()

    @Inject
    ChangelogExtensionImpl(Project project) {
        this.project = project

        this.publishingAll = this.objects.property(Boolean).convention(false)
        this.isGenerating = this.objects.property(Boolean).convention(false)
    }

    private void finish(Project project) {
        if (this.publishAll)
            ChangelogUtils.setupChangelogGenerationOnAllPublishTasks(project)
    }

    @Override
    void fromBase() {
        this.task
    }

    @Override
    void from(String marker) {
        this.task.configure { it.start.set(marker) }
    }

    @Override
    void from(Provider<?> marker) {
        this.task.configure { it.start.set(marker.map(Object.&toString)) }
    }

    @Override
    void publish(MavenPublication publication) {
        ChangelogUtils.setupChangelogGenerationForPublishing(this.project, publication)
    }

    @Override
    Property<Boolean> getPublishAll() {
        this.publishingAll
    }

    @Override
    boolean isGenerating() {
        this.isGenerating.get()
    }

    @Override
    Dependency asDependency() {
        this.asDependency(this.project.dependencies)
    }

    @Override
    Dependency asDependency(DependencyHandler dependencies) {
        dependencies.project('path': this.project.path, 'configuration': GenerateChangelog.NAME)
    }
}
