package io.cruxfinance.nodeApi

import io.cruxfinance.types.NodeApi
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

class api(nodeUrl: String) {
  def getNodeInfo: NodeApi = {
    val get = new HttpGet(
      s"${nodeUrl}/info"
    )
    val client = HttpClients.custom().build()
    val response = client.execute(get)

    val resp =
      EntityUtils.toString(response.getEntity)

    NodeApi.readJsonString(resp)
  }
}
