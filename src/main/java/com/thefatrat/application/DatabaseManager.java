package com.thefatrat.application;

import com.pascalnb.dbwrapper.Mapper;
import com.pascalnb.dbwrapper.Query;
import com.pascalnb.dbwrapper.StringMapper;
import com.pascalnb.dbwrapper.action.DatabaseAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private static final Query GET_COMPONENT_ENABLED = new Query(
        "SELECT enabled FROM component WHERE server_id=? AND component_name=?;");

    private static final Query TOGGLE_COMPONENT = new Query(
        "INSERT INTO component (server_id,component_name,enabled) VALUES(?,?,?) " +
            "ON DUPLICATE KEY UPDATE enabled=?;");

    private static final Query GET_SETTINGS = new Query(
        "SELECT value FROM setting WHERE server_id=? AND component_name=? AND name=?;");

    private static final Query ADD_SETTING = new Query(
        "INSERT INTO setting (server_id,component_name,name,value) VALUES(?,?,?,?);");

    private static final Query REMOVE_SETTING = new Query(
        "DELETE FROM setting WHERE server_id=? AND component_name=? AND name=?;");

    private static final Query REMOVE_SETTING_VALUE = new Query(
        "DELETE FROM setting WHERE server_id=? AND component_name=? AND name=? AND value=?;");

    private final String server;
    private final String component;

    public DatabaseManager(String server, String component) {
        this.server = server;
        this.component = component;
    }

    public Map<String, StringMapper> getAll(@NotNull Collection<String> settings) {
        List<DatabaseAction<Object[]>> actions = new ArrayList<>(settings.size());
        for (String setting : settings) {
            actions.add(
                DatabaseAction.of(GET_SETTINGS.withArgs(server, component, setting),
                    table -> {
                        if (table.isEmpty()) {
                            return new Object[]{setting, new StringMapper(null)};
                        }
                        return new Object[]{setting, new StringMapper(table.getRow(0).get(0))};
                    })
            );
        }
        return DatabaseAction.allOf(actions)
            .query(list -> {
                Map<String, StringMapper> map = new HashMap<>();
                for (Object[] pair : list) {
                    map.put((String) pair[0], (StringMapper) pair[1]);
                }
                return map;
            })
            .join();
    }

    public CompletableFuture<Void> removeSetting(String setting) {
        return DatabaseAction.of(REMOVE_SETTING.withArgs(server, component, setting)).execute();
    }

    public CompletableFuture<Void> removeSetting(String setting, @NotNull String value) {
        return DatabaseAction.of(REMOVE_SETTING_VALUE.withArgs(server, component, setting, value)).execute();
    }

    public CompletableFuture<Void> setSetting(String setting, @NotNull Object value) {
        return DatabaseAction.allOf(
            DatabaseAction.of(REMOVE_SETTING.withArgs(server, component, setting)),
            DatabaseAction.of(ADD_SETTING.withArgs(server, component, setting, value))
        ).execute();
    }

    public CompletableFuture<Void> addSetting(String setting, String value) {
        return DatabaseAction.of(ADD_SETTING.withArgs(server, component, setting, value)).execute();
    }

    public String getSetting(String setting) {
        return DatabaseAction.of(GET_SETTINGS.withArgs(server, component, setting))
            .query(Mapper.stringValue())
            .join();
    }

    @SuppressWarnings("unchecked")
    public <T> T getSettingOrDefault(String setting, T defaultValue) {
        return (T) DatabaseAction.of(GET_SETTINGS.withArgs(server, component, setting))
            .query(Mapper.toPrimitive(defaultValue.getClass()).orDefault(defaultValue))
            .join();
    }

    public List<String> getSettings(String setting) {
        return DatabaseAction.of(
                GET_SETTINGS.withArgs(server, component, setting),
                Mapper.stringList()
            )
            .query()
            .join();
    }

    public boolean isComponentEnabled() {
        return DatabaseAction.of(GET_COMPONENT_ENABLED.withArgs(server, component))
            .query(table -> !table.isEmpty() && "1".equals(table.getRow(0).get(0)))
            .join();
    }

    public CompletableFuture<Void> toggleComponent(boolean enable) {
        return DatabaseAction.of(TOGGLE_COMPONENT.withArgs(server, component, enable, enable)).execute();
    }

}
