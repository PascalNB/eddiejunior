package com.thefatrat.application;

import com.thefatrat.database.Database;
import com.thefatrat.database.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    private void execute(Runnable runnable) {
        new Thread(runnable).start();
    }

    public void removeSetting(String setting) {
        Runnable runnable = () ->
            Database.getInstance().connect()
                .executeStatement(Query.of(REMOVE_SETTING), server, component, setting)
                .close();
        execute(runnable);
    }

    public void removeSetting(String setting, String value) {
        Runnable runnable = () ->
            Database.getInstance().connect()
                .executeStatement(Query.of(REMOVE_SETTING_VALUE), server, component, setting, value)
                .close();
        execute(runnable);
    }

    public void setSetting(String setting, String value) {
        Runnable runnable = () ->
            Database.getInstance().connect()
                .executeStatement(Query.of(REMOVE_SETTING), server, component, setting)
                .executeStatement(Query.of(ADD_SETTING), server, component, setting, value)
                .close();
        execute(runnable);
    }

    public void addSetting(String setting, String value) {
        Runnable runnable = () ->
            Database.getInstance().connect()
                .executeStatement(Query.of(ADD_SETTING), server, component, setting, value)
                .close();
        execute(runnable);
    }

    public String getSetting(String setting) {
        return getSettingOr(setting, null);
    }

    public String getSettingOr(String setting, String defaultValue) {
        AtomicReference<String> result = new AtomicReference<>(defaultValue);
        Database.getInstance().connect()
            .queryStatement(table -> {
                if (table.getRowCount() == 0) {
                    return;
                }
                result.set(table.getRow(0).get(0));
            }, Query.of(GET_SETTINGS), server, component, setting)
            .close();
        return result.get();
    }

    public List<String> getSettings(String setting) {
        List<String> result = new ArrayList<>();
        Database.getInstance().connect()
            .queryStatement(table -> table.forEach(row ->
                    result.add(row.get(0))),
                Query.of(GET_SETTINGS), server, component, setting)
            .close();
        return result;
    }

    public boolean isComponentEnabled() {
        AtomicBoolean result = new AtomicBoolean(false);
        Database.getInstance().connect()
            .queryStatement(table -> table.forEach(row ->
                    result.set("1".equals(row.get(0)))),
                Query.of(GET_COMPONENT_ENABLED), server, component)
            .close();
        return result.get();
    }

    public void toggleComponent(boolean enable) {
        Runnable runnable = () ->
            Database.getInstance().connect()
                .executeStatement(Query.of(TOGGLE_COMPONENT), server, component, enable, enable)
                .close();
        execute(runnable);
    }

}
