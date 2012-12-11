package com.googlecode.mapperdao.state.recreation

import com.googlecode.mapperdao.UpdateConfig
import com.googlecode.mapperdao.state.persisted.PersistedNode
import com.googlecode.mapperdao.UpdateEntityMap
import com.googlecode.mapperdao.DeclaredIds
import com.googlecode.mapperdao.ColumnInfo

/**
 * @author kostantinos.kougios
 *
 * 11 Dec 2012
 */
class RecreationPhase[ID, T](
		updateConfig: UpdateConfig,
		node: PersistedNode[ID, T],
		entityMap: UpdateEntityMap) {

	def execute: T with DeclaredIds[ID] =
		entityMap.get[DeclaredIds[ID], T](node.identity).getOrElse {
			val entity = node.entity
			val tpe = entity.tpe
			val table = tpe.table

			val newVM = node.newVM
			val modified = newVM.toMap

			// create a mock
			var mockO = createMock(updateConfig.data, entity, modified)
			entityMap.put(node.identity, mockO)

			val ur = node.generatedKeys.toMap

			val keysMap = table.simpleTypeAutoGeneratedColumns.map { c =>
				val ag = ur(c)
				// many drivers return the wrong type for the autogenerated
				// keys, typically instead of Int they return Long
				table.pcColumnToColumnInfoMap(c) match {
					case ci: ColumnInfo[_, _] =>
						val fixed = typeManager.toActualType(ci.dataType, ag)
						(c.name, fixed)
				}
			}.toMap

			val finalMods = modified ++ keysMap
			val newE = tpe.constructor(updateConfig.data, ValuesMap.fromMap(node.identity, finalMods))
			// re-put the actual
			entityMap.put(node.identity, newE)
			newE
		}

}