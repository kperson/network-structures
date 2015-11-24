package com.kelt.structures.directory

import java.util.Properties

import akka.actor.Props

import com.codahale.jerkson.Json._
import com.kelt.structures.server
import com.kelt.structures.server.{Streamer, ChunkedRequestBody, SingleRequestBody, BasicSprayServer}

import spray.can.Http.RegisterChunkHandler
import spray.http.HttpHeaders.RawHeader
import spray.http._
import scala.concurrent.Future

import org.scalatra._

import scala.io.Source


trait DirectoryServer extends BasicSprayServer {

  import RichDirectoryServer._
  import context.dispatcher

  def directory: Directory

  get("/dir/*/?") { (params, client) =>
    val path = params.splatList
    directory.item(path).onSuccess { case x =>
      x match {
        case Some(FileItem(is)) =>
          val contentType = StaticResolver.resolveContentType(path.last)
          context.actorOf(Props(new Streamer(client, is, contentType)))
        case Some(ChildDirectory(dir)) =>
          dir.directoryListing onSuccess { case listing =>
            val entity = HttpEntity(ContentTypes.`application/json`, generate(listing))
            client ! HttpResponse(status = 200, entity = entity, headers = List(RawHeader("X-Type", "Directory")))
          }
        case _=> send404(client)
      }
    }
  }

  post("/dir/*/?") { (params, client, body) =>
    val path = params.splatList
    val s = sender

    if(path.isEmpty) {
      client ! HttpResponse(400)
    }
    directory.directory(path).onSuccess {
      case Some(_) =>
        val pathName = path.mkString("/")
        client ! HttpResponse(400, entity = s"${pathName} is directory not a file, delete ${pathName} first if you desire to make ${pathName} no longer a directory")
      case _ => {
        val uploadDir = path.length match {
          case 1 => Future.successful(directory)
          case _ => directory.makeDirectory(path.take(path.length - 1))
        }
        val uploadHandler = uploadDir.flatMap { x =>
          x.addFile(path.last)
        }
        uploadHandler.onSuccess { case f =>
          body match {
            case SingleRequestBody(req) =>
              val parts = req.asPartStream()
              val handler = context.actorOf(Props(new server.ServerToSourceAsyncUploader(s, parts.head.asInstanceOf[ChunkedRequestStart], f)))
              parts.tail.foreach(x => handler ! x)

            case ChunkedRequestBody(start) =>
              val handler = context.actorOf(Props(new server.ServerToSourceAsyncUploader(s, start, f)))
              sender ! RegisterChunkHandler(handler)

            case _ => send404(client)
          }
        }
      }
    }
  }

  delete("/dir/*/?") { (params, client, _) =>
    val path = params.splatList
    if(path.isEmpty) {
      client ! HttpResponse(400)
    }
    else {
      directory.item(path).flatMap {
        case Some(FileItem(is)) => directory.deleteFile(path).map { _ => HttpResponse(204) }
        case Some(ChildDirectory(dir)) => directory.deleteDirectory(path).map { _ => HttpResponse(204) }
        case _=> Future.successful(HttpResponse(404))
      }.onSuccess {
        case r => client ! r
      }
    }
  }

}

object RichDirectoryServer {

  implicit class MultiParamsExtension(self: MultiParams) {
    def splatList = self("splat").head.split("/").filter(_ != "").toList
  }

}

object StaticResolver {

  val properties = new Properties
  val source = Source.fromURL(getClass.getResource("/mime.properties"))
  properties.load(source.reader)
  def resolveContentType(resourcePath: String) = {
    val extension = properties.get(suffix(resourcePath))
    if (extension != null) Some(extension.toString) else None
  }

  private def suffix(path: String): String = path.reverse.takeWhile(_ != '.').reverse

}