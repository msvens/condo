package controllers

import org.mellowtech.sdrive.GDrive
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
  implicit def driver: GDrive
}

@Singleton
class DefaultGDriver extends GDriver {
  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  
  val clientId = Play.current.configuration.getString("google.clientId").get
  val clientSecret = Play.current.configuration.getString("google.clientSecret").get
  private var _gdrive: Option[GDrive] = None
  
  implicit def driver: GDrive = try{
    import scala.concurrent.duration._
    val tokenDAO = new TokenDAO
    _gdrive match {
      case Some(d) => d
      case None => {
        val ft = tokenDAO.getToken
        ft.map(t => t).recover{case timeout: java.util.concurrent.TimeoutException => None}
        Await.result(ft, 500 millis) match {
          case None => throw new Error("could not retrive token")
          case Some(t) => {
            val gd = new GDrive(t.token, t.refreshToken, clientId, clientSecret)
            _gdrive = Some(gd)
            _gdrive.get
          }
        }
      }
    }
  } catch {case e: Exception => throw new Error(e)}
}