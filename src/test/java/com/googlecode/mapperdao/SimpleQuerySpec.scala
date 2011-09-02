package com.googlecode.mapperdao

import org.specs2.mutable.SpecificationWithJUnit

import com.googlecode.mapperdao.jdbc.Setup
import org.scala_tools.time.Imports._

/**
 * @author kostantinos.kougios
 *
 * 15 Aug 2011
 */
class SimpleQuerySpec extends SpecificationWithJUnit {

	import SimpleQuerySpec._
	val (jdbc, mapperDao, queryDao) = Setup.setupQueryDao(TypeRegistry(JobPositionEntity))

	import SQSQueries._
	import mapperDao._
	import queryDao._

	"query select *" in {
		createJobPositionTable

		val now = Setup.now
		val j1 = insert(JobPositionEntity, JobPosition(5, "developer", now))
		val j2 = insert(JobPositionEntity, JobPosition(6, "web designer", now))
		val j3 = insert(JobPositionEntity, JobPosition(7, "manager", now))
		query(q0).toSet must_== Set(j1, j2, j3)
	}

	"query select where string value" in {
		createJobPositionTable

		val now = Setup.now
		val j1 = insert(JobPositionEntity, JobPosition(5, "developer", now))
		val j2 = insert(JobPositionEntity, JobPosition(6, "Scala Developer", now))
		val j3 = insert(JobPositionEntity, JobPosition(7, "manager", now))
		val j4 = insert(JobPositionEntity, JobPosition(8, "Scala Developer", now))
		query(q1).toSet must_== Set(j2, j4)
	}

	"query select where string value === and int value >" in {
		createJobPositionTable

		val now = Setup.now
		val j5 = insert(JobPositionEntity, JobPosition(5, "developer", now))
		val j6 = insert(JobPositionEntity, JobPosition(6, "Scala Developer", now))
		val j7 = insert(JobPositionEntity, JobPosition(7, "manager", now))
		val j8 = insert(JobPositionEntity, JobPosition(8, "Scala Developer", now))
		val j12 = insert(JobPositionEntity, JobPosition(12, "Scala Developer", now))
		val j9 = insert(JobPositionEntity, JobPosition(9, "x", now))
		query(q2).toSet must_== Set(j8, j12)
	}

	"query select where string value === or int value <" in {
		createJobPositionTable

		val now = Setup.now
		val j5 = insert(JobPositionEntity, JobPosition(5, "developer", now))
		val j6 = insert(JobPositionEntity, JobPosition(6, "Scala Developer", now))
		val j7 = insert(JobPositionEntity, JobPosition(7, "manager", now))
		val j8 = insert(JobPositionEntity, JobPosition(8, "Scala Developer", now))
		val j12 = insert(JobPositionEntity, JobPosition(12, "Scala Developer", now))
		val j9 = insert(JobPositionEntity, JobPosition(9, "x", now))
		query(q3).toSet must_== Set(j5, j6, j8, j12)
	}

	"query select where int value <=" in {
		createJobPositionTable

		val now = Setup.now
		val j5 = insert(JobPositionEntity, JobPosition(5, "developer", now))
		val j6 = insert(JobPositionEntity, JobPosition(6, "Scala Developer", now))
		val j7 = insert(JobPositionEntity, JobPosition(7, "manager", now))
		val j8 = insert(JobPositionEntity, JobPosition(8, "Scala Developer", now))
		val j12 = insert(JobPositionEntity, JobPosition(12, "Scala Developer", now))
		val j9 = insert(JobPositionEntity, JobPosition(9, "x", now))
		query(q4).toSet must_== Set(j5, j6, j7)
	}

	"query select where int value >=" in {
		createJobPositionTable

		val now = Setup.now
		val j5 = insert(JobPositionEntity, JobPosition(5, "developer", now))
		val j6 = insert(JobPositionEntity, JobPosition(6, "Scala Developer", now))
		val j7 = insert(JobPositionEntity, JobPosition(7, "manager", now))
		val j8 = insert(JobPositionEntity, JobPosition(8, "Scala Developer", now))
		val j12 = insert(JobPositionEntity, JobPosition(12, "Scala Developer", now))
		val j9 = insert(JobPositionEntity, JobPosition(9, "x", now))
		query(q5).toSet must_== Set(j7, j8, j12, j9)
	}

	"query select where int value =" in {
		createJobPositionTable

		val now = Setup.now
		val j5 = insert(JobPositionEntity, JobPosition(5, "developer", now))
		val j6 = insert(JobPositionEntity, JobPosition(6, "Scala Developer", now))
		val j7 = insert(JobPositionEntity, JobPosition(7, "manager", now))
		val j8 = insert(JobPositionEntity, JobPosition(8, "Scala Developer", now))
		val j12 = insert(JobPositionEntity, JobPosition(12, "Scala Developer", now))
		val j9 = insert(JobPositionEntity, JobPosition(9, "x", now))
		query(q6).toSet must_== Set(j7)
	}

