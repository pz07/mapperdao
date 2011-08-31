package com.rits.orm

import org.specs2.mutable.SpecificationWithJUnit
import com.rits.jdbc.Jdbc
import com.rits.jdbc.Setup

/**
 * this spec is self contained, all entities, mapping are contained in this class
 *
 * @author kostantinos.kougios
 *
 * 12 Jul 2011
 */
class OneToManyAutoGeneratedSpec extends SpecificationWithJUnit {

	import OneToManyAutoGeneratedSpec._

	val (jdbc, mapperDao) = setup

	"updating items (immutable)" in {
		createTables

		val jp1 = new JobPosition("C++ Developer", 10)
		val jp2 = new JobPosition("Scala Developer", 10)
		val jp3 = new JobPosition("Java Developer", 10)
		val jp4 = new JobPosition("Web Designer", 10)
		val jp5 = new JobPosition("Graphics Designer", 10)

		// we need to apply the same sorting for JobPositions in order for the case classes to be equal because order of list items matters
		val person = Person("Kostas", "K", Set(House("London"), House("Rhodes")), 16, List(jp1, jp2, jp3).sortWith(_.name < _.name))
		val inserted = mapperDao.insert(PersonEntity, person)
		inserted must_== person
		var updated = inserted
		def doUpdate(from: Person with IntId, to: Person) =
			{
				updated = mapperDao.update(PersonEntity, from, to)
				val localUpdated = updated // for debugging
				updated must_== to
				val id = updated.id
				val loaded = mapperDao.select(PersonEntity, id).get
				loaded must_== updated
				loaded must_== to
			}
		doUpdate(updated, new Person("Changed", "K", updated.owns, 18, updated.positions.filterNot(_ == jp1)))
		doUpdate(updated, new Person("Changed Again", "Surname changed too", updated.owns.filter(_.address == "London"), 18, jp5 :: updated.positions.filterNot(jp => jp == jp1 || jp == jp3)))

		mapperDao.delete(PersonEntity, updated)
		mapperDao.select(PersonEntity, updated.id) must beNone
	}

	"updating items (mutable)" in {
		createTables

		val jp1 = new JobPosition("C++ Developer", 10)
		val jp2 = new JobPosition("Scala Developer", 10)
		val jp3 = new JobPosition("Java Developer", 10)
		val person = new Person("Kostas", "K", Set(House("London"), House("Rhodes")), 16, List(jp1, jp2, jp3))
		val inserted = mapperDao.insert(PersonEntity, person)

		inserted.positions.foreach(_.name = "changed")
		inserted.positions.foreach(_.rank = 5)
		val updated = mapperDao.update(PersonEntity, inserted)
		updated must_== inserted

		val loaded = mapperDao.select(PersonEntity, inserted.id).get
		loaded must_== updated

		mapperDao.delete(PersonEntity, updated)
		mapperDao.select(PersonEntity, updated.id) must beNone
	}

	"removing items" in {
		createTables

		val jp1 = new JobPosition("C++ Developer", 10)
		val jp2 = new JobPosition("Scala Developer", 10)
		val jp3 = new JobPosition("Java Developer", 10)
		val person = new Person("Kostas", "K", Set(House("London"), House("Rhodes")), 16, List(jp1, jp2, jp3))
		val inserted = mapperDao.insert(PersonEntity, person)

		inserted.positions = inserted.positions.filterNot(jp => jp == jp1 || jp == jp3)
		val updated = mapperDao.update(PersonEntity, inserted)
		updated must_== inserted

		val loaded = mapperDao.select(PersonEntity, inserted.id).get
		loaded must_== updated

		mapperDao.delete(PersonEntity, updated)
		mapperDao.select(PersonEntity, updated.id) must beNone
	}

