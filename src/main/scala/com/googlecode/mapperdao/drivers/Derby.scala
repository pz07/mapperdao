package com.googlecode.mapperdao.drivers

import com.googlecode.mapperdao.jdbc.Jdbc
import com.googlecode.mapperdao.jdbc.UpdateResultWithGeneratedKeys
import com.googlecode.mapperdao.sqlbuilder.SqlBuilder
import com.googlecode.mapperdao._
import com.googlecode.mapperdao.jdbc.Batch

/**
 * @author kostantinos.kougios
 *
 * 14 Jul 2011
 */
class Derby(override val jdbc: Jdbc, val typeRegistry: TypeRegistry, val typeManager: TypeManager) extends Driver {

	def batchStrategy(autogenerated: Boolean) = if (autogenerated) Batch.NoBatch else Batch.WithBatch

	val escapeNamesStrategy = new EscapeNamesStrategy {
		val invalidColumnNames = Set("end", "select", "where", "group", "year", "no", "int", "float", "double")
		val invalidTableNames = Set("end", "select", "where", "group", "user", "User")
		override def escapeColumnNames(name: String) = if (invalidColumnNames.contains(name)) '"' + name + '"'; else name
		override def escapeTableNames(name: String) = if (invalidTableNames.contains(name)) '"' + name + '"'; else name
	}
	val sqlBuilder = new SqlBuilder(this, escapeNamesStrategy)

	protected[mapperdao] override def getAutoGenerated(
		m: java.util.Map[String, Object],
		column: SimpleColumn) =
		m.get("1")

	override protected def sequenceSelectNextSql(sequenceColumn: ColumnBase): String = sequenceColumn match {
		case PK(columnName, true, sequence, _) => "NEXT VALUE FOR %s".format(sequence.get)
	}

	override def endOfQuery[ID, PC <: DeclaredIds[ID], T](q: sqlBuilder.SqlSelectBuilder, queryConfig: QueryConfig, qe: Query.Builder[ID, PC, T]) =
		{
			queryConfig.offset.foreach(o => q.appendSql("offset " + o + " rows"))
			queryConfig.limit.foreach(l => q.appendSql("fetch next " + l + " rows only"))
			q
		}

	override def toString = "Derby"
}