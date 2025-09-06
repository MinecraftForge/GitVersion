/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import net.minecraftforge.gradleutils.shared.Lazy;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;
import java.util.Map;

abstract class ChangelogExtensionImpl implements ChangelogExtensionInternal {
    private final Project project;

    private final Property<Boolean> publishingAll;
    private final Property<Boolean> includingSubprojects;
    private final Property<Boolean> isGenerating;

    private final Lazy<TaskProvider<? extends GenerateChangelog>> task = Lazy.simple(this::makeTask);

    private TaskProvider<? extends GenerateChangelog> makeTask() {
        this.isGenerating.set(true);
        Util.ensureAfterEvaluate(this.project, this::finish);

        return ChangelogUtils.setupChangelogTask(this.project);
    }

    protected abstract @Inject ObjectFactory getObjects();

    protected abstract @Inject ProviderFactory getProviders();

    @Inject
    public ChangelogExtensionImpl(Project project) {
        this.project = project;

        this.publishingAll = this.getObjects().property(Boolean.class).convention(false);
        this.includingSubprojects = this.getObjects().property(Boolean.class).convention(false);
        this.isGenerating = this.getObjects().property(Boolean.class).convention(false);
    }

    private void finish(Project project) {
        if (this.publishingAll.getOrElse(false))
            ChangelogUtils.setupChangelogGenerationOnAllPublishTasks(project, this.includingSubprojects);
    }

    @Override
    public void fromBase() {
        this.task.get();
    }

    @Override
    public void from(String marker) {
        this.task.get().configure(t -> t.getStart().set(marker));
    }

    @Override
    public void from(Provider<?> marker) {
        this.task.get().configure(t -> t.getStart().set(marker.map(Object::toString)));
    }

    @Override
    public void publish(MavenPublication publication) {
        ChangelogUtils.setupChangelogGenerationForPublishing(this.project, publication);
    }

    @Override
    public Property<Boolean> getPublishAll() {
        return this.publishingAll;
    }

    @Override
    public Property<Boolean> getIncludeSubprojects() {
        return this.includingSubprojects;
    }

    @Override
    public boolean isGenerating() {
        return this.isGenerating.get();
    }

    @Override
    public TaskProvider<? extends CopyChangelog> copyTo(Project project) {
        // isGenerating = true and afterEvaluate ensured
        // See ChangelogUtils#setupChangelogGenerationForPublishingAfterEvaluation
        return project.getTasks().register(CopyChangelog.NAME, CopyChangelogImpl.class, task -> {
            task.dependsOn(this.task);

            var dependency = project.getDependencies().project(Map.of("path", this.project.getPath(), "configuration", GenerateChangelog.NAME));
            var configuration = project.getConfigurations().detachedConfiguration(dependency);
            configuration.setCanBeConsumed(false);
            task.getInputFile().fileProvider(this.getProviders().provider(configuration::getSingleFile));
        });
    }
}
