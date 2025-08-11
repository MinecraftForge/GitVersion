/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import net.minecraftforge.gradleutils.shared.Tool;

/// The package-private constants used throughout ForgeGradle.
public final class GitVersionTools {
    private static final String FORGE_MAVEN = "https://maven.minecraftforge.net/";

    // Git Version
    private static final String GITVERSION_NAME = "gitversion";
    private static final String GITVERSION_VERSION = "0.6.7";
    private static final String GITVERSION_URL = FORGE_MAVEN + "net/minecraftforge/gitversion/" + GITVERSION_VERSION + "/gitversion-" + GITVERSION_VERSION + "-fatjar.jar";
    private static final int GITVERSION_JAVA = 17;
    private static final String GITVERSION_MAIN = "net.minecraftforge.gitver.cli.Main";
    public static final Tool GITVERSION = Tool.of(GITVERSION_NAME, GITVERSION_NAME, GITVERSION_URL, GITVERSION_JAVA, GITVERSION_MAIN);
}
