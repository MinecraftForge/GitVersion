/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// TODO [GitVersion] Document
public class GitVersionConfig implements Serializable {
    public String tagPrefix;
    public List<String> matchFilters;

    public List<String> markerFile;
    public List<String> ignoreFile;
    public List<String> ignoredDirs;

    public GitVersionConfig() {
        this("", List.of(), List.of(), List.of(), List.of());
    }

    public GitVersionConfig(String tagPrefix, Collection<String> matchFilters, Collection<String> markerFile, Collection<String> ignoreFile, Collection<String> ignoredDirs) {
        this.markerFile = toList(markerFile);
        this.ignoreFile = toList(ignoreFile);
        this.ignoredDirs = toList(ignoredDirs);
        this.tagPrefix = tagPrefix;
        this.matchFilters = toList(matchFilters);
    }

    public static <T> List<T> toList(Collection<T> collection) {
        return Collections.unmodifiableList(
            collection instanceof List<T> list ? list : new ArrayList<>(collection)
        );
    }


    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static GitVersionConfig fromJson(File file) throws JsonSyntaxException, JsonIOException {
        try (var stream = new FileInputStream(file)) {
            return fromJson(stream);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }
    public static GitVersionConfig fromJson(byte[] data) throws JsonSyntaxException, JsonIOException {
        return fromJson(new ByteArrayInputStream(data));
    }

    public static GitVersionConfig fromJson(InputStream stream) throws JsonSyntaxException, JsonIOException {
        return GSON.fromJson(new InputStreamReader(stream), GitVersionConfig.class);
    }

    public static GitVersionConfig fromJson(String data) throws JsonSyntaxException, JsonIOException {
        return GSON.fromJson(data, GitVersionConfig.class);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public void toJson(File file) throws IOException {
        try (var writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        }
    }
}
