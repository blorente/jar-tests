import java.nio.file.{Files, Paths}
import better.files._
import better.files.File.{root => r}
import better.files.File.{home => home}
import java.io.{File => JFile}
import scala.xml.XML
import scala.util.matching.Regex

case class ProjectDependencies(project: String,
                               dependencies: Seq[DependencyMetadata]) {
  def csv =
    dependencies.map(dep => s"$project, ${dep.productIterator.mkString(", ")}")
}

object ProjectDependencies {
  val csvHeader = s"project, ${DependencyMetadata.csvHeader}"
}

case class DependencyMetadata(projname: String,
                              vendor: String,
                              version: String,
                              language: String,
                              scala_version: String)
object DependencyMetadata {
  val csvHeader = "depname, vendor, version, language, scala_version"
}

case class JarManifestInfo(projname: String, vendor: String, version: String)

object Main {
  def main(args: Array[String]): Unit = {
    val input: Iterator[String] =
      if (args.size > 0) Iterator(args: _*)
      else io.Source.stdin.getLines
    val deps: Iterator[ProjectDependencies] = for { path <- input } yield {
      println(s"Classpath: ${path}")
      val cp = r / path
      val dependencies = cp.lines
        .filter(dep => dep.endsWith(".jar") && dep.contains("/.ivy2/"))
        .map(_.split("""/\.ivy2/cache/""").tail.head)
        .map(jarInfoFromIvyPath(_))
        .map(f => { println(f); f })
        //.map(getIvyInfo)
        .toSeq
      ProjectDependencies(path, dependencies.map(toMetadata))
    }
    File("./hihi.csv")
      .writeText(s"${ProjectDependencies.csvHeader}\n")
      .appendLines(deps.flatMap(_.csv).toSeq: _*)
  }

  def toMetadata(jar: JarManifestInfo): DependencyMetadata = DependencyMetadata(
    jar.projname,
    jar.vendor,
    jar.version,
    "NA",
    "NA"
  )

  // We rely on ivy paths:
  //  <home>/.ivy2/cache/<vendor>/<projname>/jars/<jarfile>.jar
  // Jarfile is for us described as:
  //  \w[-\w]*-<version>.jar
  def jarInfoFromIvyPath(path: String): JarManifestInfo = {
    def getVersion(jarfile: String): String =
      jarfile.split("-").last.stripSuffix(".jar")

    val pathBuffer = path.split("/")
    JarManifestInfo(
      pathBuffer(1),
      pathBuffer(0),
      getVersion(pathBuffer.last)
    )
  }

  def formatCSV[A](what: { def productIterator: Iterator[A] }): String =
    what.productIterator.mkString(", ")

  def getIvyInfo(jarInfo: JarManifestInfo): DependencyMetadata = {
    def getLanguage(ivyFile: scala.xml.Node): String = {
      (ivyFile \ "info" \ "@module").text match {
        case "scala-library" => "Scala"
        case _ =>
          (ivyFile \ "dependency" \ "@name=scala-library").text match {
            case "scala-library" => "Scala"
            case _               => "Java"
          }
      }
    }

    def getScalaVersion(ivyFile: scala.xml.Node): String = {
      (ivyFile \ "info" \ "@module").text match {
        case "scala-library" => (ivyFile \ "info" \ "@revision").text
        case _ =>
          (ivyFile \ "dependency" \ "@name=scala-library").text match {
            case "scala-library" =>
              (ivyFile \ "dependency")
                .filter(_.attribute("name") == "scala-library")
                .head
                .attribute("rev")
                .get
                .text
            case _ => "NA"
          }
      }
    }

    val ivyFile = XML.loadFile(
      s"${home.path.toAbsolutePath.toString}/.ivy2/cache/${jarInfo.vendor}/${jarInfo.projname}/ivy-${jarInfo.version}.xml")
    (ivyFile \ "dependency").text
    DependencyMetadata(
      jarInfo.projname,
      jarInfo.vendor,
      jarInfo.version,
      getLanguage(ivyFile),
      getScalaVersion(ivyFile)
    )
  }
}
