package com.googlecode.mapperdao

import internal.{LazyActions, Utils}
import java.util.Calendar
import java.util.Date
import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import org.joda.time._
import scala.Some

/**
 * the main class that must be inherited to create entities.
 *
 * Entity[PC,T] takes two type parameters: PC is the primary key
 * type. The primary key must be autogenerated. T is the actual
 * entity. I.e. Entity[IntId,Product]. Entity T is decorated with
 * PC after operations like mapperDao.insert , mapperDao.select
 * and queryDao.queryForList . In other words, T is the type of
 * the entity before been persisted and T with PC is the type
 * after been persisted i.e. Product with IntId.
 *
 * For entities without autogenerated id's, please look at
 * SimpleEntity[T].
 *
 * Typical usage of this class is
 *
 * <code>
 * class ProductEntity extends Entity[IntId,Product] {
 * ... column declarations
 * def constructor(implicit m:ValuesMap) = new Product(...) with Persisted with IntId {
 * val id:Int=ProductEntity.id
 * }
 * }
 * </code>
 *
 * Columns can be declared using one of the many available methods, i.e.
 *
 * <code>
 * val id=key("id") autogenerated (_.id)
 * val title=column("title") to (_.title)
 * val attributes=manytomany(AttributeEntity) to (_.attributes)
 * </code>
 *
 * There are constructors and methods to override the default naming convention
 * both for the table name of this entity as well as for column names and
 * related tables/columns.
 *
 * @author kostantinos.kougios
 *
 *         13 Aug 2011
 */
abstract class Entity[ID, +PC <: Persisted, T](val table: String, val clz: Class[T])
{
	/**
	 * example:
	 *
	 * override val databaseSchema = Schema("myschema")
	 */
	val databaseSchema: Option[Schema] = None

	if (clz == null) throw new NullPointerException("clz can't be null")
	if (table == null) throw new NullPointerException("table can't be null")

	/**
	 * declares the extra trait that will be mixed into every persisted instance
	 * of T. So T becomes T with Stored when it is persisted.
	 */
	type Stored = PC

	def this(table: String)(implicit m: ClassManifest[T]) = this(table, m.erasure.asInstanceOf[Class[T]])

	def this()(implicit m: ClassManifest[T]) = this(m.erasure.getSimpleName, m.erasure.asInstanceOf[Class[T]])

	/**
	 * overriding any of these entity-constructors is mandatory. These
	 * should return an instance of T with PC, i.e.
	 * an instance of Product with SurrogateIntId
	 */
	def constructor(implicit m: ValuesMap): T with Stored

	def constructor(implicit data: Option[_], m: ValuesMap): T with PC = constructor(m)

	protected val tableLower = table.toLowerCase

	private[mapperdao] def init() {}

	private var persistedColumns = List[ColumnInfoBase[T with DeclaredIds[ID], _]]()
	private var columns = List[ColumnInfoBase[T, _]]()
	private[mapperdao] var onlyForQueryColumns = List[ColumnInfoBase[T, _]]()
	private val unusedPKs = new LazyActions[ColumnInfoBase[Any, Any]]
	protected[mapperdao] lazy val tpe = {
		val con: (Option[_], ValuesMap) => T with Persisted = (d, m) => {
			// construct the object
			val o = constructor(d, m)
			// set the values map
			o.mapperDaoValuesMap = m
			o
		}
		EntityType[ID, T](clz, con, new Table[ID, T](databaseSchema, table, columns.reverse, persistedColumns, unusedPKs.executeAll.reverse))
	}

	override def hashCode = table.hashCode

	override def equals(o: Any) = o match {
		case e: Entity[_, _, _] => table == e.table && clz == e.clz
		case _ => false
	}

	override def toString = "%s(%s,%s)".format(getClass.getSimpleName, table, clz.getName)

	private def keysDuringDeclaration = persistedColumns.collect {
		case ColumnInfo(pk: PK, _, _) => pk
	} ::: columns.collect {
		case ColumnInfo(pk: PK, _, _) => pk
	}

	/**
	 * converts a function T=>Option[F] to T=>F
	 */
	private def optionToValue[T, F](columnToValue: T => Option[F]): T => F = (t: T) => columnToValue(t).getOrElse(null.asInstanceOf[F])

	/**
	 * declare any primary keys that are not used for any mappings. This is especially
	 * useful in one-to-many relationships, where the many part doesn't have it's
	 * own primary key. In that case, some other column(s) can act as a primary
	 * key so that mapperdao knows how to update/delete those.
	 */
	protected def declarePrimaryKey[V](ci: ColumnInfo[T, V]) {
		unusedPKs(() => ci.asInstanceOf[ColumnInfoBase[Any, Any]])
	}

