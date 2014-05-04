package com.gw.search

import org.elasticsearch.action.search.SearchResponse
import spray.json._
import org.elasticsearch.search.facet.Facet
import org.elasticsearch.search.facet.terms.TermsFacet
import org.elasticsearch.search.SearchHit
import java.text.SimpleDateFormat
import org.elasticsearch.search.highlight.HighlightField


/**
 * Created with IntelliJ IDEA.
 * User: andyrao
 * Date: 13-12-13
 * Time: 下午4:00
 * To change this template use File | Settings | File Templates.
 */

object ResponseJsonProtocol extends DefaultJsonProtocol {


  implicit object ResponseJsonFormat extends RootJsonFormat[FinalResponse] {
    def facetsJsObejcts(response: SearchResponse): List[JsObject] = {
      import scala.collection.JavaConversions._
      val facetsList: java.util.List[Facet] = response.getFacets.facets

      val facetsScalaList = facetsList.toList
      val facetsJsonObjects: List[JsObject] = facetsScalaList.map {
        facet => facet match {
          case termFacet: TermsFacet =>
            val facetName = termFacet.getName
            val missingCount = termFacet.getMissingCount
            val otherCount = termFacet.getOtherCount
            val totalCount = termFacet.getTotalCount
            val termType = termFacet.getType
            val termEntries = termFacet.getEntries.toList
            val termJsonEntry: List[JsObject] = termEntries.map {
              termEntry => termEntry match {
                case entry: TermsFacet.Entry =>
                  JsObject(
                    "count" -> JsNumber(entry.getCount),
                    "term" -> JsString(entry.getTerm.string)
                  )
              }
            }
            JsObject(
              facetName -> JsObject(
                "total" -> JsNumber(totalCount),
                "missing" -> JsNumber(missingCount),
                "_type" -> JsString(termType),
                "other" -> JsNumber(otherCount),
                "terms" -> JsArray(termJsonEntry)

              )
            )
        }

      }
      facetsJsonObjects
    }

    def convertDateFormat(orignFormat: String) : String = {
      val inputFormat = "yyyyMMddHHmmss"
      val outputFormat = "yyyy-MM-dd HH:mm:ss"
      val inputSdf = new SimpleDateFormat(inputFormat)
      val outputSdf = new SimpleDateFormat(outputFormat)
      val finalDate = outputSdf.format(inputSdf.parse(orignFormat))
      finalDate
    }

    def getHighlightSummary(summaryLength: Int, hit:SearchHit): List[String] = {
      import scala.collection.JavaConversions._
      val highlightMap: Map[String, HighlightField] = hit.getHighlightFields.toMap
      val contentOpt: Option[HighlightField] = highlightMap.get("nContent")
      val finalStringOpt: Option[List[String]] = contentOpt map {
        highlightField => highlightField.fragments.toList.map{
        text => Util.getHighlightSummary(summaryLength,text.string)
        }
      }

      lazy val finalString1 = hit.getSource.toMap.get("Summary") match {
        case Some(value) =>
          val summaryString = value.asInstanceOf[String]
          val stringLength = summaryString.length
          if (stringLength == 0) {
            ""
          }
          else {
            val minLength: Int = if (stringLength < summaryLength) stringLength else summaryLength
            val translateString: String = summaryString.substring(0, minLength - 1).replace(12288.asInstanceOf[Char], ' ').trim
            translateString

          }
        case None => "This can not happen"
      }
      finalStringOpt getOrElse List(finalString1)


    }


    def dataJsObjects(response: FinalResponse): List[JsObject] = {
      import scala.collection.JavaConversions._
      val hits: Array[SearchHit] = response.searchResponse.getHits.hits
      val hitJson: List[JsObject] = hits.map {
        hit =>

          val javaSourceMap = hit.getSource
          if(hit.`type` == "newsinfo"){
            val date = javaSourceMap.get("pDate").asInstanceOf[String]
            javaSourceMap.update("pDate", convertDateFormat(date))

            javaSourceMap.put("reducedSummary",getHighlightSummary(response.summaryLength, hit).head )
          }
          val dataType = hit.`type` match {
            case "newsinfo" => 0
            case "edb" | "gdb" | "cdb" => 1
            case "report" => 2
          }
          val sourceMap = javaSourceMap.toMap.map {
            case (fieldName, fieldValue) => fieldValue match {
              case value: String => (fieldName, JsString(value))
              case value1: java.lang.Double => (fieldName, JsNumber(value1))
              case value2: java.lang.Integer => (fieldName, JsNumber(value2))
              case value3: java.lang.Long => (fieldName, JsNumber(value3))
              case _ => (fieldName, JsString("null"))
            }
          }

          val highlightJsValue = hit.getHighlightFields.toMap.map {
            case (name, field) =>
              val temp: List[JsString] = field.getFragments.map(value => JsString(value.toString)).toList
              (name, JsArray(temp))
          }

          JsObject(
            "dataContent" ->
              JsObject(
                "_type" -> JsString(hit.`type`),
                "_source" -> JsObject(sourceMap),
                "_id" -> JsString(hit.getId),
                "_index" -> JsString(hit.getIndex),
                "_score" -> JsNumber(hit.getScore),
                "highlight" -> JsObject(highlightJsValue)
              ),
            "dataType" -> JsNumber(dataType),
            "displayType" -> JsNumber(0),
            "score" -> JsNumber(hit.getScore)
          )
      }.toList

      hitJson


    }

    def write(response: FinalResponse) = {
      JsObject(
        "Status" -> JsObject(
          "error" -> JsNumber(0),
          "description" -> JsString("Success")
        ),
        "header" -> JsObject(
          "totalHits" -> JsNumber(response.searchResponse.getHits.getTotalHits),
          "newsHits" -> JsNumber(0),
          "took" -> JsNumber(response.searchResponse.getTook.getMillis),
          "facets" -> JsArray(facetsJsObejcts(response.searchResponse))

        ),
        "data" -> JsArray(dataJsObjects(response))

      )
    }

    def read(vale: JsValue) = {
      FinalResponse(0, new SearchResponse())
    }
  }

}


