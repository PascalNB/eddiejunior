package com.thefatrat.eddiejunior;

import com.pascalnb.dbwrapper.Mapper;
import com.pascalnb.dbwrapper.Query;
import com.pascalnb.dbwrapper.StringMapper;
import com.pascalnb.dbwrapper.action.CompletedAction;
import com.pascalnb.dbwrapper.action.DatabaseAction;
import com.thefatrat.eddiejunior.components.impl.FaqComponent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    public static final Query GET_QUESTIONS = new Query(
        "SELECT * FROM faq WHERE server_id=?;"
    );
    private static final Query REMOVE_QUESTION = new Query(
        "DELETE FROM faq WHERE server_id=? AND q_number=?;"
    );

    public static final Query SET_QUESTION = new Query(
        "INSERT INTO faq (server_id,q_number,value) VALUES(?,?,?) ON DUPLICATE KEY UPDATE value=?;"
    );

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
                    table -> table.isEmpty()
                        ? new Object[]{setting, new StringMapper(null)}
                        : new Object[]{setting, new StringMapper(table.get(0).get(0))}
                )
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
            .complete();
    }

    public CompletedAction<Void> removeSetting(String setting) {
        return DatabaseAction.of(REMOVE_SETTING.withArgs(server, component, setting)).execute();
    }

    public CompletedAction<Void> removeSetting(String setting, @NotNull String value) {
        return DatabaseAction.of(REMOVE_SETTING_VALUE.withArgs(server, component, setting, value)).execute();
    }

    public CompletedAction<Void> setSetting(String setting, @NotNull Object value) {
        return DatabaseAction.allOf(
            DatabaseAction.of(REMOVE_SETTING.withArgs(server, component, setting)),
            DatabaseAction.of(ADD_SETTING.withArgs(server, component, setting, value))
        ).execute();
    }

    public CompletedAction<Void> addSetting(String setting, String value) {
        return DatabaseAction.of(ADD_SETTING.withArgs(server, component, setting, value)).execute();
    }

    public String getSetting(String setting) {
        return DatabaseAction.of(GET_SETTINGS.withArgs(server, component, setting))
            .query(Mapper.stringValue())
            .complete();
    }

    @SuppressWarnings("unchecked")
    public <T> T getSettingOrDefault(String setting, @NotNull T defaultValue) {
        return (T) DatabaseAction.of(GET_SETTINGS.withArgs(server, component, setting))
            .query(Mapper.toPrimitive(defaultValue.getClass()).orDefault(defaultValue))
            .complete();
    }

    public List<String> getSettings(String setting) {
        return DatabaseAction.of(
                GET_SETTINGS.withArgs(server, component, setting),
                Mapper.stringList()
            )
            .query()
            .complete();
    }

    public static boolean isComponentEnabled(String serverId, String componentId) {
        return DatabaseAction.of(GET_COMPONENT_ENABLED.withArgs(serverId, componentId))
            .query(table -> !table.isEmpty() && "1".equals(table.get(0).get(0)))
            .complete();
    }

    public static CompletedAction<Void> toggleComponent(String serverId, String componentId, boolean enable) {
        return DatabaseAction.of(TOGGLE_COMPONENT.withArgs(serverId, componentId, enable, enable)).execute();
    }

    public List<FaqComponent.Question> getQuestions() {
        return DatabaseAction.of(
                GET_QUESTIONS.withArgs(server),
                Mapper.allRows()
            ).query()
            .map(list -> list.stream()
                .map(tuple -> {
                    int id = Integer.parseInt(tuple.get("q_number"));
                    String json = tuple.get("value");
                    return FaqComponent.Question.fromJson(id, json);
                })
                .toList()
            )
            .complete();
    }

    public CompletedAction<Void> removeQuestion(int id) {
        return DatabaseAction.of(
            REMOVE_QUESTION.withArgs(server, id)
        ).execute();
    }

    public CompletedAction<Void> setQuestion(int id, String json) {
        return DatabaseAction.of(
            SET_QUESTION.withArgs(server, id, json, json)
        ).execute();
    }

}
