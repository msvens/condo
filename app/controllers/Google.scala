package controllers

import play.api._
import play.api.i18n._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import dao.TokenDAO
import javax.inject.Inject
import play.api.libs.ws._
import play.api.Play.current
import scala.concurrent.Await

/**
 * @author msvens
 */
class Google @Inject() (val messagesApi: MessagesApi, ws: WSClient) extends Controller with I18nSupport{
  
  import scala.collection.JavaConverters._
  import play.api.libs.json._
  import java.net.URLEncoder
  
  case class TokenResponse(access_token: String, token_type: String, expires_in: Int,
      id_token: String, refresh_token: Option[String])
          
  val tokenDAO = new TokenDAO
  implicit val tokenReads = Json.reads[TokenResponse]
  
  val redirectURI = Play.current.configuration.getString("google.redirect").get
  val scopes = Play.current.configuration.getString("google.scopes").get
  val clientId = Play.current.configuration.getString("google.clientId").get
  val clientSecret = Play.current.configuration.getString("google.clientSecret").get
  val authURLTemplate = Play.current.configuration.getString("google.authURLTemplate").get
  
  val authUrl: String = authURLTemplate.format(clientId,URLEncoder.encode(redirectURI, "UTF-8"),URLEncoder.encode(scopes, "UTF-8"))
  
  def authToGoogle(force: Option[String]) = Action{implicit r =>
    val url = force match {
      case None => authUrl
      case Some(_) => authUrl + "&approval_prompt=force"
    }
    Logger.info("force: "+force.toString)
    Redirect(url)
  }
  
  def driveAuth = Action.async {implicit r =>
    val code = r.queryString("code").toList(0)
    val url = "https://accounts.google.com/o/oauth2/token"
    val holder = WS.url(url)
    
    val params = Map(
      "code" -> Seq(code),
      "client_id" -> Seq(clientId),
      "client_secret" -> Seq(clientSecret),
      "redirect_uri" -> Seq(redirectURI),
      "grant_type" -> Seq("authorization_code")
    )
    
    holder.post(params).map {response =>
      if(response.status == 200){
        println(response.body)
        response.json.validate[TokenResponse] match {
          case s: JsSuccess[TokenResponse] =>{
            val tr = s.get
            Logger.info("storing token: "+tr.access_token+"and refreshtoken: "+tr.refresh_token)
            tokenDAO.insertOrUpdate(tr.access_token, tr.refresh_token)
            Ok
          }
          case e: JsError => {
            Logger.error(JsError.toJson(e).toString)
            BadGateway
          }
        }
      } else BadGateway
    }
  }
  
}