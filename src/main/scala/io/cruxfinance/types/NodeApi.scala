package io.cruxfinance.types

import com.google.gson.GsonBuilder

import java.io.{FileWriter, Writer}
import scala.io.Source

case class NodeApi(
    currentTime: Long,
    network: String,
    name: String,
    stateType: String,
    difficulty: Long,
    bestFullHeaderId: String,
    bestHeaderId: String,
    peersCount: Int,
    unconfirmedCount: Int,
    appVersion: String,
    eip37Supported: Boolean,
    stateRoot: String,
    genesisBlockId: String,
    previousFullHeaderId: String,
    fullHeight: Int,
    headersHeight: Int,
    stateVersion: String,
    fullBlocksScore: Double,
    maxPeerHeight: Int,
    launchTime: Long,
    isExplorer: Boolean,
    lastSeenMessageTime: Long,
    eip27Supported: Boolean,
    headersScore: Double,
    parameters: Parameters,
    isMining: Boolean
)

case class Parameters(
    outputCost: Int,
    tokenAccessCost: Int,
    maxBlockCost: Int,
    height: Int,
    maxBlockSize: Int,
    dataInputCost: Int,
    blockVersion: Int,
    inputCost: Int,
    storageFeeFactor: Int,
    minValuePerByte: Int
)

object NodeApi {
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def read(filePath: String): NodeApi = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[NodeApi])
  }

  def readJsonString(jsonString: String): NodeApi = {
    gson.fromJson(jsonString, classOf[NodeApi])
  }

  def toJsonString(json: NodeApi): String = {
    this.gson.toJson(json)
  }

  def write(filePath: String, newConfig: NodeApi): Unit = {
    val writer: Writer = new FileWriter(filePath)
    writer.write(this.gson.toJson(newConfig))
    writer.close()
  }

}
