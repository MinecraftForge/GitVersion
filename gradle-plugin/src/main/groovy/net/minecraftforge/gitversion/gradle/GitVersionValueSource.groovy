/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec
import org.gradle.process.ProcessExecutionException

import javax.inject.Inject
import java.nio.charset.StandardCharsets

import static net.minecraftforge.gitversion.gradle.GitVersionPlugin.LOGGER

@CompileStatic
@PackageScope abstract class GitVersionValueSource<P extends Parameters> implements ValueSource<String, P> {
    static interface Parameters extends ValueSourceParameters {
        ConfigurableFileCollection getClasspath()

        Property<String> getJavaLauncher()
    }

    @Inject GitVersionValueSource() {}

    protected abstract @Inject ExecOperations getExecOperations()

    static Provider<GitVersionExtensionInternal.Output> info(Project project, ProviderFactory providers) {
        providers.of(Info) { spec ->
            spec.parameters { parameters ->
                parameters.classpath.from(project.plugins.getPlugin(GitVersionPlugin).getTool(GitVersionTools.GITVERSION))
                parameters.javaLauncher.set(Util.launcherFor(project.extensions.getByType(JavaPluginExtension), project.extensions.getByType(JavaToolchainService), 17).map { it.executablePath.toString() })

                parameters.projectPath.set(providers.provider { project.layout.projectDirectory.asFile.absolutePath })
            }
        }.map { Util.fromJson(it, GitVersionExtensionInternal.Output) }
    }

    @CompileStatic
    @PackageScope static abstract class Info extends GitVersionValueSource<Parameters> {
        static interface Parameters extends Parameters {
            Property<String> getProjectPath()
        }

        @Inject
        Info() {}

        @Override
        String obtain() {
            final parameters = this.parameters
            final output = new ByteArrayOutputStream()

            Closure javaExecSpec = { JavaExecSpec exec ->
                exec.classpath = parameters.classpath
                exec.mainClass.set(GitVersionTools.GITVERSION.mainClass)
                exec.executable = parameters.javaLauncher

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
}
