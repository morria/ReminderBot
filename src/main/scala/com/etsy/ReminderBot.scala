package com.etsy

import java.util.Date
import java.text.SimpleDateFormat
import org.jibble.pircbot.PircBot
import scala.actors._
import scala.util.matching.Regex.Match
import scopt.mutable.OptionParser
import scopt.mutable.OptionParser._

class ReminderBot(name:String, channels:List[String]) extends PircBot() with Actor {

  // Set the name of the bot
  setName(name);
  setLogin(name);
  setVerbose(true);

  /**
   * Upon connecting to the host, join a few channels
   */
  override def onConnect() : Unit =
    channels.foreach {
      channel:String =>
        joinChannel(channel)
      }

  /**
   * Check all incoming messages for reminders
   */
  override def onMessage(channel:String, senderNick:String, login:String, hostname:String, message:String) : Unit = {
    // Scan for reminders
    TimeOffset(message) match {
      case Some(timeOffset:TimeOffset) =>
        val reminder:Reminder =
          Reminder(this, timeOffset.createdEpochMs, timeOffset.asEpochMs(), channel, senderNick, message);

        // Store the reminder
        reminder.store()

        // Start counting down until the reminder matures
        reminder.start

      case _ => null
    }

    // List reminders
    """^.ls$""".r.findFirstMatchIn(message) match {
      case Some(m:Match) =>
        Reminder.reminders(this).foreach {
          reminder:Reminder =>
            val dateFormat:SimpleDateFormat = new SimpleDateFormat("dd-MM-yyy hh:mm")
            sendMessage(channel,
              reminder.senderNick + ": "
              + "[" + dateFormat.format(new Date(reminder.timestampCreate)) + "] "
              + "[" + dateFormat.format(new Date(reminder.timestampRemind)) + "] "
              + "'" + reminder.message + "'")
        }
      case _ => null
    }
  }

  /**
   * Reconnect when disconnected
   */
  override def onDisconnect() : Unit = reconnect()

  /**
   * Listen for reminders maturing, and announce them to the
   * channel from whence they came
   */
  def act {
    loop {
      react {
        case reminder:Reminder =>
          val message:String =
            reminder.senderNick + " said: " + reminder.message;
          sendMessage(reminder.channel, message);
          reminder.delete();
      }
    }
  }

}

case class ReminderBotConfig(
  var name:String = "reminderbot",
  var host:String = "irc.collapse.io",
  var port:Int = 6667,
  var password:Option[String] = None,
  var channels:List[String] = List("#reminderbot"))

object ReminderBot {

  def apply(name:String, host:String, port:Int, channels:List[String], password:Option[String]) : ReminderBot = {
    val reminderBot:ReminderBot = new ReminderBot(name, channels)
    password match {
      case Some(password:String) =>
        reminderBot.connect(host, port, password)
      case _ =>
        reminderBot.connect(host, port);
    }
    reminderBot
  }

  def main(args : Array[String]) {
    println(args)

    val config:ReminderBotConfig = ReminderBotConfig()
    val parser = new OptionParser("reminderbot") {
      opt("n", "name", "", "Bot Name", {
          value:String => config.name = value })
      opt("h", "host", "", "IRC Host Name", {
          value:String => config.host = value })
      intOpt("p", "port", "", "IRC Port", {
          value:Int => config.port = value; })
      opt("a", "password", "", "IRC Password", {
          value:String => config.password = Some(value); })
      opt("c", "channels", "", "Comma-separated list of channels", {
          value:String => config.channels = value.split(",").toList })
    }

    if (parser.parse(args)) {

        // Create the bot
        val reminderBot:ReminderBot =
          ReminderBot(config.name, config.host, config.port, config.channels, config.password);

        // Start listening for reminders to mature
        reminderBot.start

        // Read all of the previously stored reminders
        val now:Long = new Date().getTime()
        Reminder.reminders(reminderBot).foreach {
          reminder:Reminder =>
            if(reminder.timestampRemind > now)
              reminder.start
            else
              reminder.delete
        }
    }
    else {
      println("Failed to parse arguments")
    }

  }
}
