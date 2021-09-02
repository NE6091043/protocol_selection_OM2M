package org.eclipse.om2m.comm.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.obix.Obj;
import org.eclipse.om2m.commons.obix.Str;
import org.eclipse.om2m.commons.obix.io.ObixEncoder;
import org.eclipse.om2m.commons.resource.Application;
import org.eclipse.om2m.commons.resource.Container;
import org.eclipse.om2m.commons.resource.ContentInstance;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.resource.SearchStrings;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;

public class ProtocolRepository {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(ProtocolRepository.class);

	/** Discovered SCL service */
	static SclService SCL;

	/** application name */
	private static String appId = "ProtocolRepository";

	/**
	 * Application point of contact for the controller
	 * {@link CommRepositoryController}
	 */
	public final static String APOCPATH = "protocol";

	/** container id of application */
	private static String containerId = "ProtocolContainer";

	/**
	 * Constructor
	 * 
	 * @param scl
	 *            - discovered SCL
	 */
	public ProtocolRepository(SclService scl) {
		ProtocolRepository.SCL = scl;
	}

	public static SclService getScl() {
		return SCL;
	}

	public static void setScl(SclService scl) {
		ProtocolRepository.SCL = scl;
	}

	public void createCommResources() {
		// Build RequestIndication
		RequestIndication requestIndication = new RequestIndication();
		requestIndication.setMethod(Constants.METHOD_CREATE);
		requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF);
		requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
		requestIndication.setRepresentation(new Application(appId, APOCPATH));

		LOGGER.info("The requestIndication : " + requestIndication);

		// Create the "Communication" application
		ResponseConfirm responseConfirm = SCL.doRequest(requestIndication);
		if (responseConfirm.getStatusCode().equals(StatusCode.STATUS_CREATED)) {

			// Create the Communication container
			requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + appId + Refs.CONTAINERS_REF);
			requestIndication.setRepresentation(new Container(containerId));

			ResponseConfirm responseContainer = SCL.doRequest(requestIndication);
			if (responseContainer.getStatusCode().equals(StatusCode.STATUS_CREATED)) {

				Obj obj = null;
				String content = "";
				ContentInstance contentInstance = null;
				SearchStrings searchStrings = null;
				/***************************************************************************
				 * Create the MQTT contentInstance
				 **************************************************************************/
				obj = new Obj();
				obj.add(new Str("Address", "https://dl.dropboxusercontent.com/u/6098562/org.eclipse.om2m.ipu.mqtt-1.0.0-SNAPSHOT.jar"));
				// obj.add(new Str("port", "1883"));
				obj.add(new Str("Lifetime", "2018/04/21 23:59"));
				obj.add(new Str("SymbolicName", "org.eclipse.om2m.ipu.mqtt"));
				content = ObixEncoder.toString(obj);
				contentInstance = new ContentInstance(content.getBytes());
				contentInstance.setId("MQTT");

				searchStrings = new SearchStrings();
				searchStrings.getSearchString().add("ResourceType/Protocol");
				searchStrings.getSearchString().add("ResourceID/org.eclipse.om2m.ipu.mqtt");
				// searchStrings.getSearchString().add("SymbolicName/org.eclipse.om2m.comm.mqtt");
				contentInstance.setSearchStrings(searchStrings);

				requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + appId + Refs.CONTAINERS_REF + "/" + containerId + Refs.CONTENTINSTANCES_REF);
				requestIndication.setRepresentation(contentInstance);
				SCL.doRequest(requestIndication);

				/***************************************************************************
				 * Create the CoAP contentInstance
				 **************************************************************************/
				obj = new Obj();
				obj.add(new Str("Address", "https://dl.dropboxusercontent.com/u/6098562/org.eclipse.om2m.comm.coap-0.8.0-SNAPSHOT.jar"));
				// obj.add(new Str("port", "5684"));
				obj.add(new Str("Lifetime", "2018/04/21 23:59"));
				obj.add(new Str("SymbolicName", "org.eclipse.om2m.comm.coap"));
				content = ObixEncoder.toString(obj);
				contentInstance = new ContentInstance(content.getBytes());
				contentInstance.setId("CoAP");

				searchStrings = new SearchStrings();
				searchStrings.getSearchString().add("ResourceType/Protocol");
				searchStrings.getSearchString().add("ResourceID/org.eclipse.om2m.comm.coap");
				// searchStrings.getSearchString().add("SymbolicName/org.eclipse.om2m.comm.coap");
				contentInstance.setSearchStrings(searchStrings);

				requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + appId + Refs.CONTAINERS_REF + "/" + containerId + Refs.CONTENTINSTANCES_REF);
				requestIndication.setRepresentation(contentInstance);
				SCL.doRequest(requestIndication);
			}
		}
	}

	public void deleteCommResources() {
		// Delete the Communication application
		String targetId = Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + appId;
		SCL.doRequest(new RequestIndication(Constants.METHOD_DELETE, targetId, Constants.ADMIN_REQUESTING_ENTITY));
	}

	public void start() {
		createCommResources();
	}
}
