package dao

import javax.inject.{Singleton, Inject}

import play.api.Play
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider, HasDatabaseConfig}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import scala.concurrent.Future

/**
 * @author msvens
 */
@Singleton
class MemberDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  
  //protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._
  
  //case class Member(id: Option[Int], name: String, email: String, phone: Option[String], apt: Option[String], role: Option[String])
  class MemberTable(tag: Tag) extends Table[Member](tag, "member"){
    def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email")
    def phone = column[Option[String]]("phone")
    def apt = column[Option[String]]("apt")
    def role = column[Option[String]]("role")
    def * = (id,name,email,phone,apt,role) <> (Member.tupled, Member.unapply _)
  }
  
  val members = TableQuery[MemberTable]
  
  def list: Future[Seq[Member]] = {
    val q = for(t <- members) yield t
    db.run(q.result)
  }
  
  def delete(m: Member): Future[Int] = delete(m.id.get)
  
  def delete(id: Int): Future[Int] = {
    val q = members.filter(_.id === id)
    db.run(q.delete)
  }
  
  /*def insert(id: String, title: String, role: Option[String] = None): Future[Int] = {
    insert(Directory(id, title, role))
  }*/
  
  
  
  def insert(name: String, email: String, phone: Option[String], apt: Option[String], role: Option[String]): Future[Int] = {
    insert(Member(None, name, email, phone, apt, role))
  }
  
  def insert(m: Member): Future[Int] = db.run(members += m)
  
}