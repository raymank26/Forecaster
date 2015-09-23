enablePlugins(JavaAppPackaging)

name := "Forecaster"

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-Xlint")

libraryDependencies ++= {
    val akkaV = "2.3.13"
    val akkaStreamV = "1.0"
    val scalaTestV  = "2.2.5"
    Seq(
        "com.typesafe.akka" %% "akka-actor"                           % akkaV,
        "com.typesafe.akka" %% "akka-slf4j" % akkaV,
        "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
        "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
        "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
        "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
        "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamV % "test",
        "com.typesafe.akka" %% "akka-testkit"                         % akkaStreamV % "test",
        "org.scalatest"     %% "scalatest"                            % scalaTestV  % "test",

        "org.scalaj" %% "scalaj-http" % "1.1.5",

        "org.flywaydb" % "flyway-core" % "3.2.1",
        "org.scalikejdbc" %% "scalikejdbc" % "2.2.8",
        "com.zaxxer" % "HikariCP" % "2.4.1",

        "ch.qos.logback" % "logback-classic" % "1.1.3",
        "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
    )
}

mainClass in Compile := Some("com.github.raymank26.Main")