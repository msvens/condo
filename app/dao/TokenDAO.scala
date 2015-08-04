package dao

import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import scala.concurrent.Future

class TokenDAO extends HasDatabaseConfig[JdbcProfile] {
  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  import driver.api._
  
  val clientId = Play.current.configuration.getString("google.clientId").get
  
  class TokenTable(tag: Tag) extends Table[Token](tag, "token"){
    def clientId = column[String]("id", O.PrimaryKey)
    def token = column[String]("token")
    def refreshToken = column[Option[String]]("refreshToken")
    def * = (clientId,token,refreshToken) <> (Token.tupled, Token.unapply _)
  }
  
  private val tokens = TableQuery[TokenTable]
  
  private def findByClientId(id: String): Future[Option[Token]] = {
    val q = tokens.filter(_.clientId === id)
    val a = q.result.headOption
    db.run(a)
  }

  private def findById(id: String): Future[Option[Token]] = {
    val q = for(t <- tokens if t.clientId === id) yield t
    db.run(q.result.headOption)
  }

  def getToken: Future[Option[Token]] = findByClientId(clientId)
  
  def insert(token: String, refreshToken: Option[String]): Future[Int] = {
    val t = Token(clientId, token, refreshToken)
    db.run(tokens += t)
  }

  def insertOrUpdate(token: String, refreshToken: Option[String]): Future[Int] = {
    val t = Token(clientId, token, refreshToken)
    val upserAction = tokens.filter(_.clientId === clientId).result.headOption.flatMap
        {
          case None => {
            tokens += t
          }
          case Some(_) => {
            tokens.update(t)
          }
        }
    db.run(upserAction)
  }
  

  

  
    //val findById = tokens
  
  
  //Queries
}