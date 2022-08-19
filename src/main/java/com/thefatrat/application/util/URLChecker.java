package com.thefatrat.application.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

public final class URLChecker {

    private static final String URL_REGEX =
        "^https?://[a-z.\\d\\-A-Z]+\\.[a-z]+(/[\\w&=\\-.~:/?#\\[\\]@!$'()*+,;%]*)?$";

    private static final String DOMAIN_REGEX =
        "^[a-z\\-\\d]+\\.[a-z]+$";

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

}
