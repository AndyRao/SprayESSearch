package com.gw.search

import akka.actor.{ActorLogging, Actor, ActorRef}
import com.gw.search.SearchMessage.{DbQuery, SearchRequest}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticDsl, TermFacetDefinition, TermFilterDefinition, StringQueryDefinition}
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType
import scala.Some
import com.typesafe.config.Config

/**
 * Created with IntelliJ IDEA.
 * User: andyrao
 * Date: 13-12-12
 * Time: 上午9:40
 * To change this template use File | Settings | File Templates.
 */
class DbActor(esConnectionActor: ActorRef, conf: Config) extends Actor with ActorLogging {
  def receive = {
    case DbQuery(query: String,pageSize: Int, pageNumber: Int, dbType: Option[String]) => {

      val startIndex = (pageNumber - 1) * pageSize

      val stringQueryDefinition = query.operator("and").field("fPathName").field("displayName")


      val searchIndies = List("edb", "gdb", "cdb")

      val facetsDefinition = dbType match {
        case Some(dbTypeString) =>
          val termFilterDefinition = new TermFilterDefinition("_type", dbTypeString)
          new MyTermFacetDefinition("_type").facetFilter(termFilterDefinition).field("_type").order(ComparatorType.TERM).size(2000)
        case None =>
          facet.terms("_type").field("_type").order(ComparatorType.TERM).size(2000)

      }


      val script = conf.getString("es.edb.script")
      val factor = conf.getDouble("es.edb.factor")
      val customScoreDefinition = new MyCustomScoreDefinition
      customScoreDefinition.query(stringQueryDefinition)
      customScoreDefinition.script(script)
      customScoreDefinition.params(Map("factor" -> factor.asInstanceOf[AnyRef]))

      val highLightDefinition1 = highlight("fPathName").fragmentSize(10000)
      val highLightDefinition2 = highlight("displayName").fragmentSize(10000)
      val highLightDefinitionList = List(highLightDefinition1, highLightDefinition2)
      val highLightOption = new HighlightOptionsDefinition().preTags("<em class=\"highlight\">").postTags("</em>")

      val requestDefinition =
          search in(searchIndies: _*)  query customScoreDefinition from startIndex size pageSize facets facetsDefinition highlighting(highLightOption, highLightDefinitionList: _*)

      val finalDefinition = dbType match{
        case Some(dbTypeString) =>
          val filterDefinition = new TermFilterDefinition("_type", dbTypeString)
          requestDefinition filter filterDefinition
        case None =>
          requestDefinition

      }

      esConnectionActor forward SearchRequest(finalDefinition)
    }

    case _ => log.warning("unexpected message received in NewsActor")

  }
}
