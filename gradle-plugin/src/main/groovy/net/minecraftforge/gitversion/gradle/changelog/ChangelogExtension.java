/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.maven.MavenPublication;

/// Configuration for the Changelog plugin.
///
/// This extension is added to the [project][org.gradle.api.Project] when the `net.minecraftforge.changelog` plugin is
/// applied.
/// ### Enabling Generation
/// To enable changelog generation for your project, a start marker for the changelog must be specified. This can be
/// done by calling [#from(String)] or one of its sister methods.
public sealed interface ChangelogExtension permits ChangelogExtensionInternal {
    /// The name for this extension.
    String NAME = "changelog";

    /// Sets the changelog start marker to the last merge base commit.
    ///
    /// @see #from(String)
    void fromBase();

    /// Sets the changelog start marker to use when generating the changelog. This can be a tag name or a commit SHA.
    ///
    /// @param marker The start marker for the changelog
    /// @apiNote To start from the last merge base commit, use [#fromBase()]
    void from(String marker);

    /// Sets the changelog start marker to use when generating the changelog. This can be a tag name or a commit SHA.
    ///
    /// If `null`, the changelog will start from the last merge base commit.
    ///
    /// @param marker The start marker for the changelog
    void from(Provider<?> marker);

    /// Sets this project's changelog as an artifact for the given publication.
    ///
    /// @param publication The publication
    void publish(MavenPublication publication);

    /// The property that sets if the changelog generation should be enabled for all maven publications in the project.
    ///
    /// It will also set up publishing for all subprojects as long as that subproject does not have another changelog
    /// plugin overriding the propagation.
    ///
    /// @return The property for if the changelog generation is enabled for all maven publications
    Property<Boolean> getPublishAll();
}
