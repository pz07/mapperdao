package com.rits.orm

import org.specs2.mutable.SpecificationWithJUnit
import org.junit.runner.RunWith
import org.junit.runners.Suite.SuiteClasses
import org.junit.runners.Suite
import com.rits.orm.utils.ISetSpec

/**
 * @author kostantinos.kougios
 *
 * 6 Aug 2011
 */
@RunWith(classOf[Suite])
@SuiteClasses(
	Array(
		classOf[SimpleTypesSpec],
		classOf[OneToManyAutoGeneratedSpec],
		classOf[OneToManySpec],
		classOf[OneToManySelfReferencedSpec],
		classOf[EntityMapSpec],
		classOf[ManyToManySpec],
		classOf[ManyToManyAutoGeneratedSpec],
		classOf[ManyToManyNonRecursiveSpec],
		classOf[ISetSpec],
		classOf[ManyToOneSpec],
		classOf[SimpleQuerySpec],
		classOf[ManyToOneQuerySpec],
		classOf[SimpleSelfJoinQuerySpec],
		classOf[ManyToOneSelfJoinQuerySpec],
		classOf[OneToManyQuerySpec],
		classOf[ManyToManyQuerySpec]
	)
)
class All extends SpecificationWithJUnit