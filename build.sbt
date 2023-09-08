import Dependencies.*

import java.time.{LocalDateTime, ZoneId}
import scala.language.postfixOps
import scala.sys.process.*

Global / onChangedBuildSource := ReloadOnSourceChanges

Global / excludeLintKeys := Set(idePackagePrefix)

ThisBuild / scalaVersion := "3.3.1"

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
  circeParser,
  logback
)

ThisBuild / wartremoverErrors ++= Warts.allBut(Wart.ImplicitParameter)

ThisBuild / version := "1.0.0-beta.4"

ThisBuild / coverageMinimumStmtTotal := 80
ThisBuild / coverageMinimumBranchTotal := 80

ThisBuild / autoAPIMappings := true

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
      assembly / assemblyMergeStrategy := {
        case PathList("module-info.class") => MergeStrategy.discard
        case v => MergeStrategy.defaultMergeStrategy(v)
      },
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
  .enablePlugins(DockerPlugin, AutomateHeaderPlugin)
  .settings(
    name := "laas-worker",
    libraryDependencies ++= Seq(
      akka,
      akkaStream,
      akkaHttp,
      akkaStreamTestkit,
      akkaHttpTestkit,
      akkaTestkit,
      testContainers
    ),
    Test / fork := true,
    assembly / assemblyJarName := "main.jar",
    assembly / mainClass := Some("io.github.cakelier.laas.worker.main"),
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class") => MergeStrategy.discard
      case v => MergeStrategy.defaultMergeStrategy(v)
    },
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
  .dependsOn(tsClient, ui)
  .enablePlugins(DockerPlugin, AutomateHeaderPlugin)
  .settings(
    name := "laas-master",
    libraryDependencies ++= Seq(
      akka,
      akkaStream,
      akkaHttp,
      akkaStreamTestkit,
      akkaHttpTestkit,
      akkaTestkit,
      testContainers,
      testContainersPostgresql,
      postgresql,
      quill,
      commonsIO,
      jbcrypt
    ),
    assembly / assemblyJarName := "main.jar",
    assembly / mainClass := Some("io.github.cakelier.laas.master.ws.main"),
    assembly / assemblyMergeStrategy := {
      case PathList("io", "getquill", _*) => MergeStrategy.first
      case PathList("module-info.class") => MergeStrategy.discard
      case v => MergeStrategy.defaultMergeStrategy(v)
    },
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

lazy val ui = project
  .in(file("ui"))
  .settings(
    name := "laas-ui",
    Compile / resourceGenerators += Def.task {
      "bash -c cd ui; npm run build" !
      val webapp = baseDirectory.value / "build"
      (webapp ** "*")
        .pair(Path.rebase(webapp, resourceManaged.value / "main" / "ui"))
        .map {
          case (from, to) =>
            Sync.copy(from, to)
            to
        }
    }.taskValue
  )

lazy val root = project
  .in(file("."))
  .aggregate(tsCore, tsClient, tsServer, worker, master)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    name := "laas"
  )
