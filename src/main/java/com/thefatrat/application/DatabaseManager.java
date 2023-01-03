package com.thefatrat.application;

import com.thefatrat.application.util.StringMapping;
import com.thefatrat.database.Query;
import com.thefatrat.database.Tuple;
import com.thefatrat.database.action.DatabaseAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;
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

    public Map<String, StringMapping> getAll(@NotNull Collection<String> settings) {
        List<DatabaseAction<Object[]>> actions = new ArrayList<>(settings.size());
        for (String setting : settings) {
            actions.add(
                DatabaseAction.of(Query.of(GET_SETTINGS, server, component, setting),
                    table -> {
                        if (table.isEmpty()) {
                            return new Object[]{setting, StringMapping.of(null)};
                        }
                        return new Object[]{setting, StringMapping.of(table.getRow(0).get(0))};
                    })
            );
        }
        return DatabaseAction.allOf(actions)
            .query(list -> {
                Map<String, StringMapping> map = new HashMap<>();
                for (Object[] pair : list) {
                    map.put((String) pair[0], (StringMapping) pair[1]);
                }
                return map;
            })
            .join();
    }

    public CompletableFuture<Void> removeSetting(String setting) {
        return DatabaseAction.of(REMOVE_SETTING, server, component, setting).execute();
    }

    public CompletableFuture<Void> removeSetting(String setting, @NotNull String value) {
        return DatabaseAction.of(REMOVE_SETTING_VALUE, server, component, setting, value).execute();
    }

    public CompletableFuture<Void> setSetting(String setting, @NotNull Object value) {
        return DatabaseAction.allOf(
            DatabaseAction.of(REMOVE_SETTING, server, component, setting),
            DatabaseAction.of(ADD_SETTING, server, component, setting, value)
        ).execute();
    }

    public CompletableFuture<Void> addSetting(String setting, String value) {
        return DatabaseAction.of(ADD_SETTING, server, component, setting, value).execute();
    }

    public String getSetting(String setting) {
        return getSettingOrDefault(setting, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSettingOrDefault(String setting, T defaultValue) {
        return DatabaseAction.of(
                Query.of(GET_SETTINGS, server, component, setting),
                table -> {
                    if (table.isEmpty()) {
                        return defaultValue;
                    }
                    String string = table.getRow(0).get(0);
                    if (defaultValue == null) {
                        return (T) string;
                    }
                    return (T) StringMapping.of(string).as(defaultValue.getClass());
                }
            )
            .query()
            .join();
    }

    public List<String> getSettings(String setting) {
        return DatabaseAction.of(
                Query.of(GET_SETTINGS, server, component, setting),
                table -> {
                    List<String> list = new ArrayList<>();
                    for (Tuple t : table.getTuples()) {
                        String s = t.get(0);
                        list.add(s);
                    }
                    return list;
                }
            )
            .query()
            .join();
    }

    public boolean isComponentEnabled() {
        return DatabaseAction.of(GET_COMPONENT_ENABLED, server, component)
            .query(table -> !table.isEmpty() && "1".equals(table.getRow(0).get(0)))
            .join();
    }

    public CompletableFuture<Void> toggleComponent(boolean enable) {
        return DatabaseAction.of(TOGGLE_COMPONENT, server, component, enable, enable).execute();
    }

}
