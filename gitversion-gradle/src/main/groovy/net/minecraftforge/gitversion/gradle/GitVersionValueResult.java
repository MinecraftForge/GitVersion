/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

record GitVersionValueResult(GitVersionExtensionInternal.Output output, String errorOutput, Throwable execFailure) { }
