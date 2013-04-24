package org.sagebionetworks.repo.manager.migration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.MigratableTableType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;

/**
 * Abstraction for the V2 migration manager.
 * 
 * @author John
 *
 */
public interface MigrationManager {

	/**
	 * The total number of rows in the table.
	 * @return
	 */
	public long getCount(UserInfo user, MigratableTableType type);
	
	/**
	 * List all row metadata in a paginated format. All rows will be migrated in the order listed by this method.
	 * This means metadata must be listed in dependency order.  For example, if row 'b' depends on row 'a' 
	 * then row 'a' must be listed before row 'b'.  For this example, row 'a' would be migrated before row 'b'.
	 *    
	 * @param limit
	 * @param offset
	 * @return
	 */
	public QueryResults<RowMetadata> listRowMetadata(UserInfo user, MigratableTableType type, long limit, long offset);
	
	/**
	 * Given a list of ID return the RowMetadata for each row that exist in the table.
	 * This method is used to detect changes between multiple stacks.  Only return values for IDs that
	 * exist in table.  Any missing RowMetadata in the result will be interpreted as a row that does not
	 * exist in table.
	 * 
	 * @param idList
	 * @return
	 */
	public RowMetadataResult listDeltaRowMetadata(UserInfo user, MigratableTableType type, List<String> idList);
	
	/**
	 * Get a batch of objects to backup.
	 * @param clazz
	 * @param rowIds
	 * @return
	 */
	public void writeBackupBatch(UserInfo user, MigratableTableType type, List<String> rowIds, OutputStream out);

	/**
	 * Create or update a batch.
	 * @param batch - batch of objects to create or update.
	 */
	public <T> void createOrUpdateBatch(UserInfo user, MigratableTableType type, InputStream in);
	
	/**
	 * Delete objects by their IDs
	 * @param type
	 * @param idList
	 */
	public int deleteObjectsById(UserInfo user, MigratableTableType type, List<String> idList);
}
