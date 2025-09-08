/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.Internal;

non-sealed interface CopyChangelogInternal extends CopyChangelog, ChangelogTask, HasPublicType {
    @Override
    default @Internal TypeOf<?> getPublicType() {
        return TypeOf.typeOf(CopyChangelog.class);
    }
}
