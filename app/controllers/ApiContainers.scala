package controllers

import org.mellowtech.sdrive._
import play.api.data.Forms._
import play.api.data.Form
import play.api.libs.json._
import org.cvogt.play.json.Jsonx
import org.cvogt.play.json.implicits.optionWithNull


/**
 * @author msvens
 */
object ApiContainers {

  import dao.{Token,Config,Member}

  val SUCESS = "success"
  val ERROR = "error"
  case class ApiResponse(status: String = SUCESS, message: Option[String] = None, content: Option[JsValue])
  
  val tokenForm: Form[Token] = Form(
    mapping(
      "clientId" -> text,
      "token" -> text,
      "refreshToken" -> optional(text)
    )(Token.apply)(Token.unapply)    
  )
  
  //case class Config(id: Int, name: String, rootDir: String, description: Option[String], web: Option[String], email: String)
  
  val configForm: Form[Config] = Form(
    mapping(
        "id" -> default(number, 1000),
        "name" -> default(text, "####"),
        "rootDir" -> default(text,"####"),
        "description" -> optional(text),
        "web" -> optional(text),
        "email" -> default(email, "####@email.com")
        )(Config.apply)(Config.unapply)
  )
  
  val memberForm: Form[Member] = Form(
    mapping(
      "id" -> optional(number),
      "name" -> default(text, "####"),
      "email" -> default(email, "####@email.com"),
      "phone" -> optional(text),
      "apt" -> optional(text),
      "role" -> optional(text)
    )(Member.apply)(Member.unapply)
  )


  //JSON Converters
  implicit val apiResponseFmt = Json.format[ApiResponse]
  implicit val configFmt = Json.format[Config]
  implicit val sShildReferenceFmt = Json.format[SChildReference]
  implicit val sLocationFmt = Json.format[SLocation]
  implicit val sImageMediaMetadataFmt = Json.format[SImageMediaMetadata]
  implicit val sIndexableTextFmt = Json.format[SIndexableText]
  implicit val sLabelsFmt = Json.format[SLabels]
  implicit val sParentReferenceFmt = Json.format[SParentReference]
  implicit val sPermissionFmt = Json.format[SPermission]
  implicit val sPropertyFmt = Json.format[SProperty]
  implicit val sThumbnailFmt = Json.format[SThumbnail]
  implicit val sUserFmt = Json.format[SUser]
  implicit val sVideoMediaMetadataFmt = Json.format[SVideoMediaMetadata]
  implicit val sFileFmt = Jsonx.formatCaseClass[SFile]

}