package com.googlecode.mapperdao
import com.googlecode.mapperdao.jdbc.Setup
import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * @author kostantinos.kougios
 *
 * Jan 18, 2012
 */
@RunWith(classOf[JUnitRunner])
class ManyToManyUseExternalLoaderSuite extends FunSuite with ShouldMatchers {
	val (jdbc, mapperDao, queryDao) = Setup.setupMapperDao(TypeRegistry(ProductEntity, AttributeEntity))

	if (Setup.database == "h2") {
		test("persists/select") {
			createTables

			val product = Product("p1", Set(Attribute(10, "x10"), Attribute(20, "x20")))
			val inserted = mapperDao.insert(ProductEntity, product)
			inserted should be === product
			mapperDao.select(ProductEntity, inserted.id).get should be === inserted
		}
		test("updates/select, remove item") {
			createTables

			val product = Product("p1", Set(Attribute(10, "x10"), Attribute(20, "x20")))
			val inserted = mapperDao.insert(ProductEntity, product)
			val toUpdate = Product("p2", inserted.attributes.filterNot(_.id == 10))
			val updated = mapperDao.update(ProductEntity, inserted, toUpdate)
			updated should be === toUpdate
			mapperDao.select(ProductEntity, inserted.id).get should be === updated
		}
		test("updates/select, add item") {
			createTables

			val product = Product("p1", Set(Attribute(10, "x10")))
			val inserted = mapperDao.insert(ProductEntity, product)
			val toUpdate = Product("p2", inserted.attributes + Attribute(20, "x20"))
			val updated = mapperDao.update(ProductEntity, inserted, toUpdate)
			updated should be === toUpdate
			mapperDao.select(ProductEntity, inserted.id).get should be === updated
		}
	}

	def createTables {
		Setup.dropAllTables(jdbc)
		Setup.queries(this, jdbc).update("ddl")
	}

	case class Product(val name: String, val attributes: Set[Attribute])
	case class Attribute(val id: Int, val name: String)

	object ProductEntity extends Entity[IntId, Product](classOf[Product]) {
		val id = key("id") autogenerated (_.id)
		val name = column("name") to (_.name)
		val attributes = manytomany(AttributeEntity) to (_.attributes)

		def constructor(implicit m) = new Product(name, attributes) with IntId with Persisted {
			val id: Int = ProductEntity.id
		}
	}

	object AttributeEntity extends ExternalEntity[Int, Attribute](classOf[Attribute]) {

		def primaryKeyValues(a) = List(a.id)

		override def select(selectConfig: SelectConfig, ids: List[List[Int]]) = ids.map { idL =>
			val id = idL.head
			Attribute(id, "x" + id)
		}
	}

}