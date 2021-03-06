package org.sagebionetworks.table.cluster;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.report.SynapseStorageProjectStats;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.util.Callback;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;

/**
 * This is an abstraction for table index CRUD operations.
 * @author John
 *
 */
public interface TableIndexDAO {
	
	/**
	 * Create a table with the given name if it does not exist.
	 * @param tableId The ID of the table.
	 * @param isView Is this table a View?
	 */
	public void createTableIfDoesNotExist(IdAndVersion tableId, boolean isView);
	
	/**
	 * Alter the given table as needed. The table will be changed
	 * according to the passed list of column changes.  This includes,
	 * additions, deletions, and updates.
	 * 
	 * @param tableId
	 * @param changes
	 * @param alterTemp When true the temporary table will be altered.  When false the original table will be altered.
	 * @return True if the table was altered. False if the table was not changed.
	 */
	public boolean alterTableAsNeeded(IdAndVersion tableId, List<ColumnChangeDetails> changes, boolean alterTemp);
	
	/**
	 * 
	 * @param connection
	 * @param tableId
	 * @return
	 */
	public void deleteTable(IdAndVersion tableId); 
	
	/**
	 * Create or update the rows passed in the given RowSet.

	 * @param grouping group of rows that change the same columns.
	 * @return
	 */
	void createOrUpdateOrDeleteRows(IdAndVersion tableId, Grouping grouping);
	
	/**
	 * Query a RowSet from the table.
	 * @param query
	 * @return
	 */
	public RowSet query(ProgressCallback callback, SqlQuery query);
	
	/**
	 * Run a simple count query.
	 * @param sql
	 * @param parameters
	 * @return
	 */
	public Long countQuery(String sql, Map<String, Object> parameters);
	
	/**
	 * Provides the means to stream over query results without keeping the row data in memory.
	 * 
	 * @param query
	 * @param handler
	 * @return
	 */
	public boolean queryAsStream(ProgressCallback callback, SqlQuery query, RowHandler handler);
	
	/**
	 * Get the row count for this table.
	 * 
	 * @param tableId
	 * @return The row count of the table. If the table does not exist then null.
	 */
	public Long getRowCountForTable(IdAndVersion tableId);
	
	/**
	 * Get the max complete version we currently have for this table.
	 * 
	 * @param tableId
	 * @param version the max complete version to remember
	 * @return The max complete version of the table. If the table does not exist then -1L.
	 */
	public Long getMaxCurrentCompleteVersionForTable(IdAndVersion tableId);

	/**
	 * Set the max complete version for this table
	 * 
	 * @param tableId
	 * @param highestVersion
	 */
	public void setMaxCurrentCompleteVersionForTable(IdAndVersion tableId, Long highestVersion);
	
	/**
	 * Set the MD5 hex of the table's current schema.
	 * 
	 * @param tableId
	 * @param schemaMD5Hex
	 */
	public void setCurrentSchemaMD5Hex(IdAndVersion tableId, String schemaMD5Hex);
	
	/**
	 * Get the MD5 hex of the table's current schema.
	 * @param tableId
	 * @return
	 */
	public String getCurrentSchemaMD5Hex(IdAndVersion tableId);
	
	/**
	 * Create all of the secondary tables used for an index if they do not exist.
	 * @param tableId
	 */
	public void createSecondaryTables(IdAndVersion tableId);
	
	/**
	 * Get the connection
	 * @return
	 */
	public JdbcTemplate getConnection();

	/**
	 * run calls within a read transaction
	 * 
	 * @param callable
	 * @return
	 */
	public <T> T executeInReadTransaction(TransactionCallback<T> callable);
	
	/**
	 * Run the passed callable within a write transaction.
	 * @param callable
	 * @return
	 */
	public <T> T executeInWriteTransaction(TransactionCallback<T> callable);

	/**
	 * Apply the passed set of file handle Ids to the given table index.
	 * 
	 * @param tableId
	 * @param fileHandleIds
	 */
	public void applyFileHandleIdsToTable(IdAndVersion tableId,
			Set<Long> fileHandleIds);
	
	/**
	 * Given a set of FileHandleIds and a talbeId, get the sub-set of
	 * FileHandleIds that are actually associated with the table.
	 * @param toTest
	 * @param objectId
	 * @return
	 */
	public Set<Long> getFileHandleIdsAssociatedWithTable(
			Set<Long> toTest, IdAndVersion tableId);
	
	/**
	 * Does the state of the index match the given data?
	 * 
	 * @param tableId
	 * @param versionNumber
	 * @param schemaMD5Hex
	 * @return
	 */
	public boolean doesIndexStateMatch(IdAndVersion tableId, long versionNumber, String schemaMD5Hex);

