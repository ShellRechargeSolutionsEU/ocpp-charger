val ocppCharger = project
  .in(file("."))
  .enablePlugins(OssLibPlugin)
  .settings(
    name := "ocpp-charger",
    organization := "com.thenewmotion.chargenetwork",
    description := "OCPP Charger Simulator",

    libraryDependencies ++= {
      val log = {
        Seq("ch.qos.logback" % "logback-classic" % "1.1.3",
            "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2")
      }

      val ocpp = {
        def libs(xs: String*) = xs.map(x => "com.thenewmotion.ocpp" %% s"ocpp-$x" % "4.2.8-SNAPSHOT")
        libs("spray", "json")
      }

      val spray = {
        Seq("io.spray" %% "spray-can" % "1.3.3")
      }

      val akka = {
        def libs(xs: String*) = xs.map(x => "com.typesafe.akka" %% s"akka-$x" % "2.3.11")

        libs("actor", "slf4j") ++
        libs("testkit").map(_ % "test")
      }

      val commons = "commons-net" % "commons-net" % "3.3"
      val scallop = "org.rogach" %% "scallop" % "0.9.5"

      val tests =
        Seq("core", "mock", "junit").map(n => "org.specs2" %% s"specs2-$n" % "2.4.17" % "test")

      log ++ akka ++ spray ++ ocpp ++ tests :+ commons :+ scallop
    }
  )

