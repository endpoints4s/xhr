package endpoints.xhr

import endpoints.algebra.{Codec, Documentation}
import org.scalajs.dom.XMLHttpRequest

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON requests
  * and decodes JSON responses.
  */
trait JsonEntitiesFromCodec extends Endpoints with endpoints.algebra.JsonEntitiesFromCodec {

  def jsonRequest[A](docs: Documentation)(implicit codec: Codec[String, A]) = (a: A, xhr: XMLHttpRequest) => {
    xhr.setRequestHeader("Content-Type", "application/json")
    codec.encode(a)
  }

  def jsonResponse[A](docs: Documentation)(implicit codec: Codec[String, A]) =
    (xhr: XMLHttpRequest) => codec.decode(xhr.responseText)

}
