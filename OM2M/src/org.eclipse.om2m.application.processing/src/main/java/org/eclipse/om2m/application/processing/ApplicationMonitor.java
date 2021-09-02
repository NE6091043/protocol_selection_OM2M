package org.eclipse.om2m.application.processing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.resource.Application;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;

public class ApplicationMonitor {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(ApplicationMonitor.class);

	/** Discovered SCL service */
	static SclService SCL;

	/**
	 * Application point of contact for the controller
	 * {@link MqttProxyController}
	 */
	public final static String APOCPATH = "appProcessing";

	/**
	 * Constructor
	 * 
	 * @param scl
	 *            - discovered SCL
	 */
	public ApplicationMonitor(SclService scl) {
		ApplicationMonitor.SCL = scl;
	}

	public SclService getScl() {
		return SCL;
	}

	public void setScl(SclService scl) {
		ApplicationMonitor.SCL = scl;
	}

	public void start() {
		createResource();
	}

	public void createResource() {

		// Build a RequestIndication
		RequestIndication requestIndication;

		// «Ø¥ß application processing ªº application resource
		requestIndication = new RequestIndication();
		requestIndication.setMethod(Constants.METHOD_CREATE);
		requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF);
		requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
		requestIndication.setRepresentation(new Application("applicationProcessing", APOCPATH));
		LOGGER.info("The requestIndication : " + requestIndication);
		SCL.doRequest(requestIndication);
	}

	public void deleteResources() {
		String targetId = "";
		
		// Delete application resource
		targetId = Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + "applicationProcessing";
		SCL.doRequest(new RequestIndication(Constants.METHOD_DELETE, targetId, Constants.ADMIN_REQUESTING_ENTITY));

	}
}
