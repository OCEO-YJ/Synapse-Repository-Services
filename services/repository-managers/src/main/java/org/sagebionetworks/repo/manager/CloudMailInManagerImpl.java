package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.model.message.multipart.Attachment;
import org.sagebionetworks.repo.model.message.multipart.MessageBody;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class CloudMailInManagerImpl implements CloudMailInManager {
	private static final String FROM_HEADER = "From";
	private static final String TO_HEADER = "To";
	private static final String CC_HEADER = "Cc";
	private static final String BCC_HEADER = "Bcc";
	private static final String SUBJECT_HEADER = "Subject";
	
	private static final String EMAIL_SUFFIX_LOWER_CASE = StackConfiguration.getNotificationEmailSuffix().toLowerCase();
	
	@Autowired
	PrincipalAliasDAO principalAliasDAO;
	
	public CloudMailInManagerImpl() {}
	
	public CloudMailInManagerImpl(PrincipalAliasDAO principalAliasDAO) {
		this.principalAliasDAO=principalAliasDAO;
	}

	@Override
	public MessageToUserAndBody convertMessage(Message message,
			String notificationUnsubscribeEndpoint) throws NotFoundException {

		try {
			List<String> to = new ArrayList<String>();
			String from = null;
			String subject = null;
			JSONObject headers = new JSONObject(message.getHeaders());
			Iterator<String> it = headers.keys();
			while (it.hasNext()) {
				String key = it.next();
				if (SUBJECT_HEADER.equalsIgnoreCase(key)) {
					subject = headers.getString(key);
				} else if (TO_HEADER.equalsIgnoreCase(key) ||
						CC_HEADER.equalsIgnoreCase(key) ||
						BCC_HEADER.equalsIgnoreCase(key)) {
					try {
						JSONArray array = headers.getJSONArray(key);
						for (int i=0; i<array.length(); i++) {
							to.add(array.getString(i));
						}
					} catch (JSONException e) {
						// it's a singleton, not an array
						to.add(headers.getString(key));
					}
				} else if (FROM_HEADER.equalsIgnoreCase(key)) {
					from = headers.getString(key);
				}
			}
			if (from==null) throw new IllegalArgumentException("Sender ('From') is required.");
			if (to.isEmpty()) throw new IllegalArgumentException("There must be at least one recipient.");
			MessageToUser mtu = new MessageToUser();
			mtu.setSubject(subject);
			Set<String> recipients = new HashSet<String>();
			for (String email : to) {
				recipients.add(lookupPrincipalIdForSynapseEmailAddress(email).toString());
			}
			mtu.setRecipients(recipients);
			mtu.setCreatedBy(lookupPrincipalIdForRegisteredEmailAddress(from).toString());		
			mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
			MessageToUserAndBody result = new MessageToUserAndBody();
			result.setMetadata(mtu);
			result.setMimeType(ContentType.APPLICATION_JSON.getMimeType());
			MessageBody messageBody = copyMessageToMessageBody(message);
			result.setBody(EntityFactory.createJSONStringForEntity(messageBody));
			return result;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static MessageBody copyMessageToMessageBody(Message message) {
		MessageBody result = new MessageBody();
		// Note, if this is a reply we simply take the reply field and drop the html and plain fields
		if (message.getReply_plain()!=null && message.getReply_plain().length()>0) {
			result.setPlain(message.getReply_plain());
		} else {
			result.setPlain(message.getPlain());
			result.setHtml(message.getHtml());
		}
		List<Attachment> attachments = new ArrayList<Attachment>();
		if (message.getAttachments()!=null) {
			for (org.sagebionetworks.repo.model.message.cloudmailin.Attachment cloudMailInAttachment : 
					message.getAttachments()) {
				Attachment attachment = new Attachment();
				attachment.setContent(cloudMailInAttachment.getContent());
				attachment.setContent_id(cloudMailInAttachment.getContent_id());
				attachment.setContent_type(cloudMailInAttachment.getContent_type());
				attachment.setDisposition(cloudMailInAttachment.getDisposition());
				attachment.setFile_name(cloudMailInAttachment.getFile_name());
				attachment.setSize(cloudMailInAttachment.getSize());
				attachment.setUrl(cloudMailInAttachment.getUrl());
				attachments.add(attachment);
			}
		}
		result.setAttachments(attachments);
		return result;
	}

	public Long lookupPrincipalIdForSynapseEmailAddress(String email) {
		// first, make sure it's actually an email address
		AliasEnum.USER_EMAIL.validateAlias(email);
		String emailLowerCase = email.toLowerCase();
		if (!emailLowerCase.endsWith(EMAIL_SUFFIX_LOWER_CASE))
			throw new IllegalArgumentException("Email must end with "+EMAIL_SUFFIX_LOWER_CASE);
		String aliasString = emailLowerCase.substring(0,  
				emailLowerCase.length()-EMAIL_SUFFIX_LOWER_CASE.length());
		PrincipalAlias alias = principalAliasDAO.findPrincipalWithAlias(aliasString);
		if (alias==null) throw new IllegalArgumentException("Specified user, "+aliasString+" is unknown to Synapse.");
		return alias.getPrincipalId();
	}
	
	public Long lookupPrincipalIdForRegisteredEmailAddress(String email) {
		// first, make sure it's actually an email address
		AliasEnum.USER_EMAIL.validateAlias(email);
		PrincipalAlias alias = principalAliasDAO.findPrincipalWithAlias(email);
		if (alias==null) throw new IllegalArgumentException("Specified address "+email+" is not registered with Synapse.");
		return alias.getPrincipalId();
	}
	
}