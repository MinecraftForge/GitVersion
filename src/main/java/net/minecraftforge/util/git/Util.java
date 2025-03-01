/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.util.git;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Utility class for various helper methods. */
final class Util {
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
    static <T> @Nullable T findFirst(Iterable<T> iterable) {
        for (var t : iterable) return t;
        return null;
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
}
