/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

public sealed interface GitVersion extends Supplier<String> permits GitVersionExtension, GitVersionInternal {
    /* VERSIONING */

    String getVersion();

    @Override
    default String get() {
        return this.getVersion();
    }

    /* INFO */

    Provider<Info> getInfo();

    Provider<String> getUrl();

    sealed interface Info extends Serializable permits GitVersionInternal.Info {
        String getVersion();

        String getTag();

        String getOffset();

        String getHash();

        String getBranch();

        String getBranch(boolean versionFriendly);

        String getCommit();

        String getAbbreviatedId();
    }


    /* FILTERING */

    Provider<String> getTagPrefix();

    @Unmodifiable
    Provider<List<String>> getFilters();


    /* FILE SYSTEM */

    Provider<Directory> getGitDir();

    Provider<Directory> getRootDir();

    Provider<Directory> getWorkingDir();
}
