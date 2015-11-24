package com.kelt.structures.storage

import akka.actor.Props

import com.kelt.structures.server
import com.kelt.structures.server._

import java.io._

import spray.can.Http.RegisterChunkHandler
import spray.http._


trait StorageServer extends BasicSprayServer {

  import context.dispatcher

  def storage: Storage

  get("/storage/:key/?") { (params, client) =>
    val key = params("key").head
    val read = storage.read(key)
    read onSuccess {
      case is => context.actorOf(Props(new Streamer(client, is)))
    }
    read onFailure {
      case StorageNotFoundException(key) =>
        client ! HttpResponse(404)
    }
  }

  def setupStream: (InputStream, OutputStream) = {
    val in = new PipedInputStream(4096)
    val out = new PipedOutputStream(in)
    (in, out)
  }

  post("/storage/:key/?") { (params, client, body) =>
    val key = params("key").head
    body match {
      case SingleRequestBody(req) =>
        val (in, out) = setupStream
        val parts = req.asPartStream()
        val handler = context.actorOf(Props(new server.ServerToSourceUploader(sender, parts.head.asInstanceOf[ChunkedRequestStart], out)))
        parts.tail.foreach (x => handler ! x)
        storage.write(key, in)

      case ChunkedRequestBody(start) =>
        val (in, out) = setupStream
        val handler = context.actorOf(Props(new server.ServerToSourceUploader(sender, start, out)))
        sender ! RegisterChunkHandler(handler)
        storage.write(key, in)

      case _ => send404(client)
    }
  }

  delete("/storage/:key/?") { (params, client, _) =>
    val key = params("key").head
    storage.delete(key).onSuccess { case _ =>
      client ! HttpResponse(status = 204)
    }
  }

}