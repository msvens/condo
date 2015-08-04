

package dao


case class Token(clientId: String, token: String, refreshToken: Option[String])

case class Directory(id: String, title: String, role: Option[String])

case class Member(id: Option[Int], name: String, email: String, phone: Option[String], apt: Option[String], role: Option[String])

case class Config(id: Int, name: String, rootDir: String, description: Option[String], web: Option[String], email: String)