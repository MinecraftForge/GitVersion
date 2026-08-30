/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.util.List;

non-sealed interface GitVersionExtensionInternal extends GitVersionExtension, GitVersionInternal, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(GitVersionExtension.class);
    }


    /* IMPL DELEGATION */

    GitVersion getInstance();

    @Override
    default String getVersion() {
        return this.getInstance().getVersion();
    }

    @Override
    default Provider<GitVersion.Info> getInfo() {
        return this.getInstance().getInfo();
    }

    @Override
    default Provider<String> getUrl() {
        return this.getInstance().getUrl();
    }

    @Override
    default Provider<String> getTagPrefix() {
        return this.getInstance().getTagPrefix();
    }

    @Override
    default @Unmodifiable Provider<List<String>> getFilters() {
        return this.getInstance().getFilters();
    }

    @Override
    default Provider<Directory> getGitDir() {
        return this.getInstance().getGitDir();
    }

    @Override
    default Provider<Directory> getRootDir() {
        return this.getInstance().getRootDir();
    }

    @Override
    default Provider<Directory> getWorkingDir() {
        return this.getInstance().getWorkingDir();
    }
}
