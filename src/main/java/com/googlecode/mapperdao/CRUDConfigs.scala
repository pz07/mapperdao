package com.googlecode.mapperdao

/**
 * configuration for fine tuning queries & updates
 *
 * @author kostantinos.kougios
 *
 * 28 Sep 2011
 */

/**
 * mapperDao.select configuration.
 *
 * @param	skip	skip one or more relationships from loading. If skipped, a traversable will
 * 					be empty and a reference to an other entity will be null
 */
case class SelectConfig(skip: Set[ColumnInfoRelationshipBase[_, _, _]] = Set())

case class QueryConfig(
	// skip any relationship from loading?
	skip: Set[ColumnInfoRelationshipBase[_, _, _]] = Set(),
	// start index of first row, useful for paginating
	offset: Option[Long] = None,
	// limit the number of returned rows, useful for paginating
	limit: Option[Long] = None) {

	// check parameter validity
	if (offset.isDefined && offset.get < 0) throw new IllegalArgumentException("offset is " + offset)
	if (limit.isDefined && limit.get < 0) throw new IllegalArgumentException("limit is " + offset)
}

object QueryConfig {
	/**
	 * @param offset	start index of first row that will be returned
	 * @param limit		how many rows to fetch
	 */
	def limits(offset: Long, limit: Long): QueryConfig = QueryConfig(offset = Some(offset), limit = Some(limit))

	/**
	 * @param skip			a set of relationships that should not be loaded from the database
	 * @param pageNumber	The page number
	 * @param rowsPerPage	How many rows each page contains
	 */
	def pagination(skip: Set[ColumnInfoRelationshipBase[_, _, _]], pageNumber: Long, rowsPerPage: Long): QueryConfig = {
		if (pageNumber < 1) throw new IllegalArgumentException("pageNumber must be >=1")
		QueryConfig(skip, Some((pageNumber - 1) * rowsPerPage), Some(rowsPerPage))
	}

	/**
	 * @param pageNumber	The page number
	 * @param rowsPerPage	How many rows each page contains
	 */
	def pagination(pageNumber: Long, rowsPerPage: Long): QueryConfig = pagination(Set(), pageNumber, rowsPerPage)
}