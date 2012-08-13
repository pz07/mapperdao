package com.googlecode.mapperdao

import com.googlecode.mapperdao.drivers.Driver
import scala.collection.mutable.HashMap
import com.googlecode.mapperdao.exceptions._
import com.googlecode.mapperdao.utils.MapOfList
import com.googlecode.mapperdao.plugins._
import com.googlecode.mapperdao.jdbc.JdbcMap
import com.googlecode.mapperdao.plugins.SelectMock
import com.googlecode.mapperdao.events.Events
import com.googlecode.mapperdao.utils.Equality

/**
 * @author kostantinos.kougios
 *
 * 13 Jul 2011
 */
protected final class MapperDaoImpl(val driver: Driver, events: Events, val typeManager: TypeManager) extends MapperDao {
	private val typeRegistry = driver.typeRegistry
	private val lazyLoadManager = new LazyLoadManager

	private val postUpdatePlugins = List[PostUpdate](
		new OneToOneReverseUpdatePlugin(typeRegistry, typeManager, driver, this),
		new OneToManyUpdatePlugin(typeRegistry, this),
		new ManyToManyUpdatePlugin(typeRegistry, driver, this)
	)

	private val duringUpdatePlugins = List[DuringUpdate](
		new ManyToOneUpdatePlugin(typeRegistry, this),
		new OneToManyUpdatePlugin(typeRegistry, this),
		new OneToOneReverseUpdatePlugin(typeRegistry, typeManager, driver, this),
		new OneToOneUpdatePlugin(typeRegistry, this)
	)
	private val beforeInsertPlugins = List[BeforeInsert](
		new ManyToOneInsertPlugin(typeRegistry, this),
		new OneToManyInsertPlugin(typeRegistry, driver, this),
		new OneToOneReverseInsertPlugin(typeRegistry, this),
		new OneToOneInsertPlugin(typeRegistry, this)
	)

	private val postInsertPlugins = List[PostInsert](
		new OneToOneReverseInsertPlugin(typeRegistry, this),
		new OneToManyInsertPlugin(typeRegistry, driver, this),
		new ManyToManyInsertPlugin(typeManager, typeRegistry, driver, this)
	)

	private val selectBeforePlugins: List[BeforeSelect] = List(
		new ManyToOneSelectPlugin(typeRegistry, this),
		new OneToManySelectPlugin(typeRegistry, driver, this),
		new OneToOneReverseSelectPlugin(typeRegistry, driver, this),
		new OneToOneSelectPlugin(typeRegistry, driver, this),
		new ManyToManySelectPlugin(typeRegistry, driver, this)
	)

	private val mockPlugins: List[SelectMock] = List(
		new OneToManySelectPlugin(typeRegistry, driver, this),
		new ManyToManySelectPlugin(typeRegistry, driver, this),
		new ManyToOneSelectPlugin(typeRegistry, this),
		new OneToOneSelectPlugin(typeRegistry, driver, this)
	)

	private val beforeDeletePlugins: List[BeforeDelete] = List(
		new ManyToManyDeletePlugin(driver, this),
		new OneToManyDeletePlugin(typeRegistry, this),
		new OneToOneReverseDeletePlugin(typeRegistry, driver, this),
		new ManyToOneDeletePlugin
	)
	/**
	 * ===================================================================================
	 * Utility methods
	 * ===================================================================================
	 */

	private[mapperdao] def isPersisted(o: Any): Boolean = o.isInstanceOf[Persisted]

	/**
	 * ===================================================================================
	 * CRUD OPERATIONS
	 * ===================================================================================
	 */

