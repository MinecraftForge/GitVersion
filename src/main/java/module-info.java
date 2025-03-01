/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
module net.minecraftforge.gitver {
    exports net.minecraftforge.gitver;
    exports net.minecraftforge.util.git;

    requires com.google.gson;
    requires org.apache.commons.io;
    requires org.eclipse.jgit;

    requires static joptsimple;
    requires static org.jetbrains.annotations;
}