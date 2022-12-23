package com.thefatrat.application.util;

import com.thefatrat.application.exceptions.BotErrorException;
import com.thefatrat.application.exceptions.BotException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;

public final class URLUtil {

    private static final String URL_REGEX =
        "^https?://[a-z.\\d\\-A-Z]+\\.[a-z]+(/[\\w&=\\-.~:/?#\\[\\]@!$'()*+,;%]*)?$";

    private static final String DOMAIN_REGEX = "^[a-z\\-\\d]+\\.[a-z]+$";

    private static final String IS_HTTPS = "^https://.+$";

    public static boolean isDomain(String domain) {
        return domain.matches(DOMAIN_REGEX);
    }

    public static boolean isUrl(String url) {
        return url.matches(URL_REGEX);
    }

    public static boolean isSafe(String url) {
        return url.matches(IS_HTTPS);
    }

    public static String isFromDomains(String url, Collection<String> domains) throws URISyntaxException {
        URI uri = new URI(url);
        String host = uri.getHost();
        String[] split = host.split("\\.");
        String domain = String.join(".",
            Arrays.copyOfRange(split, split.length - 2, split.length));
        return domains.contains(domain) ? domain : null;
    }

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
