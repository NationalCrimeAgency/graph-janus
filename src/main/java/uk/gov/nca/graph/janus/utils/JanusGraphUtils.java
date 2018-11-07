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

import java.util.HashSet;
import java.util.Set;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.EdgeLabel;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.VertexLabel;
import org.janusgraph.core.schema.EdgeLabelMaker;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.JanusGraphManagement.IndexBuilder;
import org.janusgraph.core.schema.VertexLabelMaker;
import org.janusgraph.diskstorage.BackendException;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.nca.graph.utils.GraphUtils;

/**
 * Utility class for working with JanusGraph
 */
public class JanusGraphUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(JanusGraphUtils.class);

  private JanusGraphUtils(){
    //Private constructor for utility class
  }

  /**
   * Add a composite Vertex index to the provided JanusGraph.
   * At least one property must be specified.
   *
   * This method may take a while to return as it awaits confirmation that the index has been
   * created.
   */
  public static void addIndex(JanusGraph graph, String indexName, String... properties){
    JanusGraphManagement management = graph.openManagement();

    //Check index doesn't already exist
    if(management.containsGraphIndex(indexName)) {
      LOGGER.info("Index {} already exists", indexName);
      return;
    }

    //Build new index
    LOGGER.info("Building new index {} and committing", indexName);
    IndexBuilder builder = management.buildIndex(indexName, Vertex.class);
    for(String prop : properties){
      builder.addKey(management.getOrCreatePropertyKey(prop));
    }
    builder.buildCompositeIndex();

    //Commit index
    management.commit();

    //Wait for index to be created
    LOGGER.info("Awaiting index creation");
    try {
      ManagementSystem.awaitGraphIndexStatus(graph, indexName).call();
    } catch (InterruptedException e) {
      LOGGER.warn("Exception occurred whilst waiting for graph index to build", e);
      Thread.currentThread().interrupt();
      return;
    }

    LOGGER.info("Done creating index {}", indexName);
  }

  /**
   * Return a list of all indices on the specified class (usually {@link Vertex})
   */
  public static Set<String> listIndices(JanusGraph graph, Class<? extends Element> clazz){
    Set<String> indices = new HashSet<>();

    LOGGER.info("Looping through all indices");
    JanusGraphManagement management = graph.openManagement();
    management.getGraphIndexes(clazz).forEach(i -> indices.add(i.name()));

    LOGGER.info("{} indices found", indices.size());
    return indices;
  }

  /**
   * Drops the provided graph from JanusGraph, or if that fails then reverts to Gremlin
   * to clear the content of the graph.
   */
  public static void clearGraph(JanusGraph graph){
    LOGGER.info("Clearing JanusGraph");
    try {
      JanusGraphFactory.drop(graph);
    } catch (BackendException e) {
      LOGGER.error("Failed to clear JanusGraph, reverting to standard Gremlin approach");
      GraphUtils.clearGraph(graph);
    }
    LOGGER.info("Cleared JanusGraph");
  }

  /**
   * Copies the schema from the source graph to the target graph.
   * This method does not copy indices.
   */
  public static void copySchema(JanusGraph source, JanusGraph target){
    JanusGraphManagement srcMgmt = source.openManagement();
    JanusGraphManagement tgtMgmt = target.openManagement();

    LOGGER.info("Copying vertex labels from source to target");
    for(VertexLabel vl : srcMgmt.getVertexLabels()){
      String name = vl.name();
      LOGGER.debug("Creating vertex label {}", name);
      VertexLabelMaker vlm = tgtMgmt.makeVertexLabel(name);

      if(vl.isPartitioned())
        vlm = vlm.partition();

      if(vl.isStatic())
        vlm = vlm.setStatic();

      vlm.make();
    }

    LOGGER.info("Copying edge labels from source to target");
    for(EdgeLabel el : srcMgmt.getRelationTypes(EdgeLabel.class)){
      String name = el.name();
      LOGGER.debug("Creating edge label {}", name);
      EdgeLabelMaker elm = tgtMgmt.makeEdgeLabel(name)
          .multiplicity(el.multiplicity());

      if(el.isDirected())
        elm = elm.directed();

      elm.make();
    }

    LOGGER.info("Copying property keys from source to target");
    for(PropertyKey pk : srcMgmt.getRelationTypes(PropertyKey.class)){
      String name = pk.name();
      LOGGER.debug("Creating property key {}", name);
      tgtMgmt.makePropertyKey(name)
          .cardinality(pk.cardinality())
          .dataType(pk.dataType())
          .make();
    }

    LOGGER.info("Committing changes to schema in target graph");
    tgtMgmt.commit();

    LOGGER.info("Done copying schema");
  }
}
