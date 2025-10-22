/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import net.minecraftforge.gitversion.gradle.common.GitVersionTools
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec

import javax.inject.Inject
import java.nio.charset.StandardCharsets

@CompileStatic
@PackageScope abstract class GitVersionValueSource implements ValueSource<GitVersionValueResult, Parameters> {
    static interface Parameters extends ValueSourceParameters {
        ConfigurableFileCollection getClasspath()

        DirectoryProperty getProjectPath()
    }

    protected abstract @Inject ExecOperations getExecOperations()

    @Inject
    GitVersionValueSource() {}

    @PackageScope static GitVersionExtensionInternal.Output of(GitVersionProblems problems, GitVersionPlugin plugin, Directory projectDirectory) {
        var result = plugin.getProviders().of(GitVersionValueSource) { spec ->
            spec.parameters { parameters ->
                parameters.classpath.from(plugin.getTool(GitVersionTools.GITVERSION).classpath)
                // NOTE: We are NOT manually setting the java launcher this time
                //  Gradle 9 requires Java 17 to run, and so does Git Version
                //  We can count on the daemon JVM being 17 or higher without needing the project to apply the 'java' plugin

                parameters.projectPath.set(projectDirectory)
            }
        }.get()

        if (result.execFailure() !== null)
            problems.reportGitVersionFailure(result.errorOutput(), result.execFailure())

        return result.output()
    }

    @Override
    GitVersionValueResult obtain() {
        final parameters = this.parameters
        final stdOut = new ByteArrayOutputStream()
        final stdErr = new ByteArrayOutputStream()

        GitVersionExtensionInternal.Output output
        String errorOutput
        Throwable execFailure

        Closure javaExecSpec = { JavaExecSpec exec ->
            exec.classpath = parameters.classpath
            exec.mainClass.set(GitVersionTools.GITVERSION.mainClass)

            exec.standardOutput = stdOut
            exec.errorOutput = stdErr

            exec.args(
                '--json',
                '--project-dir', parameters.projectPath.locationOnly.get().asFile
            )
        }

        try {
            this.execOperations.javaexec(javaExecSpec).rethrowFailure().assertNormalExitValue()

            output = Util.fromJson(stdOut.toString(StandardCharsets.UTF_8), GitVersionExtensionInternal.Output)
            errorOutput = stdErr.toString(StandardCharsets.UTF_8)
            execFailure = null
        } catch (Exception e) {
            stdOut.reset()

            try {
                this.execOperations.javaexec(javaExecSpec.andThen { JavaExecSpec exec ->
                    exec.args('--disable-strict')
                    exec.errorOutput = new ByteArrayOutputStream()
                }).rethrowFailure().assertNormalExitValue()

                output = Util.fromJson(stdOut.toString(StandardCharsets.UTF_8), GitVersionExtensionInternal.Output)
            } catch (Exception suppressed) {
                e.addSuppressed(suppressed)
                output = GitVersionExtensionInternal.Output.EMPTY
            }

            errorOutput = stdErr.toString(StandardCharsets.UTF_8)
            execFailure = e
        }

        return new GitVersionValueResult(output, errorOutput, execFailure)
    }
}
