package com.gw.search

import akka.actor.{ActorRef, ActorLogging, Status, Actor}
import com.gw.search.SearchMessage._
import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.common.settings.ImmutableSettings
import akka.pattern._
import org.elasticsearch.action.ActionResponse
import scala.concurrent.Future
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Created with IntelliJ IDEA.
 * User: andyrao
 * Date: 13-12-11
 * Time: 下午2:05
 * To change this template use File | Settings | File Templates.
 */
class EsConnectionActor(conf: Config) extends Actor with ActorLogging{

  val esClusterNameString = conf.getString("es.cluster.name")
  val esIpPort = conf.getInt("es.cluster.ip_port")
  val esIpAddressString = conf.getString("es.cluster.ip_addresses")
  //  val esIpAddressString = "114.80.158.117"
  val settings = ImmutableSettings.settingsBuilder.put("cluster.name", esClusterNameString).build
  val ipAddresses: Array[(String, Int)] = esIpAddressString.split(",").map(ipAddress => (ipAddress, esIpPort))
  val client = ElasticClient.remote(settings, ipAddresses: _*)
  import context.dispatcher



  def receive = {
    case SearchRequest(searchDefinition) =>
     log.info("message received")
     val realSender = sender
     client execute(searchDefinition) map {searchResponse => Result(realSender, searchResponse)} pipeTo self
    case Result(realSender, searchResponse) =>
      log.info("reply received")
      //realSender ! searchResponse.toString
      realSender ! searchResponse

    case MultiSearchRequest(multiSearchDefinition) =>
      log.info("message received")
      val realSender = sender
      client execute(multiSearchDefinition) map {searchResponse => MultiResult(realSender, searchResponse)} pipeTo self
    case MultiResult(realSender, searchResponse) =>
      log.info("reply received")
      //realSender ! searchResponse.toString
      realSender ! searchResponse

    case akka.actor.Status.Failure(exception) => log.error(exception, "client execute failed, exception occurred")
    case _ => log.warning("unkown message received in EsConnectionActor")
  }
}
