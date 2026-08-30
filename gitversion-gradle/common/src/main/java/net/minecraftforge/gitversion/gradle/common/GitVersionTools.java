/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle.common;

import net.minecraftforge.gradleutils.shared.Tool;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class GitVersionTools {
    private static final String FORGE_MAVEN = "https://maven.minecraftforge.net/";

    // Git Version
    private static final String GITVERSION_NAME = "gitversion";
    private static final String GITVERSION_VERSION = "0.9.0";
    private static final String GITVERSION_ARTIFACT = "net.minecraftforge:gitversion:" + GITVERSION_VERSION + ":fatjar";
    private static final int GITVERSION_JAVA = 17;
    public static final Tool GITVERSION = Tool.ofForge(GITVERSION_NAME, GITVERSION_ARTIFACT, GITVERSION_JAVA);
}
