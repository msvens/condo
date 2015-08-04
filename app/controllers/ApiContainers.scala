package controllers

import play.api.data.Forms._
import play.api.data.Form


/**
 * @author msvens
 */
object ApiContainers {

  import dao.{Token,Config,Member}
  
  val tokenForm = Form(
    mapping(
      "clientId" -> text,
      "token" -> text,
      "refreshToken" -> optional(text)
    )(Token.apply)(Token.unapply)    
  )
  
  //case class Config(id: Int, name: String, rootDir: String, description: Option[String], web: Option[String], email: String)
  
  val configForm = Form(
    mapping(
        "id" -> default(number, 1000),
        "name" -> default(text, "####"),
        "rootDir" -> default(text,"####"),
        "description" -> optional(text),
        "web" -> optional(text),
        "email" -> default(email, "####@email.com")
        )(Config.apply)(Config.unapply)
  )
  
  val memberForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> default(text, "####"),
      "email" -> default(email, "####@email.com"),
      "phone" -> optional(text),
      "apt" -> optional(text),
      "role" -> optional(text)
    )(Member.apply)(Member.unapply)
  )
}