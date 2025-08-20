/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider

import javax.inject.Inject

@CompileStatic
@PackageScope abstract class ChangelogExtensionImpl implements ChangelogExtensionInternal {
    private final Project project

    private final Property<Boolean> publishingAll
    private final Property<Boolean> includingSubprojects
    private final Property<Boolean> isGenerating

    private @Lazy TaskProvider<? extends GenerateChangelog> task = {
        this.isGenerating.set(true)
        Util.ensureAfterEvaluate(this.project) { this.finish(it) }

        ChangelogUtils.setupChangelogTask(this.project)
    }()

    protected abstract @Inject ObjectFactory getObjects()

    @Inject
    ChangelogExtensionImpl(Project project) {
        this.project = project

        this.publishingAll = this.objects.property(Boolean).convention(false)
        this.includingSubprojects = this.objects.property(Boolean).convention(false)
        this.isGenerating = this.objects.property(Boolean).convention(false)
    }

    private void finish(Project project) {
        if (this.publishingAll.getOrElse(false))
            ChangelogUtils.setupChangelogGenerationOnAllPublishTasks(project, this.includingSubprojects)
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
    Property<Boolean> getIncludeSubprojects() {
        this.includingSubprojects
    }

    @Override
    boolean isGenerating() {
        this.isGenerating.get()
    }

    @Override
    TaskProvider<? extends CopyChangelog> copyTo(Project project) {
        // isGenerating = true and afterEvaluate ensured
        // See ChangelogUtils#setupChangelogGenerationForPublishingAfterEvaluation
        project.tasks.register(CopyChangelog.NAME, CopyChangelogImpl) { task ->
            task.dependsOn(this.task)

            var dependency = project.dependencies.project('path': this.project.path, 'configuration': GenerateChangelog.NAME)
            var configuration = project.configurations.detachedConfiguration(dependency).tap { canBeConsumed = false }
            task.inputFile.fileProvider(project.providers.provider(configuration.&getSingleFile))
        }
    }
}
