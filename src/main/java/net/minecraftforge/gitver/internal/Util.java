/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver.internal;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface Util {
    /**
     * Hacky method to throw a checked exception without declaring it.
     *
     * @param t   The throwable to throw
     * @param <R> The return type
     * @param <E> The throwable type
     * @return Nothing, this method always throws an exception
     * @throws E The throwable
     */
    @SuppressWarnings("unchecked")
    static <R, E extends Throwable> R sneak(Throwable t) throws E {
        throw (E) t;
    }

    static String[] rsplit(String input, String del, int limit) {
        var list = new ArrayList<String>(limit > 0 ? limit : input.split(del).length);
        int count = 0;
        int index;

        String tmp;
        for (tmp = input; (index = tmp.lastIndexOf(del)) != -1 && (limit == -1 || count++ < limit); tmp = tmp.substring(0, index)) {
            list.add(0, tmp.substring(index + del.length()));
        }
        list.add(0, tmp);

        return list.toArray(new String[0]);
    }

    /**
     * Copies a list using {@link ArrayList#ArrayList(Collection)}, then applies the given consumer to the new list.
     *
     * @param t      The list to copy
     * @param action The action to apply
     * @param <T>    The type of the list's elements
     * @return The new list
     */
    static <T> ArrayList<T> copyList(Collection<T> t, Consumer<? super ArrayList<T>> action) {
        var list = new ArrayList<>(t);
        action.accept(list);
        return list;
    }

    static <T> ArrayList<T> toList(Iterable<T> t) {
        var list = new ArrayList<T>();
        for (var i : t) list.add(i);
        return list;
    }

    @SuppressWarnings("unchecked")
    static <K, V> HashMap<K, V> removePrefix(Map<K, V> map, String prefix) {
        var ret = new HashMap<K, V>(map.size());
        map.forEach((k, v) -> ret.put(
            k instanceof String s && s.startsWith(prefix) ? (K) s.substring(prefix.length()) : k,
            v instanceof String s && s.startsWith(prefix) ? (V) s.substring(prefix.length()) : v
        ));
        return ret;
    }

    /**
     * Counts the number of elements in an iterable beginning from {@code -1}.
     *
     * @param iterable The iterable to count
     * @return The count
     */
    static int count(Iterable<?> iterable) {
        if (iterable instanceof Collection<?> c) return c.size();

        int count = -1;
        for (var i = iterable.iterator(); i.hasNext(); i.next())
            count++;
        return count;
    }

    /**
     * Finds the first element in an iterable.
     *
     * @param iterable The iterable to search
     * @param <T>      The type of element
     * @return The first element, or {@code null} if the iterable is empty
     */
    static <T> @Nullable T findFirst(@Nullable Iterable<T> iterable) {
        if (iterable != null)
            for (var t : iterable) return t;
        return null;
    }

    @FunctionalInterface
    interface CharPredicate { boolean test(char c); }
    static int indexOf(String s, CharPredicate matcher, int orElse) {
        for (var c : s.toCharArray()) {
            if (matcher.test(c)) return s.indexOf(c);
        }

        return orElse;
    }

    /**
     * Finds the first element in an iterable that matches the given predicate.
     *
     * @param iterable The iterable to search
     * @param filter   The filter to apply
     * @param <T>      The type of element
     * @return The first element that matches the filter, or {@code null} if none match
     */
    static <T> @Nullable T findFirst(Iterable<T> iterable, Predicate<T> filter) {
        for (var t : iterable)
            if (filter.test(t)) return t;

        return null;
    }

    static <T> boolean forEach(Iterable<T> iterable, Consumer<T> action) {
        var it = iterable.iterator();
        if (!it.hasNext()) return false;

        it.forEachRemaining(action);
        return true;
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

    static <T> T orElse(T t, Supplier<T> ifEmptyOrNull) {
        return t != null && !t.equals("") ? t : ifEmptyOrNull.get();
    }

    static String[] ensure(String[] array) {
        return array != null ? array : new String[0];
    }

    static <T> Collection<T> ensure(@Nullable Collection<T> collection) {
        return collection != null ? collection : Collections.emptyList();
    }
}
