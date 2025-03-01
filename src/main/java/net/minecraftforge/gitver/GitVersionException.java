/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver;

// TODO [GitVersion] Document
public class GitVersionException extends RuntimeException {
    GitVersionException(String message) {
        super(message);
    }

    GitVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
