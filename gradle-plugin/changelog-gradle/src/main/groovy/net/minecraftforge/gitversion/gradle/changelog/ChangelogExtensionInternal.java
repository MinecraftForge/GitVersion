/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import org.gradle.api.Project;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.TaskProvider;

non-sealed interface ChangelogExtensionInternal extends ChangelogExtension, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(ChangelogExtension.class);
    }

    boolean isGenerating();

    TaskProvider<? extends CopyChangelog> copyTo(Project project);
}
