/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver.cli;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraftforge.gitver.api.GitVersion;
import org.jetbrains.annotations.Nullable;

import java.io.File;

// TODO [GitVersion] Document
public final class Main {
    public static void main(String[] args) throws Exception {
        // TODO [Utils] Move option parsing to a general utility library?
        var parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        //@formatter:off
        // help message
        var help0 = parser.accepts("help",
            "Displays this help message and exits")
            .forHelp();

        // strict mode
        var disableStrict0 = parser.accepts("disable-strict",
            """
            Disables strict mode, allowing GitVersion to continue even if an error occurs.
            Version numbers will be set to default values (i.e. 0.0.0), and generated changelogs will be completely empty.""");

        // config file
        var configFile0 = parser.accepts("config-file",
            """
            The config file to read from.
            If unspecified, Git Version will attempt to locate this as '.gitversion' in the root directory.
            If the file does not exist, the default behavior is assumed and there will be no subprojects.
            Unless manually specified, the containing folder will be the project directory, and read access is required.""")
            .withOptionalArg().ofType(File.class);

        var gitDir0 = parser.accepts("git-dir",
            """
            The git directory to use.
            If unspecified, Git Version will attempt to locate this automatically.""")
            .withOptionalArg().ofType(File.class);

        var rootDir0 = parser.accepts("root-dir",
            """
            The root directory to use (ideally containing the .git directory).
            If unspecified, Git Version will attempt to locate this automatically.""")
            .withOptionalArg().ofType(File.class);

        var projectDir0 = parser.accepts("project-dir",
            """
            The project directory to use. (default: .)""")
            .withOptionalArg().ofType(File.class).defaultsTo(new File("."));

        var changelogO = parser.accepts("changelog",
            """
            Use to generate a changelog for the repository.""");

        var changelogStartO = parser.accepts("start",
            """
            The commit to start from when generating the changelog.""")
            .availableIf(changelogO).withRequiredArg().ofType(String.class);

        var changelogUrlO = parser.accepts("url",
            """
            The URL to use when generating the changelog.
            If left unspecified, will attempt to automatically get the URL from the repository.""")
            .availableIf(changelogO).withRequiredArg().ofType(String.class);

        var changelogPlainTextO = parser.accepts("plain-text",
            """
            If the generated changelog should be in plain text instead of Markdown.""")
            .availableIf(changelogO);

        var jsonO = parser.accepts("json",
            """
            Use to output the Git Version info as JSON.
            Used by the Git Version Gradle plugin.""")
            .availableUnless(changelogO);
        //@formatter:on

        var options = parser.parse(args);
        if (options.has(help0)) {
            System.out.println("Git Version");
            parser.printHelpOn(System.out);
            return;
        }

        var strict = !options.has(disableStrict0);
        var configFile = options.valueOfOptional(configFile0).orElse(null);
        var projectDir = options.valueOf(projectDir0);
        var rootDir = options.valueOfOptional(rootDir0).orElse(null);
        var gitDir = options.valueOfOptional(gitDir0).orElse(null);
        try (var version = GitVersion
            .builder()
            .gitDir(gitDir)
            .root(rootDir)
            .project(projectDir)
            .config(configFile)
            .strict(strict)
            .build()
        ) {
            if (options.has(changelogO)) {
                System.out.println(version.generateChangelog(
                    options.valueOfOptional(changelogStartO).orElse(null),
                    options.valueOfOptional(changelogUrlO).orElse(null),
                    options.has(changelogPlainTextO)
                ));
            } else if (options.has(jsonO)) {
                System.out.println(version.toJson());
            } else {
                System.out.print(version.getTagOffset());
            }
        }
    }

    private static boolean canWrite(File file) {
        return file.canWrite() || (!file.exists() && file.getParentFile().canWrite());
    }

    private static @Nullable OptionSet tryParse(OptionParser parser, String[] args) {
        try {
            return parser.parse(args);
        } catch (OptionException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }
}
