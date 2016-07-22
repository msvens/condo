package controllers

import org.mellowtech.gapi.service.GService
import play.api._
import play.api.mvc._
import play.api.i18n._
import javax.inject.Inject
import scala.concurrent.Future
import scala.util.{Success,Failure}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import dao.{Config, ConfigDAO, DirectoryDAO, MemberDAO}

class Application @Inject() (configDAO: ConfigDAO, directoryDAO: DirectoryDAO, memberDAO: MemberDAO, val messagesApi: MessagesApi, val gd: GDriver) extends Controller with I18nSupport {

  //implicit val d: GDrive = gd.driver
  import ApiContainers._
  //import gd.ds
  


  if(gd.rootDir.isEmpty) {
    configDAO.config.map(oc => oc match {
      case Some(c) => gd._rootDir(Some(c.rootDir))
      case None => gd._rootDir(None)
    })
  }
  
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
    import gd.ds
    val c = configForm.bindFromRequest.get
    val rd = c.rootDir
    for{
      t <- GService.async(ds.createFolder(c.rootDir)) if c.rootDir != "####"
      c <- configDAO.config_(c.copy(rootDir = t))
    } yield {
      gd._rootDir(Some(t))
      Redirect(routes.Application.admin)
    }

    //configDAO.config_(c).map(_ => Redirect(routes.Application.admin))
    //tokenDAO.insertOrUpdate(t.token, t.refreshToken).map(_ => Redirect(routes.Maintenance.index))
  }
  
  def createDefaultFolders = Action.async{implicit r =>
    val r: Future[Result] = for{
      d1 <- createDir("ekonomi", "economics", gd.rootDir)
      d2 <- createDir("möten", "meetings", gd.rootDir)
      d3 <- createDir("information", "info", gd.rootDir)
    } yield Redirect(routes.Application.drive(None))
    r

  }

  private def createDir(n: String, r: String, rd: Option[String]): Future[Int] = for{

    f1 <- GService.async(gd.ds.createFolder(n, rd))(gd.ds) if rd != None
    f11 <- directoryDAO.insert(f1, n, Some(r))
  } yield f11
  
  def drive(folder: Option[String]) = Action.async{implicit r =>
    import gd.ds
    val dir = folder match {
      case None => gd.rootDir
      case Some(d) => folder
    }
    GService.async(ds.files(dir)).map {s =>
      Ok(views.html.drive(s))
    }.recover{case thrown => BadRequest}

  }
  
  //Stuff that might be removed....more for testing purposes
  def listFiles = Action.async {implicit r =>
    import gd.ds
    Logger.info("some info "+ ds.about.getRootFolderId)
    GService.async(ds.files(None)).map {s =>
        Ok(views.html.listing(s))
      }.recover{case thrown => BadRequest}
  }

  def listSheets = Action.async{implicit r =>
    import gd.ss
    println("in sheets")
    GService.async(ss.sheets).map{s =>
      println("num sheets: "+s.size)
      Ok(views.html.listsheets(s))
    }.recover{case thrown =>
      thrown.printStackTrace(System.out)
      BadRequest
    }

  }

}
