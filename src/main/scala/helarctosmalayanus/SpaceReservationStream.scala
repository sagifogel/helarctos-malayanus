package helarctosmalayanus

import cats.implicits._
import cats.effect.Sync
import java.nio.file.Paths

import fs2._
import scala.util.Try
import org.joda.time.DateTime

object SpaceReservationStream {
  private final val minNumberOfColumns = 3

  def stream[F[_] : Sync](path: String): fs2.Stream[F, SpaceReservation] = {
    io.file.readAll[F](Paths.get(path), 4096)
      .through(text.utf8Decode)
      .through(text.lines)
      .drop(1)
      .map(toSpaceReservation)
  }

  private def toSpaceReservation(line: String): SpaceReservation = {
    val tokens = line.split(',').map(_.trim)

    if (tokens.length < minNumberOfColumns) SpaceReservationParsingError
    else {
      val capacity = Try(tokens(0).toInt).toOption
      val monthlyPrice = Try(tokens(1).toDouble).toOption
      val startDate = Try(DateTime.parse(tokens(2))).toOption

      if (tokens.length > minNumberOfColumns) {
        val endDate = Try(DateTime.parse(tokens(3))).toOption

        toBoundedReservation(capacity, monthlyPrice, startDate, endDate)
      }
      else {
        toIndefiniteReservation(capacity, monthlyPrice, startDate)
      }
    }
  }

  def toBoundedReservation(capacity: Option[Int], monthlyPrice: Option[Double], startDate: Option[DateTime], endDate: Option[DateTime]): SpaceReservation =
    (capacity, monthlyPrice, startDate, endDate).mapN(BoundedReservedSpace.apply)
      .getOrElse(SpaceReservationParsingError)

  def toIndefiniteReservation(capacity: Option[Int], monthlyPrice: Option[Double], startDate: Option[DateTime]): SpaceReservation =
    (capacity, monthlyPrice, startDate).mapN(IndefinitelyReservedSpace.apply)
      .getOrElse(SpaceReservationParsingError)
}
