/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import net.minecraftforge.gitversion.gradle.GitVersionTools;
import net.minecraftforge.gradleutils.shared.ToolExecBase;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/// Generates a changelog for the project based on the Git history using
/// <a href="https://github.com/MinecraftForge/GitVersion">Git Version</a>.
public abstract class GenerateChangelog extends ToolExecBase<ChangelogProblems> {
    /// The name for the task, used by the [extension][ChangelogExtension] when registering it.
    public static final String NAME = "createChangelog";

    /// Constructs a new task instance.
    @Inject
    public GenerateChangelog() {
        super(ChangelogProblems.class, GitVersionTools.GITVERSION);

        this.setDescription("Generates a changelog for the project based on the Git history using Git Version.");

        //Setup defaults: Using merge-base based text changelog generation of the local project into build/changelog.txt
        this.getOutputFile().convention(this.getProjectLayout().getBuildDirectory().file("changelog.txt").map(this.getProblems().ensureFileLocation()));

        this.getProjectPath().convention(this.getProviderFactory().provider(() -> this.getProjectLayout().getProjectDirectory().getAsFile().getAbsolutePath()));
        this.getBuildMarkdown().convention(false);
    }

    /// The output file for the changelog.
    ///
    /// @return A property for the output file
    public abstract @OutputFile RegularFileProperty getOutputFile();

    /// The absolute path to the current project.
    ///
    /// Used to configure Git Version without needing to specify the directory itself, since using the directory
    /// itself can cause implicit dependencies on other tasks that use actually it.
    ///
    /// @return A property for the project path
    protected abstract @Input Property<String> getProjectPath();

    /// The tag (or object ID) to start the changelog from.
    ///
    /// @return A property for the start tag
    public abstract @Input @Optional Property<String> getStart();

    /// The project URL to use in the changelog.
    ///
    /// Git Version will automatically attempt to find a URL from the repository's remote details if left
    /// unspecified.
    ///
    /// @return A property for the project URL
    public abstract @Input @Optional Property<String> getProjectUrl();

    /**
     * Whether to build the changelog in Markdown format.
     *
     * @return A property for Markdown formatting
     */
    public abstract @Input Property<Boolean> getBuildMarkdown();

    @Override
    protected void addArguments() {
        super.addArguments();

        this.args(
            "--changelog",
            "--project-dir", this.getProjectPath().get()
        );
        if (this.getStart().isPresent())
            this.args("--start", this.getStart().get());
        if (this.getProjectUrl().isPresent())
            this.args("--url", this.getProjectUrl().get());
        if (!this.getBuildMarkdown().getOrElse(false))
            this.args("--plain-text");
    }

    @Override
    public void exec() {
        var output = new ByteArrayOutputStream();
        this.setStandardOutput(output);
        this.setErrorOutput(Util.toLog(this.getLogger()::error));

        super.exec();

        try {
            Files.writeString(
                this.getOutputFile().get().getAsFile().toPath(),
                output.toString(),
                StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
