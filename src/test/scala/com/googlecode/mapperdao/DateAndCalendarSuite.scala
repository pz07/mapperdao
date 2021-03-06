package com.googlecode.mapperdao

import java.util.{Calendar, Date, Locale}

import com.googlecode.mapperdao.jdbc.Setup
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner
/**
 * @author kostantinos.kougios
 *
 *         9 Dec 2011
 */
@RunWith(classOf[JUnitRunner])
class DateAndCalendarSuite extends FunSuite
{

	case class DC(id: Int, date: Date, calendar: Calendar)

	object DCEntity extends Entity[Int, NaturalIntId, DC]
	{
		val id = key("id") to (_.id)
		val date = column("dt") to (_.date)
		val calendar = column("cal") to (_.calendar)

		def constructor(implicit m: ValuesMap) = new DC(id, date, calendar) with Stored
	}

	val (jdbc, mapperDao, queryDao) = Setup.setupMapperDao(List(DCEntity))

	test("CRUD") {
		createTables()

		val now = Setup.now
		val date = now.toDate
		val calendar = Setup.now.toCalendar(Locale.getDefault)

		mapperDao.insert(DCEntity, DC(1, date, calendar)) should be(DC(1, date, calendar))
		val selected = mapperDao.select(DCEntity, 1).get
		selected should be(DC(1, date, calendar))

		selected.date.setTime(now.minusHours(1).getMillis)
		selected.calendar.add(Calendar.HOUR, -1)
		val updated = mapperDao.update(DCEntity, selected)
		updated should be(selected)
		mapperDao.select(DCEntity, 1).get should be(updated)
	}

	def createTables() {
		Setup.dropAllTables(jdbc)
		Setup.queries(this, jdbc).update("ddl")
	}
}