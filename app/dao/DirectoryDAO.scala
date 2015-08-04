package dao

import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import scala.concurrent.Future

/**
 * @author msvens
 */
class DirectoryDAO extends HasDatabaseConfig[JdbcProfile] {
  
  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._
  
  class DirectoryTable(tag: Tag) extends Table[Directory](tag, "directory"){
    def id = column[String]("id", O.PrimaryKey)
    def title = column[String]("title")
    def role = column[Option[String]]("role")
    def * = (id,title,role) <> (Directory.tupled, Directory.unapply _)
  }
  
  private val dirs = TableQuery[DirectoryTable]
  
  def list: Future[Seq[Directory]] = {
    val q = for(t <- dirs) yield t
    db.run(q.result)
  }
  
  def delete(d: Directory): Future[Int] = delete(d.id)
  
  def delete(id: String): Future[Int] = {
    val q = dirs.filter(_.id === id)
    db.run(q.delete)
  }
  
  def insert(id: String, title: String, role: Option[String] = None): Future[Int] = {
    insert(Directory(id, title, role))
  } 
  
  def insert(d: Directory): Future[Int] = db.run(dirs += d)
  
}