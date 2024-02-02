package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.PermissionEntity;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.events.GenericEvent;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.sources.Server;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class NicknameComponent extends AbstractComponent {

    private Pattern pattern = Pattern.compile("[^ -¡¿-ÖØ-öø-ÿ]");
    private String replacement;

    public NicknameComponent(@NotNull Server server) {
        super(server, "nickname");

        replacement = getDatabaseManager().getSettingOrDefault("replacement", "nickname");
        String base64 = getDatabaseManager().getSetting("regex");
        if (base64 != null) {
            String pattern = new String(Base64.getDecoder().decode(base64));
            try {
                this.pattern = Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                e.printStackTrace();
            }
        }

        setComponentCommand(PermissionEntity.RequiredPermission.MANAGE);

        addSubcommands(
            new Command("replacement", "set the replacement nickname")
                .addOptions(new OptionData(OptionType.STRING, "replacement", "replacement", true))
                .setAction(this::setReplacement),

            new Command("regex", "set the regex pattern")
                .addOptions(new OptionData(OptionType.STRING, "regex", "regex", true))
                .setAction(this::setRegex)
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

    private void setRegex(CommandEvent command, InteractionReply reply) {
        String regex = command.get("regex").getAsString();
        try {
            this.pattern = Pattern.compile(regex);
            reply.ok("Set regex pattern to `%s`", regex);
            String base64 = Base64.getEncoder().encodeToString(regex.getBytes());
            getDatabaseManager().setSetting("regex", base64);
            getServer().log(command.getMember().getUser(), "Set nickname regex pattern to `%s`", regex);

        } catch (PatternSyntaxException e) {
            throw new BotWarningException("Invalid regex pattern");
        }
    }

    private void checkNickname(GenericEvent<Member> event, Void __) {
        if (!isEnabled()) {
            return;
        }

        Member member = event.getEntity();
        String nickname = member.getEffectiveName();

        if (getGuild().getSelfMember().canInteract(member) && pattern.matcher(nickname).find()) {
            String replacement;
            if ("username".equals(this.replacement)) {
                replacement = member.getUser().getName();
                if (pattern.matcher(replacement).find()) {
                    replacement = this.replacement;
                }
            } else {
                replacement = this.replacement;
            }
            event.getEntity().modifyNickname(replacement).queue();
        }
    }

    @Override
    public String getStatus() {
        return "Enabled: " + isEnabled() + "\nReplacement: " + replacement + "\nRegex: `" + pattern + '`';
    }

}