	private[mapperdao] def insertInner[PC, T](updateConfig: UpdateConfig, entity: Entity[PC, T], o: T, entityMap: UpdateEntityMap): T with PC with Persisted =
		// if a mock exists in the entity map or already persisted, then return
		// the existing mock/persisted object
		entityMap.get[PC, T](o).getOrElse {

			if (isPersisted(o)) throw new IllegalArgumentException("can't insert an object that is already persisted: " + o);

			val tpe = entity.tpe
			val table = tpe.table

			val modified = ValuesMap.fromEntity(typeManager, tpe, o).toMutableMap
			val modifiedTraversables = new MapOfList[String, Any](MapOfList.stringToLowerCaseModifier)

			val updateInfo @ UpdateInfo(parent, parentColumnInfo, parentEntity) = entityMap.peek[Any, Any, Any, PC, T]

			// create a mock
			var mockO = createMock(updateConfig.data, entity, modified ++ modifiedTraversables)
			entityMap.put(o, mockO)

			val extraArgs = beforeInsertPlugins.map { plugin =>
				plugin.before(updateConfig, entity, o, mockO, entityMap, modified, updateInfo)
			}.flatten.distinct

			// arguments
			val args = table.toListOfColumnAndValueTuples(table.simpleTypeNotAutoGeneratedColumns, o) ::: extraArgs

			// insert entity
			if (!args.isEmpty || !table.simpleTypeAutoGeneratedColumns.isEmpty) {
				events.executeBeforeInsertEvents(tpe, args)
				val ur = driver.doInsert(tpe, args)
				events.executeAfterInsertEvents(tpe, args)

				table.simpleTypeAutoGeneratedColumns.foreach { c =>
					val ag = driver.getAutoGenerated(ur, c)
					// many drivers return the wrong type for the autogenerated
					// keys, typically instead of Int they return Long
					table.pcColumnToColumnInfoMap(c) match {
						case ci: ColumnInfo[_, _] =>
							val fixed = typeManager.toActualType(ci.dataType, ag)
							modified(c.name) = fixed
					}
				}
			}

			// create a more up-to-date mock
			mockO = createMock(updateConfig.data, entity, modified ++ modifiedTraversables)
			entityMap.put(o, mockO)

			postInsertPlugins.foreach { plugin =>
				plugin.after(updateConfig, entity, o, mockO, entityMap, modified, modifiedTraversables)
			}

			val finalMods = modified ++ modifiedTraversables
			val newE = tpe.constructor(updateConfig.data, ValuesMap.fromMap(finalMods))
			// re-put the actual
			entityMap.put(o, newE)
			newE
		}

	/**
	 * insert an entity into the database
	 */
	def insert[PC, T](updateConfig: UpdateConfig, entity: Entity[PC, T], o: T): T with PC =
		{
			val entityMap = new UpdateEntityMap
			try {
				val v = insertInner(updateConfig, entity, o, entityMap)
				entityMap.done
				v
			} catch {
				case e =>
					val stack = entityMap.toErrorStr
					throw new PersistException("An error occured during insert of entity %s with value %s.\nUpdate stack (top is more recent):\n%s".format(entity, o, stack), e)
			}
		}

	/**
	 * update an entity
	 */
	private def updateInner[PC, T](updateConfig: UpdateConfig, entity: Entity[PC, T], o: T, oldValuesMap: ValuesMap, newValuesMap: ValuesMap, entityMap: UpdateEntityMap): T with PC with Persisted =
		{
			if (oldValuesMap == null)
				throw new IllegalStateException("old product in inconsistent state. Did you unlink it? For entity %s , value %s".format(entity, o))
			val tpe = entity.tpe
			def changed(column: ColumnBase) = !Equality.isEqual(newValuesMap.valueOf(column.alias), oldValuesMap.valueOf(column.alias))
			val table = tpe.table
			val modified = oldValuesMap.toMutableMap ++ newValuesMap.toMutableMap
			val modifiedTraversables = new MapOfList[String, Any](MapOfList.stringToLowerCaseModifier)

			// store a mock in the entity map so that we don't process the same instance twice
			var mockO = createMock(updateConfig.data, entity, modified ++ modifiedTraversables)
			entityMap.put(o, mockO)

			// first, lets update the simple columns that changed

			// run all DuringUpdate plugins
			val pluginDUR = duringUpdatePlugins.map {
				_.during(updateConfig, entity, o, oldValuesMap, newValuesMap, entityMap, modified, modifiedTraversables)
			}.reduceLeft { (dur1, dur2) =>
				new DuringUpdateResults(dur1.values ::: dur2.values, dur1.keys ::: dur2.keys)
			}
			// find out which simple columns changed
			val columnsChanged = table.simpleTypeNotAutoGeneratedColumns.filter(changed _)

			// if there is a change, update it
			val args = newValuesMap.toListOfSimpleColumnAndValueTuple(columnsChanged) ::: pluginDUR.values
			if (!args.isEmpty) {
				val pkArgs = oldValuesMap.toListOfSimpleColumnAndValueTuple(table.primaryKeys) ::: pluginDUR.keys

				// we now need to take into account declarePrimaryKeys
				val unused = if (table.unusedPrimaryKeyColumns.isEmpty)
					Nil
				else {
					val alreadyUsed = pkArgs.map(_._1)
					val uc = table.unusedPrimaryKeyColumns.filterNot(alreadyUsed.contains(_))
					oldValuesMap.toListOfSimpleColumnAndValueTuple(uc)
				}

				val allKeys = pkArgs ::: unused
				// execute the before update events
				events.executeBeforeUpdateEvents(tpe, args, allKeys)

				driver.doUpdate(tpe, args, allKeys)

				// execute the after update events
				events.executeAfterUpdateEvents(tpe, args, allKeys)
			}

			// update the mock
			mockO = createMock(updateConfig.data, entity, modified ++ modifiedTraversables)
			entityMap.put(o, mockO)

			if (updateConfig.depth > 0) {
				val newUC = updateConfig.copy(depth = updateConfig.depth - 1)
				postUpdatePlugins.foreach {
					_.after(newUC, entity, o, mockO, oldValuesMap, newValuesMap, entityMap, modifiedTraversables)
				}
			}

			// done, construct the updated entity
			val finalValuesMap = ValuesMap.fromMap(modified ++ modifiedTraversables)
			val v = tpe.constructor(updateConfig.data, finalValuesMap)
			entityMap.put(o, v)
			v
		}

