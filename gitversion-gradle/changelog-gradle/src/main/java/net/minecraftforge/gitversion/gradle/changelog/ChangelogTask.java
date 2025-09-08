package net.minecraftforge.gitversion.gradle.changelog;

import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import net.minecraftforge.gradleutils.shared.EnhancedTask;
import org.gradle.api.Project;

interface ChangelogTask extends EnhancedTask<ChangelogProblems> {
    @Override
    default Class<? extends EnhancedPlugin<? super Project>> pluginType() {
        return ChangelogPlugin.class;
    }

    @Override
    default Class<ChangelogProblems> problemsType() {
        return ChangelogProblems.class;
    }
}
