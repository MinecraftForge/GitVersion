/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitversion.gradle;

import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

non-sealed interface GitVersionInternal extends GitVersion, HasPublicType {
    @Override
    default TypeOf<?> getPublicType() {
        return TypeOf.typeOf(GitVersion.class);
    }


    /* INFO */

    record Info(
        String getVersion,
        String getTag,
        String getOffset,
        String getHash,
        String getBranch,
        String getCommit,
        String getAbbreviatedId
    ) implements GitVersion.Info {
        static final Info EMPTY = new Info("0.0.0","0.0", "0", "00000000", "master", "0000000000000000000000", "00000000");

        @Override
        public String getBranch(boolean versionFriendly) {
            return getBranch(this.getBranch(), versionFriendly);
        }

        private static String getBranch(String branch, boolean versionFriendly) {
            if (!versionFriendly || branch.isBlank()) return branch;

            if (branch.startsWith("pulls/"))
                branch = "pr" + branch.substring(branch.lastIndexOf('/') + 1);
            return branch.replaceAll("[\\\\/]", "-");
        }
    }


    /* SERIALIZATION */

    record Output(
        Info info,
        @Nullable String url,

        @Nullable String gitDirPath,
        @Nullable String rootPath,
        @Nullable String projectPath,

        @Nullable String tagPrefix,
        List<String> filters,
        List<String> subprojectPaths
    ) implements Serializable {
        static final Output EMPTY = new Output(
            Info.EMPTY,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            List.of());
    }
}
