package org.eclipse.om2m.sample.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.resource.Application;
import org.eclipse.om2m.commons.resource.Container;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;

public class testMonitor {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(testMonitor.class);

	/** Discovered SCL service */
	static SclService SCL;

	/** application name */
	private static String appId = "MY_SENSOR";

	/**
	 * Application point of contact for the leds controller
	 */
	public final static String APOCPATH = "";

	/**
	 * Constructor
	 * 
	 * @param scl
	 *            - discovered SCL
	 */
	public testMonitor(SclService scl) {
		testMonitor.SCL = scl;
	}

	public SclService getScl() {
		return SCL;
	}

	public void setScl(SclService scl) {
		testMonitor.SCL = scl;
	}

	public void createMqttResources() {
		RequestIndication requestIndication = null;
		ResponseConfirm responseConfirm=null;
		
		// Create application
		requestIndication = new RequestIndication();
		requestIndication.setMethod(Constants.METHOD_CREATE);
		requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF);
		requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
		requestIndication.setRepresentation(new Application(appId, APOCPATH));
		LOGGER.info("The requestIndication : " + requestIndication);

		responseConfirm = SCL.doRequest(requestIndication);
		LOGGER.info(responseConfirm);

		// Create container
		requestIndication = new RequestIndication();
		requestIndication.setMethod(Constants.METHOD_CREATE);
		requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + appId + Refs.CONTAINERS_REF);
		requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
		requestIndication.setRepresentation(new Container("DATA"));
		LOGGER.info("The requestIndication : " + requestIndication);
		responseConfirm = SCL.doRequest(requestIndication);
		LOGGER.info(responseConfirm);
	}

	public void deleteMqttResources() {
		// Delete application
		String targetId = Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + appId;
		SCL.doRequest(new RequestIndication(Constants.METHOD_DELETE, targetId, Constants.ADMIN_REQUESTING_ENTITY));
	}
}