	/**
	 * update an entity. The entity must have been retrieved from the database and then
	 * changed prior to calling this method.
	 * The whole object graph will be updated (if necessary).
	 */
	def update[PC, T](updateConfig: UpdateConfig, entity: Entity[PC, T], o: T with PC): T with PC =
		{
			if (!o.isInstanceOf[Persisted]) throw new IllegalArgumentException("can't update an object that is not persisted: " + o);
			val persisted = o.asInstanceOf[T with PC with Persisted]
			validatePersisted(persisted)
			val entityMap = new UpdateEntityMap
			try {
				val v = updateInner(updateConfig, entity, o, entityMap)
				entityMap.done
				v
			} catch {
				case e: Throwable => throw new PersistException("An error occured during update of entity %s with value %s.".format(entity, o), e)
			}
		}

	private[mapperdao] def updateInner[PC, T](updateConfig: UpdateConfig, entity: Entity[PC, T], o: T with PC, entityMap: UpdateEntityMap): T with PC with Persisted =
		// do a check if a mock is been updated
		o match {
			case p: Persisted if (p.mapperDaoMock) =>
				val v = o.asInstanceOf[T with PC with Persisted]
				// report an error if mock was changed by the user
				val tpe = entity.tpe
				val newVM = ValuesMap.fromEntity(typeManager, tpe, o, false)
				val oldVM = v.mapperDaoValuesMap
				if (newVM.isSimpleColumnsChanged(tpe, oldVM)) throw new IllegalStateException("please don't modify mock objects. Object %s is mock and has been modified.".format(p))
				v
			case _ =>
				// if a mock exists in the entity map or already persisted, then return
				// the existing mock/persisted object
				entityMap.get[PC, T](o).getOrElse {
					val persisted = o.asInstanceOf[T with PC with Persisted]
					val oldValuesMap = persisted.mapperDaoValuesMap
					val tpe = entity.tpe
					val newValuesMapPre = ValuesMap.fromEntity(typeManager, tpe, o)
					val reConstructed = tpe.constructor(updateConfig.data, newValuesMapPre)
					updateInner(updateConfig, entity, o, oldValuesMap, reConstructed.mapperDaoValuesMap, entityMap)
				}
		}
	/**
	 * update an immutable entity. The entity must have been retrieved from the database. Because immutables can't change, a new instance
	 * of the entity must be created with the new values prior to calling this method. Values that didn't change should be copied from o.
	 * For traversables, the method heavily relies on object equality to assess which entities will be updated. So please copy over
	 * traversable entities from the old collections to the new ones (but you can instantiate a new collection).
	 *
	 * The whole tree will be updated (if necessary).
	 *
	 * @param	o		the entity, as retrieved from the database
	 * @param	newO	the new instance of the entity with modifications. The database will be updated
	 * 					based on differences between newO and o
	 * @return			The updated entity. Both o and newO should be disposed (not used) after the call.
	 */
	def update[PC, T](updateConfig: UpdateConfig, entity: Entity[PC, T], o: T with PC, newO: T): T with PC = o match {
		case persisted: T with PC with Persisted =>
			validatePersisted(persisted)
			persisted.mapperDaoDiscarded = true
			try {
				val entityMap = new UpdateEntityMap
				val v = updateInner(updateConfig, entity, persisted, newO, entityMap)
				entityMap.done
				v
			} catch {
				case e => throw new PersistException("An error occured during update of entity %s with old value %s and new value %s".format(entity, o, newO), e)
			}
		case _ => throw new IllegalArgumentException("can't update an object that is not persisted: " + o);
	}

