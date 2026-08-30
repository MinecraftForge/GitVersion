/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import net.minecraftforge.gradleutils.GenerateActionsWorkflow;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Project;
import org.gradle.api.plugins.AppliedPlugin;
import org.gradle.api.provider.Property;

public class GradleUtilsIntegration {
    static void apply(AppliedPlugin appliedPlugin, Project project, GitVersionExtensionImpl extension, GitVersionImpl gitversion) {
        project.getTasks().withType(GenerateActionsWorkflow.class).configureEach(task -> {
            task.getBranch().convention(gitversion.gitversion.map(g -> g.info().getBranch()));
            task.getLocalPath().convention(project.getProviders().zip(extension.getRootDir(), extension.getWorkingDir(), (rootDir, workingDir) -> {
                var rootFile = rootDir.getAsFile();
                var workingFile = workingDir.getAsFile();
                if (rootFile.equals(workingFile)) return "";

                var rootPath = rootFile.toPath();
                var workingPath = workingFile.toPath();
                return rootPath.relativize(workingPath).toString().replace(rootPath.getFileSystem().getSeparator(), "/");
            }));
            task.getPaths().convention(gitversion.gitversion.map(g -> g.subprojectPaths().stream().map("!%s/**"::formatted).toList()));

            try {
                var gitVersionPresent = (Property<Boolean>) InvokerHelper.getProperty(task, "gitVersionPresent");
                gitVersionPresent.set(true);
            } catch (RuntimeException e) {
                // no-op, not worth causing trouble if it's broken for some reason.
            }
        });
    }
}
