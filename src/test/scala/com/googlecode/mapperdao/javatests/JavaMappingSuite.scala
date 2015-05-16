package com.googlecode.mapperdao.javatests

import com.googlecode.mapperdao.CommonEntities.createProductAttribute
import com.googlecode.mapperdao.jdbc.Setup
import com.googlecode.mapperdao.{Entity, SurrogateIntId, ValuesMap}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConverters._
/**
 * @author kostantinos.kougios
 *
 *         3 Jul 2012
 */
@RunWith(classOf[JUnitRunner])
class JavaMappingSuite extends FunSuite
{
	test("CRUD") {

		val (jdbc, mapperDao, _) = Setup.setupMapperDao(List(ProductEntityMTM, AttributeEntity))
		createProductAttribute(jdbc)

		val p = new Product
		p.setName("test")
		val attrs = new java.util.HashSet[Attribute]
		val a1 = new Attribute("colour", "red")
		attrs.add(a1)
		val a2 = new Attribute("colour", "green")
		attrs.add(a2)
		p.setAttributes(attrs)
		val i1 = mapperDao.insert(ProductEntityMTM, p)
		i1.getName should be("test")
		i1.getAttributes.size should be(2)
		i1.getAttributes.contains(a1) should be(true)
		i1.getAttributes.contains(a2) should be(true)

		i1.getAttributes.remove(a1)
		i1.setName("changed")
		val u1 = mapperDao.update(ProductEntityMTM, i1)
		u1.getName should be("changed")
		u1.getAttributes.size should be(1)
		u1.getAttributes.contains(a2) should be(true)

		val s1 = mapperDao.select(ProductEntityMTM, u1.id).get
		s1.getName should be("changed")
		s1.getAttributes.size should be(1)
		s1.getAttributes.contains(a2) should be(true)

		s1.setName("changed again")
		s1.getAttributes.add(a1)
		val u2 = mapperDao.update(ProductEntityMTM, s1)
		u2.getName should be("changed again")
		u2.getAttributes.size should be(2)
		u2.getAttributes.contains(a1) should be(true)
		u2.getAttributes.contains(a2) should be(true)

		val s2 = mapperDao.select(ProductEntityMTM, u2.id).get
		s2.getName should be("changed again")
		s2.getAttributes.size should be(2)
		s2.getAttributes.contains(a1) should be(true)
		s2.getAttributes.contains(a2) should be(true)
	}

	test("many to many, get") {
		val p = new Product
		val attrs = new java.util.HashSet[Attribute]
		attrs.add(new Attribute("colour", "red"))
		attrs.add(new Attribute("colour", "green"))
		p.setAttributes(attrs)

		val as = ProductEntityMTM.attributes.columnToValue(p)
		as.toSet should be === attrs.asScala
	}

	test("many to many, construct") {
		val p = new Product
		val attrs = new java.util.HashSet[Attribute]
		attrs.add(new Attribute("colour", "red"))
		attrs.add(new Attribute("colour", "green"))
		p.setAttributes(attrs)

		val vm = ValuesMap.fromMap(System.identityHashCode(p), Map(
			"product:1" -> attrs.asScala.toList
		))
		val pc = ProductEntityMTM.tpe.constructor(null, null, vm)
		pc.getAttributes.asScala should be === attrs.asScala.toSet
	}

	test("one to many, get") {
		val p = new Product
		val attrs = new java.util.HashSet[Attribute]
		attrs.add(new Attribute("colour", "red"))
		attrs.add(new Attribute("colour", "green"))
		p.setAttributes(attrs)

		val as = ProductEntityOTM.attributes.columnToValue(p)
		as.toSet should be === attrs.asScala
	}

	test("one to many, construct") {
		val p = new Product
		val attrs = new java.util.HashSet[Attribute]
		attrs.add(new Attribute("colour", "red"))
		attrs.add(new Attribute("colour", "green"))
		p.setAttributes(attrs)

		val vm = ValuesMap.fromMap(System.identityHashCode(p), Map(
			"product:1" -> attrs.asScala.toList
		))
		val pc = ProductEntityOTM.tpe.constructor(null, null, vm)
		pc.getAttributes.asScala should be === attrs.asScala.toSet
	}

	object ProductEntityMTM extends Entity[Int, SurrogateIntId, Product]
	{
		val id = key("id") sequence (Setup.database match {
			case "oracle" => Some("ProductSeq")
			case _ => None
		}) autogenerated (_.id)
		val name = column("name") to (_.getName)
		val attributes = manytomany(AttributeEntity) tojava (_.getAttributes)

		def constructor(implicit m: ValuesMap) = {
			val p = new Product with Stored
			{
				val id: Int = ProductEntityMTM.id
			}
			p.setName(name)
			p.setAttributes(attributes)
			p
		}
	}

	object ProductEntityOTM extends Entity[Int, SurrogateIntId, Product]
	{
		val id = key("id") autogenerated (_.id)
		val name = column("name") to (_.getName)
		val attributes = onetomany(AttributeEntity) tojava (_.getAttributes)

		def constructor(implicit m: ValuesMap) = {
			val p = new Product with Stored
			{
				val id: Int = ProductEntityMTM.id
			}
			p.setName(name)
			p.setAttributes(attributes)
			p
		}
	}

	object AttributeEntity extends Entity[Int, SurrogateIntId, Attribute]
	{
		val id = key("id") sequence (Setup.database match {
			case "oracle" => Some("AttributeSeq")
			case _ => None
		}) autogenerated (_.id)
		val name = column("name") to (_.getName)
		val value = column("value") to (_.getValue)

		def constructor(implicit m: ValuesMap) = {
			val a = new Attribute(name, value) with Stored
			{
				val id: Int = AttributeEntity.id
			}
			a.setName(name)
			a.setValue(value)
			a
		}
	}

}
