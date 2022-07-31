package com.thefatrat.application.handlers;

import com.thefatrat.application.Command;
import net.dv8tion.jda.api.entities.Member;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CommandHandler implements Handler<Command> {

    private final Map<String, ProtectedCommand> map = new HashMap<>();

    public void addListener(String key, Consumer<Command> listener, Predicate<Member> predicate) {
        ProtectedCommand command = new ProtectedCommand(listener);
        command.setPredicate(predicate);
        map.put(key.toLowerCase(), command);
    }

    public void setPredicate(String key, Predicate<Member> predicate) {
        map.get(key.toLowerCase()).setPredicate(predicate);
    }

    public void addListener(String key, Consumer<Command> listener) {
        map.put(key.toLowerCase(), new ProtectedCommand(listener));
    }

    public void removeListener(String key) {
        map.remove(key);
    }

    @Override
    public boolean handle(Command command) {
        ProtectedCommand listener = map.get(command.command());
        if (listener == null) {
            return false;
        }
        if (listener.isAllowed(command.member())) {
            listener.accept(command);
        }
        return true;
    }

    private static class ProtectedCommand {

        private final Consumer<Command> consumer;
        private Predicate<Member> predicate = (__) -> true;

        private ProtectedCommand(Consumer<Command> consumer) {
            this.consumer = consumer;
        }

        public void accept(Command command) {
            consumer.accept(command);
        }

        public boolean isAllowed(Member member) {
            return predicate.test(member);
        }

        public void setPredicate(Predicate<Member> predicate) {
            this.predicate = predicate;
        }

    }

}
