package com.thefatrat.eddiejunior.components.impl;

import com.thefatrat.eddiejunior.components.AbstractComponent;
import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.UserRole;
import com.thefatrat.eddiejunior.events.CommandEvent;
import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotWarningException;
import com.thefatrat.eddiejunior.reply.InteractionReply;
import com.thefatrat.eddiejunior.sources.Server;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.sticker.GuildSticker;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Set;

public class StickerComponent extends AbstractComponent {

    public static final Set<String> FILE_MIMES = Set.of("image/png", "image/apng", "image/gif");

    public StickerComponent(@NotNull Server server) {
        super(server, "sticker");

        addCommands(
            new Command("sticker", "add a new sticker")
                .addPermissions(Permission.MANAGE_GUILD_EXPRESSIONS)
                .setRequiredUserRole(UserRole.USE)
                .addOptions(
                    new OptionData(OptionType.ATTACHMENT, "image", "image file", true),
                    new OptionData(OptionType.STRING, "name", "The sticker name", true),
                    new OptionData(OptionType.STRING, "description", "The sticker description", true),
                    new OptionData(OptionType.STRING, "tag", "Emoji or tag", true)
                )
                .setAction(this::newSticker)
        );
    }

    private void newSticker(CommandEvent command, InteractionReply reply) {
        if (!(command.getMember().hasPermission(Permission.CREATE_GUILD_EXPRESSIONS)
            || command.getMember().hasPermission(Permission.ADMINISTRATOR))
            || !(getGuild().getSelfMember().hasPermission(Permission.CREATE_GUILD_EXPRESSIONS)
            || getGuild().getSelfMember().hasPermission(Permission.ADMINISTRATOR))) {
            throw new BotErrorException("Insufficient permissions");
        }

        reply.defer();
        reply.hide();

        Message.Attachment attachment = command.get("image").getAsAttachment();
        String name = command.get("name").getAsString();
        String description = command.get("description").getAsString();
        String tag = command.get("tag").getAsString();

        if (!FILE_MIMES.contains(attachment.getContentType())) {
            throw new BotWarningException("Unsupported file type");
        }

        if (attachment.getSize() > 512 * 1024) {
            throw new BotWarningException("File size too large");
        }

        attachment.getProxy()
            .download()
            .thenAccept(inputStream -> {
                FileUpload fileUpload = FileUpload.fromData(inputStream, attachment.getFileName());

                GuildSticker sticker;

                try {
                    sticker = getGuild().createSticker(name, description, fileUpload, tag).complete();
                } catch (Exception e) {
                    try {
                        inputStream.close();
                    } catch (IOException ignore) {
                    }
                    reply.send(new BotErrorException(e.getMessage()));
                    return;
                }

                try {
                    inputStream.close();
                } catch (IOException ignore) {
                }

                reply.ok("Sticker `%s` created", sticker.getName());
                getServer().log(command.getMember().getUser(), "Created sticker `%s`", sticker.getName());
            });
    }

    @Override
    public String getStatus() {
        return "Enabled: " + isEnabled();
    }

}