	"query select where string value like" in {
		createJobPositionTable

		val now = Setup.now
		val j5 = insert(JobPositionEntity, JobPosition(5, "developer", now))
		val j6 = insert(JobPositionEntity, JobPosition(6, "Scala Developer", now))
		val j7 = insert(JobPositionEntity, JobPosition(7, "manager", now))
		val j8 = insert(JobPositionEntity, JobPosition(8, "Scala Developer", now))
		val j12 = insert(JobPositionEntity, JobPosition(12, "Scala Developer", now))
		val j9 = insert(JobPositionEntity, JobPosition(9, "x", now))
		query(q7).toSet must_== Set(j5, j6, j8, j12)
	}

	"query select parenthesis" in {
		createJobPositionTable

		val now = Setup.now
		val j5 = insert(JobPositionEntity, JobPosition(5, "developer", now))
		val j6 = insert(JobPositionEntity, JobPosition(6, "Scala Developer", now))
		val j7 = insert(JobPositionEntity, JobPosition(7, "manager", now))
		val j8 = insert(JobPositionEntity, JobPosition(8, "Scala Developer", now))
		val j12 = insert(JobPositionEntity, JobPosition(12, "Scala Developer", now))
		val j9 = insert(JobPositionEntity, JobPosition(9, "x", now))
		query(q8).toSet must_== Set(j5, j7, j9, j12)
	}

	"query select complex parenthesis" in {
		createJobPositionTable

		val now = Setup.now
		val j10 = insert(JobPositionEntity, JobPosition(10, "correct", now))
		val j11 = insert(JobPositionEntity, JobPosition(11, "correct", now))
		val j12 = insert(JobPositionEntity, JobPosition(12, "wrong", now))
		val j15 = insert(JobPositionEntity, JobPosition(15, "correct", now))
		val j16 = insert(JobPositionEntity, JobPosition(16, "correct", now))
		val j17 = insert(JobPositionEntity, JobPosition(17, "correct", now))
		val j20 = insert(JobPositionEntity, JobPosition(20, "correct", now))

		val j30 = insert(JobPositionEntity, JobPosition(30, "correct", now))
		val j31 = insert(JobPositionEntity, JobPosition(31, "correct", now))
		val j32 = insert(JobPositionEntity, JobPosition(32, "correct", now))
		val j33 = insert(JobPositionEntity, JobPosition(33, "correct", now))
		val j37 = insert(JobPositionEntity, JobPosition(37, "wrong", now))
		val j41 = insert(JobPositionEntity, JobPosition(41, "correct", now))
		query(q9).toSet must_== Set(j11, j15, j16, j17, j31, j32, j33)
	}

	"query select datetime" in {
		createJobPositionTable

		val now = Setup.now
		val j5 = insert(JobPositionEntity, JobPosition(5, "developer", now.minusDays(2)))
		val j6 = insert(JobPositionEntity, JobPosition(6, "Scala Developer", now.minusDays(1)))
		val j7 = insert(JobPositionEntity, JobPosition(7, "manager", now))
		val j8 = insert(JobPositionEntity, JobPosition(8, "Scala Developer", now.plusDays(1)))
		val j12 = insert(JobPositionEntity, JobPosition(12, "Scala Developer", now.plusDays(2)))
		val j9 = insert(JobPositionEntity, JobPosition(9, "x", now.plusDays(3)))
		query(q10).toSet must_== Set(j8, j12)
	}

	def createJobPositionTable {
		Setup.database match {
			case "postgresql" =>
				jdbc.update("drop table if exists JobPosition cascade")
				jdbc.update("""
					create table JobPosition (
						id int not null,
						name varchar(100) not null,
						start timestamp with time zone,
						primary key (id)
					)""")
			case "mysql" =>
				jdbc.update("drop table if exists JobPosition cascade")
				jdbc.update("""
					create table JobPosition (
						id int not null,
						name varchar(100) not null,
						start datetime,
						primary key (id)
					)""")
		}
	}
}

object SimpleQuerySpec {
	// the scala compiler gets an internal error if I embed those inside the spec, due to the implicits!
	object SQSQueries {
		val jpe = JobPositionEntity
		import Query._

		def q0 = select from JobPositionEntity
		def q1 = select from JobPositionEntity where jpe.name === "Scala Developer"
		def q2 = select from JobPositionEntity where jpe.name === "Scala Developer" and jpe.id > 6
		def q3 = select from JobPositionEntity where jpe.name === "Scala Developer" or jpe.id < 7
		def q4 = select from JobPositionEntity where jpe.id <= 7
		def q5 = select from JobPositionEntity where jpe.id >= 7
		def q6 = select from JobPositionEntity where jpe.id === 7
		def q7 = select from JobPositionEntity where (jpe.name like "%eveloper%")
		def q8 = select from JobPositionEntity where (jpe.id >= 9 or jpe.id < 6) or jpe.name === "manager"
		def q9 = select from JobPositionEntity where ((jpe.id > 10 and jpe.id < 20) or (jpe.id > 30 and jpe.id < 40)) and jpe.name === "correct"
		def q10 = select from JobPositionEntity where jpe.start > DateTime.now + 0.days and jpe.start < DateTime.now + 3.days - 60.seconds
	}
	case class JobPosition(val id: Int, var name: String, val start: DateTime)

	object JobPositionEntity extends SimpleEntity(classOf[JobPosition]) {
		val id = pk("id", _.id)
		val name = string("name", _.name)
		val start = datetime("start", _.start)

		val constructor = (m: ValuesMap) => new JobPosition(m(id), m(name), m(start)) with Persisted {
			val valuesMap = m
		}
	}
}