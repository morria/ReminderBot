package com.etsy

import java.util.Date
import scala.util.matching.Regex._

class TimeOffset(val amount:Int, val unit:String) {

  // Store the current time, at which the offset was
  // presumably relevant
  val createdEpochMs:Long = new Date().getTime();

  /**
   * Get the time offset in milliseconds
   */
  def asMs() : Long =
    unit match {
        case "second"  => amount * 1000L
        case "sec"     => amount * 1000L
        case "minute"  => amount * 1000L * 60L
        case "min"     => amount * 1000L * 60L
        case "hour"    => amount * 1000L * 60L  * 60L
        case "day"     => amount * 1000L * 60L  * 60L * 24L
        case "week"    => amount * 1000L * 60L  * 60L * 24L * 7L
        case "month"   => amount * 1000L * 60L  * 60L * 24L * 30L
        case "year"    => amount * 1000L * 60L  * 60L * 24L * 365L
        case "decade"  => amount * 1000L * 60L  * 60L * 24L * 365L * 10L
        case "century" => amount * 1000L * 60L  * 60L * 24L * 365L * 100L
      }

  /**
   * Get the time as epoch milliseconds from now
   */
  def asEpochMs() : Long =
    (createdEpochMs + asMs())
}

object TimeOffset {
  /**
   * Get a TimeOffset from a string such as "5 seconds"
   */
  def apply(message:String) : Option[TimeOffset] =
    """(a|one|two|a couple|a few|[0-9]+)\s+((sec(ond)?|min(ute)?|hour|day|week|month)s?)""".r.findFirstMatchIn(message.toLowerCase) match {
      case Some(m:Match) =>
        val amount:Int = m.group(1) match {
          case "a" => 1
          case "one" => 1
          case "two" => 2
          case "a couple" => 2
          case "a few" => 3
          case _ => m.group(1).toInt
        }
        Some(new TimeOffset(amount, m.group(3)))
      case _ =>
        None
    }
}
