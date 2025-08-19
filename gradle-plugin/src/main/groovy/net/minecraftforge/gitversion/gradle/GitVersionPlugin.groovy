/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import net.minecraftforge.gradleutils.shared.EnhancedPlugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionAware

import javax.inject.Inject

@CompileStatic
@PackageScope([PackageScopeTarget.CLASS, PackageScopeTarget.FIELDS])
abstract class GitVersionPlugin extends EnhancedPlugin<ExtensionAware> {
    static final String NAME = 'gitversion'
    static final String DISPLAY_NAME = 'Git Version'

    static final Logger LOGGER = Logging.getLogger(GitVersionPlugin)

    @Inject
    GitVersionPlugin() {
        super(NAME, DISPLAY_NAME)
    }

    @Override
    void setup(ExtensionAware target) {
        if (target instanceof Project && target.layout.projectDirectory.asFile == this.buildLayout.rootDirectory.asFile) {
            var gitversion = target.gradle.extensions.findByType(GitVersionExtension)
            if (gitversion !== null) {
                target.extensions.add('gitversion', gitversion)
                return
            }
        }

        var gitversion = target.extensions.findByType(GitVersionExtension) ?: target.extensions.create(GitVersionExtension.NAME, GitVersionExtensionImpl, this, target)
        if (target instanceof Settings) {
            target.gradle.extensions.add(GitVersionExtension.NAME, gitversion)
            target.gradle.beforeProject { Project project ->
                project.version = gitversion.tagOffset
            }
        }
    }
}
