package com.googlecode.mapperdao

import com.googlecode.mapperdao.jdbc.Setup
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner
/**
 * @author kostantinos.kougios
 *
 *         5 Sep 2011
 */
@RunWith(classOf[JUnitRunner])
class ManyToOneAutoGeneratedSuite extends FunSuite
{
	val (jdbc, mapperDao, queryDao) = Setup.setupMapperDao(List(PersonEntity, CompanyEntity, HouseEntity))

	def noise() {
		mapperDao.insert(PersonEntity, Person("X", Company("X"), House("X")))
	}

	test("batch insert, on new one-part") {
		createTables()
		noise()

		val c1 = Company("C1")
		val h1 = House("H1")
		val c2 = Company("C2")
		val h2 = House("H2")

		val p1 = Person("P1", c1, h1)
		val p2 = Person("P2", c1, h1)
		val p3 = Person("P3", c2, h2)

		val List(i1, i2, i3) = mapperDao.insertBatch(PersonEntity, List(p1, p2, p3))
		i1 should be(p1)
		i2 should be(p2)
		i3 should be(p3)

		mapperDao.select(PersonEntity, i1.id).get should be(i1)
		mapperDao.select(PersonEntity, i2.id).get should be(i2)
		mapperDao.select(PersonEntity, i3.id).get should be(i3)
	}

	test("batch insert, on existing one-part") {
		createTables()
		noise()

		val List(c1, c2) = mapperDao.insertBatch(CompanyEntity, List(Company("C1"), Company("C2")))
		val List(h1, h2) = mapperDao.insertBatch(HouseEntity, List(House("H1"), House("H2")))

		val p1 = Person("P1", c1, h1)
		val p2 = Person("P2", c1, h1)
		val p3 = Person("P3", c2, h2)

		val List(i1, i2, i3) = mapperDao.insertBatch(PersonEntity, List(p1, p2, p3))
		i1 should be(p1)
		i2 should be(p2)
		i3 should be(p3)

		mapperDao.select(PersonEntity, i1.id).get should be(i1)
		mapperDao.select(PersonEntity, i2.id).get should be(i2)
		mapperDao.select(PersonEntity, i3.id).get should be(i3)
	}

	test("batch update, on inserted") {
		createTables()
		noise()

		val List(c1, c2) = mapperDao.insertBatch(CompanyEntity, List(Company("C1"), Company("C2")))
		val List(h1, h2) = mapperDao.insertBatch(HouseEntity, List(House("H1"), House("H2")))

		val p1 = Person("P1", c1, h1)
		val p2 = Person("P2", c1, h1)
		val p3 = Person("P3", c2, h2)

		val List(i1, i2, i3) = mapperDao.insertBatch(PersonEntity, List(p1, p2, p3))

		val u1 = i1.copy(company = c2, lives = h2)
		val u2 = i2.copy(company = c2, lives = h2)
		val u3 = i3.copy(company = c1, lives = h1)

		val List(up1, up2, up3) = mapperDao.updateBatch(PersonEntity, List((i1, u1), (i2, u2), (i3, u3)))
		up1 should be(u1)
		up2 should be(u2)
		up3 should be(u3)

		mapperDao.select(PersonEntity, i1.id).get should be(up1)
		mapperDao.select(PersonEntity, i2.id).get should be(up2)
		mapperDao.select(PersonEntity, i3.id).get should be(up3)
	}

	test("batch update, on selected") {
		createTables()
		noise()

		val List(c1, c2) = mapperDao.insertBatch(CompanyEntity, List(Company("C1"), Company("C2")))
		val List(h1, h2) = mapperDao.insertBatch(HouseEntity, List(House("H1"), House("H2")))

		val p1 = Person("P1", c1, h1)
		val p2 = Person("P2", c1, h1)
		val p3 = Person("P3", c2, h2)

		val List(i1, i2, i3) = mapperDao.insertBatch(PersonEntity, List(p1, p2, p3)).map {
			p =>
				mapperDao.select(PersonEntity, p.id).get
		}

		val u1 = i1.copy(company = c2, lives = h2)
		val u2 = i2.copy(company = c2, lives = h2)
		val u3 = i3.copy(company = c1, lives = h1)

		val List(up1, up2, up3) = mapperDao.updateBatch(PersonEntity, List((i1, u1), (i2, u2), (i3, u3)))
		up1 should be(u1)
		up2 should be(u2)
		up3 should be(u3)

		mapperDao.select(PersonEntity, i1.id).get should be(up1)
		mapperDao.select(PersonEntity, i2.id).get should be(up2)
		mapperDao.select(PersonEntity, i3.id).get should be(up3)
	}

