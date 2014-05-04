/**
 * Created with IntelliJ IDEA.
 * User: andyrao
 * Date: 13-12-5
 * Time: 上午10:30
 * To change this template use File | Settings | File Templates.
 */
package com.gw.search


import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}
import spray.routing.SimpleRoutingApp
import ExecutionContext.Implicits.global
import com.gw.search.SearchMessage.{ClientQuery, StatQuery, DbQuery, NewsQuery}
import spray.json.DefaultJsonProtocol
import ResponseConstruct._
import org.elasticsearch.action.search.{MultiSearchResponse, SearchResponse}
import com.typesafe.config.ConfigFactory


// !!! IMPORTANT, else `convertTo` and `toJson` won't work correctly

import spray.httpx.SprayJsonSupport._
import ResponseJsonProtocol._
import ClientJsonProtocol._


case class FinalResponse(summaryLength: Int, searchResponse: SearchResponse)

object SearchSpray extends App with SimpleRoutingApp {

  val conf = ConfigFactory.load()
  implicit val system = ActorSystem("my-system")

  val esConnectionActor = system.actorOf(Props(classOf[EsConnectionActor], conf), "esConnectionActor")

  val newsActor = system.actorOf(Props(classOf[NewsActor], esConnectionActor, conf), "newsActor")

  val dbActor = system.actorOf(Props(classOf[DbActor], esConnectionActor, conf), "DbActor")

  val statActor = system.actorOf(Props(classOf[StatActor], esConnectionActor), "statActor")

  val clientSearchActor = system.actorOf(Props(classOf[ClientSearchActor], esConnectionActor, conf), "clientSearchActor")

  val route = path("api" / "sr" / "news") {
    parameters('query, 'searchBy ? "content", 'dCategory.?, 'pageSize.as[Int] ? 20, 'pageNumber.as[Int] ? 1, 'sortBy ? "time", 'summaryLength ? 100) {
      (query, searchBy, dCategory, pageSize, pageNumber, sortBy, summaryLength) =>
        implicit val timeout = Timeout(5 seconds)
        //val future: Future[SrResponse] = (newsActor ? NewsQuery(query, searchBy, dCategory, pageSize, pageNumber, sortBy)).mapTo[String].map(ResponseConstruct.newsResponse)
        val future = (newsActor ? NewsQuery(query, searchBy, dCategory, pageSize, pageNumber, sortBy)).mapTo[SearchResponse]
        onComplete(future) {
          case Success(srResponse) => {

            complete(FinalResponse(summaryLength, srResponse))
          }
          case Failure(exception) => complete(s"An error occurred: ${exception.getMessage}")
        }
    }
  } ~ path("api" / "sr" / "db") {
    parameters('query, 'pageSize.as[Int] ? 20, 'pageNumber.as[Int] ? 1, '_type.?) {
      (query, pageSize, pageNumber, _type) =>
        implicit val timeout = Timeout(5 seconds)
        val future = (dbActor ? DbQuery(query, pageSize, pageNumber, _type)).mapTo[SearchResponse]
        onComplete(future) {
          case Success(srResponse) => {

            complete(FinalResponse(0, srResponse))
          }
          case Failure(exception) => complete(s"An error occurred: ${exception.getMessage}")
        }


    }
  } ~ path("api" / "sr" / "stat") {
    parameters('query, 'pageSize.as[Int] ? 20, 'pageNumber.as[Int] ? 1, 'Category.?) {
      (query, pageSize, pageNumber, Category) =>
        implicit val timeout = Timeout(5 seconds)
        val future: Future[SearchResponse] = (statActor ? StatQuery(query, pageSize, pageNumber, Category)).mapTo[SearchResponse]
        onComplete(future) {
          case Success(srResponse) => {

            complete(FinalResponse(0, srResponse))
          }
          case Failure(exception) => complete(s"An error occurred: ${exception.getMessage}")
        }

    }
  } ~ path("api" / "sr" / "character") {
    parameters('query, 'pageSize.as[Int] ? 20, 'pageNumber.as[Int] ? 1, 'Category.?) {
      (query, pageSize, pageNumber, Category) =>
        implicit val timeout = Timeout(5 seconds)
        val future: Future[SearchResponse] = (statActor ? StatQuery(query, pageSize, pageNumber, Category)).mapTo[SearchResponse]
        onComplete(future) {
          case Success(srResponse) => {

            complete(FinalResponse(0, srResponse))
          }
          case Failure(exception) => complete(s"An error occurred: ${exception.getMessage}")
        }

    }
  }~ path("api" / "tip" / Rest) {
    queryTypes =>
      parameters('query, 'size.as[Int] ? 5, 'pageNumber.as[Int] ? 1) {
        (query, size, pageNumber) =>
          implicit val timeout = Timeout(5 seconds)
          val future: Future[MultiSearchResponse] = (clientSearchActor ? ClientQuery(queryTypes, query, size, pageNumber)).mapTo[MultiSearchResponse]
          onComplete(future) {
            case Success(srResponse) => {

              complete(srResponse)
            }
            case Failure(exception) => complete(s"An error occurred: ${exception.getMessage}")
          }
//          complete(s"queryTypes: $queryTypes")




      }
  }
  val portNumber = conf.getInt("com.gw.web.rest.port")

  startServer(interface = "0.0.0.0", port = portNumber)(route)

}



