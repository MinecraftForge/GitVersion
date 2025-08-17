/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
module net.minecraftforge.gitver {
    exports net.minecraftforge.gitver.api;

    requires org.tomlj;
    requires org.eclipse.jgit;

    requires com.google.gson;
    requires static org.jetbrains.annotations;
}
