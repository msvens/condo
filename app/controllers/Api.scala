package controllers

import java.time.LocalDateTime
import javax.inject.Inject

import ApiContainers._
import dao.{Member, ConfigDAO, DirectoryDAO, MemberDAO}
import org.mellowtech.gapi.service.{CellAddr, DriveService, GService}
import play.api._
import play.api.data.Form
import play.api.i18n._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter

import scala.concurrent.Future
import scala.util.{Try, Failure, Success}


trait Api extends Controller {

  //def configDAO: ConfigDAO
  //def directoryDAO: DirectoryDAO
  //def memberDAO: MemberDAO

  def checkRootDir(gd: GDriver, configDAO: ConfigDAO): Unit = if(gd.rootDir.isEmpty) {
    configDAO.config.map {
      case Some(c) => gd._rootDir(Some(c.rootDir))
      case None => gd._rootDir(None)
    }
  }



  def asyncNoRes[A](message: String)(f: => Any)(implicit s: GService[A]): Future[Result] = {
    GService.async(f).map(_ => ok(message)).recover{case e =>
      Logger.info("badrequest", e)
      bad(message)
    }
  }

  def asyncRes[A,B](message: String)(f: => A)(implicit w: Writes[A], s: GService[B]): Future[Result] = {
    GService.async(f).map(a => ok(message, Some(Json.toJson(a)))).recover{case e =>
      Logger.info("badrequest", e)
      bad(message)
    }

  }
  def asyncOrBad[A](message: String, f: Future[A])(mf: A => Result): Future[Result] = {
    f.map(a => mf(a)).recover{case e =>
      Logger.info("badrequest", e)
      bad(message)
    }
  }

  //Helper functions:
  def success(message: String, content: Option[JsValue] = None): JsValue = {
    Json.toJson( ApiResponse(ApiContainers.SUCESS, Some(message), content))
  }

  def error(message: String, content: Option[JsValue] = None): JsValue = {
    Json.toJson( ApiResponse(ApiContainers.ERROR, Some(message), content))
  }

  def succ(message: String, content: Option[JsValue] = None)(f: JsValue => Result): Result = {
    Logger.info(message)
    f(success(message, content))
  }

  def fail(message: String, content: Option[JsValue] = None)(f: JsValue => Result): Result = {
    Logger.info(message)
    f(error(message, content))
  }

  def ok(message: String, content: Option[JsValue] = None): Result =
    succ(message, content)(f => Ok(Json.prettyPrint(f)).as(JSON))


  def bad(message: String, content: Option[JsValue] = None): Result =
    fail(message, content)(f => BadRequest(f))

  def badForm[A](f: Form[A]): Result = {
    val e = f.globalError.map(m => m.message).getOrElse("")
    bad("bad form: "+e)
  }

  def someOrBad[A](message: String, a: Option[A]): Result = a match {
    case Some(_) => ok(message)
    case None => bad("fail: "+message)
  }

  def tryOrBad[A](message: String, a: Try[A]): Result = a match {
    case Success(_) => ok(message)
    case Failure(_) => bad("fail: "+message)
  }
}

class SheetApi @Inject()(configDAO: ConfigDAO, directoryDAO: DirectoryDAO, memberDAO: MemberDAO, val messagesApi: MessagesApi, val gd: GDriver) extends Api with I18nSupport {

  import gd.ss

  checkRootDir(gd, configDAO)

  def exportMembers() = Action.async{implicit r =>
    def memberToCell(m: Member, r: Int, c: Int = 1): Seq[CellAddr] = {
      List(CellAddr(r,c, m.name), CellAddr(r, c+1, m.email), CellAddr(r, c+2, m.phone.getOrElse("")),
        CellAddr(r,c+3, m.apt.getOrElse("")),CellAddr(r, c+4,m.role.getOrElse("")))
    }

    def memToCells(m: Seq[Member]): Seq[CellAddr] = {
      val l = List(CellAddr(1,1,"name"), CellAddr(1,2, "email"), CellAddr(1,3, "phone"), CellAddr(1,4, "apt"), CellAddr(1,5, "role"))
      m.foldLeft(l){(l,mem) =>
        val r = l.last.row + 1
        l ++ memberToCell(mem, r)
      }
    }
    val time = LocalDateTime.now
    val t = "exportmembers-"+time.toString

    val ret = for {
      x <- GService.async(gd.ds.createFile(t, gd.rootDir, DriveService.SHEET_TYPE))(gd.ds)
      mems <- memberDAO.list
      y <- GService.async(ss.createWorksheet(x,"members",Some(5),Some(mems.length+1)))
      z <- GService.async(ss.insertCells(x, "members", memToCells(mems)))
      //z <- GService.async(ss.insertCell(x, "members", CellAddr(1,1, "someval")))
    } yield (ok("export complete"))
    ret.recover{case e => bad("faied export")}

  }