	protected def declarePrimaryKey[V, FID, F](ci: ColumnInfoManyToOne[T, FID, F]) {
		unusedPKs(() => ci.asInstanceOf[ColumnInfoBase[Any, Any]])
	}

	protected def declarePrimaryKey[V, FID, F](ci: ColumnInfoOneToOne[T, FID, F]) {
		unusedPKs(() => ci.asInstanceOf[ColumnInfoBase[Any, Any]])
	}

	/**
	 * to avoid StackOverflow exceptions due to cyclic-referenced entities, we pass
	 * this as by-name param
	 */
	protected def declarePrimaryKey[FID, F](ci: => ColumnInfoTraversableOneToMany[FID, F, ID, T]) = {
		unusedPKs(() => ci.asInstanceOf[ColumnInfoBase[Any, Any]])
		new ColumnInfoTraversableOneToManyDeclaredPrimaryKey(ci)
	}

	// implicit conversions to be used implicitly into the constructor method.
	// these shouldn't be explicitly be called.
	protected implicit def columnToBoolean(ci: ColumnInfo[_ <: T, Boolean])(implicit m: ValuesMap): Boolean = m(ci)

	protected implicit def columnToBooleanOption(ci: ColumnInfo[_ <: T, Boolean])(implicit m: ValuesMap): Option[Boolean] =
		if (m.isNull(ci)) None else Some(m(ci))

	protected implicit def columnToByte(ci: ColumnInfo[_ <: T, Byte])(implicit m: ValuesMap): Byte = m(ci)

	protected implicit def columnToOptionByte(ci: ColumnInfo[_ <: T, Byte])(implicit m: ValuesMap): Option[Byte] =
		if (m.isNull(ci)) None else Some(m(ci))

	protected implicit def columnToShort(ci: ColumnInfo[_ <: T, Short])(implicit m: ValuesMap): Short = m(ci)

	protected implicit def columnToOptionShort(ci: ColumnInfo[_ <: T, Short])(implicit m: ValuesMap): Option[Short] =
		if (m.isNull(ci)) None else Some(m(ci))

	protected implicit def columnToInt(ci: ColumnInfo[_ <: T, Int])(implicit m: ValuesMap): Int = m(ci)

	protected implicit def columnToOptionInt(ci: ColumnInfo[_ <: T, Int])(implicit m: ValuesMap): Option[Int] =
		if (m.isNull(ci)) None else Some(m(ci))

	protected implicit def columnToIntIntId(ci: ColumnInfo[_ <: T with SurrogateIntId, Int])(implicit m: ValuesMap): Int = m(ci)

	protected implicit def columnToLong(ci: ColumnInfo[_ <: T, Long])(implicit m: ValuesMap): Long = m(ci)

	protected implicit def columnToOptionLong(ci: ColumnInfo[_ <: T, Long])(implicit m: ValuesMap): Option[Long] =
		if (m.isNull(ci)) None else Some(m(ci))

	protected implicit def columnToLongLongId(ci: ColumnInfo[T with SurrogateLongId, Long])(implicit m: ValuesMap): Long = m(ci)

	protected implicit def columnToDateTime(ci: ColumnInfo[_ <: T, DateTime])(implicit m: ValuesMap): DateTime = m(ci)

	protected implicit def columnToPeriod(ci: ColumnInfo[_ <: T, Period])(implicit m: ValuesMap): Period = m(ci)

	protected implicit def columnToDuration(ci: ColumnInfo[_ <: T, Duration])(implicit m: ValuesMap): Duration = m(ci)

	protected implicit def columnToLocalDate(ci: ColumnInfo[_ <: T, LocalDate])(implicit m: ValuesMap): LocalDate = m(ci)

	protected implicit def columnToLocalTime(ci: ColumnInfo[_ <: T, LocalTime])(implicit m: ValuesMap): LocalTime = m(ci)

	protected implicit def columnToOptionPeriod(ci: ColumnInfo[_ <: T, Period])(implicit m: ValuesMap): Option[Period] = Option(m(ci))

	protected implicit def columnToOptionDuration(ci: ColumnInfo[_ <: T, Duration])(implicit m: ValuesMap): Option[Duration] = Option(m(ci))

	protected implicit def columnToOptionDateTime(ci: ColumnInfo[_ <: T, DateTime])(implicit m: ValuesMap): Option[DateTime] = Option(m(ci))

	protected implicit def columnToOptionLocalDate(ci: ColumnInfo[_ <: T, LocalDate])(implicit m: ValuesMap): Option[LocalDate] = Option(m(ci))

