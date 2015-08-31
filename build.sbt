name := "Forecaster"

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
    val akkaV       = "2.3.12"
    val akkaStreamV = "1.0"
    val scalaTestV  = "2.2.5"
    Seq(
        "com.typesafe.akka" %% "akka-actor"                           % akkaV,
        "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
        "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
        "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
        "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
        "com.typesafe.akka" %% "akka-http-testkit-experimental"       % akkaStreamV,
        "org.scalatest"     %% "scalatest"                            % scalaTestV % "test",

        "com.zaxxer" % "HikariCP" % "2.4.1"
    )
}