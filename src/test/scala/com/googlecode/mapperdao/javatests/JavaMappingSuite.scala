package com.googlecode.mapperdao.javatests

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import com.googlecode.mapperdao.Entity
import com.googlecode.mapperdao.SurrogateIntId
import com.googlecode.mapperdao.Persisted
import scala.collection.JavaConverters._
import com.googlecode.mapperdao.ValuesMap

/**
 * @author kostantinos.kougios
 *
 * 3 Jul 2012
 */
@RunWith(classOf[JUnitRunner])
class JavaMappingSuite extends FunSuite with ShouldMatchers {

	test("many to many, get") {
		val p = new Product
		val attrs = new java.util.HashSet[Attribute]
		attrs.add(new Attribute("colour", "red"))
		attrs.add(new Attribute("colour", "green"))
		p.setAttributes(attrs)

		val table = ProductEntityMTM.tpe.table
		val as = ProductEntityMTM.attributes.columnToValue(p)
		as.toSet should be === attrs.asScala
	}

	test("many to many, construct") {
		val p = new Product
		val attrs = new java.util.HashSet[Attribute]
		attrs.add(new Attribute("colour", "red"))
		attrs.add(new Attribute("colour", "green"))
		p.setAttributes(attrs)

		val vm = ValuesMap.fromMap(Map(
			"alias1" -> attrs.asScala.toList
		))
		val pc = ProductEntityMTM.tpe.constructor(null, vm)
		pc.getAttributes.asScala should be === attrs.asScala.toSet
	}

	test("one to many, get") {
		val p = new Product
		val attrs = new java.util.HashSet[Attribute]
		attrs.add(new Attribute("colour", "red"))
		attrs.add(new Attribute("colour", "green"))
		p.setAttributes(attrs)

		val table = ProductEntityOTM.tpe.table
		val as = ProductEntityOTM.attributes.columnToValue(p)
		as.toSet should be === attrs.asScala
	}

	test("one to many, construct") {
		val p = new Product
		val attrs = new java.util.HashSet[Attribute]
		attrs.add(new Attribute("colour", "red"))
		attrs.add(new Attribute("colour", "green"))
		p.setAttributes(attrs)

		val vm = ValuesMap.fromMap(Map(
			"alias1" -> attrs.asScala.toList
		))
		val pc = ProductEntityOTM.tpe.constructor(null, vm)
		pc.getAttributes.asScala should be === attrs.asScala.toSet
	}

	object ProductEntityMTM extends Entity[Int, SurrogateIntId, Product] {
		val id = key("id") autogenerated (_.id)
		val name = column("name") to (_.getName)
		val attributes = manytomany(AttributeEntity) tojava (_.getAttributes)

		def constructor(implicit m) = {
			val p = new Product with SurrogateIntId {
				val id: Int = ProductEntityMTM.id
			}
			p.setName(name)
			p.setAttributes(attributes)
			p
		}
	}

	object ProductEntityOTM extends Entity[Int, SurrogateIntId, Product] {
		val id = key("id") autogenerated (_.id)
		val name = column("name") to (_.getName)
		val attributes = onetomany(AttributeEntity) tojava (_.getAttributes)

		def constructor(implicit m) = {
			val p = new Product with SurrogateIntId {
				val id: Int = ProductEntityMTM.id
			}
			p.setName(name)
			p.setAttributes(attributes)
			p
		}
	}

	object AttributeEntity extends Entity[Int, SurrogateIntId, Attribute] {
		val id = key("id") autogenerated (_.id)
		val name = column("name") to (_.getName)
		val value = column("value") to (_.getValue)

		def constructor(implicit m) = {
			val a = new Attribute(name, value) with SurrogateIntId {
				val id: Int = AttributeEntity.id
			}
			a.setName(name)
			a.setValue(value)
			a
		}
	}
}
