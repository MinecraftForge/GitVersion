package net.minecraftforge.gitversion.gradle;

record GitVersionValueResult(GitVersionExtensionInternal.Output output, String errorOutput, Throwable execFailure) { }