	private[mapperdao] def updateInner[PC, T](updateConfig: UpdateConfig, entity: Entity[PC, T], o: T with PC with Persisted, newO: T, entityMap: UpdateEntityMap): T with PC =
		{
			val oldValuesMap = o.mapperDaoValuesMap
			val newValuesMap = ValuesMap.fromEntity(typeManager, entity.tpe, newO)
			updateInner(updateConfig, entity, newO, oldValuesMap, newValuesMap, entityMap)
		}

	private def validatePersisted(persisted: Persisted) {
		if (persisted.mapperDaoDiscarded) throw new IllegalArgumentException("can't operate on an object twice. An object that was updated/deleted must be discarded and replaced by the return value of update(), i.e. onew=update(o) or just be disposed if it was deleted. The offending object was : " + persisted);
		if (persisted.mapperDaoMock) throw new IllegalArgumentException("can't operate on a 'mock' object. Mock objects are created when there are cyclic dependencies of entities, i.e. entity A depends on B and B on A on a many-to-many relationship.  The offending object was : " + persisted);
	}

	/**
	 * select an entity but load only part of the entity's graph. SelectConfig contains configuration regarding which relationships
	 * won't be loaded, i.e.
	 *
	 * SelectConfig(skip=Set(ProductEntity.attributes)) // attributes won't be loaded
	 */
	def select[PC, T](selectConfig: SelectConfig, entity: Entity[PC, T], ids: List[Any]): Option[T with PC] =
		{
			if (ids == null) throw new NullPointerException("ids can't be null")
			if (ids.isEmpty) throw new IllegalArgumentException("ids can't be empty")
			val pkSz = entity.tpe.table.primaryKeysSize
			if (pkSz != ids.size) throw new IllegalArgumentException("entity has %d keys, can't use these keys: %s".format(pkSz, ids))
			val entityMap = new EntityMap
			val v = selectInner(entity, selectConfig, ids, entityMap)
			v
		}

	private[mapperdao] def selectInner[PC, T](entity: Entity[PC, T], selectConfig: SelectConfig, ids: List[Any], entities: EntityMap): Option[T with PC] =
		{
			val clz = entity.clz
			val tpe = entity.tpe
			if (tpe.table.primaryKeysSize != ids.size) throw new IllegalStateException("Primary keys number dont match the number of parameters. Primary keys: %s".format(tpe.table.primaryKeys))

			entities.get[T with PC](tpe.clz, ids) {
				try {
					val (pks, declared) = ids.splitAt(tpe.table.primaryKeys.size)
					val pkArgs = tpe.table.primaryKeys.zip(pks)
					// convert unused keys to their simple values
					val declaredArgs = if (tpe.table.unusedPKs.isEmpty)
						Nil
					else
						(
							(tpe.table.unusedPKs zip declared) map {
								case (u, v) =>
									u.ci match {
										case ci: ColumnInfoRelationshipBase[PC, T, _, Any] =>
											val foreign = ci.column.foreign
											val fentity = foreign.entity
											val ftable = fentity.tpe.table
											u.columns zip ftable.toListOfPrimaryKeyValues(v)
										case _ => throw new IllegalArgumentException("Please use declarePrimaryKey only for relationships. For normal data please use key(). This occured for entity %s".format(entity.getClass))
									}
							}).flatten

					val args = pkArgs ::: declaredArgs

					events.executeBeforeSelectEvents(tpe, args)
					val om = driver.doSelect(selectConfig, tpe, args)
					events.executeAfterSelectEvents(tpe, args)
					if (om.isEmpty) None
					else if (om.size > 1) throw new IllegalStateException("expected 1 result for %s and ids %s, but got %d. Is the primary key column a primary key in the table?".format(clz.getSimpleName, ids, om.size))
					else {
						val l = toEntities(om, entity, selectConfig, entities)
						Some(l.head)
					}
				} catch {
					case e => throw new QueryException("An error occured during select of entity %s and primary keys %s".format(entity, ids), e)
				}
			}
		}

