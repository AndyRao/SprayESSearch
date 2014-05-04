package com.gw.search

import akka.actor.{ActorLogging, Actor, ActorRef}
import com.gw.search.{MyCustomScoreDefinition, MyTermFacetDefinition}
import com.gw.search.SearchMessage.{StatQuery, SearchRequest, DbQuery}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.TermFilterDefinition
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType
import scala.Some

/**
 * Created with IntelliJ IDEA.
 * User: andyrao
 * Date: 13-12-12
 * Time: 下午3:48
 * To change this template use File | Settings | File Templates.
 */
class StatActor (esConnectionActor: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case StatQuery(query: String,pageSize: Int, pageNumber: Int, category:Option[String]) => {

      val startIndex = (pageNumber - 1) * pageSize

      val stringQueryDefinition = query.operator("and").field("MatPath")

      val facetsDefinition =  category match {
        case Some(categoryString) =>
          val termFilterDefinition = new TermFilterDefinition("Category", categoryString)
          new MyTermFacetDefinition("Category").facetFilter(termFilterDefinition).field("Category").order(ComparatorType.TERM).size(2000)
        case None =>
          facet.terms("Category").field("Category").order(ComparatorType.TERM).size(2000)

      }

      val highLightDefinition1 = highlight("MatPath").fragmentSize(10000)
      val highLightDefinition2 = highlight("MatName").fragmentSize(10000)
      val highLightDefinitionList = List(highLightDefinition1, highLightDefinition2)
      val highLightOption = new HighlightOptionsDefinition().preTags("<em class=\"highlight\">").postTags("</em>")

      val requestDefinition =
        search in "report"  query stringQueryDefinition from startIndex size pageSize facets facetsDefinition highlighting(highLightOption, highLightDefinitionList: _*)

      val finalDefinition = category match{
        case Some(categoryString) =>
          val filterDefinition = new TermFilterDefinition("Category", categoryString)
          requestDefinition filter filterDefinition
        case None =>
          requestDefinition

      }

      esConnectionActor forward SearchRequest(finalDefinition)
    }

    case _ => log.warning("unexpected message received in NewsActor")

  }
}
