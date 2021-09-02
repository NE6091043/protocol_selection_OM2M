package org.eclipse.om2m.comm.repository;

import java.io.ByteArrayInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.resource.ErrorInfo;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.ipu.service.IpuService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class ProtocolRepositoryController implements IpuService {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(ProtocolRepositoryController.class);

	/** Returns the implemented Application Point of Contact id */
	public String getAPOCPath() {
		return ProtocolRepository.APOCPATH;
	}

	public ResponseConfirm doExecute(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

	public ResponseConfirm doRetrieve(RequestIndication requestIndication) {

		/**
		 * GET http://localhost:8080/om2m/nscl/applications/ProtocolRepository/
		 * protocol/port/1234
		 * 
		 */
		try {
			System.out.println("requestIndication.getBase() = " + requestIndication.getBase());
			System.out.println("requestIndication.getMethod() = " + requestIndication.getMethod());
			System.out.println("requestIndication.getProtocol() = " + requestIndication.getProtocol());
			System.out.println("requestIndication.getRequestingEntity() = " + requestIndication.getRequestingEntity());
			System.out.println("requestIndication.getUrl() = " + requestIndication.getUrl());
			System.out.println("requestIndication.getParameters() = " + requestIndication.getParameters());
			System.out.println("requestIndication.getTargetID() = " + requestIndication.getTargetID());

			String[] info = requestIndication.getTargetID().split("/");
			String strPortName = info[info.length - 2];
			String strPortNumber = info[info.length - 1];

			System.out.println("strPortName = " + strPortName);
			System.out.println("strPortNumber = " + strPortNumber);

			if (strPortName.toLowerCase().equals("port")) {
				if (strPortNumber.equals("1883")) {
					return new ResponseConfirm(StatusCode.STATUS_OK, "org.eclipse.om2m.ipu.mqtt");
				} else if (strPortNumber.equals("5683")) {
					return new ResponseConfirm(StatusCode.STATUS_OK, "org.eclipse.om2m.comm.coap");
				}
				return new ResponseConfirm(StatusCode.STATUS_NOT_FOUND, "Not found bundle for port number: " + strPortNumber);
			}
			return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, "IPU Error"));

		} catch (Exception e) {
			LOGGER.error("IPU Error", e);
			return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, "IPU Error"));
		}
	}

	public ResponseConfirm doCreate(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

	public ResponseConfirm doUpdate(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

	public ResponseConfirm doDelete(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

}