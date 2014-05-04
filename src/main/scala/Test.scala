/**
 * Created with IntelliJ IDEA.
 * User: andyrao
 * Date: 13-11-29
 * Time: 下午5:34
 * To change this template use File | Settings | File Templates.
 */

import com.mongodb.util.JSON
import com.sksamuel.elastic4s.{StringQueryDefinition, ElasticClient}
import com.sksamuel.elastic4s.ElasticDsl._
import java.lang.Object
import java.util
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.common.settings.{ImmutableSettings, Settings}
import org.elasticsearch.search.SearchHit
import org.json.simple.{JSONArray, JSONObject}
import org.json.simple.parser.JSONParser
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import com.mongodb.casbah.Imports._


import scala.concurrent.ExecutionContext.Implicits.global


object Test extends App {

  val esClusterNameString = "elasticsearch-dzh"
  val esIpPort = 9300
 // val esIpAddressString = "10.15.144.56,10.15.144.58,10.15.144.60"
  val esIpAddressString = "10.15.144.56,10.15.144.58,10.15.144.60"
  val queryString = "大智慧"
  val settings = ImmutableSettings.settingsBuilder.put("cluster.name", esClusterNameString).build
  val ipAddresses = esIpAddressString.split(",").map(ipAddress => (ipAddress, esIpPort))
  val client = ElasticClient.remote(settings, ipAddresses:_*)
  /**
   * string query
   */
 // val stringQuery = new StringQueryDefinition(queryString).operator("and").field("nTitle")

  /**
   * matchall query
   */
  val requestFuture: Future[SearchResponse] = client execute {search in "people" query matchall from 0 size 113992}
  val searchResponse = Await.result(requestFuture, 30 seconds)
  /**
   * Do not forget to close es client,
   * otherwise something wired will happen when you type sbt run in sbt console
   */
  client.close()


  /**
   * connect to mongo
   */
  val mongoClient = MongoClient("10.15.144.60", 27017)
  val db = mongoClient("People")
  val collection = db("RelevancePeople_1")

  val searchHit: Array[SearchHit] = searchResponse.getHits.getHits

  print(searchHit.size)

  searchHit.map{ searchHit =>
    val source: util.Map[String, AnyRef] = searchHit.getSource
    import scala.collection.JavaConversions._
    val scalaSourceMap = source.toMap
    val builder = MongoDBObject.newBuilder
    for((key, value) <- scalaSourceMap){
      builder += key -> value

    }
    collection.insert(builder.result)

}

//  val parser = new JSONParser
//  val `object`= parser.parse(searchResponse.toString)
//  val jsonObject: JSONObject = `object`.asInstanceOf[JSONObject]
//  val hitsObject: JSONObject = jsonObject.get("hits").asInstanceOf[JSONObject]
//  val hitsArray: JSONArray = hitsObject.get("hits").asInstanceOf[JSONArray]
//  val iterator = hitsArray.iterator






  //val a = MongoDBObject("hello" -> "world")
  //print(db.collectionNames)
 // print(searchResponse.toString())



  }

