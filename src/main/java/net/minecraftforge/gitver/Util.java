/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver;

import org.eclipse.jgit.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

// TODO [GitVersion] Optionally document
final class Util {
    @SuppressWarnings("unchecked")
    static <R, E extends Throwable> R sneak(Throwable t) throws E {
        throw (E) t;
    }

    static String[] rsplit(String input, String del) {
        return rsplit(input, del, -1);
    }

    static String[] rsplit(String input, String del, int limit) {
        if (input == null)
            throw new IllegalArgumentException("Input for split cannot be null");

        var list = new ArrayList<String>();
        int count = 0;
        int index;

        String tmp;
        for (tmp = input; (index = tmp.lastIndexOf(del)) != -1 && (limit == -1 || count++ < limit); tmp = tmp.substring(0, index)) {
            list.add(0, tmp.substring(index + del.length()));
        }
        list.add(0, tmp);

        return list.toArray(new String[0]);
    }

    static <T> T make(Supplier<T> t) {
        return t.get();
    }

    static <T> T make(T t, Consumer<T> action) {
        if (t != null) action.accept(t);
        return t;
    }

    static @Nullable String replace(@Nullable String s, UnaryOperator<String> action) {
        return s != null ? action.apply(s) : s;
    }

    static <T> T orElse(T t, Supplier<T> ifNull) {
        return t != null ? t : ifNull.get();
    }

    static String orElse(String s, Supplier<String> ifEmptyOrNull) {
        return !StringUtils.isEmptyOrNull(s) ? s : ifEmptyOrNull.get();
    }

    static <C extends Collection<T>, T> C make(C c, T[] array) {
        if (array != null && c != null)
            c.addAll(Arrays.asList(array));

        return c;
    }

    static <C extends Collection<T>, T> C make(C c, Iterable<T> iterable) {
        if (c != null && iterable != null)
            iterable.forEach(c::add);

        return c;
    }

    static <T> boolean contains(T[] c, T t) {
        if (c == null || t == null) return false;

        for (T e : c)
            if (e == t || e.equals(t)) return true;

        return false;
    }

    static <T> boolean contains(Iterable<T> c, T t) {
        if (c == null || t == null) return false;

        for (T e : c)
            if (e == t || e.equals(t)) return true;

        return false;
    }

    static <T> boolean isEmptyOrNull(T[] array) {
        return array == null || array.length == 0;
    }

    static boolean isEmptyOrNull(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