	"adding items" in {
		createTables

		val person = new Person("Kostas", "K", Set(House("London"), House("Rhodes")), 16, List(new JobPosition("Scala Developer", 10), new JobPosition("Java Developer", 10)))
		val inserted = mapperDao.insert(PersonEntity, person)
		val id = inserted.id

		val loaded = mapperDao.select(PersonEntity, id).get

		// add more elements to the collection
		loaded.positions = (new JobPosition("Groovy Developer", 5) :: new JobPosition("C++ Developer", 8) :: loaded.positions).sortWith(_.name < _.name)
		val updatedPositions = mapperDao.update(PersonEntity, loaded)
		updatedPositions must_== loaded

		val updatedReloaded = mapperDao.select(PersonEntity, id).get
		updatedReloaded must_== updatedPositions

		mapperDao.delete(PersonEntity, updatedReloaded)
		mapperDao.select(PersonEntity, updatedReloaded.id) must beNone
	}

	"using already persisted entities" in {
		createTables
		jdbc.update("alter table House alter person_id drop not null")
		val h1 = mapperDao.insert(HouseEntity, House("London"))
		val h2 = mapperDao.insert(HouseEntity, House("Rhodes"))
		val h3 = mapperDao.insert(HouseEntity, House("Santorini"))

		val person = new Person("Kostas", "K", Set(h1, h2), 16, List(new JobPosition("Scala Developer", 10), new JobPosition("Java Developer", 10)))
		val inserted = mapperDao.insert(PersonEntity, person)
		val id = inserted.id

		mapperDao.select(PersonEntity, id).get must_== inserted

		val changed = Person("changed", "K", inserted.owns.filterNot(_ == h1), 18, List())
		val updated = mapperDao.update(PersonEntity, inserted, changed)
		updated must_== changed

		mapperDao.select(PersonEntity, id).get must_== updated
	}

	"CRUD (multi purpose test)" in {
		createTables

		val items = for (i <- 1 to 10) yield {
			val person = new Person("Kostas", "K", Set(House("London"), House("Rhodes")), 16, List(new JobPosition("Java Developer", 10), new JobPosition("Scala Developer", 10)))
			val inserted = mapperDao.insert(PersonEntity, person)
			inserted must_== person

			val id = inserted.id

			val loaded = mapperDao.select(PersonEntity, id).get
			loaded must_== person

			// update

			loaded.name = "Changed"
			loaded.age = 24
			loaded.positions.head.name = "Java/Scala Developer"
			loaded.positions.head.rank = 123
			val updated = mapperDao.update(PersonEntity, loaded)
			updated must_== loaded

			val reloaded = mapperDao.select(PersonEntity, id).get
			reloaded must_== loaded

			// add more elements to the collection
			reloaded.positions = new JobPosition("C++ Developer", 8) :: reloaded.positions
			val updatedPositions = mapperDao.update(PersonEntity, reloaded)
			updatedPositions must_== reloaded

			val updatedReloaded = mapperDao.select(PersonEntity, id).get
			updatedReloaded must_== updatedPositions

			// remove elements from the collection
			updatedReloaded.positions = updatedReloaded.positions.filterNot(_ == updatedReloaded.positions(1))
			val removed = mapperDao.update(PersonEntity, updatedReloaded)
			removed must_== updatedReloaded

			val removedReloaded = mapperDao.select(PersonEntity, id).get
			removedReloaded must_== removed

			// remove them all
			removedReloaded.positions = List()
			mapperDao.update(PersonEntity, removedReloaded) must_== removedReloaded

			val rereloaded = mapperDao.select(PersonEntity, id).get
			rereloaded must_== removedReloaded
			rereloaded.positions = List(JobPosition("Java Developer %d".format(i), 15 + i))
			rereloaded.name = "final name %d".format(i)
			val reupdated = mapperDao.update(PersonEntity, rereloaded)

			val finalLoaded = mapperDao.select(PersonEntity, id).get
			finalLoaded must_== reupdated
			(id, finalLoaded)
		}
		val ids = items.map(_._1)
		ids.toSet.size must_== ids.size
		// verify all still available
		items.foreach { item =>
			mapperDao.select(PersonEntity, item._1).get must_== item._2
		}
		success
	}