	test("select with skip") {
		createTables()

		val company = Company("Coders limited")
		val house = House("Rhodes,Greece")
		val person = Person("Kostas", company, house)

		val inserted = mapperDao.insert(PersonEntity, person)
		mapperDao.select(SelectConfig(skip = Set(PersonEntity.company)), PersonEntity, inserted.id).get should be(Person("Kostas", null, house))
		mapperDao.select(SelectConfig(skip = Set(PersonEntity.lives)), PersonEntity, inserted.id).get should be(Person("Kostas", company, null))
	}

	test("update to null both FK") {
		createTables()

		val company1 = mapperDao.insert(CompanyEntity, Company("Coders limited"))
		val house = House("Rhodes,Greece")
		val person = Person("Kostas", company1, house)

		val inserted = mapperDao.insert(PersonEntity, person)
		inserted should be(person)

		val modified = Person("changed", null, null)
		val updated = mapperDao.update(PersonEntity, inserted, modified)
		updated should be(modified)

		val selected = mapperDao.select(PersonEntity, inserted.id).get
		selected should be(updated)

		mapperDao.delete(PersonEntity, selected)
		mapperDao.select(PersonEntity, selected.id) should be(None)
	}

	test("update to null") {
		createTables()

		val company1 = mapperDao.insert(CompanyEntity, Company("Coders limited"))
		val house = House("Rhodes,Greece")
		val person = Person("Kostas", company1, house)

		val inserted = mapperDao.insert(PersonEntity, person)
		inserted should be(person)

		val modified = Person("changed", null, inserted.lives)
		val updated = mapperDao.update(PersonEntity, inserted, modified)
		updated should be(modified)

		val selected = mapperDao.select(PersonEntity, updated.id).get
		selected should be(updated)

		mapperDao.delete(PersonEntity, selected)
		mapperDao.select(PersonEntity, selected.id) should be(None)
	}

	test("insert") {
		createTables()

		val company = Company("Coders limited")
		val house = House("Rhodes,Greece")
		val person = Person("Kostas", company, house)

		val inserted = mapperDao.insert(PersonEntity, person)
		inserted should be(person)
	}

	test("insert with existing foreign entity") {
		createTables()

		import mapperDao._
		// randomizing the autogenerated id's
		insert(CompanyEntity, Company("A1"))
		insert(CompanyEntity, Company("A2"))

		val company = insert(CompanyEntity, Company("Coders limited"))
		val house = House("Rhodes,Greece")
		val person = Person("Kostas", company, house)

		val inserted = insert(PersonEntity, person)
		inserted should be(person)

		val selected = select(PersonEntity, inserted.id).get
		selected should be(inserted)

		mapperDao.delete(PersonEntity, inserted)
		mapperDao.select(PersonEntity, inserted.id) should be(None)
	}

	test("select") {
		createTables()

		import mapperDao._
		val company = Company("Coders limited")
		val house = House("Rhodes,Greece")
		val person = Person("Kostas", company, house)

		val inserted = insert(PersonEntity, person)

		val selected = select(PersonEntity, inserted.id).get
		selected should be(inserted)

		mapperDao.delete(PersonEntity, inserted)
		mapperDao.select(PersonEntity, inserted.id) should be(None)
	}

	test("select with null FK") {
		createTables()

		import mapperDao._
		val house = House("Rhodes,Greece")
		val person = Person("Kostas", null, house)

		val inserted = insert(PersonEntity, person)

		val selected = select(PersonEntity, inserted.id).get
		selected should be(inserted)

		mapperDao.delete(PersonEntity, inserted)
		mapperDao.select(PersonEntity, inserted.id) should be(None)
	}

