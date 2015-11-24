package com.kelt.structures.util

import java.net.{URL, URI}

import spray.http.Uri

/**
 * Created by keltonperson on 11/24/15.
 */
trait RichURI {

  implicit class URIExtension(self: URI) {

    def isSecure = self.getScheme == "https"

  }

  implicit class URLExtension(self: URL) {
    def toSprayUri: Uri = Uri(self.toString)

    def isSecure = self.getProtocol == "https"

    def protocolAdjustedPort = self.getPort match {
      case -1 if self.isSecure => 443
      case -1 if !self.isSecure => 80
      case _ => self.getPort
    }
  }
  }
