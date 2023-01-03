package com.thefatrat.application.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings("unused")
public class StringMapping {

    private final String string;

    private StringMapping(@Nullable String string) {
        this.string = string;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T as(@Nullable T type) {
        if (type == null) {
            return null;
        }
        return (T) cast(string, type.getClass());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T as(@Nonnull Class<T> clazz) {
        return (T) cast(string, clazz);
    }

    @Nullable
    public <T> T applyIfNotNull(Function<String, T> mapper) {
        if (string == null) {
            return null;
        }
        return mapper.apply(string);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public <T> T asOrDefault(@Nonnull T fallback) {
        if (string == null) {
            return fallback;
        }
        return (T) cast(string, fallback.getClass());
    }

    @Nullable
    public String asString() {
        return string;
    }

    public int asInt() {
        Objects.requireNonNull(string);
        return (int) cast(string, Integer.TYPE);
    }

    public boolean asBoolean() {
        Objects.requireNonNull(string);
        return (boolean) cast(string, Boolean.TYPE);
    }

    public double asDouble() {
        Objects.requireNonNull(string);
        return (double) cast(string, Double.TYPE);
    }

    public long asLong() {
        Objects.requireNonNull(string);
        return (long) cast(string, Long.TYPE);
    }

    public float asFloat() {
        Objects.requireNonNull(string);
        return (float) cast(string, Float.TYPE);
    }

    public char asChar() {
        Objects.requireNonNull(string);
        return (char) cast(string, Character.TYPE);
    }

    public short asShort() {
        Objects.requireNonNull(string);
        return (short) cast(string, Short.TYPE);
    }

    public boolean isNull() {
        return string == null;
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    public static StringMapping of(Object object) {
        if (object == null) {
            return new StringMapping(null);
        }
        return new StringMapping(object.toString());
    }

    @Contract(value = "null, _ -> null; !null, _ -> !null", pure = true)
    @Nullable
    private static Object cast(@Nullable String string, Class<?> clazz) {
        if (string == null) {
            return null;
        }
        if (clazz == String.class) {
            return string;
        }
        if (clazz == Integer.TYPE || clazz == Integer.class) {
            return Integer.parseInt(string);
        }
        if (clazz == Boolean.TYPE || clazz == Boolean.class) {
            return Boolean.parseBoolean(string);
        }
        if (clazz == Double.TYPE || clazz == Double.class) {
            return Double.parseDouble(string);
        }
        if (clazz == Long.TYPE || clazz == Long.class) {
            return Long.parseLong(string);
        }
        if (clazz == Float.TYPE || clazz == Float.class) {
            return Float.parseFloat(string);
        }
        if (clazz == Character.TYPE || clazz == Character.class) {
            return string.charAt(0);
        }
        if (clazz == Short.TYPE || clazz == Short.class) {
            return Short.parseShort(string);
        }
        return string;
    }

}
