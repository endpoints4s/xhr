import xerial.sbt.Sonatype.GitHubHosting
import com.lightbend.paradox.markdown.Writer
import com.typesafe.tools.mima.core.{ProblemFilters, DirectMissingMethodProblem}

inThisBuild(
  List(
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    organization := "org.endpoints4s",
    sonatypeProjectHosting := Some(
      GitHubHosting("endpoints4s", "xhr", "julien@richard-foy.fr")
    ),
    homepage := Some(sonatypeProjectHosting.value.get.scmInfo.browseUrl),
    licenses := Seq(
      "MIT License" -> url("http://opensource.org/licenses/mit-license.php")
    ),
    developers := List(
      Developer(
        "julienrf",
        "Julien Richard-Foy",
        "julien@richard-foy.fr",
        url("http://julien.richard-foy.fr")
      )
    ),
    scalaVersion := "2.13.10",
    crossScalaVersions := Seq("2.13.10", "3.1.3", "2.12.13"),
    versionPolicyIgnoredInternalDependencyVersions := Some("^\\d+\\.\\d+\\.\\d+\\+\\d+".r)
  )
)

val `xhr-client` =
  project
    .in(file("client"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      name := "xhr-client",
      mimaBinaryIssueFilters ++= Seq(
        // Was private to Scala users
        ProblemFilters.exclude[DirectMissingMethodProblem]("endpoints4s.xhr.EndpointsSettings.this")
      ),
      libraryDependencies ++= Seq(
        "org.endpoints4s" %%% "algebra" % "1.10.0",
        "org.endpoints4s" %%% "openapi" % "4.4.0",
        "org.scala-js" %%% "scalajs-dom" % "2.4.0",
        "org.scalatest" %%% "scalatest" % "3.2.17" % Test,
        "org.endpoints4s" %%% "algebra-testkit" % "4.1.0" % Test,
        "org.endpoints4s" %%% "algebra-circe-testkit" % "4.1.0" % Test,
        "org.endpoints4s" %%% "json-schema-generic" % "1.10.0" % Test,
      ),
      Test / jsEnv := new org.scalajs.jsenv.selenium.SeleniumJSEnv(
        new org.openqa.selenium.chrome.ChromeOptions().addArguments(
          // recommended options
          "--headless", // necessary for CI
          "--disable-gpu",
          "--window-size=1920,1200",
          "--ignore-certificate-errors",
          "--disable-extensions",
          "--no-sandbox",
          "--disable-dev-shm-usage",
          "--disable-web-security" // for CORS
        )
        // useful for development
        //org.scalajs.jsenv.selenium.SeleniumJSEnv.Config().withKeepAlive(true)
      )
    )

val `xhr-client-faithful` =
  project
    .in(file("client-faithful"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      name := "xhr-client-faithful",
      libraryDependencies += ("org.julienrf" %%% "faithful" % "2.0.0")
        .cross(CrossVersion.for3Use2_13),
      publish / skip := scalaBinaryVersion.value.startsWith("3"),
      Test / jsEnv := new org.scalajs.jsenv.selenium.SeleniumJSEnv(
        new org.openqa.selenium.chrome.ChromeOptions().addArguments(
          // recommended options
          "--headless", // necessary for CI
          "--disable-gpu",
          "--window-size=1920,1200",
          "--ignore-certificate-errors",
          "--disable-extensions",
          "--no-sandbox",
          "--disable-dev-shm-usage",
          "--disable-web-security" // for CORS
        )
        // useful for development
        //org.scalajs.jsenv.selenium.SeleniumJSEnv.Config().withKeepAlive(true)
      )
    )
    .dependsOn(`xhr-client`)

val `xhr-client-circe` =
  project
    .in(file("client-circe"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      name := "xhr-client-circe",
      libraryDependencies ++= Seq(
        "io.circe" %%% "circe-parser" % "0.14.1",
        "org.endpoints4s" %%% "algebra-circe" % "2.4.0",
        "org.endpoints4s" %%% "json-schema-circe" % "2.4.0",
      ),
      Test / jsEnv := new org.scalajs.jsenv.selenium.SeleniumJSEnv(
        new org.openqa.selenium.chrome.ChromeOptions().addArguments(
          // recommended options
          "--headless", // necessary for CI
          "--disable-gpu",
          "--window-size=1920,1200",
          "--ignore-certificate-errors",
          "--disable-extensions",
          "--no-sandbox",
          "--disable-dev-shm-usage",
          "--disable-web-security" // for CORS
        )
        // useful for development
        //org.scalajs.jsenv.selenium.SeleniumJSEnv.Config().withKeepAlive(true)
      )
    )
    .dependsOn(
      `xhr-client` % "test->test;compile->compile",
    )

val documentation =
  project.in(file("documentation"))
    .enablePlugins(
      ParadoxMaterialThemePlugin,
      ParadoxPlugin,
      ParadoxSitePlugin,
      ScalaUnidocPlugin,
      SitePreviewPlugin
    )
    .settings(
      publish / skip := true,
      autoAPIMappings := true,
      Compile / paradoxMaterialTheme := {
        val theme = (Compile / paradoxMaterialTheme).value
        val repository =
          (ThisBuild / sonatypeProjectHosting).value.get.scmInfo.browseUrl.toURI
        theme
          .withRepository(repository)
          .withSocial(repository)
          .withCustomStylesheet("snippets.css")
      },
      paradoxProperties ++= Map(
        "version" -> version.value,
        "scaladoc.base_url" -> s".../${(packageDoc / siteSubdirName).value}",
        "github.base_url" -> s"${homepage.value.get}/blob/v${version.value}"
      ),
      paradoxDirectives += ((_: Writer.Context) =>
        org.endpoints4s.paradox.coordinates.CoordinatesDirective
      ),
      ScalaUnidoc / unidoc / scalacOptions ++= Seq(
        "-implicits",
        "-diagrams",
        "-groups",
        "-doc-source-url",
        s"${homepage.value.get}/blob/v${version.value}€{FILE_PATH}.scala",
        "-sourcepath",
        (ThisBuild / baseDirectory).value.absolutePath
      ),
      ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
        `xhr-client`,
        `xhr-client-faithful`,
        `xhr-client-circe`,
      ),
      packageDoc / siteSubdirName := "api",
      addMappingsToSiteDir(
        ScalaUnidoc / packageDoc / mappings,
        packageDoc / siteSubdirName
      ),
    )

val xhr =
  project.in(file("."))
    .aggregate(`xhr-client`, `xhr-client-circe`, `xhr-client-faithful`)
    .settings(
      publish / skip := true
    )

Global / onChangedBuildSource := ReloadOnSourceChanges
