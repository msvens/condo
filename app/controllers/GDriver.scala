package controllers

import java.util.concurrent.TimeoutException

import org.mellowtech.gapi.service.{SheetService, DriveService}
import javax.inject._
import com.google.inject.ImplementedBy
import scala.util.Try
import dao.TokenDAO
import scala.concurrent.Await
import play.api.Play

/**
 * @author msvens
 */
@ImplementedBy(classOf[DefaultGDriver])
trait GDriver {
  implicit def ds: DriveService
  implicit def ss: SheetService
  implicit def rootDir: Option[String]
  def _rootDir(s: Option[String]): Unit
}

@Singleton
class DefaultGDriver @Inject()(tokenDAO: TokenDAO) extends GDriver {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import scala.concurrent.duration._

  val clientId = Play.current.configuration.getString("google.clientId").get
  val clientSecret = Play.current.configuration.getString("google.clientSecret").get
  private var driveService: Option[DriveService] = None
  private var sheetService: Option[SheetService] = None

  private var _rootDir: Option[String] = None

  def _rootDir(s : Option[String]) = _rootDir = s
  def rootDir = _rootDir

  implicit def ss: SheetService = try{
    sheetService match {
      case Some(s) => s
      case None => {
        val ft = tokenDAO.getToken
        ft.map(t => t).recover{case timeout: TimeoutException => None}
        Await.result(ft, 500 millis) match {
          case None => throw new Error("could not retrieve token")
          case Some(t) => {
            sheetService = Some(SheetService(t.token, t.refreshToken, clientId, clientSecret))
            sheetService.get
          }
        }
      }
    }
  } catch {case e: Exception => throw new Error(e)}
  
  implicit def ds: DriveService = try{
    driveService match {
      case Some(d) => d
      case None => {
        val ft = tokenDAO.getToken
        ft.map(t => t).recover{case timeout: TimeoutException => None}
        Await.result(ft, 500 millis) match {
          case None => throw new Error("could not retrive token")
          case Some(t) => {
            println("token: "+t.token+" "+t.refreshToken)
            val gd = DriveService(t.token, t.refreshToken, clientId, clientSecret)
            driveService = Some(gd)
            driveService.get
          }
        }
      }
    }
  } catch {case e: Exception => throw new Error(e)}
}