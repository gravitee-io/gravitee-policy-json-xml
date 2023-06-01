package com.graviteesource.policy.json2xml.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.policy.json2xml.utils.CharsetHelper;
import io.gravitee.common.http.MediaType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CharsetHelperTest {

    @Test
    public void shouldReturnDefaultCharset_noContentType() {
        Charset charset = CharsetHelper.extractFromContentType(null);
        assertThat(charset).isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    public void shouldReturnDefaultCharset_contentType_withoutCharset() {
        Charset charset = CharsetHelper.extractFromContentType(MediaType.APPLICATION_JSON);
        assertThat(charset).isEqualTo(StandardCharsets.UTF_8);
    }

    @Test
    public void shouldReturnDefaultCharset_contentType_withCharset() {
        Charset charset = CharsetHelper.extractFromContentType(MediaType.APPLICATION_XML + ";charset=ISO-8859-1");
        assertThat(charset.name()).isEqualTo("ISO-8859-1");
    }

    @Test
    public void shouldReturnDefaultCharset_contentType_withCharset_Capitalize() {
        Charset charset = CharsetHelper.extractFromContentType(MediaType.APPLICATION_XML + "; Charset=ISO-8859-1");
        assertThat(charset.name()).isEqualTo("ISO-8859-1");
    }

    @Test
    public void shouldReturnDefaultCharset_contentType_withCharset_Capitalize_quoted() {
        Charset charset = CharsetHelper.extractFromContentType(MediaType.APPLICATION_XML + "; Charset=\"ISO-8859-1\"");
        assertThat(charset.name()).isEqualTo("ISO-8859-1");
    }
}
