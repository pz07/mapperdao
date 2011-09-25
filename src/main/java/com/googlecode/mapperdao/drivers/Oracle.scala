package com.googlecode.mapperdao.drivers

import com.googlecode.mapperdao.jdbc.Jdbc
import com.googlecode.mapperdao.jdbc.UpdateResultWithGeneratedKeys
import com.googlecode.mapperdao.ColumnBase
import com.googlecode.mapperdao.TypeRegistry
import com.googlecode.mapperdao.PK
import com.googlecode.mapperdao.AutoGenerated

/**
 * @author kostantinos.kougios
 *
 * 23 Sep 2011
 */
class Oracle(override val jdbc: Jdbc, override val typeRegistry: TypeRegistry) extends Driver {
	private val invalidColumnNames = Set("select", "where", "group", "start")
	private val invalidTableNames = Set("end", "select", "where", "group", "user", "User")

	override def escapeColumnNames(name: String) = if (invalidColumnNames.contains(name)) '"' + name + '"'; else name
	override def escapeTableNames(name: String): String = if (invalidTableNames.contains(name)) '"' + name + '"'; else name

	override protected[mapperdao] def getAutoGenerated(ur: UpdateResultWithGeneratedKeys, column: ColumnBase): Any = ur.keys.get(column.columnName.toUpperCase).get

	override protected def sequenceSelectNextSql(sequenceColumn: ColumnBase): String = sequenceColumn match {
		case PK(ag: AutoGenerated) => "%s.nextval".format(ag.sequence.get)
	}

}