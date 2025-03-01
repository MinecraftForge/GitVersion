/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver;

import java.util.Optional;

/**
 * Finds Version data from a package, with possible default values
 *
 * @deprecated Will be moved to a utility project
 */
@Deprecated(forRemoval = true)
final class JarVersionLookupHandler {
    record Version(Optional<String> title, Optional<String> vendor, Optional<String> version) {
        @Override
        public String toString() {
            return "[" +
                "title=" + title.orElse(null) +
                ", vendor=" + vendor.orElse(null) +
                ", version=" + version.orElse(null) +
                "]";
        }
    }
    record Info(Version spec, Version impl) {}

    static Info getInfo(String pkgName) {
        return getInfo(Package.getPackage(pkgName));
    }

    static Info getInfo(Class<?> clazz) {
        return getInfo(clazz.getPackage());
    }

    static Info getInfo(Package pkg) {
        return new Info(
            new Version(
                Optional.ofNullable(pkg.getSpecificationTitle()),
                Optional.ofNullable(pkg.getSpecificationVendor()),
                Optional.ofNullable(pkg.getSpecificationVersion())
            ),
            new Version(
                Optional.ofNullable(pkg.getImplementationTitle()),
                Optional.ofNullable(pkg.getImplementationVendor()),
                Optional.ofNullable(pkg.getImplementationVersion())
            )
        );
    }
}
