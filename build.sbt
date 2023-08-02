import Dependencies._
import java.time.{LocalDateTime, ZoneId}

Global / onChangedBuildSource := ReloadOnSourceChanges

Global / excludeLintKeys := Set(idePackagePrefix)

ThisBuild / scalaVersion := "3.3.0"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings"
)

ThisBuild / scalafixDependencies ++= Seq(
  "io.github.ghostbuster91.scalafix-unified" %% "unified" % "0.0.9",
  "net.pixiv" %% "scalafix-pixiv-rule" % "4.5.3"
)
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

ThisBuild / idePackagePrefix := Some("io.github.cakelier")

ThisBuild / libraryDependencies ++= Seq(
  scalactic,
  scalatest,
  circeCore,
  circeGeneric,
  circeParser
)

ThisBuild / wartremoverErrors ++= Warts.allBut(Wart.ImplicitParameter)

ThisBuild / version := "0.0.0"

ThisBuild / coverageMinimumStmtTotal := 80
ThisBuild / coverageMinimumBranchTotal := 80

lazy val tsCore =
  project
    .in(file("core"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(
      name := "laas-ts-core",
      headerLicense := Some(HeaderLicense.MIT(
        LocalDateTime.now(ZoneId.of("UTC+1")).getYear.toString,
        "Matteo Castellucci"
      ))
    )

lazy val tsClient =
  project
    .in(file("client"))
    .dependsOn(tsCore)
    .enablePlugins(AutomateHeaderPlugin)
    .settings(
      name := "laas-ts-client",
      libraryDependencies ++= Seq(
        akka,
        akkaStream,
        akkaHttp,
        akkaStreamTestkit,
        akkaHttpTestkit,
        akkaTestkit
      ),
      headerLicense := Some(HeaderLicense.MIT(
        LocalDateTime.now(ZoneId.of("UTC+1")).getYear.toString,
        "Matteo Castellucci"
      ))
    )

lazy val tsServer =
  project
    .in(file("server"))
    .dependsOn(tsCore)
    .enablePlugins(DockerPlugin, AutomateHeaderPlugin)
    .settings(
      name := "laas-ts-server",
      libraryDependencies ++= Seq(
        akka,
        akkaStream,
        akkaHttp,
        akkaStreamTestkit,
        akkaHttpTestkit,
        akkaTestkit
      ),
      assembly / assemblyJarName := "main.jar",
      assembly / mainClass := Some("io.github.cakelier.laas.tuplespace.server.main"),
      docker / dockerfile := NativeDockerfile(file("server") / "Dockerfile"),
      docker / imageNames := Seq(
        ImageName(
          namespace = Some("matteocastellucci3"),
          repository = name.value,
          tag = Some("latest")
        ),
        ImageName(
          namespace = Some("matteocastellucci3"),
          repository = name.value,
          tag = Some(version.value)
        )
      ),
      headerLicense := Some(HeaderLicense.MIT(
        LocalDateTime.now(ZoneId.of("UTC+1")).getYear.toString,
        "Matteo Castellucci"
      ))
    )

lazy val worker = project
  .in(file("worker"))
  .dependsOn(tsClient)
  .settings(
    name := "laas-worker",
    libraryDependencies ++= Seq(
      akka,
      akkaStream,
      akkaHttp,
      akkaStreamTestkit,
      akkaHttpTestkit,
      akkaTestkit
    ),
    assembly / assemblyJarName := "main.jar",
    assembly / mainClass := Some("io.github.cakelier.laas.worker"),
    docker / dockerfile := NativeDockerfile(file("worker") / "Dockerfile"),
    docker / imageNames := Seq(
      ImageName(
        namespace = Some("matteocastellucci3"),
        repository = name.value,
        tag = Some("latest")
      ),
      ImageName(
        namespace = Some("matteocastellucci3"),
        repository = name.value,
        tag = Some(version.value)
      )
    ),
    headerLicense := Some(HeaderLicense.MIT(
      LocalDateTime.now(ZoneId.of("UTC+1")).getYear.toString,
      "Matteo Castellucci"
    ))
  )

lazy val master = project
  .in(file("master"))
  .dependsOn(tsClient)
  .settings(
    name := "laas-master",
    libraryDependencies ++= Seq(
      akka,
      akkaStream,
      akkaHttp,
      akkaStreamTestkit,
      akkaHttpTestkit,
      akkaTestkit
    ),
    assembly / assemblyJarName := "main.jar",
    assembly / mainClass := Some("io.github.cakelier.laas.master"),
    docker / dockerfile := NativeDockerfile(file("master") / "Dockerfile"),
    docker / imageNames := Seq(
      ImageName(
        namespace = Some("matteocastellucci3"),
        repository = name.value,
        tag = Some("latest")
      ),
      ImageName(
        namespace = Some("matteocastellucci3"),
        repository = name.value,
        tag = Some(version.value)
      )
    ),
    headerLicense := Some(HeaderLicense.MIT(
      LocalDateTime.now(ZoneId.of("UTC+1")).getYear.toString,
      "Matteo Castellucci"
    ))
  )

lazy val root = project
  .in(file("."))
  .aggregate(tsCore, tsClient, tsServer, worker, master)
  .settings(
    name := "laas"
  )