	protected implicit def columnToOptionLocalTime(ci: ColumnInfo[_ <: T, LocalTime])(implicit m: ValuesMap): Option[LocalTime] = Option(m(ci))

	protected implicit def columnToDate(ci: ColumnInfo[_ <: T, Date])(implicit m: ValuesMap): Date = m.date(ci)

	protected implicit def columnToOptionDate(ci: ColumnInfo[_ <: T, Date])(implicit m: ValuesMap): Option[Date] = Option(m.date(ci))

	protected implicit def columnToCalendar(ci: ColumnInfo[_ <: T, Calendar])(implicit m: ValuesMap): Calendar = m.calendar(ci)

	protected implicit def columnToOptionCalendar(ci: ColumnInfo[_ <: T, Calendar])(implicit m: ValuesMap): Option[Calendar] = Option(m.calendar(ci))

	protected implicit def columnToString(ci: ColumnInfo[_ <: T, String])(implicit m: ValuesMap): String = m(ci)

	protected implicit def columnToOptionString(ci: ColumnInfo[_ <: T, String])(implicit m: ValuesMap): Option[String] = Option(m(ci))

	protected implicit def columnToBigDecimal(ci: ColumnInfo[_ <: T, BigDecimal])(implicit m: ValuesMap): BigDecimal = m.bigDecimal(ci)

	protected implicit def columnToOptionBigDecimal(ci: ColumnInfo[_ <: T, BigDecimal])(implicit m: ValuesMap): Option[BigDecimal] = Option(m(ci))

	protected implicit def columnToBigInteger(ci: ColumnInfo[_ <: T, BigInt])(implicit m: ValuesMap): BigInt = m.bigInt(ci)

	protected implicit def columnToOptionBigInteger(ci: ColumnInfo[_ <: T, BigInt])(implicit m: ValuesMap): Option[BigInt] = Option(m(ci))

	protected implicit def columnToFloat(ci: ColumnInfo[_ <: T, Float])(implicit m: ValuesMap): Float = m(ci)

	protected implicit def columnToOptionFloat(ci: ColumnInfo[_ <: T, Float])(implicit m: ValuesMap): Option[Float] =
		if (m.isNull(ci)) None else Some(m(ci))

	protected implicit def columnToDouble(ci: ColumnInfo[_ <: T, Double])(implicit m: ValuesMap): Double = m(ci)

	protected implicit def columnToOptionDouble(ci: ColumnInfo[_ <: T, Double])(implicit m: ValuesMap): Option[Double] =
		if (m.isNull(ci)) None else Some(m(ci))

	// many to many : Scala
	protected implicit def columnTraversableManyToManyToSet[FID, F](ci: ColumnInfoTraversableManyToMany[T, FID, F])(implicit m: ValuesMap): Set[F] = m(ci).toSet

	protected implicit def columnTraversableManyToManyToList[FID, F](ci: ColumnInfoTraversableManyToMany[T, FID, F])(implicit m: ValuesMap): List[F] = m(ci).toList

	protected implicit def columnTraversableManyToManyToIndexedSeq[FID, F](ci: ColumnInfoTraversableManyToMany[T, FID, F])(implicit m: ValuesMap): IndexedSeq[F] = m(ci).toIndexedSeq

	protected implicit def columnTraversableManyToManyToArray[FID, F](ci: ColumnInfoTraversableManyToMany[T, FID, F])(implicit m: ValuesMap, e: ClassManifest[F]): Array[F] = m(ci).toArray

	// many to one
	protected implicit def columnManyToOneToValue[FID, F](ci: ColumnInfoManyToOne[T, FID, F])(implicit m: ValuesMap): F = m(ci)

	protected implicit def columnManyToOneToOptionValue[T, FID, F](ci: ColumnInfoManyToOne[T, FID, F])(implicit m: ValuesMap): Option[F] = Option(m(ci))

	// one to many : Scala
	protected implicit def columnTraversableOneToManyList[FID, F](ci: ColumnInfoTraversableOneToMany[ID, T, FID, F])(implicit m: ValuesMap): List[F] = m(ci).toList

	protected implicit def columnTraversableOneToManySet[FID, F](ci: ColumnInfoTraversableOneToMany[ID, T, FID, F])(implicit m: ValuesMap): Set[F] = m(ci).toSet

	protected implicit def columnTraversableOneToManyIndexedSeq[FID, F](ci: ColumnInfoTraversableOneToMany[ID, T, FID, F])(implicit m: ValuesMap): IndexedSeq[F] = m(ci).toIndexedSeq

	protected implicit def columnTraversableOneToManyArray[FID, F](ci: ColumnInfoTraversableOneToMany[ID, T, FID, F])(implicit m: ValuesMap, e: ClassManifest[F]): Array[F] = m(ci).toArray

