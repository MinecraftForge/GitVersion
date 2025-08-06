package net.minecraftforge.gitversion.gradle.changelog;

import net.minecraftforge.gradleutils.shared.EnhancedProblems;

import javax.inject.Inject;

abstract class ChangelogProblems extends EnhancedProblems {
    @Inject
    public ChangelogProblems() {
        super(ChangelogPlugin.NAME, ChangelogPlugin.DISPLAY_NAME);
    }
}
