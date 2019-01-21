package helarctosmalayanus

sealed trait MenuOption

final case object RunReport extends MenuOption

final case object Quit extends MenuOption
