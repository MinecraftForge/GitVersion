> [!WARNING]
> This README is out-of-date, and needs to be updated to reflect the current state of GitVersion and GradleUtils. This
> will be done in due time.

# Git Version

GitVersion is a framework-agnostic implementation of Git-based versioning using the JGit library. While primarily
designed for Java projects within the Minecraft Forge organization, it can be used anywhere as a command-line tool or in
a Gradle project, with a default implementation available in [GradleUtils](https://github.com/MinecraftForge/GradleUtils).

> [!CAUTION]
> **GitVersion is still very early in development!** Nothing is set in stone, and many things may change in the future.
> It is currently a port of features from GradleUtils 2.3 and SharedActions, with some additional utilities added in
> as part of the command-line tool.

## Usage as a Command-Line Tool

GitVersion can be used as a command-line tool to generate version numbers and changelogs. You can learn more about
its usage by running it with the help command:
```bash
java -jar gitversion-0.5.0-fatjar.jar --help
```

An important thing to note about GitVersion is that it is designed to save a config file every time it is run unless the
`--do-not-save` flag is passed. This config file is used to store the tag prefix, match filters, marker file names,
ignore file names, and ignored directories. This is so that CI agents are able to run GitVersion without needing to pass
these arguments manually within their configurations.

## Usage in a Gradle buildscript

As of right now, GitVersion does not have a dedicated plugin for Gradle buildscripts. However, a default implementation
exists within GradleUtils 2.4 and newer. You can find more details on GradleUtils itself, but here is a basic example of
its usage in a Gradle buildscript:

```groovy
gradleutils.version.tagPrefix = '1.21.4-'
gradleutils.version.matchFilter = '1.*.4.*'

version = gradleutils.version.tagOffsetVersion
```