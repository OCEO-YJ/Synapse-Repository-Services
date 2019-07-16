package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.util.ValidateArgument;

public class FileHandleUrlRequest {
	
	private UserInfo userInfo;
	private boolean bypassAuthCheck = false;
	private FileHandleAssociateType associationType;
	private String associationId;
	private String fileHandleId;
	
	public FileHandleUrlRequest(UserInfo userInfo, String fileHandleId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(fileHandleId, "fileHandleId");
		this.userInfo = userInfo;
		this.fileHandleId = fileHandleId;
	}
	
	public FileHandleUrlRequest withBypassAuthCheck(boolean bypassAuthCheck) {
		this.bypassAuthCheck = bypassAuthCheck;
		return this;
	}
	
	public FileHandleUrlRequest withAssociation(FileHandleAssociateType associationType, String associationId) {
		ValidateArgument.required(associationType, "associationType");
		ValidateArgument.required(associationId, "associationId");
		this.associationType = associationType;
		this.associationId = associationId;
		return this;
	}
	
	public UserInfo getUserInfo() {
		return userInfo;
	}

	public boolean bypassAuthCheck() {
		return bypassAuthCheck;
	}
	
	public boolean hasAssociation() {
		return associationType != null && associationId != null;
	}

	public FileHandleAssociateType getAssociationType() {
		return associationType;
	}

	public String getAssociationId() {
		return associationId;
	}

	public String getFileHandleId() {
		return fileHandleId;
	}
	
	
	

}