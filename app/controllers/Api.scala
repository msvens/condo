package controllers

import javax.inject.Inject

import controllers.ApiContainers._
import dao.{ConfigDAO, DirectoryDAO, MemberDAO}
import org.mellowtech.sdrive.GApi
import play.api._
import play.api.data.Form
import play.api.i18n._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter

import scala.concurrent.Future
import scala.util.{Try, Failure, Success}

class Api @Inject()(val messagesApi: MessagesApi, val gd: GDriver) extends Controller with I18nSupport {

  //implicit val d: GDrive = gd.driver
  import ApiContainers._
  import gd._
  
  val configDAO = new ConfigDAO
  val directoryDAO = new DirectoryDAO
  val memberDAO = new MemberDAO

  if(gd.rootDir == None) {
    configDAO.config.map(oc => oc match {
      case Some(c) => gd._rootDir(Some(c.rootDir))
      case None => gd._rootDir(None)
    })
  }

  def deletefile(id: String) = Action.async {implicit r =>
    GApi.asyncRetry(GApi.rm(id)).map{u =>
      ok("delete file")
    }.recover{case e => {
        Logger.info("badrequest", e)
        bad("delete file")
      }
    }
  }

  def apiJsRoutes = Action {implicit r =>
    Ok(
      JavaScriptReverseRouter("apiJsRoutes")(
        routes.javascript.Api.deletefile
      )
    ).as("text/javascript")

  }
  def file(id: String) = Action.async {implicit r =>
    asyncOrBad("get file")(GApi.file(id))
  }

  def list(id: String) = Action.async { implicit r =>
    val f = GApi.asyncRetry(GApi.list(id))
    asyncOrBad("list", f){l =>
      ok("list", Some(Json.toJson(l)))
    }
  }

  def config = Action.async { implicit r =>
    configDAO.config.map{
      case Some(c) => ok("root", Some(Json.toJson(c)))
      case None => bad("no config")
    }
  }

  //JSON based api
  /*def deleteFile(id: String) = Action.async {implicit r =>
    for{
      f1 <- GApi.
    }
  }*/

  private def asyncOrBad[A](message: String)(f: => A)(implicit w: Writes[A]): Future[Result] = {
    GApi.asyncRetry(f).map(a => ok(message, Some(Json.toJson(a)))).recover{case e => {
        Logger.info("badrequest", e)
        bad(message)
      }
    }

  }
  private def asyncOrBad[A](message: String, f: Future[A])(mf: A => Result): Future[Result] = {
    f.map(a => mf(a)).recover{case e => {
      Logger.info("badrequest", e)
      bad(message)
    }}
  }

  //Helper functions:
  private def success(message: String, content: Option[JsValue] = None): JsValue = {
    Json.toJson( ApiResponse(ApiContainers.SUCESS, Some(message), content))
  }

  private def error(message: String, content: Option[JsValue] = None): JsValue = {
    Json.toJson( ApiResponse(ApiContainers.ERROR, Some(message), content))
  }

  private def succ(message: String, content: Option[JsValue] = None)(f: JsValue => Result): Result = {
    Logger.info(message)
    f(success(message, content))
  }

  private def fail(message: String, content: Option[JsValue] = None)(f: JsValue => Result): Result = {
    Logger.info(message)
    f(error(message, content))
  }

  private def ok(message: String, content: Option[JsValue] = None): Result =
    succ(message, content)(f => Ok(Json.prettyPrint(f)).as(JSON))


  private def bad(message: String, content: Option[JsValue] = None): Result =
    fail(message, content)(f => BadRequest(f))

  private def badForm[A](f: Form[A]): Result = {
    val e = f.globalError.map(m => m.message).getOrElse("")
    bad("bad form: "+e)
  }

  private def someOrBad[A](message: String, a: Option[A]): Result = a match {
    case Some(_) => ok(message)
    case None => bad("fail: "+message)
  }

  private def tryOrBad[A](message: String, a: Try[A]): Result = a match {
    case Success(_) => ok(message)
    case Failure(_) => bad("fail: "+message)
  }

}

object ApiHelpers {





}
