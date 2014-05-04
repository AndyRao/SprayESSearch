package com.gw.search

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import akka.actor.ActorRef
import org.elasticsearch.action.search.{MultiSearchResponse, SearchResponse}

object SearchMessage {

  case class SearchRequest(searchDefinition: SearchDefinition)

  case class MultiSearchRequest(multiSearchDefinition :MultiSearchDefinition)

  case class Result(realSender: ActorRef, searchResponse: SearchResponse)

  case class MultiResult(realSender: ActorRef, searchResponse: MultiSearchResponse)

  case class NewsQuery(query: String, searchBy: String, dCategory: Option[String], pageSize: Int, pageNumber: Int, sortBy: String)

  case class NewsResult(result: String)

  case class DbQuery(query: String, pageSize: Int, pageNumber: Int, dbType:Option[String])

  case class StatQuery(query: String, pageSize: Int, pageNumber: Int, category:Option[String])

  case class ClientQuery(queryTypes: String, query: String, size: Int, pageNumber: Int)

}