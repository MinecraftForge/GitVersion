package net.minecraftforge.gitversion.gradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import com.google.gson.stream.JsonReader;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FirstParam;
import net.minecraftforge.gradleutils.shared.Closures;
import net.minecraftforge.gradleutils.shared.SharedUtil;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.Callable;

final class Util extends SharedUtil {
    private static final Gson GSON = new GsonBuilder()
        .setObjectToNumberStrategy(Util::readNumber)
        .setPrettyPrinting()
        .create();

    private static Number readNumber(JsonReader in) throws IOException {
        try {
            return ToNumberPolicy.LONG_OR_DOUBLE.readNumber(in);
        } catch (Throwable suppressed) {
            try {
                return ToNumberPolicy.BIG_DECIMAL.readNumber(in);
            } catch (Throwable e) {
                IOException throwing = new IOException("Failed to read number from " + in, e);
                throwing.addSuppressed(suppressed);
                throw throwing;
            }
        }
    }

    public static <T> T fromJson(File file, Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return fromJson(stream, classOfT);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    public static <T> T fromJson(byte[] data, Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
        return fromJson(new ByteArrayInputStream(data), classOfT);
    }

    public static <T> T fromJson(InputStream stream, Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
        return GSON.fromJson(new InputStreamReader(stream), classOfT);
    }

    public static <T> T fromJson(String data, Class<T> classOfT) throws JsonSyntaxException, JsonIOException {
        return GSON.fromJson(data, classOfT);
    }
}
