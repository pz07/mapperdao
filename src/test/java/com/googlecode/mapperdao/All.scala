package com.googlecode.mapperdao

import org.junit.runner.RunWith
import org.junit.runners.Suite.SuiteClasses
import org.junit.runners.Suite
import org.scalatest.FunSuite

/**
 * run all Suites within the IDE
 *
 * Note:this won't run when building via maven, as surefire will run each test separately
 *
 * @author kostantinos.kougios
 *
 * 6 Aug 2011
 */
@SuiteClasses(
	Array(
		classOf[utils.EqualitySuite],
		classOf[utils.TraversableSeparationSuite],
		classOf[EntityMapSuite],
		classOf[ManyToManyAutoGeneratedSuite],
		classOf[ManyToManyMutableAutoGeneratedSuite],
		classOf[ManyToManyNonRecursiveSuite],
		classOf[ManyToManyQuerySuite],
		classOf[ManyToManyQueryWithAliasesSuite],
		classOf[ManyToManySuite],
		classOf[ManyToManyLazyLoadSuite],
		classOf[ManyToOneAndOneToManyCyclicSuite],
		classOf[ManyToOneAndOneToManyCyclicAutoGeneratedSuite],
		classOf[ManyToOneAutoGeneratedSuite],
		classOf[ManyToOneMutableAutoGeneratedSuite],
		classOf[SimpleEntitiesSuite],
		classOf[OneToManyAutoGeneratedSuite],
		classOf[OneToManySuite],
		classOf[OneToManySelfReferencedSuite],
		classOf[OneToOneMutableTwoWaySuite],
		classOf[OneToOneImmutableOneWaySuite],
		classOf[OneToOneAutogeneratedTwoWaySuite],
		classOf[ManyToOneSuite],
		classOf[SimpleQuerySuite],
		classOf[ManyToOneQuerySuite],
		classOf[SimpleSelfJoinQuerySuite],
		classOf[SimpleEntitiesAutoGeneratedSuite],
		classOf[ManyToOneSelfJoinQuerySuite],
		classOf[OneToManyQuerySuite],
		classOf[OneToOneQuerySuite],
		classOf[OneToOneWithoutReverseSuite],
		classOf[TwoPrimaryKeysSimpleSuite],
		classOf[IntermediateImmutableEntityWithStringFKsSuite],
		classOf[jdbc.JdbcSuite],
		classOf[jdbc.TransactionSuite],
		classOf[utils.DaoMixinsSuite],
		classOf[CRUDConfigsSuite],
		classOf[MemoryMapperDaoSuite],
		classOf[MockMapperDaoSuite],
		classOf[MockQueryDaoSuite],
		classOf[MockDaosSuite],
		classOf[DeclarePrimaryKeysSuite],
		classOf[UpdateConfigSuite],
		classOf[utils.HelpersSuite],
		classOf[OptionSuite],
		classOf[OneToManySimpleTypesSuite],
		classOf[ManyToManySimpleTypesSuite],
		classOf[DateAndCalendarSuite],
		classOf[QueryDomainModelSuite],
		classOf[QueryAutogeneratedSuite],
		classOf[ManyToManyExternalEntitySuite],
		classOf[ManyToOneExternalEntitySuite],
		classOf[OneToOneReverseExternalEntitySuite],
		classOf[OneToManyExternalEntitySuite],
		classOf[UseCaseFileSystemSuite],
		classOf[UseCaseMapRawColumnOneToManySuite],
		classOf[CachedDriverSuite],
		//classOf[ehcache.EHCacheSuite],
		classOf[CachingEndToEndSuite],
		classOf[ManyToOneLazyLoadSuite],
		classOf[OneToManyLazyLoadSuite],
		classOf[ManyToManyManuallyLazyLoadSuite],
		classOf[LazyLoadManagerSuite],
		classOf[OneToOneLazyLoadSuite],
		classOf[LinkSuite],
		classOf[OneToManyDeclarePrimaryKeysSuite],
		classOf[ValuesMapSuite],
		classOf[NullValuesSuite],
		classOf[OneToManyWithoutFKQuerySuite],
		classOf[EntityRelationshipVisitorSuite]
	//classOf[MultiThreadedQuerySuite]
	)
)
@RunWith(classOf[Suite])
class All