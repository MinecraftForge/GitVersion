/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
import org.jspecify.annotations.NullMarked;

@NullMarked
module net.minecraftforge.gitver {
    exports net.minecraftforge.gitver.api;

    requires org.tomlj;
    requires org.eclipse.jgit;

    requires com.google.gson;
    requires transitive org.jspecify;
    requires static org.jetbrains.annotations;
}