	private[mapperdao] def toEntities[PC, T](
		lm: List[DatabaseValues],
		entity: Entity[PC, T],
		selectConfig: SelectConfig,
		entities: EntityMap): List[T with PC] =
		lm.map { jdbcMap =>
			val tpe = entity.tpe
			val table = tpe.table
			// calculate the id's for this tpe
			val ids = table.primaryKeys.map { pk => jdbcMap(pk.name) } ::: selectBeforePlugins.map {
				_.idContribution(tpe, jdbcMap, entities)
			}.flatten ::: table.unusedPrimaryKeyColumns.collect { case c: SimpleColumn => jdbcMap(c.name) }
			if (ids.isEmpty)
				throw new IllegalStateException("entity %s without primary key, please use declarePrimaryKeys() to declare the primary key columns of tables into your entity declaration")

			entities.get[T with PC](tpe.clz, ids) {
				val mods = jdbcMap.toMap
				val mock = createMock(selectConfig.data, entity, mods)
				entities.putMock(tpe.clz, ids, mock)

				val allMods = mods ++ selectBeforePlugins.map {
					_.before(entity, selectConfig, jdbcMap, entities)
				}.flatten.map {
					case SelectMod(k, v, lazyBeforeLoadVal) =>
						(k, v)
				}.toMap

				val vm = ValuesMap.fromMap(allMods)
				// if the entity should be lazy loaded and it has relationships, then
				// we need to lazy load it
				val entityV = if (lazyLoadManager.isLazyLoaded(selectConfig.lazyLoad, entity)) {
					lazyLoadEntity(entity, selectConfig, vm)
				} else tpe.constructor(selectConfig.data, vm)
				Some(entityV)
			}.get
		}

	private def lazyLoadEntity[PC, T](
		entity: Entity[PC, T],
		selectConfig: SelectConfig,
		vm: ValuesMap) = {
		// substitute lazy loaded columns with empty values
		val tpe = entity.tpe
		val table = tpe.table
		val lazyLoad = selectConfig.lazyLoad

		val lazyLoadedMods = (table.columnInfosPlain.map { ci =>
			val ll = lazyLoad.isLazyLoaded(ci)
			ci match {
				case mtm: ColumnInfoTraversableManyToMany[_, _, _] =>
					(ci.column.alias, if (ll) Nil else vm.valueOf(ci))
				case mto: ColumnInfoManyToOne[_, _, _] =>
					(ci.column.alias, if (ll) null else vm.valueOf(ci))
				case mtm: ColumnInfoTraversableOneToMany[_, _, _] =>
					(ci.column.alias, if (ll) Nil else vm.valueOf(ci))
				case otor: ColumnInfoOneToOneReverse[_, _, _] =>
					(ci.column.alias, if (ll) null else vm.valueOf(ci))
				case _ => (ci.column.alias, vm.valueOf(ci))
			}
		} ::: table.extraColumnInfosPersisted.map { ci =>
			(ci.column.alias, vm.valueOf(ci))
		}).toMap
		val lazyLoadedVM = ValuesMap.fromMap(lazyLoadedMods)
		val constructed = tpe.constructor(selectConfig.data, lazyLoadedVM)
		val proxy = lazyLoadManager.proxyFor(constructed, entity, lazyLoad, vm)
		proxy
	}
	/**
	 * create a mock of the current entity, to avoid cyclic dependencies
	 * doing infinite loops.
	 */
	private def createMock[PC, T](data: Option[Any], entity: Entity[PC, T], mods: scala.collection.Map[String, Any]): T with PC with Persisted =
		{
			val mockMods = new scala.collection.mutable.HashMap[String, Any] ++ mods
			mockPlugins.foreach {
				_.updateMock(entity, mockMods)
			}
			val tpe = entity.tpe
			val vm = ValuesMap.fromMap(mockMods)
			val preMock = tpe.constructor(data, vm)
			val mock = tpe.constructor(data, ValuesMap.fromEntity(typeManager, tpe, preMock))
			// mark it as mock
			mock.mapperDaoMock = true
			mock
		}

