package com.googlecode.mapperdao

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.joda.time.chrono.ISOChronology
import utils.Helpers

/**
 * @author kostantinos.kougios
 *
 *         27 Apr 2012
 */
@RunWith(classOf[JUnitRunner])
class LazyLoadManagerSuite extends FunSuite with ShouldMatchers
{

	val lazyLoadManager = new LazyLoadManager
	val typeManager = new DefaultTypeManager(ISOChronology.getInstance)

	test("lazy load Array") {
		val h1 = new House("Rhodes")
		val h2 = new House("Athens")
		val p = new Person("Kostas", Set(), IndexedSeq(), Nil, Array()) with SurrogateLongId
		{
			val id = 5.toLong
		}
		val vm = ValuesMap.fromType(typeManager, PersonEntity.tpe, p)
		vm(PersonEntity.array) = () => List(h1, h2) // vm always stores lists

		val lazyP = lazyLoadManager.proxyFor(p, PersonEntity, LazyLoad.all, vm)
		vm.isLoaded(PersonEntity.array) should be(false)

		lazyP.array should be(Array(h1, h2))
	}

	test("lazy load Set") {
		val h1 = new House("Rhodes")
		val h2 = new House("Athens")
		val p = new Person("Kostas", Set(), IndexedSeq(), Nil, Array()) with SurrogateLongId
		{
			val id = 5.toLong
		}
		val vm = ValuesMap.fromType(typeManager, PersonEntity.tpe, p)
		vm(PersonEntity.owns) = () => List(h1, h2) // vm always stores lists

		val lazyP = lazyLoadManager.proxyFor(p, PersonEntity, LazyLoad.all, vm)
		vm.isLoaded(PersonEntity.owns) should be(false)

		lazyP.owns should be(Set(h1, h2))
	}

	test("lazy load IndexedSeq") {
		val h1 = new House("Rhodes")
		val h2 = new House("Athens")
		val p = new Person("Kostas", Set(), IndexedSeq(), Nil, Array()) with SurrogateLongId
		{
			val id = 5.toLong
		}
		val vm = ValuesMap.fromType(typeManager, PersonEntity.tpe, p)
		vm(PersonEntity.nearby) = () => List(h1, h2) // vm always stores lists

		val lazyP = lazyLoadManager.proxyFor(p, PersonEntity, LazyLoad.all, vm)
		vm.isLoaded(PersonEntity.nearby) should be(false)

		lazyP.nearby should be(IndexedSeq(h1, h2))
	}

	test("lazy load Traversable") {
		val h1 = new House("Rhodes")
		val h2 = new House("Athens")
		val p = new Person("Kostas", Set(), IndexedSeq(), Nil, Array()) with SurrogateLongId
		{
			val id = 5.toLong
		}
		val vm = ValuesMap.fromType(typeManager, PersonEntity.tpe, p)
		vm(PersonEntity.traversable) = () => List(h1, h2) // vm always stores lists

		val lazyP = lazyLoadManager.proxyFor(p, PersonEntity, LazyLoad.all, vm)
		vm.isLoaded(PersonEntity.traversable) should be(false)

		lazyP.traversable.toList should be(List(h1, h2))
	}

	test("lazy load with LongId") {
		val h1 = new House("Rhodes")
		val h2 = new House("Athens")
		val p = new Person("Kostas", Set(), IndexedSeq(), Nil, Array()) with SurrogateLongId
		{
			val id = 5.toLong
		}
		val vm = ValuesMap.fromType(typeManager, PersonEntity.tpe, p)
		vm(PersonEntity.owns) = () => List(h1, h2) // vm always stores lists

		val lazyP = Helpers.asSurrogateLongId(lazyLoadManager.proxyFor(p, PersonEntity, LazyLoad.all, vm))
		lazyP.id should be === 5
	}

	case class Person(name: String,
		owns: Set[House],
		nearby: IndexedSeq[House],
		traversable: Traversable[House],
		array: Array[House])

	case class House(address: String)

	object HouseEntity extends Entity[Long,SurrogateLongId, House]
	{
		val id = key("id") autogenerated (_.id)
		val address = column("address") to (_.address)

		def constructor(implicit m) = new House(address) with Stored
		{
			val id: Long = HouseEntity.id
		}
	}

	object PersonEntity extends Entity[Long,SurrogateLongId, Person]
	{
		val id = key("id") autogenerated (_.id)
		val name = column("name") to (_.name)
		val owns = onetomany(HouseEntity) getter ("owns") to (_.owns)
		val nearby = onetomany(HouseEntity) getter ("nearby") to (_.nearby)
		val traversable = onetomany(HouseEntity) getter ("traversable") to (_.traversable)
		val array = onetomany(HouseEntity) getter ("array") to (_.array)

		def constructor(implicit m) = new Person(name, owns, nearby, m(traversable).toList, array) with Stored
		{
			val id: Long = PersonEntity.id
		}
	}

}