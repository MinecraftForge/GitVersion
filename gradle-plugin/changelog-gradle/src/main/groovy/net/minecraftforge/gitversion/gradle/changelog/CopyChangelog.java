/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import org.gradle.api.Task;

/// Copies a changelog from a parent project to this subproject.
public sealed interface CopyChangelog extends Task permits CopyChangelogInternal {
    /// The name for this task.
    String NAME = "copyChangelog";
}