	test("update") {
		createTables()

		val company1 = mapperDao.insert(CompanyEntity, Company("Coders limited"))
		val company2 = mapperDao.insert(CompanyEntity, Company("Scala Inc"))
		val house = House("Rhodes,Greece")
		val person = Person("Kostas", company1, house)

		val inserted = mapperDao.insert(PersonEntity, person)
		inserted should be(person)

		val modified = Person("changed", company2, inserted.lives)
		val updated = mapperDao.update(PersonEntity, inserted, modified)
		updated should be(modified)

		val selected = mapperDao.select(PersonEntity, updated.id).get
		selected should be(updated)

		mapperDao.delete(PersonEntity, selected)
		mapperDao.select(PersonEntity, selected.id) should be(None)
	}

	test("update, only related") {
		createTables()

		val company1 = mapperDao.insert(CompanyEntity, Company("Coders limited"))
		val company2 = mapperDao.insert(CompanyEntity, Company("Scala Inc"))
		val house = House("Rhodes,Greece")
		val person = Person("Kostas", company1, house)

		val inserted = mapperDao.insert(PersonEntity, person)
		inserted should be(person)

		val modified = inserted.copy(company = company2)
		val updated = mapperDao.update(PersonEntity, inserted, modified)
		updated should be(modified)

		val selected = mapperDao.select(PersonEntity, updated.id).get
		selected should be(updated)
	}

	test("update, to non-existing entity") {
		createTables()

		val company1 = mapperDao.insert(CompanyEntity, Company("Coders limited"))
		val company2 = Company("Scala Inc")
		val house = House("Rhodes,Greece")
		val person = Person("Kostas", company1, house)

		val inserted = mapperDao.insert(PersonEntity, person)
		inserted should be(person)

		val modified = inserted.copy(company = company2)
		val updated = mapperDao.update(PersonEntity, inserted, modified)
		updated should be(modified)

		val selected = mapperDao.select(PersonEntity, updated.id).get
		selected should be(updated)
	}

	def createTables() {
		Setup.dropAllTables(jdbc)
		Setup.queries(this, jdbc).update("ddl")
		Setup.database match {
			case "oracle" =>
				Setup.createSeq(jdbc, "CompanySeq")
				Setup.createSeq(jdbc, "HouseSeq")
				Setup.createSeq(jdbc, "PersonSeq")
			case _ =>
		}
	}

	case class Person(name: String, company: Company, lives: House)

	case class Company(name: String)

	case class House(address: String)

	object PersonEntity extends Entity[Int, SurrogateIntId, Person]
	{
		val id = key("id") sequence (Setup.database match {
			case "oracle" => Some("PersonSeq")
			case _ => None
		}) autogenerated (_.id)
		val name = column("name") to (_.name)
		val company = manytoone(CompanyEntity) to (_.company)
		val lives = manytoone(HouseEntity) to (_.lives)

		def constructor(implicit m: ValuesMap) = new Person(name, company, lives) with Stored
		{
			val id: Int = PersonEntity.id
		}
	}

	object CompanyEntity extends Entity[Int, SurrogateIntId, Company]
	{
		val id = key("id") sequence (Setup.database match {
			case "oracle" => Some("CompanySeq")
			case _ => None
		}) autogenerated (_.id)
		val name = column("name") to (_.name)

		def constructor(implicit m: ValuesMap) = new Company(name) with Stored
		{
			val id: Int = CompanyEntity.id
		}
	}

	object HouseEntity extends Entity[Int, SurrogateIntId, House]
	{
		val id = key("id") sequence (Setup.database match {
			case "oracle" => Some("HouseSeq")
			case _ => None
		}) autogenerated (_.id)
		val address = column("address") to (_.address)

		def constructor(implicit m: ValuesMap) = new House(address) with Stored
		{
			val id: Int = HouseEntity.id
		}
	}

}