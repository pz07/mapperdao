package com.googlecode.mapperdao

import com.googlecode.mapperdao.jdbc.Setup
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, FunSuite}

/**
 * @author kostantinos.kougios
 *
 *         11 Sep 2011
 */
@RunWith(classOf[JUnitRunner])
class ManyToOneAndOneToManyCyclicAutoGeneratedSuite extends FunSuite with Matchers
{

	import ManyToOneAndOneToManyCyclicAutoGeneratedSuite._

	val (jdbc, mapperDao, queryDao) = Setup.setupMapperDao(List(PersonEntity, CompanyEntity))

	def noise() {
		val c1 = mapperDao.insert(CompanyEntity, Company("x", Nil))
		mapperDao.insertBatch(PersonEntity, List(Person("p1", c1), Person("p2", c1)))
	}

	test("batch insert, existing one-part, inserted") {
		createTables()
		noise()
		import mapperDao._

		val c1 = Company("C1", List())
		val c2 = Company("C2", List())
		val List(ic1, ic2) = insertBatch(CompanyEntity, List(c1, c2))
		val p1 = Person("Coder1", ic1)
		val p2 = Person("Coder2", ic2)
		val p3 = Person("Coder3", ic2)
		val List(ip1, ip2, ip3) = insertBatch(PersonEntity, List(p1, p2, p3))
		ip1 should be(p1)
		ip2 should be(p2)
		ip3 should be(p3)

		select(PersonEntity, ip1.id).get should be(Person("Coder1", Company("C1", List(ip1))))
		select(PersonEntity, ip2.id).get should be(Person("Coder2", Company("C2", List(ip2, ip3))))
		select(PersonEntity, ip3.id).get should be(Person("Coder3", Company("C2", List(ip2, ip3))))
	}

	test("batch insert, existing one-part, selected") {
		createTables()
		noise()
		import mapperDao._

		val c1 = Company("C1", List())
		val c2 = Company("C2", List())
		val List(ic1, ic2) = insertBatch(CompanyEntity, List(c1, c2)).map {
			c =>
				select(CompanyEntity, c.id).get
		}
		val p1 = Person("Coder1", ic1)
		val p2 = Person("Coder2", ic2)
		val p3 = Person("Coder3", ic2)
		val List(ip1, ip2, ip3) = insertBatch(PersonEntity, List(p1, p2, p3))
		ip1 should be(p1)
		ip2 should be(p2)
		ip3 should be(p3)

		select(PersonEntity, ip1.id).get should be(Person("Coder1", Company("C1", List(ip1))))
		select(PersonEntity, ip2.id).get should be(Person("Coder2", Company("C2", List(ip2, ip3))))
		select(PersonEntity, ip3.id).get should be(Person("Coder3", Company("C2", List(ip2, ip3))))
	}

	test("batch insert, new one-part") {
		createTables()
		noise()
		import mapperDao._

		val c1 = Company("C1", List())
		val c2 = Company("C2", List())
		val p1 = Person("Coder1", c1)
		val p2 = Person("Coder2", c2)
		val p3 = Person("Coder3", c2)
		val List(ip1, ip2, ip3) = insertBatch(PersonEntity, List(p1, p2, p3))
		ip1 should be(p1)
		ip2 should be(p2)
		ip3 should be(p3)

		select(PersonEntity, ip1.id).get should be(Person("Coder1", Company("C1", List(ip1))))
		select(PersonEntity, ip2.id).get should be(Person("Coder2", Company("C2", List(ip2, ip3))))
		select(PersonEntity, ip3.id).get should be(Person("Coder3", Company("C2", List(ip2, ip3))))
	}

	test("batch update on inserted") {
		createTables()
		noise()
		import mapperDao._

		val c1 = Company("C1", List())
		val c2 = Company("C2", List())
		val c3 = Company("C3", List())
		val p1 = Person("Coder1", c1)
		val p2 = Person("Coder2", c2)
		val p3 = Person("Coder3", c2)
		val List(ip1, ip2, ip3) = insertBatch(PersonEntity, List(p1, p2, p3))

		val up1 = ip1.copy(company = c2)
		val up2 = ip2.copy(company = c3)
		val up3 = ip3.copy(company = c3)

		val List(u1, u2, u3) = updateBatch(PersonEntity, List((ip1, up1), (ip2, up2), (ip3, up3)))
		u1 should be(up1)
		u2 should be(up2)
		u3 should be(up3)

		select(PersonEntity, ip1.id).get should be(Person("Coder1", Company("C2", List(u1))))
		select(PersonEntity, ip2.id).get should be(Person("Coder2", Company("C3", List(u2, u3))))
		select(PersonEntity, ip3.id).get should be(Person("Coder3", Company("C3", List(u2, u3))))
	}

