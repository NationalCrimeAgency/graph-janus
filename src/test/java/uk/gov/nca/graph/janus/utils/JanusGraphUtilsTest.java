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

package uk.gov.nca.graph.janus.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.junit.Test;
import uk.gov.nca.graph.utils.GraphUtils;

public class JanusGraphUtilsTest {

  private static final String INDEX_NAME = "test_index";
  private static final String PERSON_LABEL = "Person";
  private static final String ANIMAL_LABEL = "Animal";
  private static final String PET_LABEL = "hasPet";
  private static final String NAME_PROPERTY = "name";

  @Test
  public void testAddIndex(){
    JanusGraph graph = createInMemoryJanusGraph();

    assertFalse(graph.openManagement().containsGraphIndex(INDEX_NAME));
    JanusGraphUtils.addIndex(graph, INDEX_NAME, NAME_PROPERTY);
    assertTrue(graph.openManagement().containsGraphIndex(INDEX_NAME));

    graph.close();
  }

  @Test
  public void testListIndices(){
    JanusGraph graph = createInMemoryJanusGraph();

    JanusGraphManagement jgm = graph.openManagement();
    jgm.buildIndex(INDEX_NAME, Vertex.class)
        .addKey(jgm.getOrCreatePropertyKey(NAME_PROPERTY))
        .buildCompositeIndex();

    jgm.commit();

    Set<String> indices = JanusGraphUtils.listIndices(graph, Vertex.class);

    assertEquals(1, indices.size());
    assertTrue(indices.contains(INDEX_NAME));

    graph.close();
  }

  @Test
  public void testCopySchema(){
    JanusGraph graph1 = createInMemoryJanusGraph();
    JanusGraph graph2 = createInMemoryJanusGraph();

    Vertex v1 = graph1.addVertex(T.label, PERSON_LABEL, NAME_PROPERTY, "Alice");
    Vertex v2 = graph1.addVertex(ANIMAL_LABEL);
    v1.addEdge(PET_LABEL, v2);

    GraphUtils.commitGraph(graph1);

    assertTrue(graph1.containsVertexLabel(PERSON_LABEL));
    assertTrue(graph1.containsPropertyKey(NAME_PROPERTY));
    assertTrue(graph1.containsVertexLabel(ANIMAL_LABEL));
    assertTrue(graph1.containsEdgeLabel(PET_LABEL));

    assertFalse(graph2.containsVertexLabel(PERSON_LABEL));
    assertFalse(graph2.containsPropertyKey(NAME_PROPERTY));
    assertFalse(graph2.containsVertexLabel(ANIMAL_LABEL));
    assertFalse(graph2.containsEdgeLabel(PET_LABEL));

    JanusGraphUtils.copySchema(graph1, graph2);

    assertTrue(graph2.containsVertexLabel(PERSON_LABEL));
    assertTrue(graph2.containsPropertyKey(NAME_PROPERTY));
    assertTrue(graph2.containsVertexLabel(ANIMAL_LABEL));
    assertTrue(graph2.containsEdgeLabel(PET_LABEL));

    graph1.close();
    graph2.close();
  }

  private JanusGraph createInMemoryJanusGraph(){
    return JanusGraphFactory.build().set("storage.backend", "inmemory").open();
  }
}