	/**
	 * Get the distinct Long values for a given column ID.
	 * 
	 * @param id
	 * @return
	 */
	public Set<Long> getDistinctLongValues(IdAndVersion tableId, String columnIds);

	/**
	 * Truncate all of the data in the given table.
	 * 
	 * @param tableId
	 */
	public void truncateTable(IdAndVersion tableId);
	
	
	/**
	 * Get information about each column of a database table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<DatabaseColumnInfo> getDatabaseInfo(IdAndVersion tableId);
	
	/**
	 * Provide the cardinality for the given columns and table.
	 * 
	 * Note: A single query will be executed, and the results added to the passed info list.
	 * 
	 * @param list
	 * @param tableId
	 */
	public void provideCardinality(List<DatabaseColumnInfo> list, IdAndVersion tableId);
	
	/**
	 * Provide the index name for each column in the table.
	 * @param list
	 * @param tableId
	 */
	public void provideIndexName(List<DatabaseColumnInfo> list, IdAndVersion tableId);
	
	
	/**
	 * The provided column data is used to optimize the indices on the given
	 * table. Indices are added until either all columns have an index or the
	 * maximum number of indices per table is reached. When a table has more
	 * columns than the maximum number of indices, indices are assigned to
	 * columns with higher cardinality before columns with low cardinality.
	 * 
	 * @param list
	 *            The current column information of this table used for the
	 *            optimization.
	 * @param tableId
	 *            The table to optimize.
	 * @param maxNumberOfIndex
	 *            The maximum number of indices allowed on a single table.
	 */
	public void optimizeTableIndices(List<DatabaseColumnInfo> list, IdAndVersion tableId, int maxNumberOfIndex);
	
	/**
	 * Populate the separate index table for the given list column.
	 * @param tableId
	 * @param listColumn
	 * @param rowIds Optional.  When included, only rows with the given IDs will be populated.
	 */
	public void populateListColumnIndexTable(IdAndVersion tableId, ColumnModel listColumn, Set<Long> rowIds);

		/**
		 * Create a temporary table like the given table.
		 * @param tableId
		 */
	public void createTemporaryTable(IdAndVersion tableId);

	/**
	 * Copy all of the data from the original table to the temporary table.
	 * @param tableId
	 */
	public void copyAllDataToTemporaryTable(IdAndVersion tableId);

	/**
	 * Delete the temporary table associated with the given table.
	 */
	public void deleteTemporaryTable(IdAndVersion tableId);

	/**
	 * Count the rows in the temp table.
	 * @param tableId
	 * @return
	 */
	public long getTempTableCount(IdAndVersion tableId);
	
	/**
	 * Create the entity replication tables if they do not exist.
	 * 
	 */
	void createEntityReplicationTablesIfDoesNotExist();

	/**
	 * Delete all entity data with the given Ids.
	 * @param progressCallback 
	 * 
	 * @param allIds
	 */
	public void deleteEntityData(List<Long> allIds);

	/**
	 * Add the given entity data to the index.
	 * 
	 * @param entityDTOs
	 */
	public void addEntityData(List<EntityDTO> entityDTOs);
	
	/**
	 * Get the entity DTO for a given entity ID.
	 * @param entityId
	 * @return
	 */
	public EntityDTO getEntityData(Long entityId);

	/**
	 * Given a container scope calculate the CRC32 of the entity replication table on 'id-etag'.
	 * @param viewType 
	 * 
	 * @param allContainersInScope
	 * @return
	 */
	public long calculateCRC32ofEntityReplicationScope(
			Long viewTypeMask, Set<Long> allContainersInScope);

	/**
	 * Copy the data from the entity replication tables to the given view.
	 * 
	 * @param viewId
	 * @param viewType
	 * @param allContainersInScope
	 * @param currentSchema
	 */
	public void copyEntityReplicationToView(Long viewId, Long viewTypeMask,
			Set<Long> allContainersInScope, List<ColumnModel> currentSchema);
	
	/**
	 * Copy the data from the entity replication tables to the given view.
	 * 
	 * @param viewId
	 * @param viewTypeMask
	 * @param allContainersInScope
	 * @param currentSchema
	 * @param rowIdsToCopy Optional.  When included, copy rows with these Ids to the view.
	 */
	public void copyEntityReplicationToView(Long viewId, Long viewTypeMask, Set<Long> allContainersInScope,
			List<ColumnModel> currentSchema, Set<Long> rowIdsToCopy);
	
	/**
	 * Copy the data from the entity replication tables to the given view's table.
	 * 
	 * @param viewId
	 * @param viewType
	 * @param allContainersInScope
	 * @param currentSchema
	 */
	public void createViewSnapshotFromEntityReplication(Long viewId, Long viewTypeMask,
			Set<Long> allContainersInScope, List<ColumnModel> currentSchema, CSVWriterStream outStream);

