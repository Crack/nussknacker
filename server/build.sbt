import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbt.Keys._

val scalaV = "2.11.8"

organization  := "pl.touk.esp"

name := "esp-ui"

scalaVersion  := scalaV

resolvers ++= Seq(
  "local" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  "touk repo" at "http://nexus.touk.pl/nexus/content/groups/public",
  "touk snapshots" at "http://nexus.touk.pl/nexus/content/groups/public-snapshots",
  Resolver.bintrayRepo("hseeberger", "maven")
)

scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-encoding", "utf8",
  "-Xfatal-warnings",
  "-feature",
  "-language:postfixOps",
  "-language:existentials",
  "-target:jvm-1.8"
)

javacOptions := Seq(
  "-Xss4M" // to avoid SOF
)

sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

val espEngineV = "0.1-SNAPSHOT"
val akkaHttpArgonautV = "1.8.0"
val akkaV = "2.4.8"
val catsV = "0.6.1"
val slf4jV = "1.7.21"
val logbackV = "1.1.3"
val scalaTestV = "3.0.0-M15"
val flinkV = "1.0.3"
val slickV = "3.1.1"
val hsqldbV = "2.3.4"
val flywayV = "4.0.3"

libraryDependencies ++= {
  Seq(
    "de.heikoseeberger" %% "akka-http-argonaut" % akkaHttpArgonautV,
    //fixme tutaj nie powinnismy nawet probowac resolvowac esp-process-sample, trzeba poprawic konfiguracje testow it
    "pl.touk.esp" %% "esp-management" % espEngineV changing() exclude("pl.touk.esp", "esp-process-sample_2.11"),
    "pl.touk.esp" %% "esp-interpreter" % espEngineV changing(),

    "org.apache.flink" %% "flink-streaming-scala" % flinkV % "runtime", // na potrzeby optymalizacji procesów

    //to musimy podac explicite, zeby wymusic odpowiednia wersje dla flinka
    "com.typesafe.akka" %% "akka-remote" % akkaV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "org.hsqldb" % "hsqldb" % hsqldbV,
    "org.flywaydb" % "flyway-core" % flywayV,

    "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test",
    "com.typesafe.slick" %% "slick-testkit" % slickV % "test",
    "org.scalatest" %% "scalatest" % scalaTestV % "test"

  )
}

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)