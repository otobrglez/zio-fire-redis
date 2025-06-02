lazy val zioSBTVersion = "0.4.0-alpha.31"

addSbtPlugin("dev.zio"        % "zio-sbt-ecosystem"      % zioSBTVersion)
addSbtPlugin("dev.zio"        % "zio-sbt-ci"             % zioSBTVersion)
addSbtPlugin("dev.zio"        % "zio-sbt-website"        % zioSBTVersion)
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"           % "2.5.4")
addSbtPlugin("org.jmotor.sbt" % "sbt-dependency-updates" % "1.2.9")

resolvers ++= Resolver.sonatypeOssRepos("public")
