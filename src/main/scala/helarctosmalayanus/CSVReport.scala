package helarctosmalayanus

final case class CSVReport(revenue: Double, totalCapacity: Int)

object CSVReport {
  val empty = CSVReport(0, 0)
}