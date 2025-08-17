package net.minecraftforge.gitversion.gradle.changelog;

import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

/// Generates a changelog for the project based on the Git history using
/// <a href="https://github.com/MinecraftForge/GitVersion">Git Version</a>.
public sealed interface GenerateChangelog extends Task permits GenerateChangelogInternal {
    /// The name for the task, used by the [extension][ChangelogExtension] when registering it.
    String NAME = "createChangelog";

    /// The output file for the changelog.
    ///
    /// @return A property for the output file
    @OutputFile RegularFileProperty getOutputFile();

    /// The tag (or object ID) to start the changelog from.
    ///
    /// @return A property for the start tag
    @Input @Optional Property<String> getStart();

    /// The project URL to use in the changelog.
    ///
    /// Git Version will automatically attempt to find a URL from the repository's remote details if left unspecified.
    ///
    /// @return A property for the project URL
    @Input @Optional Property<String> getProjectUrl();

    /**
     * Whether to build the changelog in Markdown format.
     *
     * @return A property for Markdown formatting
     */
    @Input Property<Boolean> getBuildMarkdown();
}
