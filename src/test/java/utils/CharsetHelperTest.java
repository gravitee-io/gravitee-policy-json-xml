/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils;

import io.gravitee.common.http.MediaType;
import io.gravitee.policy.json2xml.transformer.JSONObject;
import io.gravitee.policy.json2xml.transformer.XML;
import io.gravitee.policy.json2xml.utils.CharsetHelper;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CharsetHelperTest {

  @Test
  public void shouldReturnDefaultCharset_noContentType() {
    Charset charset = CharsetHelper.extractFromContentType(null);
    Assert.assertEquals(StandardCharsets.UTF_8, charset);
  }

  @Test
  public void shouldReturnDefaultCharset_contentType_withoutCharset() {
    Charset charset = CharsetHelper.extractFromContentType(
      MediaType.APPLICATION_JSON
    );
    Assert.assertEquals(StandardCharsets.UTF_8, charset);
  }

  @Test
  public void shouldReturnDefaultCharset_contentType_withCharset() {
    Charset charset = CharsetHelper.extractFromContentType(
      MediaType.APPLICATION_XML + ";charset=ISO-8859-1"
    );
    Assert.assertEquals("ISO-8859-1", charset.name());
  }

  @Test
  public void shouldReturnDefaultCharset_contentType_withCharset_Capitalize() {
    Charset charset = CharsetHelper.extractFromContentType(
      MediaType.APPLICATION_XML + "; Charset=ISO-8859-1"
    );
    Assert.assertEquals("ISO-8859-1", charset.name());
  }

  @Test
  public void shouldReturnDefaultCharset_contentType_withCharset_Capitalize_quoted() {
    Charset charset = CharsetHelper.extractFromContentType(
      MediaType.APPLICATION_XML + "; Charset=\"ISO-8859-1\""
    );
    Assert.assertEquals("ISO-8859-1", charset.name());
  }

}
