package com.googlecode.mapperdao.drivers

import com.googlecode.mapperdao.jdbc.Jdbc
import com.googlecode.mapperdao.jdbc.UpdateResultWithGeneratedKeys
import com.googlecode.mapperdao.Query
import com.googlecode.mapperdao.ColumnBase
import com.googlecode.mapperdao.PK
import com.googlecode.mapperdao.QueryConfig
import com.googlecode.mapperdao.SimpleColumn
import com.googlecode.mapperdao.TypeManager
import com.googlecode.mapperdao.TypeRegistry
import com.googlecode.mapperdao.sqlbuilder.SqlBuilder

/**
 * @author kostantinos.kougios
 *
 * 14 Jul 2011
 */
class Derby(override val jdbc: Jdbc, val typeRegistry: TypeRegistry, val typeManager: TypeManager) extends Driver {

	val escapeNamesStrategy = new EscapeNamesStrategy {
		val invalidColumnNames = Set("end", "select", "where", "group", "year", "no")
		val invalidTableNames = Set("end", "select", "where", "group", "user", "User")
		override def escapeColumnNames(name: String) = if (invalidColumnNames.contains(name)) '"' + name + '"'; else name
		override def escapeTableNames(name: String) = if (invalidTableNames.contains(name)) '"' + name + '"'; else name
	}
	protected[mapperdao] override def getAutoGenerated(ur: UpdateResultWithGeneratedKeys, column: SimpleColumn): Any =
		ur.keys.get("1").get

	override protected def sequenceSelectNextSql(sequenceColumn: ColumnBase): String = sequenceColumn match {
		case PK(columnName, true, sequence) => "NEXT VALUE FOR %s".format(sequence.get)
	}

	override def endOfQuery[PC, T](q: sqlBuilder.SqlSelectBuilder, queryConfig: QueryConfig, qe: Query.Builder[PC, T]) =
		{
			queryConfig.offset.foreach(o => q.appendSql("offset " + o + " rows"))
			queryConfig.limit.foreach(l => q.appendSql("fetch next " + l + " rows only"))
			q
		}

	override def toString = "Derby"
}