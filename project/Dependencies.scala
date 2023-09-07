import sbt.*

object Dependencies {

  lazy val scalactic = "org.scalactic" %% "scalactic" % "3.2.16"

  lazy val scalatest = "org.scalatest" %% "scalatest" % "3.2.16" % Test

  lazy val circeCore = "io.circe" %% "circe-core" % "0.14.5"

  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.14.5"

  lazy val circeParser = "io.circe" %% "circe-parser" % "0.14.5"

  lazy val postgresql = "org.postgresql" % "postgresql" % "42.6.0"

  lazy val quill = "io.getquill" %% "quill-jdbc" % "4.6.0.1"

  lazy val akka = "com.typesafe.akka" %% "akka-actor-typed" % "2.8.4"

  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream-typed" % "2.8.4"

  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.5.2"

  lazy val akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % "2.8.4" % Test

  lazy val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % "10.5.2" % Test

  lazy val akkaTestkit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.8.4" % Test

  lazy val testContainers = "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.41.0" % Test

  lazy val testContainersPostgresql = "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.40.17" % Test

  lazy val commonsIO = "commons-io" % "commons-io" % "2.13.0" % Test

  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.4.7"

  lazy val jbcrypt = "org.mindrot" % "jbcrypt" % "0.4"
}
