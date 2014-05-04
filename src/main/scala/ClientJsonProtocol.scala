package com.gw.search
import java.util


import org.elasticsearch.action.search.{MultiSearchResponse, SearchResponse}
import org.elasticsearch.search.SearchHit
import spray.json._

/**
 * Created with IntelliJ IDEA.
 * User: andyrao
 * Date: 14-1-8
 * Time: 下午3:40
 * To change this template use File | Settings | File Templates.
 */
object ClientJsonProtocol extends DefaultJsonProtocol {

  implicit object ClientResponseJsonFormat extends RootJsonFormat[MultiSearchResponse] {

    def dataJsObjects(multiResponse: MultiSearchResponse): List[JsObject] = {
      import scala.collection.JavaConversions._
      val responsesJson: List[JsObject] = multiResponse.getResponses.map {
      item =>
          val response = item.getResponse
          val hits: Array[SearchHit] = response.getHits.hits
          var dataType = 0
          val hitJson: List[JsObject] = hits.map {
            hit =>

              val javaSourceMap = hit.getSource

              dataType = hit.`type` match {
                case "newsinfo" => 0
                case "edb" | "gdb" | "cdb" => 1
                case "report" => 2
                case "people" => 3
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


              var finalSourceMap = sourceMap + ("_score" -> JsNumber(hit.getScore)) + ("_type" ->JsString(hit.getType))

              val highlightJsValue = hit.getHighlightFields.toMap.map {
                case (name, field) =>
                  val temp: List[JsString] = field.getFragments.map(value => JsString(value.toString)).toList
                  finalSourceMap = finalSourceMap + (name -> temp(0))
                  (name, temp(0))
              }

              JsObject(finalSourceMap)


          }.toList

          JsObject(
            "dataContent" -> JsArray(hitJson),
            "dataType" -> JsNumber(dataType),
            "total" -> JsNumber(response.getHits.getTotalHits)
          )
      }.toList

      responsesJson
    }

    def write(response: MultiSearchResponse): JsValue = {
      val timeList = response.getResponses.map{
        item =>
          item.getResponse.getTook.getMillis
      }.toList
      JsObject(
        "Status" -> JsObject(
          "error" -> JsNumber(0),
          "description" -> JsString("Success"),
          "took" -> JsNumber(timeList.max)
        ),

        "data" -> JsArray(dataJsObjects(response))

      )
    }

    def read(json: JsValue): MultiSearchResponse = null
  }

}
