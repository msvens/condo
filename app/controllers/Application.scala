package controllers

import play.api._
import play.api.mvc._
import play.api.i18n._
import javax.inject.Inject
import org.mellowtech.sdrive.GApi
import scala.util.{Success,Failure}
import org.mellowtech.sdrive.GDrive
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import dao.{ConfigDAO, DirectoryDAO, MemberDAO}

class Application @Inject() (val messagesApi: MessagesApi, val gd: GDriver) extends Controller with I18nSupport {

  //implicit val d: GDrive = gd.driver
  import gd._
  import ApiContainers._
  
  val configDAO = new ConfigDAO
  val directoryDAO = new DirectoryDAO
  val memberDAO = new MemberDAO
  
  def index = Action {implicit r => 
    val m = messagesApi.messages
    m foreach {case (key, value) => println (key + "-->" + value.size)}
    Ok(views.html.index("Your new application is ready.", "menu.home"))
  }
  
  def members = Action.async{implicit r =>
    for{
      l <- memberDAO.list
    } yield Ok(views.html.members(l, memberForm))
    //val l = memberDAO.list
    //Ok(views.html.members(List(), memberForm))
  }
  
  def addMember = Action.async{implicit r =>
    val m = memberForm.bindFromRequest.get
    for{
      i <- memberDAO.insert(m)
    } yield Redirect(routes.Application.members)
    //Ok(views.html.members(List(), memberForm))
  }
  
  /************Admin stuff....might be moved to a new controller**********/
  def admin = Action.async{implicit r =>
    configDAO.config.map{ x => x match {
        case None => Ok(views.html.admin(configForm))
        case Some(c) => Ok(views.html.admin(configForm.fill(c)))
      }
    }
  }
  
  def updateConfig = Action.async{implicit r =>
    val c = configForm.bindFromRequest.get
    val rd = c.rootDir
    for{
      t <- GApi.asyncRetry(GApi.addDir(c.rootDir)) if c.rootDir != "####"
      c <- configDAO.config_(c.copy(rootDir = t.get))
    } yield Redirect(routes.Application.admin)
    //configDAO.config_(c).map(_ => Redirect(routes.Application.admin))
    //tokenDAO.insertOrUpdate(t.token, t.refreshToken).map(_ => Redirect(routes.Maintenance.index))
  }
  
  def createDefaultFolders = Action.async{implicit r =>
    for{
      c <- configDAO.config
      f1 <- GApi.asyncRetry(GApi.addDir("ekonomi", Some(c.get.rootDir))) if c != None
      f11 <- directoryDAO.insert(f1.get, "ekonomi", Some("economics")) if f1.isSuccess
      f2 <- GApi.asyncRetry(GApi.addDir("möten", Some(c.get.rootDir))) if c != None
      f21 <- directoryDAO.insert(f2.get, "möten", Some("meetings")) if f2.isSuccess
    } yield Redirect(routes.Application.drive) 
  }
  
  def drive = Action{implicit r =>
    Ok(views.html.index("Your new application is ready.", "menu.drive"))
  }
  
  //Stuff that might be removed....more for testing purposes
  def listFiles = Action.async {implicit r =>
    Logger.info("some info "+ gd.driver.about.getRootFolderId)
    GApi.asyncRetry(GApi.files(None)).map {_ match {
      case Success(s) => {
        Logger.info("could list files")
        Ok(views.html.listing(s))
      }
      case Failure(e) => {
        Logger.error("could not list files", e)
        BadRequest
      }
      } 
    }
  }

}
