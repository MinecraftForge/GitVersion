/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gitversion.gradle.common.GitVersionTools
import net.minecraftforge.gradleutils.shared.ToolExecBase
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

import javax.inject.Inject

@CompileStatic
@PackageScope abstract class GenerateChangelogImpl extends ToolExecBase<ChangelogProblems> implements GenerateChangelogInternal {
    @Inject
    GenerateChangelogImpl() {
        super(ChangelogProblems, GitVersionTools.GITVERSION)

        this.description = 'Generates a changelog for the project based on the Git history using Git Version.'

        this.outputFile.convention(this.getDefaultOutputFile('txt'))

        this.buildMarkdown.convention(false)

        // Internal inputs
        this.inputs.property('projectVersion', this.project.version)
        this.inputs.property('projectPath', this.projectLayout.projectDirectory.asFile.absolutePath)
    }

    @Override abstract @OutputFile RegularFileProperty getOutputFile()
    @Override abstract @Input @Optional Property<String> getStart()
    @Override abstract @Input @Optional Property<String> getProjectUrl()
    @Override abstract @Input Property<Boolean> getBuildMarkdown()

    @Override
    protected void addArguments() {
        super.addArguments()

        this.args(
            '--changelog',
            '--project-dir', this.inputs.properties.get('projectPath'),
            '--output', this.outputFile.asFile.get()
        )
        if (this.start.isPresent())
            this.args('--start', this.start.get())
        if (this.projectUrl.isPresent())
            this.args('--url', this.projectUrl.get())
        if (!this.buildMarkdown.getOrElse(false))
            this.args('--plain-text')
    }
}
