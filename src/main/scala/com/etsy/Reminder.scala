package com.etsy

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.Date
import scala.actors._
import scala.actors.Actor._

class Reminder(
  val reminderBot:ReminderBot,
  val timestampCreate:Long,
  val timestampRemind:Long,
  val channel:String,
  val senderNick:String,
  val message:String) extends Actor {

  /**
   * Store this reminder
   */
  def store() : Unit = {
    val statement:PreparedStatement =
      Reminder.connection.prepareStatement(
      "INSERT INTO Reminders VALUES (?, ?, ?, ?, ?);")
    statement.setLong(1, timestampCreate)
    statement.setLong(2, timestampRemind)
    statement.setString(3, channel)
    statement.setString(4, senderNick)
    statement.setString(5, message)
    statement.executeUpdate()
  }

  /**
   * Delete this reminder
   */
  def delete() : Unit = {
    val statement:PreparedStatement =
      Reminder.connection.prepareStatement(
        "DELETE FROM Reminders WHERE timestamp_create=?;")
    statement.setLong(1, timestampCreate)
    statement.executeUpdate()
  }

  /**
   * Alert our bot when this reminder matures
   */
  def act {
    reactWithin(timestampRemind - new Date().getTime()) {
      case TIMEOUT => reminderBot ! this
    }
  }

}

object Reminder {
  // Connect to the SQLite database of reminders
  private lazy val connection:Connection = openConnection();

  def apply(reminderBot:ReminderBot,
    timestampCreate:Long,
    timestampRemind:Long,
    channel:String,
    senderNick:String,
    message:String) : Reminder = {
    new Reminder(reminderBot, timestampCreate, timestampRemind, channel, senderNick, message)
  }

  def reminders(reminderBot:ReminderBot) : List[Reminder] = {
    val resultSet:ResultSet =
      connection.prepareStatement("SELECT * FROM Reminders;").executeQuery()

    val reminderList:List[Reminder] = 
      new Iterator[Reminder] {
        def hasNext = resultSet.next()
        def next() = {
          val timestampCreate:Long = resultSet.getLong("timestamp_create");
          val timestampRemind:Long = resultSet.getLong("timestamp_remind");
          val channel:String = resultSet.getString("channel");
          val senderNick:String = resultSet.getString("senderNick");
          val message:String = resultSet.getString("message");
          Reminder(reminderBot, timestampCreate, timestampRemind, channel, senderNick, message)
        }
      } toList

    resultSet.close();

    reminderList
  }

  /**
   * Open a connection to the DB, creating the Reminders table
   * if it doesn't exist
   */
  private def openConnection() : Connection = {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch {
      case exception:ClassNotFoundException =>
        println("Can't find SQLite JDBC driver")
    }

    val connection:Connection =
      DriverManager.getConnection("jdbc:sqlite:reminders.db")

    connection.createStatement().executeUpdate(
      "CREATE TABLE IF NOT EXISTS Reminders"
      + " ( "
      + "   timestamp_create BIGINT,"
      + "   timestamp_remind BIGINT,"
      + "   channel TEXT,"
      + "   senderNick TEXT,"
      + "   message TEXT,"
      + "   PRIMARY KEY (timestamp_create)"
      + " );")

    connection
  }
}
