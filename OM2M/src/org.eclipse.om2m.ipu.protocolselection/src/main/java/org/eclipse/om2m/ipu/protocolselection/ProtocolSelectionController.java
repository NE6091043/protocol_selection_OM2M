package org.eclipse.om2m.ipu.protocolselection;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.resource.ErrorInfo;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.comm.RestClient;
import org.eclipse.om2m.ipu.service.IpuService;
import org.json.JSONException;
import org.json.JSONObject;

public class ProtocolSelectionController implements IpuService {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(ProtocolSelectionController.class);

	/** Returns the implemented Application Point of Contact id */
	public String getAPOCPath() {
		return ProtocolSelectionMonitor.APOCPATH;
	}

	public ProtocolSelectionController() {

	}

	public ResponseConfirm doExecute(RequestIndication requestIndication) {
		LOGGER.info("ResourceController [" + this.getClass().getSimpleName() + "]");
		LOGGER.info("RequestIndication [" + requestIndication + "]");
		try {
			String[] info = requestIndication.getTargetID().split("/");
			for (int i = 0; i < info.length; i++) {
				output("info[" + i + "]+" + info[i]);
			}

			output("info[info.length - 2] = " + info[info.length - 2]);
			output("info[info.length - 1] = " + info[info.length - 1]);
			if (info[info.length - 2].equals("process")) {
				if (info[info.length - 1].equals("start")) {
					new Thread(){
	                    public void run(){
	                        try {
	                        	writePacketLossRateTask task = new writePacketLossRateTask();
	        					task.run();
	                        } catch (Exception e) {
	                            e.printStackTrace();
	                        }
	                    }
	                }.start();
				
				} else if (info[info.length - 1].equals("stop")) {
					writePacketLossRateTask.getProcess().destroy();
				}
			}

			return new ResponseConfirm(StatusCode.STATUS_OK);

		} catch (Exception e) {
			LOGGER.error("ProtocolSelectionController Error", e);
			return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "ProtocolSelectionController Error"));
		}
	}

	public ResponseConfirm doCreate(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

	public ResponseConfirm doRetrieve(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

	public ResponseConfirm doUpdate(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

	public ResponseConfirm doDelete(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

	private void output(String msg) {
		for (int i = 1; i <= 3; i++) {
			System.out.println(msg);
		}
	}
}
