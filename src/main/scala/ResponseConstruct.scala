package com.gw.search

import spray.json._

/**
 * Created with IntelliJ IDEA.
 * User: andyrao
 * Date: 13-12-11
 * Time: 下午7:09
 * To change this template use File | Settings | File Templates.
 */

case class Status(error: Int, description: String)

case class Header(totalHits: BigDecimal, newsHits: Long, took: BigDecimal, facets: Seq[JsObject])

case class Data(dataContent: JsObject, dataType: Int, displayType: Int, error: String, score: BigDecimal)

case class SrResponse(status: Status, header: Header, datas: Seq[Data])
object ResponseConstruct {

  def newsResponse(responseJsonString: String) = {
    val jsonAST: JsValue = responseJsonString.asJson
    val jsonObject = jsonAST.asJsObject("Not a jsonObject")
    val jsonFacetsFields: Seq[JsValue] = jsonObject.getFields("facets")
    val jasonFacets = jsonFacetsFields.map {
      jsValue =>
        jsValue.asJsObject("facect jsValue convert failed")

    }

    val hitsFields: Seq[JsValue] = jsonObject.getFields("hits")

    val tookFields: Seq[JsValue] = jsonObject.getFields("took")
    val time = for(JsNumber(took) <- tookFields) yield took

    val totalHit =
      for{
        jsVaule <- hitsFields
        JsNumber(total) <- jsVaule.asJsObject("hits jsValue convert failed").getFields("total")
      } yield {
        total
      }
    val header = Header(totalHit.head, 0, time.head, jasonFacets)
    val status = Status(0, "Success")

    val hitsdata =
      for {
        jsValue <- hitsFields
        JsArray(elements) <- jsValue.asJsObject("hits jsValue convert failed").getFields("hits")
        element <- elements
      } yield {
        val elementJsObject = element.asJsObject("not valid hit")
        val dataType = 0
        val displayType = 0
        val err = "no erro"
        val score: BigDecimal = elementJsObject.getFields("_score")(0) match {
          case JsNumber(score) => score
          case _ => throw new DeserializationException("JsNumber Expected")

        }
        Data(elementJsObject, dataType, displayType, err, score)
      }
    SrResponse(status, header, hitsdata)
  }
}