	test("batch update on selected") {
		createTables()
		noise()
		import mapperDao._

		val c1 = Company("C1", List())
		val c2 = Company("C2", List())
		val c3 = Company("C3", List())
		val p1 = Person("Coder1", c1)
		val p2 = Person("Coder2", c2)
		val p3 = Person("Coder3", c2)
		val List(ip1, ip2, ip3) = insertBatch(PersonEntity, List(p1, p2, p3)).map {
			p =>
				select(PersonEntity, p.id).get
		}

		val up1 = ip1.copy(company = c2)
		val up2 = ip2.copy(company = c3)
		val up3 = ip3.copy(company = c3)

		val List(u1, u2, u3) = updateBatch(PersonEntity, List((ip1, up1), (ip2, up2), (ip3, up3)))
		u1 should be(up1)
		u2 should be(up2)
		u3 should be(up3)

		select(PersonEntity, ip1.id).get should be(Person("Coder1", Company("C2", List(u1))))
		select(PersonEntity, ip2.id).get should be(Person("Coder2", Company("C3", List(u2, u3))))
		select(PersonEntity, ip3.id).get should be(Person("Coder3", Company("C3", List(u2, u3))))
	}

	test("batch update, existing many-part, on selected") {
		createTables()
		noise()
		import mapperDao._

		val List(c1, c2, c3) = insertBatch(CompanyEntity, List(Company("C1", List()), Company("C2", List()), Company("C3", List())))
		val p1 = Person("Coder1", c1)
		val p2 = Person("Coder2", c2)
		val p3 = Person("Coder3", c2)
		val List(ip1, ip2, ip3) = insertBatch(PersonEntity, List(p1, p2, p3)).map {
			p =>
				select(PersonEntity, p.id).get
		}

		val up1 = ip1.copy(company = c2)
		val up2 = ip2.copy(company = c3)
		val up3 = ip3.copy(company = c3)

		val List(u1, u2, u3) = updateBatch(PersonEntity, List((ip1, up1), (ip2, up2), (ip3, up3)))
		u1 should be(up1)
		u2 should be(up2)
		u3 should be(up3)

		select(PersonEntity, ip1.id).get should be(Person("Coder1", Company("C2", List(u1))))
		select(PersonEntity, ip2.id).get should be(Person("Coder2", Company("C3", List(u2, u3))))
		select(PersonEntity, ip3.id).get should be(Person("Coder3", Company("C3", List(u2, u3))))
	}

	test("select") {
		createTables()
		import mapperDao._

		val company = insert(CompanyEntity, Company("Coders Ltd", List()))
		val inserted = insert(PersonEntity, Person("Coder1", company))

		// the person in the list is a mock object due to the cyclic dependency, and company is null
		select(PersonEntity, inserted.id).get should be === Person("Coder1",
			Company("Coders Ltd",
				List(
					Person("Coder1",
						Company("Coders Ltd",
							List()
						)
					)
				)
			)
		)
	}

	test("insert") {
		createTables()
		val company = mapperDao.insert(CompanyEntity, Company("Coders Ltd", List()))
		val person = Person("Coder1", company)
		mapperDao.insert(PersonEntity, person) should be === person
	}

	test("update") {
		createTables()

		val company = mapperDao.insert(CompanyEntity, Company("Coders Ltd", List()))
		val inserted = mapperDao.insert(PersonEntity, Person("Coder1", company))

		val selected = mapperDao.select(PersonEntity, inserted.id).get

		val updated = mapperDao.update(PersonEntity, selected, Person("Coder1-changed", company))
		updated should be === Person("Coder1-changed", Company("Coders Ltd", List()))

		mapperDao.select(CompanyEntity, company.id).get should be === Company("Coders Ltd", List(Person("Coder1-changed", Company("Coders Ltd", List()))))
	}

	def createTables() {
		Setup.dropAllTables(jdbc)
		Setup.queries(this, jdbc).update("ddl")
		Setup.database match {
			case "oracle" =>
				Setup.createSeq(jdbc, "CompanySeq")
				Setup.createSeq(jdbc, "PersonSeq")
			case _ =>
		}
	}
}

object ManyToOneAndOneToManyCyclicAutoGeneratedSuite
{

	case class Person(name: String, company: Company)

	case class Company(name: String, employees: List[Person])

	object PersonEntity extends Entity[Int, SurrogateIntId, Person]
	{
		val id = key("id") sequence (Setup.database match {
			case "oracle" => Some("PersonSeq")
			case _ => None
		}) autogenerated (_.id)
		val name = column("name") to (_.name)
		val company = manytoone(CompanyEntity) to (_.company)

		def constructor(implicit m: ValuesMap) = new Person(name, company) with Stored
		{
			val id = m(PersonEntity.id)
		}
	}

	object CompanyEntity extends Entity[Int, SurrogateIntId, Company]
	{
		val id = key("id") sequence (Setup.database match {
			case "oracle" => Some("CompanySeq")
			case _ => None
		}) autogenerated (_.id)
		val name = column("name") to (_.name)
		val employees = onetomany(PersonEntity) to (_.employees)

		def constructor(implicit m: ValuesMap) = new Company(name, employees) with Stored
		{
			val id = m(CompanyEntity.id)
		}
	}

}