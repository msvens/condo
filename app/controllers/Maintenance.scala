package controllers

import play.api._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import dao.TokenDAO
import dao.Token
import play.api.data.Forms._
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import javax.inject.Inject

/**
 * @author msvens
 */
class Maintenance @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport{
  
  val tokenDAO = new TokenDAO
  
  val tokenForm = Form(
    mapping(
      "clientId" -> text,
      "token" -> text,
      "refreshToken" -> optional(text)
    )(Token.apply)(Token.unapply)    
  )
  
  def token = Action.async {r =>
    tokenDAO.getToken.map { x => x match {
        case None => Ok(views.html.index("No token found", "token"))
        case Some(t) => Ok(views.html.index("Your new application "+ t.clientId, "token"))
      } 
    }
  }
  
  def insertToken = Action.async{implicit r =>
    val t = tokenForm.bindFromRequest.get
    tokenDAO.insertOrUpdate(t.token, t.refreshToken).map(_ => Redirect(routes.Maintenance.index))
  }
  
  def index = Action.async {implicit r =>
    tokenDAO.getToken.map(x => Ok(views.html.maintenance.landing(x)))
  }
  
  
  
}