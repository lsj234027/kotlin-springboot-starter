package com.seongenie.example.controller.elasticsearch

import com.google.gson.Gson
import com.seongenie.example.index.infra.ESQueryHelper
import com.seongenie.example.index.infra.ESRequestHelper
import com.seongenie.example.index.infra.SearchRequest
import com.seongenie.example.index.naver.NaverStoreCrawler
import com.seongenie.example.domain.naver.NaverStore
import com.seongenie.example.index.naver.NaverConstants
import com.seongenie.example.service.NaverStoreService
import org.elasticsearch.client.RequestOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/elasticsearch")
class ESController {

  @Autowired
  lateinit var esRequestHelper: ESRequestHelper

  @Autowired
  lateinit var service: NaverStoreService

  @Autowired
  lateinit var esQueryHelper: ESQueryHelper

  /**
   * Crawling elasticsearch stores and indexing result to elasticsearch
   */
  @RequestMapping(value = "/crawl", method = [RequestMethod.GET])
  fun crawlNaverStore(@RequestParam("text") text: String) {
    val client = esRequestHelper.createClient()
    var crawler = NaverStoreCrawler(client)

    var start = 1
    var total = NaverConstants.DISPLAY + 1

    crawler.crawl(text, start) { it ->
      val bulkRequest = esRequestHelper.bulkIndexRequest(it.items!!)
      val response = client.bulk(bulkRequest, RequestOptions.DEFAULT)
    }


  }

  /**
   * Get elasticsearch stores from DB
   */
  @RequestMapping(value ="/stores", method = [RequestMethod.GET])
  fun getNaverStores(): List<NaverStore> {
    return service.getNaverStores()
  }

  /**
   * Search elasticsearch stores
   */
  @RequestMapping(value = "/stores/search", method = [RequestMethod.POST])
  fun searchNaverStore(@RequestBody request: SearchRequest): List<NaverStore> {
    val query = esQueryHelper.simpleMatchQuery(request)
    val searchRequest = esRequestHelper.searchRequest(query, request.size)
    val client = esRequestHelper.createClient()
    var searchResult = listOf<NaverStore>()
    val gson = Gson()
    try {
      val response = client.search(searchRequest, RequestOptions.DEFAULT)
      searchResult = response.hits.hits.map { it ->
//        System.out.println(it.sourceAsString)
        val result = gson.fromJson(it.sourceAsString, NaverStore::class.java)
        result.id = it.id
        result
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      client.close()
    }
    return searchResult
  }
}