/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

// This task class is internal. Do NOT attempt to use it directly.
// If you need the output, use `project.tasks.named('copyChangelog').outputs.files` instead
@CompileStatic
@PackageScope abstract class CopyChangelogImpl extends DefaultTask implements CopyChangelogInternal {
    protected abstract @Inject ProviderFactory getProviders()

    @Inject
    CopyChangelogImpl() {
        this.description = 'Copies a changelog file to this project\'s build directory.'

        this.outputFile.convention(this.getDefaultOutputFile('txt'))
    }

    protected abstract @InputFile RegularFileProperty getInputFile()
    protected abstract @OutputFile RegularFileProperty getOutputFile()

    @TaskAction
    void exec() {
        var input = this.providers.fileContents(this.inputFile).asBytes.get()
        var output = this.outputFile.asFile.get()

        output.bytes = input
    }
}