  def createSheet(id: String, title: String) = Action.async(asyncNoRes("create sheet")(ss.createWorksheet(id,title, None, None)))

  def sheets = Action.async(asyncRes("sheets")(ss.sheets))

  def sheet(id: String) = Action.async(asyncRes("sheet")(ss.sheet(id)))

  def worksheet(id: String, title: String) = Action.async(asyncRes("get worksheet")(ss.worksheet(id, title)))

}

class DriveApi @Inject()(configDAO: ConfigDAO, directoryDAO: DirectoryDAO, memberDAO: MemberDAO, val messagesApi: MessagesApi, val gd: GDriver) extends Api with I18nSupport {

  import gd.ds

  checkRootDir(gd, configDAO)

  def exportMembers() = Action.async{implicit r =>
    def memToList(m: Member): Seq[Any] = Seq(m.name, m.email, m.phone.getOrElse(""), m.apt.getOrElse(""), m.role.getOrElse(""))
    val headers =Seq("name", "email", "phone", "apt", "role")
    val time = LocalDateTime.now
    val t = "exportmembers-"+time.toString
    val ret = for {
      mems <- memberDAO.list
      l = headers +: (mems.foldLeft(Seq[Seq[Any]]()){(cells,m) => memToList(m) +: cells})
      wb = org.mellowtech.mpoi.Workbook.simplified(true, l)
      f <- GService.async(ds.uploadSheet(t, gd.rootDir, wb))
      //wb = org.mellowtec
    } yield(ok("export complete"))
    ret.recover{case e => bad("failed export")}

    /*def memberToCell(m: Member, r: Int, c: Int = 1): Seq[CellAddr] = {
      List(CellAddr(r,c, m.name), CellAddr(r, c+1, m.email), CellAddr(r, c+2, m.phone.getOrElse("")),
        CellAddr(r,c+3, m.apt.getOrElse("")),CellAddr(r, c+4,m.role.getOrElse("")))
    }

    def memToCells(m: Seq[Member]): Seq[CellAddr] = {
      val l = List(CellAddr(1,1,"name"), CellAddr(1,2, "email"), CellAddr(1,3, "phone"), CellAddr(1,4, "apt"), CellAddr(1,5, "role"))
      m.foldLeft(l){(l,mem) =>
        val r = l.last.row + 1
        l ++ memberToCell(mem, r)
      }
    }
    val time = LocalDateTime.now
    val t = "exportmembers-"+time.toString

    val ret = for {
      x <- GService.async(gd.ds.createFile(t, gd.rootDir, DriveService.SHEET_TYPE))(gd.ds)
      mems <- memberDAO.list
      y <- GService.async(ss.createWorksheet(x,"members",Some(5),Some(mems.length+1)))
      z <- GService.async(ss.insertCells(x, "members", memToCells(mems)))
    //z <- GService.async(ss.insertCell(x, "members", CellAddr(1,1, "someval")))
    } yield (ok("export complete"))
    ret.recover{case e => bad("faied export")}*/
  }

  def deletefile(id: String) = Action.async {asyncNoRes("delete file")(ds.deleteFile(id))}

  def apiJsRoutes = Action {implicit r =>
    Ok(
      JavaScriptReverseRouter("apiJsRoutes")(
        routes.javascript.DriveApi.deletefile
      )
    ).as("text/javascript")
  }


  def file(id: String) = Action.async {asyncRes("get file")(ds.file(id))}

  def list(id: String) = Action.async { implicit r =>
    asyncRes("list")(ds.list(id))
    /*val f = GService.async(ds.list(id))
    asyncOrBad("list", f){l =>
      ok("list", Some(Json.toJson(l)))
    }*/
  }

  def config = Action.async { implicit r =>
    configDAO.config.map{
      case Some(c) => ok("root", Some(Json.toJson(c)))
      case None => bad("no config")
    }
  }

}


