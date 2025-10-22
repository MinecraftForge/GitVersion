/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import java.io.Serializable;

@SuppressWarnings("ClassCanBeRecord") // Gradle hates records -- ctor needs to be public
final class GitVersionValueResult implements Serializable {
    private final GitVersionExtensionInternal.Output output;
    private final String errorOutput;
    private final Throwable execFailure;

    public GitVersionValueResult(GitVersionExtensionInternal.Output output, String errorOutput, Throwable execFailure) {
        this.output = output;
        this.errorOutput = errorOutput;
        this.execFailure = execFailure;
    }

    public GitVersionExtensionInternal.Output output() {
        return this.output;
    }

    public String errorOutput() {
        return this.errorOutput;
    }

    public Throwable execFailure() {
        return this.execFailure;
    }
}
