/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// This task class is internal. Do NOT attempt to use it directly.
// If you need the output, use `project.tasks.named('copyChangelog').outputs.files` instead
abstract class CopyChangelog extends DefaultTask implements HasPublicType {
    static final String NAME = "copyChangelog";

    protected abstract @Inject ProviderFactory getProviders();

    @Inject
    public CopyChangelog(ProjectLayout layout) {
        this.setDescription("Copies a changelog file to this project's build directory.");

        this.getOutputFile().convention(layout.getBuildDirectory().file("changelog.txt"));
    }

    @Override
    public @Internal TypeOf<?> getPublicType() {
        return TypeOf.typeOf(DefaultTask.class);
    }

    public abstract @OutputFile RegularFileProperty getOutputFile();

    public abstract @InputFile RegularFileProperty getInputFile();

    @TaskAction
    public void exec() {
        byte[] input;
        try {
            // ProviderFactory#fileContents so Gradle is aware of our usage of the input
            input = this.getProviders().fileContents(this.getInputFile()).getAsBytes().get();
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        }

        File output = this.getOutputFile().get().getAsFile();

        if (!output.getParentFile().exists() && !output.getParentFile().mkdirs())
            throw new IllegalStateException();

        try {
            Files.write(
                output.toPath(),
                input
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