	// simple typec entities, one-to-many
	protected implicit def columnTraversableOneToManyListStringEntity[T, EID](ci: ColumnInfoTraversableOneToMany[ID, T, EID, StringValue])(implicit m: ValuesMap): List[String] =
		m(ci).map(_.value).toList

	protected implicit def columnTraversableOneToManySetStringEntity[T, EID](ci: ColumnInfoTraversableOneToMany[ID, T, EID, StringValue])(implicit m: ValuesMap): Set[String] =
		m(ci).map(_.value).toSet

	protected implicit def columnTraversableOneToManyListIntEntity[T, EID](ci: ColumnInfoTraversableOneToMany[ID, T, EID, IntValue])(implicit m: ValuesMap): List[Int] =
		m(ci).map(_.value).toList

	protected implicit def columnTraversableOneToManySetIntEntity[T, EID](ci: ColumnInfoTraversableOneToMany[ID, T, EID, IntValue])(implicit m: ValuesMap): Set[Int] =
		m(ci).map(_.value).toSet

	protected implicit def columnTraversableOneToManyListLongEntity[T, EID](ci: ColumnInfoTraversableOneToMany[ID, T, EID, LongValue])(implicit m: ValuesMap): List[Long] =
		m(ci).map(_.value).toList

	protected implicit def columnTraversableOneToManySetLongEntity[T, EID](ci: ColumnInfoTraversableOneToMany[ID, T, EID, LongValue])(implicit m: ValuesMap): Set[Long] =
		m(ci).map(_.value).toSet

	protected implicit def columnTraversableOneToManyListFloatEntity[T, EID](ci: ColumnInfoTraversableOneToMany[ID, T, EID, FloatValue])(implicit m: ValuesMap): List[Float] =
		m(ci).map(_.value).toList

	protected implicit def columnTraversableOneToManySetFloatEntity[T, EID](ci: ColumnInfoTraversableOneToMany[ID, T, EID, FloatValue])(implicit m: ValuesMap): Set[Float] =
		m(ci).map(_.value).toSet

	protected implicit def columnTraversableOneToManyListDoubleEntity[T, EID](ci: ColumnInfoTraversableOneToMany[ID, T, EID, DoubleValue])(implicit m: ValuesMap): List[Double] =
		m(ci).map(_.value).toList

	protected implicit def columnTraversableOneToManySetDoubleEntity[T, EID](ci: ColumnInfoTraversableOneToMany[ID, T, EID, DoubleValue])(implicit m: ValuesMap): Set[Double] =
		m(ci).map(_.value).toSet

	// simple typec entities, many-to-many
	protected implicit def columnTraversableManyToManyListStringEntity[T, EID](ci: ColumnInfoTraversableManyToMany[T, EID, StringValue])(implicit m: ValuesMap): List[String] =
		m(ci).map(_.value).toList

	protected implicit def columnTraversableManyToManySetStringEntity[T, EID](ci: ColumnInfoTraversableManyToMany[T, EID, StringValue])(implicit m: ValuesMap): Set[String] =
		m(ci).map(_.value).toSet

	protected implicit def columnTraversableManyToManyListIntEntity[T, EID](ci: ColumnInfoTraversableManyToMany[T, EID, IntValue])(implicit m: ValuesMap): List[Int] =
		m(ci).map(_.value).toList

	protected implicit def columnTraversableManyToManySetIntEntity[T, EID](ci: ColumnInfoTraversableManyToMany[T, EID, IntValue])(implicit m: ValuesMap): Set[Int] =
		m(ci).map(_.value).toSet

	protected implicit def columnTraversableManyToManyListLongEntity[T, EID](ci: ColumnInfoTraversableManyToMany[T, EID, LongValue])(implicit m: ValuesMap): List[Long] =
		m(ci).map(_.value).toList

	protected implicit def columnTraversableManyToManySetLongEntity[T, EID](ci: ColumnInfoTraversableManyToMany[T, EID, LongValue])(implicit m: ValuesMap): Set[Long] =
		m(ci).map(_.value).toSet

	protected implicit def columnTraversableManyToManyListFloatEntity[T, EID](ci: ColumnInfoTraversableManyToMany[T, EID, FloatValue])(implicit m: ValuesMap): List[Float] =
		m(ci).map(_.value).toList

	protected implicit def columnTraversableManyToManySetFloatEntity[T, EID](ci: ColumnInfoTraversableManyToMany[T, EID, FloatValue])(implicit m: ValuesMap): Set[Float] =
		m(ci).map(_.value).toSet