	def createTables {
		jdbc.update("drop table if exists Person cascade")
		jdbc.update("""
			create table Person (
				id serial not null,
				name varchar(100) not null,
				surname varchar(100) not null,
				age int not null,
				primary key (id)
			)
		""")
		jdbc.update("drop table if exists JobPosition cascade")
		jdbc.update("""
			create table JobPosition (
				id serial not null,
				name varchar(100) not null,
				rank int not null,
				person_id int not null,
				primary key (id),
				constraint FK_JobPosition_Person foreign key (person_id) references Person(id) on delete cascade on update cascade
			)
		""")
		jdbc.update("drop table if exists House cascade")
		jdbc.update("""
			create table House (
				id serial not null,
				address varchar(100) not null,
				person_id int not null,
				primary key (id),
				constraint FK_House_Person foreign key (person_id) references Person(id) on delete cascade on update cascade
			)
		""")
	}
	def setup =
		{
			val typeRegistry = TypeRegistry(JobPositionEntity, HouseEntity, PersonEntity)

			Setup.setupMapperDao(typeRegistry)
		}

}

object OneToManyAutoGeneratedSpec {
	/**
	 * ============================================================================================================
	 * the entities
	 * ============================================================================================================
	 */
	/**
	 * the only reason this is a case class, is to ease testing. There is no requirement
	 * for persisted classes to follow any convention.
	 *
	 * Also the only reason for this class to be mutable is for testing. In a real application
	 * it could be immutable.
	 */
	case class JobPosition(var name: String, var rank: Int) {
		// this can have any arbitrary methods, no problem!
		def whatRank = rank
		// also any non persisted fields, no prob! It's up to the mappings regarding which fields will be used
		val whatever = 5
	}

	/**
	 * the only reason this is a case class, is to ease testing. There is no requirement
	 * for persisted classes to follow any convention
	 *
	 * Also the only reason for this class to be mutable is for testing. In a real application
	 * it could be immutable.
	 */
	case class Person(var name: String, val surname: String, owns: Set[House], var age: Int, var positions: List[JobPosition]) {
	}

	case class House(val address: String)

	/**
	 * ============================================================================================================
	 * Mapping for JobPosition class
	 * ============================================================================================================
	 */

	object JobPositionEntity extends Entity[IntId, JobPosition]("JobPosition", classOf[JobPosition]) {
		val id = autoGeneratedPK("id", _.id) // this is the primary key
		val name = string("name", _.name) // _.name : JobPosition => Any . Function that maps the column to the value of the object
		val rank = int("rank", _.rank)

		val constructor = (m: ValuesMap) => new JobPosition(m(name), m(rank)) with Persisted with IntId {
			// this holds the original values of the object as retrieved from the database.
			// later on it is used to compare what changed in this object.
			val valuesMap = m
			val id = m(JobPositionEntity.id)
		}
	}

	object HouseEntity extends Entity[IntId, House]("House", classOf[House]) {
		val id = autoGeneratedPK("id", _.id)
		val address = string("address", _.address)

		val constructor = (m: ValuesMap) => new House(m(address)) with Persisted with IntId {
			val valuesMap = m
			val id = m(HouseEntity.id)
		}
	}

	object PersonEntity extends Entity[IntId, Person]("Person", classOf[Person]) {
		val id = autoGeneratedPK("id", _.id)
		val name = string("name", _.name)
		val surname = string("surname", _.surname)
		val owns = oneToMany(classOf[House], "person_id", _.owns)
		val age = int("age", _.age)
		/**
		 * a traversable one-to-many relationship with JobPositions.
		 * The type of the relationship is classOf[JobPosition] and the alias
		 * for retrieving the Traversable is jobPositionsAlias. This is used above, when
		 * creating Person: new Person(....,m.toList("jobPositionsAlias")) .
		 * JobPositions table has a person_id foreign key which references Person table.
		 */
		val jobPositions = oneToMany(classOf[JobPosition], "person_id", _.positions)

		val constructor = (m: ValuesMap) => new Person(m(name), m(surname), m(owns).toSet, m(age), m(jobPositions).toList.sortWith(_.name < _.name)) with Persisted with IntId {
			val valuesMap = m
			val id = m(PersonEntity.id)
		}
	}
}