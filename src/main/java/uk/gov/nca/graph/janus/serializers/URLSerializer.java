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

import java.net.MalformedURLException;
import java.net.URL;
import org.janusgraph.core.attribute.AttributeSerializer;
import org.janusgraph.diskstorage.ScanBuffer;
import org.janusgraph.diskstorage.WriteBuffer;
import org.janusgraph.graphdb.database.serialize.attribute.StringSerializer;

/**
 * Serialize {@link URL} objects for use with JanusGraph.
 */
public class URLSerializer implements AttributeSerializer<URL> {
    private final StringSerializer serializer = new StringSerializer();

    @Override
    public URL read(ScanBuffer buffer) {
        String str = serializer.read(buffer);
        try{
            return new URL(str);
        }catch (MalformedURLException mue){
            throw new RuntimeException(mue);
        }
    }

    @Override
    public void write(WriteBuffer buffer, URL attribute) {
        serializer.write(buffer, attribute.toString());
    }
}