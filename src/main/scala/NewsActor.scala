package com.gw.search

import akka.actor.{ActorLogging, Actor}
import akka.actor._
import com.gw.search.SearchMessage.{NewsResult, SearchRequest, NewsQuery}
import com.sksamuel.elastic4s.{TermFacetDefinition, TermFilterDefinition, StringQueryDefinition}
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType
import com.sksamuel.elastic4s.ElasticDsl._
import scala.Some
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}


/**
 * Created with IntelliJ IDEA.
 * User: andyrao
 * Date: 13-12-11
 * Time: 下午5:02
 * To change this template use File | Settings | File Templates.
 */
class NewsActor(esConnectionActor: ActorRef, conf: Config) extends Actor with ActorLogging{

  def receive = {
    case NewsQuery(query: String, searchBy: String, dCategory: Option[String], pageSize: Int, pageNumber: Int, sortBy: String) => {

      val startIndex = (pageNumber - 1) * pageSize

      val stringQueryDefinition = query.operator("and").field("nTitle")
      if (searchBy != "title")
        stringQueryDefinition.field("nContent")



      val facetsDefinition = dCategory match {
        case Some(dCategoryStr) =>
          val termFilterDefinition = new TermFilterDefinition("dCategory", dCategoryStr)
         // facet.filter("dCategory").facetFilter(termFilterDefinition)
          new MyTermFacetDefinition("dCategory").facetFilter(termFilterDefinition).field("dCategory").order(ComparatorType.TERM).size(2000)
        case None =>
          facet.terms("dCategory").field("dCategory").order(ComparatorType.TERM).size(2000)

      }

      val script = conf.getString("es.news.script")
      val score =  conf.getDouble("es.news.factor")
      log.info("script: " + script)
      log.info("score: " + score)
      val customScoreDefinition = new MyCustomScoreDefinition
      customScoreDefinition.query(stringQueryDefinition)
      customScoreDefinition.script(script)
      customScoreDefinition.params(Map("factor" -> score.asInstanceOf[AnyRef]))

      val highLightDefinition1 = highlight("nContent").fragmentSize(10000)
      val highLightDefinition2 = highlight("nTitle").fragmentSize(10000)
      val highLightDefinition3 = highlight("Summary").fragmentSize(10000)
      val highLightDefinitionList = List(highLightDefinition1, highLightDefinition2, highLightDefinition3)
      val highLightOption = new HighlightOptionsDefinition().preTags("<em class=\"highlight\">").postTags("</em>")

      val requestDefinition =
        if (sortBy == "time")
          search in "newsinfo" query customScoreDefinition
        else
          search in "newsinfo" query stringQueryDefinition

      val tempSearchDefinition = requestDefinition from startIndex size pageSize facets facetsDefinition highlighting(highLightOption, highLightDefinitionList: _*)

      val finalDefinition = dCategory match{
        case Some(dCategoryStr) =>
          val filterDefinition = new TermFilterDefinition("dCategory", dCategoryStr)
          tempSearchDefinition filter filterDefinition
        case None =>
          tempSearchDefinition

      }

      esConnectionActor forward SearchRequest(finalDefinition)
    }

    case _ => log.warning("unexpected message received in NewsActor")

  }
}
