import AssemblyKeys._ // put this at the top of the file

assemblySettings

name := "ScalaCyc"

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-actor"       % "2.2.3" withSources() withJavadoc(),
  "com.typesafe.akka"  %% "akka-slf4j"       % "2.2.3" withSources() withJavadoc(),
  "io.spray" % "spray-can" % "1.2.0"  withSources() withJavadoc(),
  "io.spray" % "spray-routing" % "1.2.0"  withSources() withJavadoc(),
  "io.spray" %%  "spray-json" % "1.2.5" withSources() withJavadoc(),
  "ch.qos.logback"      % "logback-classic"  % "1.0.13",
  "org.specs2"         %% "specs2"           % "1.14"         % "test",
  "com.typesafe.akka"  %% "akka-testkit"     % "2.2.3"        % "test" withSources() withJavadoc(),
  "com.novocode"        % "junit-interface"  % "0.7"          % "test->default",
  "com.sksamuel.elastic4s" % "elastic4s_2.10" % "0.90.7.4"  withSources() withJavadoc(),
  "org.reactivemongo" %% "reactivemongo" % "0.9" withSources() withJavadoc(),
  "org.mongodb" % "casbah_2.10" % "2.6.4",
  "com.googlecode.json-simple" % "json-simple" % "1.1.1" withSources() withJavadoc(),
  "com.ning" % "async-http-client" % "1.7.19"  withSources() withJavadoc()
  )



resolvers += "spray repo" at "http://repo.spray.io"

javacOptions ++= Seq("-encoding", "UTF-8")

mainClass in assembly := Some("com.gw.search.SearchSpray")

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
      case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
      case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
      case "reference.conf" => MergeStrategy.concat
      case _ => MergeStrategy.first
  }
}
