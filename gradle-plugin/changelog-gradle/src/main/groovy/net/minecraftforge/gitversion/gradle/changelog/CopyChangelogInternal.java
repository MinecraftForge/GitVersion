/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import net.minecraftforge.gradleutils.shared.EnhancedTask;
import org.gradle.api.Project;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.Internal;

non-sealed interface CopyChangelogInternal extends CopyChangelog, EnhancedTask, HasPublicType {
    @Override
    default Class<? extends EnhancedPlugin<? super Project>> pluginType() {
        return ChangelogPlugin.class;
    }

    @Override
    default @Internal TypeOf<?> getPublicType() {
        return TypeOf.typeOf(CopyChangelog.class);
    }
}