	protected implicit def columnTraversableManyToManyListDoubleEntity[T, EID](ci: ColumnInfoTraversableManyToMany[T, EID, DoubleValue])(implicit m: ValuesMap): List[Double] =
		m(ci).map(_.value).toList

	protected implicit def columnTraversableManyToManySetDoubleEntity[T, EID](ci: ColumnInfoTraversableManyToMany[T, EID, DoubleValue])(implicit m: ValuesMap): Set[Double] =
		m(ci).map(_.value).toSet

	// one to one
	protected implicit def columnOneToOne[FID, F](ci: ColumnInfoOneToOne[T, FID, F])(implicit m: ValuesMap): F = m(ci)

	protected implicit def columnOneToOneOption[FID, F](ci: ColumnInfoOneToOne[T, FID, F])(implicit m: ValuesMap): Option[F] = Option(m(ci))

	protected implicit def columnOneToOneReverse[FID, F](ci: ColumnInfoOneToOneReverse[T, FID, F])(implicit m: ValuesMap): F = m(ci)

	protected implicit def columnOneToOneReverseOption[FID, F](ci: ColumnInfoOneToOneReverse[T, FID, F])(implicit m: ValuesMap): Option[F] = Option(m(ci))

	protected implicit def columnToByteArray(ci: ColumnInfo[T, Array[Byte]])(implicit m: ValuesMap): Array[Byte] = m(ci)

	/**
	 * dsl for declaring columns
	 */
	private var aliasCnt = 0

	private def createAlias(clz: Class[_]) = {
		aliasCnt += 1
		tableLower + ":" + aliasCnt
	}

	/**
	 * primary key declarations. use like this:
	 *
	 * val id=key("id") autogenerated (_.id)
	 * or
	 * val id=key("id") to (_.id)
	 * or
	 * val id=key("id") sequence("mySequence") autogenerated (_.id)
	 */
	def key(column: String) = new PKBuilder(column)

	protected class PKBuilder(columnName: String)
	{
		private var seq: Option[String] = None

		def to[V](columnToValue: T => V)(implicit m: Manifest[V]): ColumnInfo[T, V] = {
			val tpe = m.erasure.asInstanceOf[Class[V]]
			var ci = ColumnInfo(PK(Entity.this, columnName, false, None, m.erasure), columnToValue, tpe)
			columns ::= ci
			ci
		}

		def sequence(seq: String) = {
			this.seq = Some(seq)
			this
		}

		def sequence(seq: Option[String]) = {
			this.seq = seq
			this
		}

		def autogenerated[V](columnToValue: T with Stored => V)(implicit m: Manifest[V]): ColumnInfo[T with DeclaredIds[ID], V] = {
			val tpe = m.erasure.asInstanceOf[Class[V]]
			var ci = ColumnInfo(PK(Entity.this, columnName, true, seq, m.erasure), columnToValue, tpe).asInstanceOf[ColumnInfo[T with DeclaredIds[ID], V]]
			persistedColumns ::= ci
			ci
		}
	}

	/**
	 * simple column declarations, use as
	 *
	 * val title=column("title") to (_.title)
	 */
	def column(column: String) = new ColumnBuilder(column)

	protected class ColumnBuilder(column: String)
		extends OnlyForQueryDefinition
	{
		def to[V](columnToValue: T => V)(implicit m: Manifest[V]): ColumnInfo[T, V] = {
			val tpe = m.erasure.asInstanceOf[Class[V]]
			val ci = ColumnInfo[T, V](Column(Entity.this, column, tpe), columnToValue, tpe)
			if (!onlyForQuery) columns ::= ci else onlyForQueryColumns ::= ci
			ci
		}

		def option[V](columnToValue: T => Option[V])(implicit m: Manifest[V]): ColumnInfo[T, V] = to(optionToValue(columnToValue))
	}

