package com.thefatrat.application.handlers;

import com.thefatrat.application.PermissionChecker;
import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import com.thefatrat.application.util.CommandEvent;
import com.thefatrat.application.util.Reply;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class CommandHandler implements Handler<CommandEvent> {

    private final Map<String, ProtectedCommand> map = new HashMap<>();

    public void addListener(String key, BiConsumer<CommandEvent, Reply> listener,
        Predicate<Member> predicate) {
        ProtectedCommand command = new ProtectedCommand(listener, predicate);
        map.put(key.toLowerCase(), command);
    }

    public ProtectedCommand getCommand(String key) {
        return map.get(key.toLowerCase());
    }

    public void addListener(String key, BiConsumer<CommandEvent, Reply> listener) {
        map.put(key.toLowerCase(), new ProtectedCommand(listener));
    }

    public void removeListener(String key) {
        map.remove(key);
    }

    @Override
    public void handle(CommandEvent command, Reply reply) throws BotException {
        ProtectedCommand listener = map.get(command.getCommand());
        if (listener == null) {
            return;
        }
        if (listener.isAllowed(command.getMember())) {
            listener.accept(command, reply);
        } else {
            throw new BotErrorException("You don't have permission to use this command");
        }
    }

    public static class ProtectedCommand {

        private final BiConsumer<CommandEvent, Reply> listener;
        private final List<String> roles = new ArrayList<>();
        private final Predicate<Member> startPredicate;
        private Predicate<Member> predicate = __ -> false;

        private ProtectedCommand(BiConsumer<CommandEvent, Reply> listener,
            Predicate<Member> allowed) {
            this.listener = listener;
            this.startPredicate = allowed;
            setPredicate();
        }

        private ProtectedCommand(BiConsumer<CommandEvent, Reply> listener) {
            this.listener = listener;
            startPredicate = __ -> true;
            setPredicate();
        }

        public void accept(CommandEvent command, Reply reply) {
            listener.accept(command, reply);
        }

        public boolean isAllowed(Member member) {
            return predicate.test(member);
        }

        private void setPredicate() {
            predicate = startPredicate
                .or(PermissionChecker.hasAnyRole(roles.toArray(String[]::new)));
        }

        public void removeAllRoles() {
            this.roles.clear();
            setPredicate();
        }

        public void addRoles(Role... roles) {
            for (Role role : roles) {
                this.roles.add(role.getId());
            }
            setPredicate();
        }

        public void removeRoles(Role... roles) {
            for (Role role : roles) {
                this.roles.remove(role.getId());
            }
            setPredicate();
        }

    }

}
