/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gitversion.gradle.common.GitVersionTools
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec
import org.gradle.process.ProcessExecutionException

import javax.inject.Inject
import java.nio.charset.StandardCharsets

import static net.minecraftforge.gitversion.gradle.GitVersionPlugin.LOGGER

@CompileStatic
@PackageScope abstract class GitVersionValueSource implements ValueSource<String, Parameters> {
    static interface Parameters extends ValueSourceParameters {
        ConfigurableFileCollection getClasspath()

        Property<String> getProjectPath()
    }

    @Inject
    GitVersionValueSource() {}

    protected abstract @Inject ExecOperations getExecOperations()

    @PackageScope static Provider<GitVersionExtensionInternal.Output> of(GitVersionPlugin plugin, ProviderFactory providers, File projectDirectory) {
        providers.of(GitVersionValueSource) { spec ->
            spec.parameters { parameters ->
                parameters.classpath.from(plugin.getTool(GitVersionTools.GITVERSION))
                // NOTE: We are NOT manually setting the java launcher this time
                //  Gradle 9 requires Java 17 to run, and so does Git Version
                //  We can count on the daemon JVM being 17 or higher without needing the project to apply the 'java' plugin

                parameters.projectPath.set(projectDirectory.absolutePath)
            }
        }.map { Util.fromJson(it, GitVersionExtensionInternal.Output) }
    }

    @Override
    String obtain() {
        final parameters = this.parameters
        final output = new ByteArrayOutputStream()

        Closure javaExecSpec = { JavaExecSpec exec ->
            exec.classpath = parameters.classpath
            exec.mainClass.set(GitVersionTools.GITVERSION.mainClass)

            exec.standardOutput = output
            exec.errorOutput = Util.toLog(LOGGER.&error)

            exec.args(
                '--json',
                '--project-dir', parameters.projectPath.get()
            )
        }

        try {
            this.execOperations.javaexec(javaExecSpec).rethrowFailure().assertNormalExitValue()
        } catch (ProcessExecutionException e) {
            output.reset()

            try {
                this.execOperations.javaexec(javaExecSpec.andThen { JavaExecSpec exec ->
                    exec.args('--disable-strict')
                }).rethrowFailure().assertNormalExitValue()
            } catch (ProcessExecutionException suppressed) {
                e.addSuppressed(suppressed)
                throw e
            }
        }

        output.toString(StandardCharsets.UTF_8)
    }
}