	/**
	 * Calculate the Cyclic-Redundancy-Check (CRC) of a table view's concatenation
	 * of ROW_ID + ETAG.  Used to determine if a view is synchronized with the
	 * truth.
	 * 
	 * @param viewId
	 * 
	 * @return
	 */
	long calculateCRC32ofTableView(Long viewId);

	/**
	 * Save both the current version and schema MD5 for current index.
	 * 
	 * @param tableId
	 * @param viewCRC
	 * @param schemaMD5Hex
	 */
	public void setIndexVersionAndSchemaMD5Hex(IdAndVersion tableId, Long viewCRC,
			String schemaMD5Hex);

	/**
	 * Get the distinct possible ColumnModels for a given set of container ids.
	 * @param containerIds
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<ColumnModel> getPossibleColumnModelsForContainers(
			Set<Long> containerIds, Long viewTypeMask, Long limit, Long offset);
	
	/**
	 * The process for synchronizing entity replication data with the truth is
	 * expensive, so the frequency of the synchronization is limited. Since
	 * synchronization occurs at the entity container level, each time a
	 * container is synchronized, a new expiration date is set for that
	 * container. The container should not be re-synchronized until the set
	 * expiration date is past.
	 * 
	 * For a given set of entity container IDs, this method will return the sub-set
	 * of containers which have expired.
	 * 
	 * If a given container ID does not have a set expiration, it will be returned.
	 * 
	 * @param entityContainerIds
	 * @return
	 */
	List<Long> getExpiredContainerIds(List<Long> entityContainerIds);
	

	/**
	 * @see {@link #getExpiredContainerIds(List)}.
	 * 
	 * Set the expiration for a set of containers.
	 * 
	 * @param expirations
	 */
	public void setContainerSynchronizationExpiration(List<Long> toSet, long newExpirationDateMS);
	
	/**
	 * Clear all expirations.
	 */
	public void truncateReplicationSyncExpiration();

	/**
	 * For each parent, get the sum of CRCs of their children.
	 *   
	 * @return Map.key = parentId and map.value = sum of children CRCs.
	 */
	public Map<Long, Long> getSumOfChildCRCsForEachParent(List<Long> parentIds);

	/**
	 * Get the Id and Etag for each child of the given Entity parentId.
	 * @param outOfSynchParentId
	 * @return
	 */
	public List<IdAndEtag> getEntityChildren(Long parentId);

	/**
	 * Get the rowIds for the given query.
	 * 
	 * @param sqlSelectIds
	 * @param parameters
	 * @return
	 */
	public List<Long> getRowIds(String sqlSelectIds, Map<String, Object> parameters);

	/**
	 * Get the sum of the files sizes for the given row IDs.
	 * 
	 * @param rowIds
	 * @return
	 */
	public long getSumOfFileSizes(List<Long> rowIds);

	/**
	 * Get the statistics about Synapse storage usage per-project.
	 * @return
	 */
	public void streamSynapseStorageStats(Callback<SynapseStorageProjectStats> callback);

	/**
	 * Populate a view from a snapshot.
	 * 
	 * @param idAndVersion
	 * @param input
	 * @param maxBytesPerBatch Used to limit the size of each batch of data pushed
	 *                         to the database. Only a single batch will reside in
	 *                         memory at a time. A batch will always contain at
	 *                         least one row even if the size of the row is larger
	 *                         than maxBytesPerBatch.
	 */
	public void populateViewFromSnapshot(IdAndVersion idAndVersion, Iterator<String[]> input, long maxBytesPerBatch);

	/**
	 * Initialize this dao by setting its database connection.
	 * 
	 * @param dataSource
	 */
	void setDataSource(DataSource dataSource);

	/**
	 * Get a single page (up to the provided limit) of rowIds that are out-of-date
	 * for the given view. A row is out-of-date if any of these conditions are true:
	 * <ul>
	 * <li>A row exists in the replication table but does not exist in the
	 * view.</li>
	 * <li>A row exists in the view but does not exist in the replication
	 * table.</li>
	 * <li>The rowId, etag, or benefactorId do not match in the view and the
	 * replication table.</li>
	 * </ul>
	 * 
	 * @param viewId The id of the view to check.
	 * @param viewTypeMask  The type of view.
	 * @param allContainersInScope All of the containers that define the scope 
	 * @param limit Limit the number of rows returned. 
	 * @return
	 */
	public Set<Long> getOutOfDateRowsForView(IdAndVersion viewId, long viewTypeMask, Set<Long> allContainersInScope, long limit);

	/**
	 * Delete a batch of rows from a view.
	 * 
	 * @param viewId
	 * @param idsToDelete
	 */
	public void deleteRowsFromViewBatch(IdAndVersion viewId, Long...idsToDelete);

}
