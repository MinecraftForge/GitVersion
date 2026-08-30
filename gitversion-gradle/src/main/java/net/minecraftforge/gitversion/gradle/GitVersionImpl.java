/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import org.gradle.api.file.Directory;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.jetbrains.annotations.Unmodifiable;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

abstract class GitVersionImpl implements GitVersionInternal {
    final Property<Output> gitversion;
    private final Property<GitVersionExtension.Info> info;

    protected abstract @Inject ObjectFactory getObjects();

    protected abstract @Inject ProviderFactory getProviders();

    @Inject
    public GitVersionImpl(GitVersionPlugin plugin, Directory projectDirectory) {
        this.gitversion = Util.finalizeProperty(getObjects().property(Output.class)
            .value(GitVersionValueSource.of(getObjects().newInstance(GitVersionProblems.class), plugin, projectDirectory)));
        this.info = Util.finalizeProperty(getObjects().property(GitVersionExtension.Info.class)
            .value(this.gitversion.map(Output::info)));
    }

    /* VERSIONING */

    @Override
    public String getVersion() {
        return this.info.map(GitVersion.Info::getVersion).get();
    }

    /* INFO */

    @Override
    public Provider<GitVersionExtension.Info> getInfo() {
        return this.info;
    }

    @Override
    public Provider<String> getUrl() {
        return this.gitversion.map(Output::url);
    }


    /* FILTERING */

    @Override
    public Provider<String> getTagPrefix() {
        return this.gitversion.map(Output::tagPrefix);
    }

    @Override
    public @Unmodifiable Provider<List<String>> getFilters() {
        return this.gitversion.map(Output::filters);
    }


    /* FILE SYSTEM */

    @Override
    public Provider<Directory> getGitDir() {
        return getObjects().directoryProperty().fileProvider(getProviders().provider(() -> {
            var path = this.gitversion.get().gitDirPath();
            return path == null ? null : new File(path);
        }));
    }

    @Override
    public Provider<Directory> getRootDir() {
        return getObjects().directoryProperty().fileProvider(getProviders().provider(() -> {
            var path = this.gitversion.get().rootPath();
            return path == null ? null : new File(path);
        }));
    }

    @Override
    public Provider<Directory> getWorkingDir() {
        return getObjects().directoryProperty().fileProvider(getProviders().provider(() -> {
            var path = this.gitversion.get().projectPath();
            return path == null ? null : new File(path);
        }));
    }
}
