package udata.directory

import java.util.Properties

import akka.actor.Props

import com.codahale.jerkson.Json.generate

import org.scalatra._

import spray.can.Http.RegisterChunkHandler
import spray.http.HttpHeaders.RawHeader
import spray.http._

import scala.io.Source

import udata.server._


trait DirectoryServer extends BasicSprayServer {

  import RichDirectoryServer._
  import context.dispatcher

  def directory: Directory

  get("/dir/*/?") { (params, client) =>
    val path = params.splatList
    directory.item(path).onSuccess { case x =>
      x match {
        case Some(FileItem(is)) if !path.isEmpty =>
          val contentType = StaticResolver.resolveContentType(path.last)
          context.actorOf(Props(new Streamer(client, is(), contentType)))
        case Some(ChildDirectory(listing)) =>
            val entity = HttpEntity(ContentTypes.`application/json`, generate(listing))
            client ! HttpResponse(status = 200, entity = entity, headers = List(RawHeader("X-Type", "Directory")))
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
    val directoryCheck = directory.item(path)
    val resourcePath = path.mkString("/")
    directoryCheck.onSuccess {
      case Some(ChildDirectory(listing)) =>
        client ! HttpResponse(400, entity = s"${resourcePath} is directory not a file, delete ${resourcePath} first if you desire to make ${resourcePath} no longer a directory")
      case _ => {
        val out = directory.addFile(path)
        body match {
          case SingleRequestBody(req) =>
            val parts = req.asPartStream()
            val handler = context.actorOf(Props(new ServerToSourceUploader(client, parts.head.asInstanceOf[ChunkedRequestStart], out)))
            parts.tail.foreach(x => handler ! x)

          case ChunkedRequestBody(start) =>
            val handler = context.actorOf(Props(new ServerToSourceUploader(client, start, out)))
            s ! RegisterChunkHandler(handler)

          case _ => send404(client)
        }
      }
    }

    directoryCheck.onFailure { case f =>
      client ! HttpResponse(500)
    }

  }

  delete("/dir/*/?") { (params, client, _) =>
    val path = params.splatList
    if(path.isEmpty) {
      client ! HttpResponse(400)
    }
    else {
      directory.delete(path).onSuccess { case _ => client ! HttpResponse(204) }
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