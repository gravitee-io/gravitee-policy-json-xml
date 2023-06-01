package com.graviteesource.policy.json2xml.utils;

import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CharsetHelper {

    private static final Charset UTF_8_CHARSET = StandardCharsets.UTF_8;
    private static final String CHARSET_TAG = "charset=";
    private static final Pattern CHARSET_PATTERN = Pattern.compile(Pattern.quote(CHARSET_TAG), Pattern.CASE_INSENSITIVE);

    public static Charset extractCharset(HttpHeaders httpHeaders) {
        return extractFromContentType(httpHeaders.get(HttpHeaderNames.CONTENT_TYPE));
    }

    /**
     * Extract the charset from media-type as per https://tools.ietf.org/html/rfc7231#section-3.1.1.1
     *
     * @param mediaType
     * @return
     */
    public static Charset extractFromContentType(String mediaType) {
        if (mediaType == null || !CHARSET_PATTERN.matcher(mediaType).find()) {
            return UTF_8_CHARSET;
        }

        String charsetName = mediaType.substring(mediaType.lastIndexOf('=') + 1);
        charsetName = charsetName.replace("\"", "");

        return Charset.isSupported(charsetName) ? Charset.forName(charsetName) : Charset.defaultCharset();
    }
}
