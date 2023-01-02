package com.thefatrat.application;

import com.thefatrat.database.DatabaseAction;
import com.thefatrat.database.Tuple;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private static final String GET_COMPONENT_ENABLED =
        "SELECT enabled FROM component WHERE server_id=? AND component_name=?;";

    private static final String TOGGLE_COMPONENT =
        "INSERT INTO component (server_id,component_name,enabled) VALUES(?,?,?) " +
            "ON DUPLICATE KEY UPDATE enabled=?;";

    private static final String GET_SETTINGS =
        "SELECT value FROM setting WHERE server_id=? AND component_name=? AND name=?;";

    private static final String ADD_SETTING =
        "INSERT INTO setting (server_id,component_name,name,value) VALUES(?,?,?,?);";

    private static final String REMOVE_SETTING =
        "DELETE FROM setting WHERE server_id=? AND component_name=? AND name=?;";

    private static final String REMOVE_SETTING_VALUE =
        "DELETE FROM setting WHERE server_id=? AND component_name=? AND name=? AND value=?;";

    private final String server;
    private final String component;

    public DatabaseManager(String server, String component) {
        this.server = server;
        this.component = component;
    }

    public CompletableFuture<Void> removeSetting(String setting) {
        return new DatabaseAction<>(REMOVE_SETTING, server, component, setting).execute();
    }

    public CompletableFuture<Void> removeSetting(String setting, @NotNull String value) {
        return new DatabaseAction<>(REMOVE_SETTING_VALUE, server, component, setting, value).execute();
    }

    public CompletableFuture<Void> setSetting(String setting, @NotNull Object value) {
        return DatabaseAction.allOf(
            new DatabaseAction<>(REMOVE_SETTING, server, component, setting),
            new DatabaseAction<>(ADD_SETTING, server, component, setting, value)
        );
    }

    public CompletableFuture<Void> addSetting(String setting, String value) {
        return new DatabaseAction<>(ADD_SETTING, server, component, setting, value).execute();
    }

    public String getSetting(String setting) {
        return getSettingOrDefault(setting, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSettingOrDefault(String setting, T defaultValue) {
        return new DatabaseAction<T>(GET_SETTINGS, server, component, setting)
            .queue(table -> {
                if (table.isEmpty()) {
                    return defaultValue;
                }
                String string = table.getRow(0).get(0);
                if (defaultValue == null) {
                    return (T) string;
                }
                return (T) toPrimitive(string, defaultValue.getClass());
            })
            .join();
    }

    private static Object toPrimitive(String string, Class<?> clazz) {
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
        return null;
    }

    public List<String> getSettings(String setting) {
        return new DatabaseAction<List<String>>(GET_SETTINGS, server, component, setting)
            .queue(table -> {
                List<String> list = new ArrayList<>();
                for (Tuple t : table.getTuples()) {
                    String s = t.get(0);
                    list.add(s);
                }
                return list;
            })
            .join();
    }

    public boolean isComponentEnabled() {
        return new DatabaseAction<Boolean>(GET_COMPONENT_ENABLED, server, component)
            .queue(table -> !table.isEmpty() && "1".equals(table.getRow(0).get(0)))
            .join();
    }

    public CompletableFuture<Void> toggleComponent(boolean enable) {
        return new DatabaseAction<>(TOGGLE_COMPONENT, server, component, enable, enable).execute();
    }

}
