/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import net.minecraftforge.gitversion.gradle.common.GitVersionTools;
import net.minecraftforge.gradleutils.shared.ToolExecBase;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

import javax.inject.Inject;
import java.util.Map;

abstract class GenerateChangelogImpl extends ToolExecBase<ChangelogProblems> implements GenerateChangelogInternal {
    @Override public abstract @OutputFile RegularFileProperty getOutputFile();
    @Override public abstract @Input @Optional Property<String> getStart();
    @Override public abstract @Input @Optional Property<String> getProjectUrl();
    @Override public abstract @Input Property<Boolean> getBuildMarkdown();

    protected abstract @Inject ProviderFactory getProviders();
    protected abstract @Inject ProjectLayout getProjectLayout();

    @Inject
    public GenerateChangelogImpl() {
        super(GitVersionTools.GITVERSION);

        this.setDescription("Generates a changelog for the project based on the Git history using Git Version.");

        this.getOutputFile().convention(this.getDefaultOutputFile("txt"));

        this.getBuildMarkdown().convention(false);

        // Internal inputs
        this.getInputs().property("projectVersion", this.getProviders().provider(this.getProject()::getVersion));
        this.getInputs().property("projectPath", this.getProjectLayout().getProjectDirectory().getAsFile().getAbsoluteFile());
    }

    @Override
    protected void addArguments() {
        this.args("--changelog");
        this.args(Map.of(
            "--project-dir", this.getInputs().getProperties().get("projectPath"),
            "--output", this.getOutputFile().getLocationOnly(),
            "--start", this.getStart(),
            "--url", this.getProjectUrl(),
            "--plain-text", this.getBuildMarkdown()
        ));

        super.addArguments();
    }
}
