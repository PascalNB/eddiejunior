package com.thefatrat.application;

import com.thefatrat.database.Database;
import com.thefatrat.database.DatabaseAction;
import com.thefatrat.database.Query;
import com.thefatrat.database.Tuple;

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
        return new DatabaseAction<Void>(REMOVE_SETTING, server, component, setting).execute();
    }

    public CompletableFuture<Void> removeSetting(String setting, String value) {
        return new DatabaseAction<Void>(REMOVE_SETTING_VALUE, server, component, setting, value).execute();
    }

    public CompletableFuture<Void> setSetting(String setting, String value) {
        return new DatabaseAction<Void>(REMOVE_SETTING, server, component, setting).execute()
            .thenRun(() -> Database.getInstance().connect()
                .executeStatement(Query.of(ADD_SETTING, server, component, setting, value))
                .close()
            );
    }

    public CompletableFuture<Void> addSetting(String setting, String value) {
        return new DatabaseAction<Void>(ADD_SETTING, server, component, setting, value).execute();
    }

    public String getSetting(String setting) {
        return getSettingOr(setting, null);
    }

    public String getSettingOr(String setting, Object defaultValue) {
        String defaultString = defaultValue == null ? null : defaultValue.toString();
        return new DatabaseAction<String>(GET_SETTINGS, server, component, setting)
            .queue(table -> {
                if (table.isEmpty()) {
                    return defaultString;
                }
                return table.getRow(0).get(0);
            })
            .join();
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
        return new DatabaseAction<Void>(TOGGLE_COMPONENT, server, component, enable, enable).execute();
    }

}