	override def delete[PC, T](entity: Entity[PC, T], ids: List[AnyVal]): Unit =
		{
			val tpe = entity.tpe
			val table = tpe.table
			val pks = table.primaryKeys
			if (pks.size != ids.size) throw new IllegalArgumentException("number of primary key values don't match number of primary keys : %s != %s".format(pks, ids))
			val keyValues = pks zip ids
			// do the actual delete database op
			driver.doDelete(tpe, keyValues)
		}
	/**
	 * deletes an entity from the database
	 */
	def delete[PC, T](deleteConfig: DeleteConfig, entity: Entity[PC, T], o: T with PC): T = {
		val entityMap = new UpdateEntityMap
		val deleted = deleteInner(deleteConfig, entity, o, entityMap)
		entityMap.done
		deleted
	}

	private[mapperdao] def deleteInner[PC, T](deleteConfig: DeleteConfig, entity: Entity[PC, T], o: T with PC, entityMap: UpdateEntityMap): T = o match {
		case persisted: T with PC with Persisted =>
			if (persisted.mapperDaoDiscarded) throw new IllegalArgumentException("can't operate on an object twice. An object that was updated/deleted must be discarded and replaced by the return value of update(), i.e. onew=update(o) or just be disposed if it was deleted. The offending object was : " + o);
			//persisted.mapperDaoDiscarded = true

			val tpe = entity.tpe
			val table = tpe.table

			try {
				val keyValues0 = table.toListOfPrimaryKeySimpleColumnAndValueTuples(o) ::: beforeDeletePlugins.flatMap(
					_.idColumnValueContribution(tpe, deleteConfig, events, persisted, entityMap)
				)

				val unusedPKColumns = table.unusedPKs.filterNot(unusedColumn => keyValues0.map(_._1).contains(unusedColumn))
				val keyValues = keyValues0 ::: table.toListOfUnusedPrimaryKeySimpleColumnAndValueTuples(o)
				// call all the before-delete plugins
				beforeDeletePlugins.foreach {
					_.before(entity, deleteConfig, events, persisted, keyValues, entityMap)
				}

				// execute the before-delete events
				events.executeBeforeDeleteEvents(tpe, keyValues, o)

				// do the actual delete database op
				driver.doDelete(tpe, keyValues)

				// execute the after-delete events
				events.executeAfterDeleteEvents(tpe, keyValues, o)

				// return the object
				o
			} catch {
				case e => throw new PersistException("An error occured during delete of entity %s with value %s".format(entity, o), e)
			}
		case _ => throw new IllegalArgumentException("can't delete an object that is not persisted: " + o);
	}

	override def link[T](entity: SimpleEntity[T], o: T): T = {
		val tpe = entity.tpe
		val vm = ValuesMap.fromEntity(typeManager, tpe, o)
		tpe.constructor(None, vm)
	}

	override def link[T](entity: Entity[IntId, T], o: T, id: Int): T with IntId =
		linkInner(entity, o, id)

	override def link[T](entity: Entity[LongId, T], o: T, id: Long): T with LongId =
		linkInner(entity, o, id)

	private def linkInner[PC, T](entity: Entity[PC, T], o: T, id: Any): T with PC = {
		val tpe = entity.tpe
		val vm = ValuesMap.fromEntity(typeManager, tpe, o)
		vm(entity.tpe.table.extraColumnInfosPersisted.head) = id
		tpe.constructor(None, vm)
	}

	override def unlink[PC, T](entity: Entity[PC, T], o: T): T = {
		val unlinkVisitor = new UnlinkEntityRelationshipVisitor
		unlinkVisitor.visit(entity, o)
		unlinkVisitor.unlink(o)
		o
	}

	/**
	 * ===================================================================================
	 * common methods
	 * ===================================================================================
	 */
	override def toString = "MapperDao(%s)".format(driver)
}
