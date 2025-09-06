/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.changelog;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;

abstract class CopyChangelogImpl extends DefaultTask implements CopyChangelogInternal {
    protected abstract @Inject ProviderFactory getProviders();

    @Inject
    public CopyChangelogImpl() {
        this.setDescription("Copies a changelog file to this project's build directory.");

        this.getOutputFile().convention(this.getDefaultOutputFile("txt"));
    }

    protected abstract @InputFile RegularFileProperty getInputFile();
    protected abstract @OutputFile RegularFileProperty getOutputFile();

    @TaskAction
    void exec() throws IOException {
        Files.write(
            this.getOutputFile().getAsFile().get().toPath(),
            this.getProviders().fileContents(this.getInputFile()).getAsBytes().get()
        );
    }
}