	/**
	 * many-to-many, examples
	 *
	 * val attributes=manytomany(AttributeEntity) to (_.attributes)
	 * or, to override the default naming convention
	 * val attributes=manytomany(AttributeEntity) join("Product_To_Attributes","p_id","a_id") to (_.attributes)
	 */
	def manytomany[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT]) = new ManyToManyBuilder(referenced, false)

	def manytomanyreverse[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT]) = new ManyToManyBuilder(referenced, true)

	protected class ManyToManyBuilder[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT], reverse: Boolean)
		extends GetterDefinition with OnlyForQueryDefinition
	{
		val clz = Entity.this.clz
		private var linkTable = if (reverse) referenced.table + "_" + table else table + "_" + referenced.table
		private var schemaO: Option[Schema] = referenced.databaseSchema

		/**
		 * create the columns based on default naming conventions
		 */
		private var leftColumns = keysDuringDeclaration.map(pk => tableLower + "_" + pk.name)
		private var rightColumns = referenced match {
			case ee: ExternalEntity[_, _] => List(referenced.tableLower + "_id")
			case _ => referenced.keysDuringDeclaration.map(pk => referenced.tableLower + "_" + pk.name)
		}

		if (leftColumns.isEmpty) throw new IllegalStateException("%s didn't declare any primary keys or pk declaration before this declaration".format(clz))
		if (rightColumns.isEmpty) throw new IllegalStateException("%s didn't declare any primary keys or pk declaration".format(referenced.clz))

		def join(linkTable: String, leftColumn: String, rightColumn: String) = {
			this.linkTable = linkTable
			this.leftColumns = List(leftColumn)
			this.rightColumns = List(rightColumn)
			this
		}

		def join(linkTable: String, leftColumns: List[String], rightColumns: List[String]) = {
			this.linkTable = linkTable
			this.leftColumns = leftColumns
			this.rightColumns = rightColumns
			this
		}

		def schema(s: Schema) = {
			schemaO = Option(s)
			this
		}

		def to(columnToValue: T => Traversable[FT]): ColumnInfoTraversableManyToMany[T, FID, FT] = {
			if (keysDuringDeclaration.size != leftColumns.size) throw new IllegalStateException("join is invalid, left part keys %s and right part %s".format(keysDuringDeclaration, leftColumns))
			if (referenced.keysDuringDeclaration.size != rightColumns.size) throw new IllegalStateException("join is invalid, left part keys %s and right part %s".format(referenced.keysDuringDeclaration, rightColumns))

			val left = keysDuringDeclaration zip leftColumns
			val right = referenced.keysDuringDeclaration zip rightColumns

			val ci = ColumnInfoTraversableManyToMany[T, FID, FT](
				ManyToMany(
					Entity.this,
					LinkTable(
						schemaO,
						linkTable,
						left.map {
							case (k, c) =>
								Column(Entity.this, c, k.tpe)
						}, right.map {
							case (k, c) =>
								Column(Entity.this, c, k.tpe)
						}),
					TypeRef(createAlias(referenced.clz), referenced)
				),
				columnToValue,
				getterMethod
			)
			if (!onlyForQuery) columns ::= ci else onlyForQueryColumns ::= ci
			ci
		}

		def tojava(columnToValue: T => java.lang.Iterable[FT]): ColumnInfoTraversableManyToMany[T, FID, FT] =
			to((ctv: T) => columnToValue(ctv).asScala)

		def tostring(columnToValue: T => Traversable[String]): ColumnInfoTraversableManyToMany[T, FID, FT] =
			to((t: T) => {
				columnToValue(t).map(StringValue(_)).asInstanceOf[Traversable[FT]]
			})

		def toint(columnToValue: T => Traversable[Int]): ColumnInfoTraversableManyToMany[T, FID, FT] =
			to((t: T) => {
				columnToValue(t).map(IntValue(_)).asInstanceOf[Traversable[FT]]
			})

		def tofloat(columnToValue: T => Traversable[Float]): ColumnInfoTraversableManyToMany[T, FID, FT] =
			to((t: T) => {
				columnToValue(t).map(FloatValue(_)).asInstanceOf[Traversable[FT]]
			})

		def todouble(columnToValue: T => Traversable[Double]): ColumnInfoTraversableManyToMany[T, FID, FT] =
			to((t: T) => {
				columnToValue(t).map(DoubleValue(_)).asInstanceOf[Traversable[FT]]
			})

		def tolong(columnToValue: T => Traversable[Long]): ColumnInfoTraversableManyToMany[T, FID, FT] =
			to((t: T) => {
				columnToValue(t).map(LongValue(_)).asInstanceOf[Traversable[FT]]
			})
	}

	/**
	 * one-to-one, examples:
	 *
	 * val inventory=onetoone(InventoryEntity) to (_.inventory)
	 * or
	 * val inventory=onetoone(InventoryEntity) option (_.inventory)
	 */
	def onetoone[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT]) = new OneToOneBuilder(referenced)

	protected class OneToOneBuilder[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT])
		extends OnlyForQueryDefinition
	{
		private var cols = referenced.keysDuringDeclaration.map {
			k =>
				referenced.tableLower + "_" + k.name
		}

		def foreignkey(fk: String) = {
			cols = List(fk)
			this
		}

		def foreignkeys(cs: List[String]) = {
			cols = cs
			this
		}

		def to(columnToValue: T => FT): ColumnInfoOneToOne[T, FID, FT] = {
			val fPKs = referenced.keysDuringDeclaration
			if (fPKs.size != cols.size) throw new IllegalStateException("keys don't match foreign keys for %s -> %s".format(cols, referenced))
			val fkeys = fPKs zip cols
			val ci = ColumnInfoOneToOne(OneToOne(Entity.this, TypeRef(createAlias(referenced.clz), referenced), fkeys.map {
				case (k, col) =>
					Column(Entity.this, col, k.tpe)
			}), columnToValue)
			if (!onlyForQuery) columns ::= ci else onlyForQueryColumns ::= ci
			ci
		}

		def option(columnToValue: T => Option[FT]): ColumnInfoOneToOne[T, FID, FT] = to(optionToValue(columnToValue))
	}

	/**
	 * one-to-one reverse, i.e.
	 * val product=onetoonereverse(ProductEntity) to (_.product)
	 */
	def onetoonereverse[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT]) = new OneToOneReverseBuilder(referenced)

	protected class OneToOneReverseBuilder[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT])
		extends GetterDefinition
		with OnlyForQueryDefinition
	{
		val clz = Entity.this.clz
		private var fkcols = keysDuringDeclaration.map {
			k =>
				tableLower + "_" + k.name
		}

		def foreignkey(fk: String) = {
			fkcols = List(fk)
			this
		}

		def foreignkeys(cs: List[String]) = {
			fkcols = cs
			this
		}

		def to(columnToValue: T => FT): ColumnInfoOneToOneReverse[T, FID, FT] = {
			if (keysDuringDeclaration.size != fkcols.size) throw new IllegalStateException("keys don't match foreign keys for %s -> %s".format(fkcols, referenced))
			val fkeys = keysDuringDeclaration zip fkcols
			val ci = ColumnInfoOneToOneReverse(OneToOneReverse(Entity.this, TypeRef(createAlias(referenced.clz), referenced), fkeys.map {
				case (k, col) =>
					Column(Entity.this, col, k.tpe)
			}), columnToValue, getterMethod)
			if (!onlyForQuery) columns ::= ci else onlyForQueryColumns ::= ci
			ci
		}
	}

	/**
	 * one-to-many, i.e.
	 *
	 * val houses=onetomany(HouseEntity) to (_.houses)
	 */
	def onetomany[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT]) = new OneToManyBuilder(referenced)

	protected class OneToManyBuilder[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT])
		extends GetterDefinition
		with OnlyForQueryDefinition
	{
		val clz = Entity.this.clz
		private var fkcols = keysDuringDeclaration.map(tableLower + "_" + _.name)
		if (fkcols.isEmpty) throw new IllegalStateException("couldn't find any declared keys for %s, are keys declared before this onetomany?".format(clz))

		def foreignkey(fk: String) = {
			fkcols = List(fk)
			this
		}

		def foreignkeys(cs: List[String]) = {
			fkcols = cs
			this
		}

		def to(columnToValue: T => Traversable[FT]): ColumnInfoTraversableOneToMany[ID, T, FID, FT] = {
			if (keysDuringDeclaration.size != fkcols.size) throw new IllegalArgumentException("foreign keys declaration not correct, foreign keys %s , declared %s".format(referenced.keysDuringDeclaration, fkcols))
			val fkeys = keysDuringDeclaration zip fkcols
			val ci = ColumnInfoTraversableOneToMany[ID, T, FID, FT](
				OneToMany(
					Entity.this,
					TypeRef(createAlias(referenced.clz), referenced),
					fkeys.map {
						case (k, c) =>
							Column(Entity.this, c, k.tpe)
					}
				),
				columnToValue,
				getterMethod,
				Entity.this)
			if (!onlyForQuery) columns ::= ci else onlyForQueryColumns ::= ci
			ci
		}

		def tojava(columnToValue: T => java.lang.Iterable[FT]): ColumnInfoTraversableOneToMany[ID, T, FID, FT] =
			to((ctv: T) => columnToValue(ctv).asScala)

		def tostring(columnToValue: T => Traversable[String]): ColumnInfoTraversableOneToMany[ID, T, FID, FT] =
			to((t: T) => {
				columnToValue(t).map(StringValue(_)).asInstanceOf[Traversable[FT]]
			})

		def toint(columnToValue: T => Traversable[Int]): ColumnInfoTraversableOneToMany[ID, T, FID, FT] =
			to((t: T) => {
				columnToValue(t).map(IntValue(_)).asInstanceOf[Traversable[FT]]
			})

		def tofloat(columnToValue: T => Traversable[Float]): ColumnInfoTraversableOneToMany[ID, T, FID, FT] =
			to((t: T) => {
				columnToValue(t).map(FloatValue(_)).asInstanceOf[Traversable[FT]]
			})

		def todouble(columnToValue: T => Traversable[Double]): ColumnInfoTraversableOneToMany[ID, T, FID, FT] =
			to((t: T) => {
				columnToValue(t).map(DoubleValue(_)).asInstanceOf[Traversable[FT]]
			})

		def tolong(columnToValue: T => Traversable[Long]): ColumnInfoTraversableOneToMany[ID, T, FID, FT] =
			to((t: T) => {
				columnToValue(t).map(LongValue(_)).asInstanceOf[Traversable[FT]]
			})
	}

	/**
	 * many-to-one, i.e.
	 *
	 * val person=manytoone(PersonEntity) to (_.person)
	 */
	def manytoone[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT]) = new ManyToOneBuilder(referenced)

	protected class ManyToOneBuilder[FID, FPC <: Persisted, FT](referenced: Entity[FID, FPC, FT])
		extends GetterDefinition
		with OnlyForQueryDefinition
	{

		val clz = Entity.this.clz
		private var fkcols = referenced.keysDuringDeclaration map {
			pk =>
				referenced.tableLower + "_" + pk.name
		}

		def foreignkey(fk: String) = {
			fkcols = List(fk)
			this
		}

		def foreignkeys(cs: List[String]) = {
			fkcols = cs
			this
		}

		def to(columnToValue: T => FT): ColumnInfoManyToOne[T, FID, FT] = {
			if (referenced.keysDuringDeclaration.size != fkcols.size) throw new IllegalArgumentException("the number of foreign columns doesn't match the number of keys for %s => %s".format(referenced.keysDuringDeclaration, fkcols))
			val keys = referenced.keysDuringDeclaration zip fkcols

			val ci = ColumnInfoManyToOne(
				ManyToOne(
					Entity.this,
					keys.map {
						case (k, c) =>
							Column(Entity.this, c, k.tpe)
					},
					TypeRef(createAlias(referenced.clz), referenced)),
				columnToValue,
				getterMethod
			)
			if (!onlyForQuery) columns ::= ci else onlyForQueryColumns ::= ci
			ci
		}

		def option(columnToValue: T => Option[FT]): ColumnInfoManyToOne[T, FID, FT] =
			to(optionToValue(columnToValue))
	}

	// ===================== Java section ================================
	protected implicit def columnToJBoolean(ci: ColumnInfo[T, java.lang.Boolean])(implicit m: ValuesMap): java.lang.Boolean = m(ci)

	protected implicit def columnToJShort(ci: ColumnInfo[T, java.lang.Short])(implicit m: ValuesMap): java.lang.Short = m.short(ci)

	protected implicit def columnToJInteger(ci: ColumnInfo[T, java.lang.Integer])(implicit m: ValuesMap): java.lang.Integer = m.int(ci)

	protected implicit def columnToJLong(ci: ColumnInfo[T, java.lang.Long])(implicit m: ValuesMap): java.lang.Long = m.long(ci)

	protected implicit def columnToJDouble(ci: ColumnInfo[T, java.lang.Double])(implicit m: ValuesMap): java.lang.Double = m.double(ci)

	protected implicit def columnToJFloat(ci: ColumnInfo[T, java.lang.Float])(implicit m: ValuesMap): java.lang.Float = m.float(ci)

	// many to many : Java
	protected implicit def toJavaSet[FID, F](ci: ColumnInfoTraversableManyToMany[T, FID, F])(implicit m: ValuesMap): java.util.Set[F] =
		m(ci) match {
			case null => null
			case v => Utils.toJavaSet(v)
		}

	protected implicit def toJavaList[FID, F](ci: ColumnInfoTraversableManyToMany[T, FID, F])(implicit m: ValuesMap): java.util.List[F] = m(ci) match {
		case null => null
		case v => Utils.toJavaList(v)
	}

	// one to many : Java
	protected implicit def toJavaList[FID, F](ci: ColumnInfoTraversableOneToMany[ID, T, FID, F])(implicit m: ValuesMap): java.util.List[F] = m(ci) match {
		case null => null
		case v => Utils.toJavaList(v)
	}

	protected implicit def toJavaSet[FID, F](ci: ColumnInfoTraversableOneToMany[ID, T, FID, F])(implicit m: ValuesMap): java.util.Set[F] = m(ci) match {
		case null => null
		case v => Utils.toJavaSet(v)
	}

	// ===================== /Java section ================================

	/**
	 * utility method to cast an entity to it's persisted type
	 *
	 * @param t     the entity
	 * @return      entity with Stored
	 */
	def toPersistedType(t: T): T with Stored = t.asInstanceOf[T with Stored]
}
