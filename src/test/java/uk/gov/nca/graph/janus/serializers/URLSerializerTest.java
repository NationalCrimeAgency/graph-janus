/*
National Crime Agency (c) Crown Copyright 2018

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.gov.nca.graph.janus.serializers;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import org.janusgraph.diskstorage.WriteBuffer;
import org.janusgraph.diskstorage.util.WriteByteBuffer;
import org.junit.Test;

public class URLSerializerTest {

  @Test
  public void test() throws MalformedURLException {
    URL url = new URL("http://www.nationalcrimeagency.gov.uk");

    WriteBuffer wb = new WriteByteBuffer();
    URLSerializer serializer = new URLSerializer();

    serializer.write(wb, url);

    URL url2 = serializer.read(wb.getStaticBuffer().asReadBuffer());

    assertEquals(url, url2);
  }
}
