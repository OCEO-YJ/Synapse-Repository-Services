package org.sagebionetworks.repo.model;


/**
 * This is the data transfer object for Dataset Layers.
 * 
 * 
 * @author bhoff
 * 
 */
public interface DatasetLayer extends BaseChild {
	public String getReleaseNotes();

	public void setReleaseNotes(String releaseNotes);


}
