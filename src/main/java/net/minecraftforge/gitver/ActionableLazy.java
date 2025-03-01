/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gitver;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

// TODO [Utils] Move this to some general utilities project
/**
 * A lazy value that can be modified after creation. When modifications are requested, its state is reset and
 * modifications are applied at the next invocation of {@link #get()}.
 *
 * @param <T> The type of the value
 */
class ActionableLazy<T> implements Supplier<T> {
    private boolean reload = true;
    private Supplier<T> supplier;
    private @Nullable T value;

    public static <T> ActionableLazy<T> of(Supplier<T> supplier) {
        return new ActionableLazy<>(supplier);
    }

    private ActionableLazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    void ifPresent(Consumer<T> action) {
        if (this.value != null)
            action.accept(this.value);
    }

    @Override
    public T get() {
        return this.reload ? this.evaluate() : this.value;
    }

    private T evaluate() {
        this.reload = false;
        return this.value = this.supplier.get();
    }

    void reset() {
        this.reload = true;
        this.value = null;
    }

    void modify(Consumer<T> action) {
        Supplier<T> value = this.value != null ? () -> this.value : this.supplier;
        this.supplier = () -> {
            T t = value.get();
            action.accept(t);
            return t;
        };
        this.reload = true;
    }
}
