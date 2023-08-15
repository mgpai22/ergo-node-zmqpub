import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.satergo.ergonnection.ErgoSocket
import com.satergo.ergonnection.records.Peer
import com.satergo.ergonnection.Version

import java.net.{SocketException, URI}
import com.satergo.ergonnection.messages.Inv
import com.satergo.ergonnection.modifiers.ErgoTransaction

import java.util.HexFormat
import java.util.stream.Collectors
import com.satergo.ergonnection.protocol.ProtocolMessage
import com.satergo.ergonnection.modifiers.Header
import com.satergo.ergonnection.messages.ModifierRequest
import io.cruxfinance.nodeApi.api
import io.cruxfinance.types.Config
import org.zeromq.ZContext
import org.zeromq.SocketType

object EventPublish {
  def main(args: Array[String]) = {

    val config = Config.read("config.json")

    val nodeURL = config.nodeURL
    // port must be 9022 for testnet
    val nodePort = config.nodePeersPort

    val nodeApi = new api(nodeURL)

    val nodeURI = new URI(nodeURL)

    val zeroMQIP = config.zmqIP
    val zeroMQPort = config.zmqPort

    val TESTNET_MAGIC = Array[Byte](2, 0, 2, 3)

    var ergoSocket: ErgoSocket = {

      if (nodeApi.getNodeInfo.network == "mainnet") {
        new ErgoSocket(
          nodeURI.getHost,
          nodePort.toInt,
          new Peer(
            nodeApi.getNodeInfo.name,
            nodeApi.getNodeInfo.appVersion,
            Version.parse("5.0.12"),
            ErgoSocket.BASIC_FEATURE_SET
          )
        )
      } else if (nodeApi.getNodeInfo.network == "testnet") {
        new ErgoSocket(
          nodeURI.getHost,
          nodePort.toInt,
          new Peer(
            nodeApi.getNodeInfo.name,
            nodeApi.getNodeInfo.appVersion,
            Version.parse("5.0.12"),
            ErgoSocket.BASIC_FEATURE_SET
          ),
          TESTNET_MAGIC
        )
      } else {
        throw new IllegalArgumentException("Invalid network type")
      }
    }

    ergoSocket.sendHandshake()
    ergoSocket.acceptHandshake()

    val zContext: ZContext = new ZContext()
    val socket = zContext.createSocket(SocketType.PUB)
    socket.bind(f"tcp://${zeroMQIP}:${zeroMQPort}")

    println(f"Peer info: ${ergoSocket.getPeerInfo}");

    while (true) {
      try {
        val msg = ergoSocket.acceptMessage();
        msg match {
          case inv: Inv => {
            inv.typeId() match {
              case ErgoTransaction.TYPE_ID =>
                val txIds = inv
                  .elements()
                  .stream()
                  .map(HexFormat.of().formatHex(_))
                  .toList
                println(
                  f"[${hhmmss()}] Received ID(s) of transaction(s) in Inv message: ${txIds
                      .stream()
                      .collect(Collectors.joining(", "))}"
                );
                txIds.forEach(txId => {
                  socket.sendMore("mempool")
                  socket.send(txId)
                })
                ergoSocket.send(
                  new ModifierRequest(ErgoTransaction.TYPE_ID, inv.elements())
                );
              case Header.TYPE_ID =>
                val headerIds = inv
                  .elements()
                  .stream()
                  .map(HexFormat.of().formatHex(_))
                  .toList
                println(
                  f"[${hhmmss()}] Received ID(s) of headers(s) in Inv message: ${headerIds
                      .stream()
                      .collect(Collectors.joining(", "))}"
                );
                headerIds.forEach(headerId => {
                  socket.sendMore("newBlock")
                  socket.send(headerId)
                })
              case _ =>
            }
          }
          case _: ProtocolMessage =>

        }
      } catch {
        case se: SocketException => {
          println("Socket failed, attempting to reconnect");
          try {
            ergoSocket.close();
          } catch {
            case e: Exception => println(f"Closing socket: ${e.getMessage}");
          }
          ergoSocket = new ErgoSocket(
            nodeURI.getHost,
            nodePort.toInt,
            new Peer(
              "ergoref",
              "ergo-mainnet-5.0.12",
              Version.parse("5.0.12"),
              ErgoSocket.BASIC_FEATURE_SET
            )
          );
          ergoSocket.sendHandshake();
          ergoSocket.acceptHandshake();
        }
        case iae: IllegalArgumentException => {
          println("Received incorrect magic, attempting to reconnect");
          try {
            ergoSocket.close();
          } catch {
            case e: Exception => println(f"Closing socket: ${e.getMessage}");
          }
          ergoSocket = new ErgoSocket(
            nodeURI.getHost,
            nodePort.toInt,
            new Peer(
              "ergoref",
              "ergo-mainnet-5.0.12",
              Version.parse("5.0.12"),
              ErgoSocket.BASIC_FEATURE_SET
            )
          );
          ergoSocket.sendHandshake();
          ergoSocket.acceptHandshake();
        }
        case e: Exception => throw e;
      }
    }
  }

  def hhmmss() = {
    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
  }
}
