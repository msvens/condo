package dao

/**
 * @author msvens
 */

import javax.inject.{Inject,Singleton}

import play.api.Play

import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import scala.concurrent.Future

@Singleton
class ConfigDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._
  
  class ConfigTable(tag: Tag) extends Table[Config](tag, "config"){
    //id: Int, name: String, rootDir: String, description: Option[String], web: Option[String], email: String
    def id = column[Int]("id", O.PrimaryKey)
    def name = column[String]("name")
    def rootDir = column[String]("rootDir")
    def description = column[Option[String]]("description")
    def web = column[Option[String]]("web")
    def email = column[String]("email")
    def * = (id, name, rootDir, description, web, email) <> (Config.tupled, Config.unapply _)
  }
  
  val ID = 1000; //hard coded for now
  val configs = TableQuery[ConfigTable]
  
  def config: Future[Option[Config]] = {
    val q = configs.filter(_.id === ID)
    db.run(q.result.headOption)
  }
  
  def config_(name: String, rootDir: String, description: Option[String], web: Option[String], email: String): Future[Int] = {
    config_(Config(ID, name, rootDir, description, web, email))
  }
  
  def config_(c: Config): Future[Int] = {
    val c1 = c.copy(id = ID); //this might change in future versions
    val upserAction = configs.filter(_.id === ID).result.headOption.flatMap{
      case None => configs += c
      case Some(_) => configs.update(c)
    }
    db.run(upserAction)
  }

  
}