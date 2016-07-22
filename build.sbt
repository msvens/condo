name := """condo"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
	"org.webjars" %% "webjars-play" % "2.4.0",
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1",
  "org.mellowtech" %% "sdrive" % "1.1-SNAPSHOT",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  //"com.google.apis" % "google-api-services-drive" % "v2-rev158-1.19.1",
  "com.adrianhurt" %% "play-bootstrap3" % "0.4.4-P24",
  "org.cvogt" %% "play-json-extensions" % "0.5.0",
  cache,
  ws,
  specs2 % Test
)

//webjars
libraryDependencies ++= Seq(
  "org.webjars" % "bootbox" % "4.4.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator


