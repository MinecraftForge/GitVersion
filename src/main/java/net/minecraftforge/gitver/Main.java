/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraftforge.util.git.GitUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.stream.Collectors;

// TODO [GitVersion] Document
public final class Main {
    public static void main(String[] args) throws Exception {
        // TODO [Utils] Move option parsing to a general utility library?
        var parser = Util.make(new OptionParser(), OptionParser::allowsUnrecognizedOptions);

        //@formatter:off
        // help message
        var help0 = parser.accepts("help",
            "Displays this help message and exits")
            .forHelp();

        var doNotSave0 = parser.accepts("do-not-save",
        "Do not save or modify the config file");

        var configFile0 = parser.accepts("config-file",
            """
            The config file to read from and write to. (default: .gitversion)
            If the file does not exist, the default config will be used and the file will be created.
            The containing folder will be the working directory for GitVersion, and read access is required.
            If write access is unavailable, the config file will not be saved.""")
            .withOptionalArg().ofType(File.class).defaultsTo(new File("./.gitversion"));

        // tag prefix
        var tagPrefix0 = parser.accepts("tag-prefix",
            """
            The prefix to use when filtering tags for this version.
            This will be saved to the config file and overwite the tag prefix in it.""")
            .withOptionalArg().ofType(String.class);

        // match filters
        var matchFilter0 = parser.accepts("tag-match-filter",
            """
            Additional glob filter(s) to use when matching tags.
            This will be saved to the config file and overwrite any existing filters in it.""")
            .withOptionalArg().ofType(String.class);

        // marker file
        var marker0 = parser.accepts("marker-file-name",
            """
            Marker file name(s) to indicate the root of projects, which is used to filter out subprojects the version number.
            This will be saved to the config file and overwrite any existing marker file names in it.""")
            .withRequiredArg().ofType(String.class);

        // ignore file
        var ignoreFile0 = parser.accepts("ignore-file-name",
            """
            Marker file name(s) to indicate that a detected subproject should not be treated as such. (default: '%s')
            This is effectively the inverse of the marker file, allowing projects that would normally be filtered out from the version number to be included.
            This will be saved to the config file and overwrite any existing ignore file names in it.""".formatted(GitVersion.DEFAULT_IGNORE_FILE))
            .withRequiredArg().ofType(String.class).defaultsTo(GitVersion.DEFAULT_IGNORE_FILE);

        // ignore dir
        var ignoreDir0 = parser.accepts("ignore-dir",
            """
            The directories to always ignore from counting as a subproject, thus filtering them into the version number.
            This is a more fine-tuned version of the 'ignore-file-name' parameter, allowing specific directories to be ignored.
            This will be saved to the config file and overwrite any existing ignored directories in it.""")
            .withOptionalArg().ofType(File.class);

        var options = tryParse(parser, args);
        if (options == null || options.has(help0)) {
            //noinspection removal
            System.out.println("GitVersion " + JarVersionLookupHandler.getInfo(Main.class).impl().version().orElse(""));
            System.out.println();
            parser.printHelpOn(System.out);
            return;
        }

        File project;
        GitVersionConfig config;
        var configFile = options.valueOf(configFile0);
        if (!configFile.exists() || !configFile.canRead()) {
            project = ensureReadAccess(parentOrWorkingDir(configFile));

            var tagPrefix = options.valueOf(tagPrefix0);
            var matchFilters = options.valuesOf(matchFilter0);

            var markerFile = options.valuesOf(marker0);
            var ignoreFile = options.valuesOf(ignoreFile0);
            var ignoreDir = options.valuesOf(ignoreDir0);

            config = new GitVersionConfig(tagPrefix, matchFilters, markerFile, ignoreFile, ignoreDir.stream().map(dir -> GitUtils.getRelativePath(project, dir)).collect(Collectors.toCollection(ArrayList::new)));
        } else {
            project = ensureReadAccess(configFile.getParentFile());
            config = GitVersionConfig.fromJson(configFile);
        }

        try (var version = new GitVersion(project, config)) {
            System.out.print(version.getTagOffset());

            if (!options.has(doNotSave0) && canWrite(configFile)) {
                configFile.createNewFile();
                version.makeConfig().toJson(configFile);
            }
        }
    }

    private static File parentOrWorkingDir(File file) {
        var parent = file.getParentFile();
        return parent.exists() ? parent : new File(".");
    }

    private static File ensureReadAccess(File file) throws AccessDeniedException {
        if (!file.canRead())
            throw new AccessDeniedException("GitVersion cannot execute as the project directory cannot be read.");

        return file;
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
