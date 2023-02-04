package com.thefatrat.eddiejunior.util;

import com.thefatrat.eddiejunior.exceptions.BotErrorException;
import com.thefatrat.eddiejunior.exceptions.BotException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URLUtil {

    public static final Pattern URL_REGEX = Pattern.compile(
        "^https?://" +
            "(?:(?<sub>[a-z\\d\\-A-Z]+(?:\\.[a-z\\d\\-A-Z]+)*)\\.)?" +
            "(?<domain>[a-z\\d\\-A-Z]+)" +
            "\\.(?<tld>[a-z]{2,})" +
            "/?(?:[\\w&=\\-.~:/?#\\[\\]@!$'()*+,;%]*)?$"
    );

    private static final String DOMAIN_REGEX = "^[a-z\\-\\d]+\\.[a-z]+$";

    private static final String IS_HTTPS = "^https://.+$";

    @Contract(pure = true)
    public static boolean isDomain(@NotNull String domain) {
        return domain.matches(DOMAIN_REGEX);
    }

    @Contract(pure = true)
    public static boolean isSafe(@NotNull String url) {
        return url.matches(IS_HTTPS);
    }

    @Nullable
    public static Matcher matchUrl(String url) {
        Matcher matcher = URL_REGEX.matcher(url);
        if (matcher.find()) {
            return matcher;
        }
        return null;
    }

    @NotNull
    public static Message messageFromURL(@NotNull String url, @NotNull Guild guild) throws BotException {
        Matcher matcher = Message.JUMP_URL_PATTERN.matcher(url);
        String[] jump;
        if (matcher.find()) {
            jump = new String[]{matcher.group(1), matcher.group(2), matcher.group(3)};
        } else {
            throw new BotErrorException("Please use a proper jump url for the message");
        }
        if (!guild.getId().equals(jump[0])) {
            throw new BotErrorException("Please reference a message from this server");
        }
        MessageChannel channel = guild.getTextChannelById(jump[1]);
        if (channel == null) {
            throw new BotErrorException("Channel of referenced message not found");
        }
        Message message;
        try {
            message = channel.retrieveMessageById(jump[2]).complete();
            if (message == null) {
                throw new BotErrorException("Referenced message not found");
            }
        } catch (InsufficientPermissionException e) {
            throw new BotErrorException("Requires permission %s", Permission.MESSAGE_HISTORY.getName());
        }
        return message;
    }

}
