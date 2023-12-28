package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.events.GenericEvent;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.sources.Server;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class NicknameComponent extends AbstractComponent {

    private final Pattern pattern = Pattern.compile("[^ -¡¿-ÖØ-öø-ÿ]");
    private String replacement;

    public NicknameComponent(@NotNull Server server) {
        super(server, "nickname");

        replacement = getDatabaseManager().getSettingOrDefault("replacement", "nickname");

        setComponentCommand();

        addSubcommands(
            new Command("replacement", "set the replacement nickname")
                .addOptions(new OptionData(OptionType.STRING, "replacement", "replacement", true))
                .setAction(this::setReplacement)
        );

        server.<Member>getGenericHandler().addListener("member", this::checkNickname);
    }

    private void setReplacement(CommandEvent command, InteractionReply reply) {
        String newReplacement = command.get("replacement").getAsString();
        this.replacement = newReplacement;
        getDatabaseManager().setSetting("replacement", newReplacement);
        getServer().log(command.getMember().getUser(), "Changed nickname replacement to `%s`", newReplacement);
        reply.ok("Nickname changed to `%s`", newReplacement);
    }

    private void checkNickname(GenericEvent<Member> event, Void __) {
        if (!isEnabled()) {
            return;
        }

        Member member = event.getEntity();
        String nickname = member.getEffectiveName();

        if (pattern.matcher(nickname).find() && getGuild().getSelfMember().canInteract(member)) {
            event.getEntity().modifyNickname(this.replacement).queue();
        }
    }

    @Override
    public String getStatus() {
        return "Enabled: " + isEnabled() + "\nReplacement: " + replacement;
    }

}
