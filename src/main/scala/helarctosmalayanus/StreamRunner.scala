package helarctosmalayanus

import java.text.NumberFormat
import java.nio.file.{Files, Path, Paths}

import pureconfig._
import cats.implicits._
import SpaceReservationAlgebra._
import cats.effect.{ExitCode, IO, IOApp}
import com.github.nscala_time.time.Imports._
import pureconfig.error.{CannotReadFile, ConfigReaderFailure, ConfigReaderFailures}

import scala.util.Try

object StreamRunner extends IOApp {
  private val dateFormatter = DateTimeFormat.forPattern("YYYY-mm")
  private val revenueFormatter = NumberFormat.getIntegerInstance

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      either <- validateConfig
      exitCode <- either.fold(errors =>
        putStrLn(errors.toList.map(_.description).intercalate(", ")) >> error,
        mainLoop)
    } yield exitCode
  }

  def invalidFilePath(path: Path): ConfigReaderFailure = CannotReadFile(path, None)

  def validateConfig: IO[Either[ConfigReaderFailures, String]] = {
    val config = loadConfig[CSVFileConfiguration]("csv-file-configuration")

    val either = config match {
      case Right(CSVFileConfiguration(path)) if Files.exists(Paths.get(path)) => {
        path.asRight[ConfigReaderFailures]
      }
      case Right(CSVFileConfiguration(path)) => {
        val invalidPath = invalidFilePath(Paths.get(path))

        ConfigReaderFailures(invalidPath).asLeft[String]
      }
      case Left(configReaderFailures) => configReaderFailures.asLeft[String]
    }

    IO.pure(either)
  }

  def mainLoop(path: String): IO[ExitCode] = {
    for {
      _ <- printMenu
      line <- getStrLn
      exitCode <- parseSelection(line).fold(invalidChoice >> mainLoop(path)) {
        case RunReport => runReport(path).flatMap(printReportResult) >> mainLoop(path)
        case Quit => putStrLn("goodbye") >> success
      }
    } yield exitCode
  }

  def printReportResult(report: CSVReport): IO[Unit] =
    putStrLn(s"expected revenue: ${"$" ++ revenueFormatter.format(report.revenue)}, " +
      s"expected total capacity of the unreserved space: ${report.totalCapacity}")

  def runReport(path: String): IO[CSVReport] =
    for {
      date <- parseDate
      report <- calculateReport(date, path)
    } yield report

  def parseDate: IO[DateTime] =
    for {
      _ <- putStrLn("Please enter your desired month in {YYYY-MM} format")
      line <- getStrLn
      date <- Try(dateFormatter.parseDateTime(line)).fold(ex =>
        putStrLn(ex.getMessage) >> parseDate, IO.pure)
    } yield date

  def success: IO[ExitCode] = IO.pure(ExitCode.Success)

  def error: IO[ExitCode] = IO.pure(ExitCode.Error)

  def invalidChoice: IO[Unit] =
    for {
      _ <- putStrLn("Your choice was invalid.")
    } yield ()

  private def putStrLn(line: String): IO[Unit] = IO.delay(println(line))

  private def getStrLn: IO[String] = IO.delay[String](scala.io.StdIn.readLine)

  private def parseSelection(choice: String): Option[MenuOption] = {
    choice match {
      case "1" => Some(RunReport)
      case "2" => Some(Quit)
      case _ => None
    }
  }

  def printMenu: IO[Unit] =
    putStrLn(
      s"""
         |What would you like to do?
         |1. Run the report for a specific month
         |2. Quit
       """.stripMargin)
}