package com.gw.search

import com.sksamuel.elastic4s.{ScoreDefinition, TermFilterDefinition, TermFacetDefinition, QueryDefinition}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder
import org.elasticsearch.index.query.functionscore.factor.FactorBuilder

class MyCustomScoreDefinition extends QueryDefinition {
  private var _query: QueryDefinition = _
  private var _boost: Double = _
  private var _lang: String = _
  private var _script: String = _
  private var _params: Map[String, AnyRef] = _

  def builder = {
    require(_query != null, "must specify query for custom score query")
    import scala.collection.JavaConversions._
    QueryBuilders.customScoreQuery(_query.builder).script(_script).lang(_lang).params(mapAsJavaMap(_params))
  }

  def query(query: QueryDefinition): MyCustomScoreDefinition = {
    this._query = query
    this
  }

  def params(params: Map[String, AnyRef]): MyCustomScoreDefinition = {
    this._params = params
    this
  }

  def boost(b: Double): MyCustomScoreDefinition = {
    _boost = b
    this
  }

  def script(script: String): MyCustomScoreDefinition = {
    _script = script
    this
  }

  def lang(lang: String): MyCustomScoreDefinition = {
    _lang = lang
    this
  }
}

class MyTermFacetDefinition(name: String) extends TermFacetDefinition(name) {
  def facetFilter(termFilterDefinition: TermFilterDefinition): MyTermFacetDefinition = {
    builder.facetFilter(termFilterDefinition.builder)
    this
  }
}

class FactorScoreDefinition(factor: Float) extends ScoreDefinition[FactorScoreDefinition]{
  val builder: ScoreFunctionBuilder = new FactorBuilder().boostFactor(factor)
}
