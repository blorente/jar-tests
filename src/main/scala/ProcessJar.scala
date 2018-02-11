import java.nio.file.{Files, Paths}
import better.files._
import better.files.File.{root => r}
import better.files.File.{home => home}
import java.io.{File => JFile}
import scala.xml.XML
import scala.util.matching.Regex

object Main {

  case class ProjectDependencies(project: String,
                                 dependencies: Seq[DependencyMetadata])

  def main(args: Array[String]) = {
    val input: Iterator[String] =
      if (args.size > 0) Iterator(args:_*)
      else io.Source.stdin.getLines
    for (path <- input) {
      println(s"Classpath: ${path}")
      val cp = r / path
      val dependencies = cp.lines
        .filter(_.endsWith(".jar"))
        .map(File(_))
        .map(_.unzip())
        //.map(dir => {dir.listRecursively.map(println); dir})
        .map(_ / "META-INF" / "MANIFEST.MF")
        //.map(f => {println(f.lines); f})
        .map(getJarManifestInfo)
        .map(getIvyInfo)
        .toSeq
      ProjectDependencies(path, dependencies)
    }

    // Print to CSV
    println(formatCSV(getIvyInfo(getJarManifestInfo(r))))
  }

  case class JarManifestInfo(projname: String, vendor: String, version: String)
  def getJarManifestInfo(manifest: File): JarManifestInfo = {
    def getField(fieldPart: String): String = {
        manifest.lines
          .find(_.startsWith(fieldPart))
          .getOrElse("N/A")
          .stripPrefix(fieldPart)  
    }

    val r = JarManifestInfo(
      getField(""".*-Title"""),
      getField(""".*-Vendor"""),
      getField("""[^M].+-Version""")
    )
    println(r)
    r
  }

  def formatCSV[A](what: { def productIterator: Iterator[A] }): String =
    what.productIterator.mkString(", ")

  case class DependencyMetadata(projname: String,
                                vendor: String,
                                version: String,
                                language: String,
                                scala_version: String)
  def getIvyInfo(jarInfo: JarManifestInfo): DependencyMetadata = {
    val ivyFile = XML.loadFile(
      s"${home.path.toAbsolutePath.toString}/.ivy2/cache/${jarInfo.vendor}/${jarInfo.projname}/ivy-${jarInfo.version}.xml")
    (ivyFile \ "dependency").text
    DependencyMetadata("stub", "stub", "stub", "stub", "stub")
  }
}

/**
case class CliConfig(root: String = "Nodir",
                     classpath: String = "Nocp",
                     outdir: String = "./tmp")
object Cli {
  def apply(args: Array[String]): Option[CliConfig] = {
    val optParser = new scopt.OptionParser[CliConfig]("collector") {
      head("PRL-PRG Classpath interpreter", "0.1")
      arg[String]("<directory>")
        .validate(
          x =>
            if (Files.exists(Paths.get(x)) && Files.isDirectory(Paths.get(x)))
              success
            else failure("Folder does not exist or is not a folder"))
        .action((x, c) => c.copy(root = x))
        .text("The directory where the project(s) is stored")
      arg[String]("<stored classpath>")
        .validate(
          x =>
            if (Files.exists(Paths.get(x)))
              success
            else failure("Classpath not found"))
        .action((x, c) => c.copy(classpath = x))
        .text("The stored classpath")
      arg[String]("<output folder>")
        .validate(
          x =>
            if (Files.exists(Paths.get(x)) && Files.isDirectory(Paths.get(x)))
              success
            else failure("Out folder does not exist or is not a directory")
        )
        .action((x, c) => c.copy(outdir = x))
        .text("The desired place to output the files")
    }

    optParser.parse(args, CliConfig())
  }

}
  */
