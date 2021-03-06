# Introduction #

  * [Features](WhyMapperDao.md)
  * [Tutorial](Tutorial.md)

# Setup #

  * [Setting up mapperdao](SetupDaos.md)
  * [Sbt/Maven Configuration](MavenConfiguration.md)
  * [Integrating with popular web frameworks](IntegrationWithWebFrameworks.md)
  * [Examples](https://code.google.com/p/mapperdao-examples/)
  * [MapperDao test cases source code](TestCasesAsExamples.md)

# Mapping #

  * [Getting Started and simple mappings](SimpleMapping.md)
  * [MapperDao and Ids](IDS.md)
  * [CRUD operations](CRUD.md)
  * [Batch inserts and updates](Batch.md)
  * [Natural, Surrogate and composite keys](DiffBetweenNaturalAndSurrogateKeys.md)
  * [Auto-Generated keys](AutoGenerated.md)
  * [Sequences](Sequences.md)
  * [Configure Selects, Deletes and Updates](ConfigurableCRUD.md)
  * [One To Many](OneToManyMappings.md)
  * [Many To One](ManyToOneMapping.md)
  * [Many To Many](ManyToManyMapping.md)
  * [One To One](OneToOneMapping.md)
  * [Many-To-One and One-To-Many mapping of 2 entities](ManyToOneAndOneToManyCyclic.md)
  * [One-To-Many relationship of an entity to itself](OneToManySelfReferenced.md)
  * [Embedding Entities](EmbeddingEntities.md)
  * [Mapping Enumerations](Enumerations.md)
  * [Mapping class hierarchies](ClassHierarchyMappings.md)
  * [Option support](OptionSupport.md)
  * [Mapping relationships of simple types](SimpleTypesMapping.md)
  * [Mapping of external entities (entities not managed by mapperdao)](ExternalEntities.md)
  * [Declaring keys that are not available on mappings](DeclarePrimaryKeys.md)
  * [Blobs, mapping binary data](Blobs.md)
  * [mapping sql functions](SqlFunctions.md)
  * [mapping to tables on non-default schema](Schemas.md)
  * [Supported Data Types](SupportedDataTypes.md)

# Querying / Query DSL #

  * [Simple queries and joins](Queries.md)
  * [Self Join queries](SelfJoinQueries.md)
  * [Querying & joining using aliases](QueryAlias.md)
  * [Creating queries dynamically](DynamicQueries.md)
  * [Configure data retrieval for queries](ConfigurableQueries.md)
  * [Pagination: retrieving pages of data](Pagination.md)
  * [Aliases when querying](QueryAlias.md)

# Update / Delete DSL #

  * [Updating data with the update DSL](Update.md)
  * [Deleting data with the delete DSL](Delete.md)

# Transactions #

  * [Transactions](Transactions.md)

# Caching #

  * [Caching configuration and usage](Caching.md)

# Lazy Loading #

  * [Lazy Loading of relationships](LazyLoading.md)

# Creating Daos #

  * [Creating CRUD daos](CRUDDaos.md)
  * [Mixing traits that provide extra functionality](DAOMixins.md)

# Advanced #

  * [Cyclic dependency of entities](CyclicDependencies.md)
  * [Updating graphs of immutable objects](UpdatingImmutableGraphs.md)
  * [Data sharding: On the fly changing which tables to insert/update/select](SchemaModifications.md)
  * [Customize scala to database type mappings](CustomizeTypeMappings.md)

# Things to note #
  * [Object equality, equals() and hashCode() methods](ObjectEquality.md)
  * [Mock objects and Entities with recursive references to each other](RecursiveObjectRefereces.md)
  * [MySql notes](MysqlParticularities.md)
  * [Postgresql notes](PostGreSqlParticularities.md)
  * [Derby notes](DerbyParticularities.md)