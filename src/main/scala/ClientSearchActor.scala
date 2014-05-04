package com.gw.search

import akka.actor.{ActorRef, ActorLogging, Actor}
import com.gw.search.{MyCustomScoreDefinition, MyTermFacetDefinition}
import com.gw.search.SearchMessage.{MultiSearchRequest, SearchRequest, ClientQuery}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{FunctionScoreQueryDefinition, TermFilterDefinition}
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType
import com.typesafe.config.Config

/**
 * Created with IntelliJ IDEA.
 * User: andyrao
 * Date: 14-1-8
 * Time: 上午10:41
 * To change this template use File | Settings | File Templates.
 */
class ClientSearchActor(esConnectionActor: ActorRef, conf: Config) extends Actor with ActorLogging {

  def newsSearchDefinitionConstruct(query: String, pageSize: Int, pageNumber: Int): SearchDefinition = {
    val startIndex = (pageNumber - 1) * pageSize

    val stringQueryDefinition = query.operator("and").field("nTitle")

    val script = conf.getString("es.news.script")
    val factor =  conf.getDouble("es.news.factor")
    log.info("script: " + script)
    log.info("score: " + factor)
    val customScoreDefinition = new MyCustomScoreDefinition
    customScoreDefinition.query(stringQueryDefinition)
    customScoreDefinition.script(script)
    customScoreDefinition.params(Map("factor" -> factor.asInstanceOf[AnyRef]))

    val highLightDefinition1 = highlight("nContent").fragmentSize(10000)
    val highLightDefinition2 = highlight("nTitle").fragmentSize(10000)
    val highLightDefinitionList = List(highLightDefinition1, highLightDefinition2)
    val highLightOption = new HighlightOptionsDefinition().preTags("<em class=\"highlight\">").postTags("</em>")

    val requestDefinition =
      search in "newsinfo" query customScoreDefinition from startIndex size pageSize  highlighting(highLightOption, highLightDefinitionList: _*)
    requestDefinition
  }

  def statSearchDefinitionConstruct(query: String, pageSize: Int, pageNumber: Int): SearchDefinition = {
    val startIndex = (pageNumber - 1) * pageSize

    val stringQueryDefinition = query.operator("and").field("MatPath")

    val highLightDefinition1 = highlight("MatPath").fragmentSize(10000)
    val highLightDefinition2 = highlight("MatName").fragmentSize(10000)
    val highLightDefinitionList = List(highLightDefinition1, highLightDefinition2)
    val highLightOption = new HighlightOptionsDefinition().preTags("<em class=\"highlight\">").postTags("</em>")

    val requestDefinition =
      search in "report"  query stringQueryDefinition from startIndex size pageSize highlighting(highLightOption, highLightDefinitionList: _*)

    requestDefinition

  }

  def dbSearchDefinitionConstruct(query: String, pageSize: Int, pageNumber: Int): SearchDefinition = {
    val startIndex = (pageNumber - 1) * pageSize

    val stringQueryDefinition = query.operator("and").field("fPathName").field("displayName")


    val searchIndies = List("edb", "gdb", "cdb")

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
      search in(searchIndies: _*)  query customScoreDefinition from startIndex size pageSize  highlighting(highLightOption, highLightDefinitionList: _*)

    requestDefinition

  }

  def characterSearchDefinitionConstruct(query: String, pageSize: Int, pageNumber: Int): SearchDefinition = {
    val startIndex = (pageNumber - 1) * pageSize

    val stringQueryDefinition = query.operator("and").field("Cname")
                                  .field("display")
                                  .field("ITName")
                                  .field("POSITION")
                                  .field("spelling")
                                  .field("initials")

    val filterDefinition = new TermFilterDefinition("Category", "政府人物")
    val factorScoreDefinition = new FactorScoreDefinition(4.0.toFloat)
    factorScoreDefinition.filter(filterDefinition)
    val functionScoreQueryDefnition = functionScoreQuery(stringQueryDefinition).scorers(factorScoreDefinition)
   
    val highLightDefinition1 = highlight("Cname").fragmentSize(10000)
    val highLightDefinition2 = highlight("display").fragmentSize(10000)
    val highLightDefinition3 = highlight("ITName").fragmentSize(10000)
    val highLightDefinition4 = highlight("POSITION").fragmentSize(10000)
    val highLightDefinitionList = List(highLightDefinition1, highLightDefinition2, highLightDefinition3, highLightDefinition4)
    val highLightOption = new HighlightOptionsDefinition().preTags("<em class=\"highlight\">").postTags("</em>")

    

    val requestDefinition =
      search in "people" query functionScoreQueryDefnition from startIndex size pageSize  highlighting(highLightOption, highLightDefinitionList: _*)

    requestDefinition
  }


  def receive = {
    case ClientQuery(queryTypes: String, query: String, size: Int, pageNumber: Int) => {
      val queryTypesArray = queryTypes.split(",")
      val searchDefinitionList = queryTypesArray.map {
        queryType =>
          queryType match {
            case "news" => newsSearchDefinitionConstruct(query, size, pageNumber)
            case "db" => statSearchDefinitionConstruct(query, size, pageNumber)
            case "stat" => dbSearchDefinitionConstruct(query, size, pageNumber)
            case "character" => characterSearchDefinitionConstruct(query, size, pageNumber)
          }
      }
      val multiSearchDefinition = new MultiSearchDefinition(searchDefinitionList)
      esConnectionActor forward MultiSearchRequest(multiSearchDefinition)

    }


  }
}
