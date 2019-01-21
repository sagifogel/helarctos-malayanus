package helarctosmalayanus

import cats.effect.IO
import cats.implicits._
import org.joda.time.DateTime
import scala.language.postfixOps

sealed trait SpaceReservation

final case class BoundedReservedSpace(capacity: Int, monthlyPrice: Double, startDay: DateTime, endDay: DateTime)
  extends SpaceReservation

final case class IndefinitelyReservedSpace(capacity: Int, monthlyPrice: Double, startDay: DateTime)
  extends SpaceReservation

final case object SpaceReservationParsingError extends SpaceReservation

object SpaceReservationAlgebra {
  def calculateReport(yearMonth: DateTime, path: String): IO[CSVReport] = {
    val partialAccumulateFn = accumulateReport(_: (Double, Int), _: SpaceReservation, yearMonth)

    SpaceReservationStream.stream[IO](path)
      .compile
      .fold((0.0, 0))(partialAccumulateFn)
      .map(CSVReport.apply _ tupled)
  }

  private def accumulateReport(accumulator: (Double, Int), spaceReservation: SpaceReservation, yearMonth: DateTime): (Double, Int) = {
    spaceReservation match {
      case BoundedReservedSpace(_, monthlyPrice, startDate, endDate) if isDateBetweenRange(yearMonth, startDate, endDate) =>
        accumulator.leftMap(_ + calculateMonthlyPrice(yearMonth, startDate, endDate.some, monthlyPrice))

      case IndefinitelyReservedSpace(_, monthlyPrice, startDate) if !yearMonth.isBefore(startDate) =>
        accumulator.leftMap(_ + calculateMonthlyPrice(yearMonth, startDate, None, monthlyPrice))

      case bounded: BoundedReservedSpace => accumulator.bimap(identity, _ + bounded.capacity)
      case indefinite: IndefinitelyReservedSpace => accumulator.bimap(identity, _ + indefinite.capacity)
      case _ => accumulator
    }
  }

  private def isDateBetweenRange(yearMonth: DateTime, startDate: DateTime, endDate: DateTime): Boolean = {
    !yearMonth.isAfter(endDate) && !yearMonth.isBefore(startDate)
  }

  def calculateMonthlyPrice(yearMonth: DateTime, rangeStartDate: DateTime, rangeOptionalEndDate: Option[DateTime], monthlyPrice: Double): Double = {
    val lastDayOfMonth = yearMonth.dayOfMonth.withMaximumValue
    val numberOfDaysInMonth = yearMonth.dayOfMonth.getMaximumValue.toDouble
    val endDate = minDate(rangeOptionalEndDate.getOrElse(lastDayOfMonth), lastDayOfMonth)
    val startDate = maxDate(rangeStartDate, yearMonth)
    val numberOfDays = endDate.getDayOfMonth - startDate.getDayOfMonth + 1
    val ratio = numberOfDays.toDouble / numberOfDaysInMonth

    monthlyPrice * ratio
  }

  def minDate(first: DateTime, second: DateTime): DateTime =
    if (first.isBefore(second)) first else second

  def maxDate(first: DateTime, second: DateTime): DateTime =
    if (first.isAfter(second)) first else second
}
