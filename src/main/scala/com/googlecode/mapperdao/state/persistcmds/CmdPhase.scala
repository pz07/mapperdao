package com.googlecode.mapperdao.state.persistcmds

import com.googlecode.mapperdao._
import java.util.IdentityHashMap
import com.googlecode.mapperdao.utils.TraversableSeparation
import com.googlecode.mapperdao.utils.NYI

/**
 * entities are converted to PersistOps
 *
 * @author kostantinos.kougios
 *
 *         21 Nov 2012
 */
class CmdPhase(typeManager: TypeManager) {

	private var alreadyProcessed = Map[Int, List[PersistCmd]]()

	def toInsertCmd[ID, T](
		entity: Entity[ID, _ <: DeclaredIds[ID], T],
		newVM: ValuesMap
	) = insert(entity.asInstanceOf[Entity[ID, DeclaredIds[ID], T]], newVM, true)

	def toUpdateCmd[ID, T](
		entity: Entity[ID, _ <: DeclaredIds[ID], T],
		oldValuesMap: ValuesMap,
		newValuesMap: ValuesMap
	) = update(entity.asInstanceOf[Entity[ID, DeclaredIds[ID], T]], oldValuesMap, newValuesMap, true)

	private def insert[ID, T](
		entity: Entity[ID, DeclaredIds[ID], T],
		newVM: ValuesMap,
		mainEntity: Boolean
	): List[PersistCmd] = {
		alreadyProcessed.get(newVM.identity) match {
			case None =>
				val tpe = entity.tpe
				val table = tpe.table
				val columnAndValues = newVM.toListOfSimpleColumnAndValueTuple(table.simpleTypeNotAutoGeneratedColumns)
				val op = InsertCmd(entity, newVM, columnAndValues, mainEntity) :: related(entity, None, newVM)
				alreadyProcessed += (newVM.identity -> op)
				op
			case Some(x) =>
				AlreadyProcessedCmd :: Nil
		}
	}

	private def update[ID, T](
		entity: Entity[ID, DeclaredIds[ID], T],
		oldVM: ValuesMap,
		newVM: ValuesMap,
		mainEntity: Boolean
	): List[PersistCmd] = {
		val op = alreadyProcessed.get(newVM.identity)
		if (op.isDefined) {
			AlreadyProcessedCmd :: Nil
		} else {
			val tpe = entity.tpe
			val table = tpe.table
			val newColumnAndValues = newVM.toListOfColumnAndValueTuple(table.simpleTypeNotAutoGeneratedColumns)
			val oldColumnAndValues = oldVM.toListOfColumnAndValueTuple(table.simpleTypeNotAutoGeneratedColumns)
			val changedColumnAndValues = (newColumnAndValues zip oldColumnAndValues) collect {
				case ((nc, nv), (oc, ov)) if (nv != ov) => (nc, nv)
			}
			val op = UpdateCmd(entity, oldVM, newVM, changedColumnAndValues, mainEntity) :: related(entity, Some(oldVM), newVM)
			alreadyProcessed += (newVM.identity -> op)
			op
		}
	}

	private def related[ID, T](
		entity: Entity[ID, DeclaredIds[ID], T],
		oldVMO: Option[ValuesMap],
		newVM: ValuesMap
	): List[PersistCmd] = {
		entity.tpe.table.relationshipColumnInfos.map {
			case ColumnInfoTraversableManyToMany(column, columnToValue, getterMethod) =>
				val foreignEntity = column.foreign.entity
				if (oldVMO.isDefined) {
					val oldVM = oldVMO.get
					val oldT = oldVM.manyToMany(column)
					val newT = newVM.manyToMany(column)
					val (added, intersect, removed) = TraversableSeparation.separate(foreignEntity, oldT, newT)
					val r=added.toList.map {
						fo => insertOrUpdate(foreignEntity, fo)
					}.flatten
					r
				} else {
					newVM.manyToMany(column).map {
						case p: Persisted =>
							doUpdate(foreignEntity, p)
						case o =>
							val foreignVM = ValuesMap.fromEntity(typeManager, foreignEntity, o)
							InsertManyToManyCmd(
								entity,
								foreignEntity,
								column,
								newVM,
								foreignVM) :: insert(foreignEntity, foreignVM, false)
					}.flatten
				}
		}.flatten
	}

	private def insertOrUpdate[ID, T](foreignEntity: Entity[ID, DeclaredIds[ID], T], o: T) = o match {
		case p: T with  Persisted => doUpdate(foreignEntity, p)
		case _ => doInsert(foreignEntity, o)
	}

	private def doInsert[ID, T](foreignEntity: Entity[ID, DeclaredIds[ID], T], o: T) = {
		val newVM = ValuesMap.fromEntity(typeManager, foreignEntity, o)
		insert(foreignEntity, newVM, false)
	}

	private def doUpdate[ID, T](foreignEntity: Entity[ID, DeclaredIds[ID], T], p: T with Persisted) = {
		val newVM = ValuesMap.fromEntity(typeManager, foreignEntity, p)
		update(foreignEntity, p.mapperDaoValuesMap, newVM, false)
	}
}