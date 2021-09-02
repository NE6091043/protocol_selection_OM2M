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

public class ProtocolSelectionController_tmp implements IpuService {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(ProtocolSelectionController_tmp.class);

	/** Returns the implemented Application Point of Contact id */
	public String getAPOCPath() {
		return ProtocolSelectionMonitor.APOCPATH;
	}

	public ProtocolSelectionController_tmp() {

	}

	public ResponseConfirm doExecute(RequestIndication requestIndication) {
		return test_performance(requestIndication);
	}

	public ResponseConfirm doCreate(RequestIndication requestIndication) {
		return test_performance(requestIndication);
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

	private ResponseConfirm test_performance(RequestIndication requestIndication) {

		LOGGER.info("ResourceController [" + this.getClass().getSimpleName() + "]");
		LOGGER.info("RequestIndication [" + requestIndication + "]");
		try {
			String[] info = requestIndication.getTargetID().split("/");

			if (info[info.length - 1].equalsIgnoreCase("test")) {
				// http://140.116.247.77:8181/om2m/gscl/applications/D1/selection/test
				// http://140.116.247.77:8181/om2m/gscl/applications/protocol/selection/test

				String strPayload = requestIndication.getRepresentation();
				output("received meessage from device =" + strPayload);
				
				// 計算收到訊息的Delay--Start
				calculateDelayWhenReceiveMessage(strPayload);
				// 計算收到訊息的Delay--End

//				return new ResponseConfirm(StatusCode.STATUS_OK, str_generator(200));
				return new ResponseConfirm(StatusCode.STATUS_OK, "successful");

			}

			else if (info[info.length - 3].equalsIgnoreCase("test")) {
				// http://140.116.247.77:8181/om2m/gscl/applications/D1/selection/test/xmpp/50
				// http://140.116.247.77:8181/om2m/gscl/applications/D1/selection/test/coap/50
				// http://140.116.247.77:8181/om2m/gscl/applications/D1/selection/test/mqtt/50
				// http://140.116.247.77:8181/om2m/gscl/applications/D1/selection/test/none/50

				String appIdTest = info[info.length - 5];
				String strProtocolTest = info[info.length - 2];
				String strBytesTest = info[info.length - 1];
				String strPayloadTest = requestIndication.getRepresentation();

				ResponseConfirm test_result = test(appIdTest, strProtocolTest, strBytesTest, strPayloadTest);
				return test_result;
			}

		} catch (Exception e) {
			LOGGER.error("ProtocolSelectionController Error", e);
			return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "ProtocolSelectionController Error"));
		}

		return null;
	}

	/**
	 * 計算收到訊息的Delay
	 * 
	 * @param strPayload
	 * @param strDataSize
	 * @throws JSONException
	 * @throws IOException
	 */
	private void calculateDelayWhenReceiveMessage(String strPayload) throws JSONException, IOException {
		
		JSONObject jsonTodevice = new JSONObject(strPayload);
		
		if(!jsonTodevice.has("data")){
			throw new JSONException("no data property");
		}
		
		// 取得Device送出訊息時的時間
		if (jsonTodevice.has("timestamp")) {
			String strDataSize = String.valueOf(jsonTodevice.getString("data").getBytes().length);
			
			long previous_time = Long.parseLong(jsonTodevice.getString("timestamp"));

			// 取得目前時間
			long current_time = new Date().getTime();

			// 將時間相減，算出Delay(ms)
			long result = current_time - previous_time;

			// 將結果寫到文字檔裡
			FileWriter fw = new FileWriter("test_delay_" + strDataSize + "Bytes_whenGSCLReceiveDeviceMessage.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
			BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
			bw.write(String.valueOf(result));
			bw.write("\r\n");
			bw.flush();
			bw.close();
		}
	}

	private ResponseConfirm test(String appId, String strProtocol, String strBytes, String strPayload) throws JSONException {

		String[] arrAppId = appId.split("_");
		String strDeviceID = arrAppId[0];
		String strFunction = "";
		String strData = str_generator(Integer.parseInt(strBytes));

		if (arrAppId.length > 1) {
			strFunction = arrAppId[1];
		}

		String strDeviceLatestData = ProtocolSelectionMonitor.getDeviceLatestData(strDeviceID).toLowerCase();
		System.out.println("strDeviceLatestData = " + strDeviceLatestData);

		String strDeviceIP = getDeviceIP(strDeviceLatestData);
		String strCoapPort = getCoapPort(strDeviceLatestData);

		/***************************************************************************
		 * 選擇協定
		 **************************************************************************/
		Protocol selectedProtocol = Protocol.MQTT; // default protocol
		if (strProtocol.equalsIgnoreCase("none")) {
			// 由 ProtocolSelection Bundle決定用哪個協定傳輸

			// 取得開始時間
			long timeForSelectProtocolStart = new Date().getTime();

			// 取得device的average packet loss rate
			Integer iAveragePacketLossRate = ProtocolSelectionMonitor.getDevicePacketLossRate(strDeviceID);
			output("current PacketLossRate = " + iAveragePacketLossRate);

			// 選擇 Protocol
			selectedProtocol = selectProtocol(iAveragePacketLossRate, strData);

			// 取得結束時間
			long timeForSelectProtocolEnd = new Date().getTime();

			// 將時間相減，算出ProtocolSelection選擇協定花了多少時間
			long timeForSelectProtocolResult = timeForSelectProtocolEnd - timeForSelectProtocolStart;
			output("timeForSelectProtocolResult = " + String.valueOf(timeForSelectProtocolResult));

			// 將device的packet loss rate寫到文字檔裡
			try {
				FileWriter fw = new FileWriter("test_" + strDeviceID + "_packetlossrate.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
				BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
				bw.write(iAveragePacketLossRate.toString());
				bw.write("\r\n");
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// 將選擇的Protocol寫到文字檔裡
			try {
				FileWriter fw = new FileWriter("test_" + strDeviceID + "_protocol.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
				BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
				bw.write(selectedProtocol.toString());
				bw.write("\r\n");
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// 將選擇協定所花費的時間寫到文字檔裡
			try {
				FileWriter fw = new FileWriter("test_" + strDeviceID + "_selectionCost.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
				BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
				bw.write(String.valueOf(timeForSelectProtocolResult));
				bw.write("\r\n");
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// 由URI的參數決定用哪個協定傳輸
			if (strProtocol.toLowerCase().equals("mqtt")) {
				selectedProtocol = Protocol.MQTT;
			} else if (strProtocol.toLowerCase().equals("coap")) {
				selectedProtocol = Protocol.CoAP;
			} else if (strProtocol.toLowerCase().equals("xmpp")) {
				selectedProtocol = Protocol.XMPP;
			}
		}

		output("selectedProtocol = " + selectedProtocol.toString());

		/***************************************************************************
		 * Create json for device ----- Start
		 **************************************************************************/
		JSONObject jsonTodevice;
		if (strPayload.trim().equals("")) {
			jsonTodevice = new JSONObject();
		} else {
			jsonTodevice = new JSONObject(strPayload);
		}
		jsonTodevice.put("data", strData);

		if (!jsonTodevice.has("timestamp")) {
			long timestamp = new Date().getTime();
			jsonTodevice.put("timestamp", String.valueOf(timestamp));
		}
		// Create json for device ----- End

		if (selectedProtocol.equals(Protocol.MQTT)) {

			RequestIndication requestToDevice = new RequestIndication();
			requestToDevice.setMethod("CREATE");
			requestToDevice.setBase("mqtt://");
			requestToDevice.setTargetID(strDeviceID);
			requestToDevice.setRepresentation(jsonTodevice.toString());
			requestToDevice.setRequestingEntity("");
			requestToDevice.setProtocol("");
			return new RestClient().sendRequest(requestToDevice);

		} else if (selectedProtocol.equals(Protocol.CoAP)) {
			RequestIndication requestToDevice = new RequestIndication();
			requestToDevice.setMethod("CREATE");
			requestToDevice.setBase("coap://" + strDeviceIP + ":" + strCoapPort);
			requestToDevice.setTargetID("");
			requestToDevice.setRepresentation(jsonTodevice.toString());
			requestToDevice.setRequestingEntity("");
			requestToDevice.setProtocol("");
			return new RestClient().sendRequest(requestToDevice);

		} else if (selectedProtocol.equals(Protocol.XMPP)) {
			RequestIndication requestToDevice = new RequestIndication();
			requestToDevice.setMethod("CREATE");
			requestToDevice.setBase("xmpp://");
			requestToDevice.setTargetID(strDeviceID);
			requestToDevice.setRepresentation(jsonTodevice.toString());
			requestToDevice.setRequestingEntity("");
			requestToDevice.setProtocol("");
			return new RestClient().sendRequest(requestToDevice);
		}

		return new ResponseConfirm(StatusCode.STATUS_OK);
	}

	/**
	 * 產生指定大小的資料量
	 */
	public String str_generator(int size) {
		StringBuffer out = new StringBuffer();
		String strChars = ascii_letters() + digits();
		for (int i = 0; i < size; i++) {
			int idx = (int) (Math.random() * strChars.length());
			String str = strChars.substring(idx, idx + 1);
			out.append(str);
		}
		return out.toString();
	}

	public String ascii_letters() {
		StringBuffer out = new StringBuffer();
		for (char c = 'a'; c <= 'z'; c++) {
			out.append(c);
		}

		for (char c = 'A'; c <= 'Z'; c++) {
			out.append(c);
		}

		return out.toString();
	}

	public String digits() {
		StringBuffer out = new StringBuffer();
		for (int c = 0; c <= 9; c++) {
			out.append(c);
		}
		return out.toString();
	}

	private String getCoapPort(String strDeviceLatestData) {

		String strCoapPort = "";
		Pattern pattern;
		Matcher matcher;

		String patternStr = "<str name=[\"'][\\w]*coapport[\\w]*[\"'] val=[\"'](\\S+)[\"']/>";

		pattern = Pattern.compile(patternStr);
		matcher = pattern.matcher(strDeviceLatestData);
		if (matcher.find()) {
			strCoapPort = matcher.group(1);
		}

		System.out.println("strCoapPort = " + strCoapPort);
		return strCoapPort; // 5683
	}

	private String getDeviceIP(String strDeviceLatestData) {

		String strIP = "";
		Pattern pattern;
		Matcher matcher;

		String patternStr = "<str name=[\"'][\\w]*ip[\\w]*[\"'] val=[\"'](\\S+)[\"']/>";

		pattern = Pattern.compile(patternStr);
		matcher = pattern.matcher(strDeviceLatestData);
		if (matcher.find()) {
			strIP = matcher.group(1);
		}

		System.out.println("strIP = " + strIP);
		return strIP; // "192.168.1.105"
	}

	private Protocol selectProtocol(int iPacketLossRate, String strData) {

		int dataBytes = strData.getBytes().length;
		if (dataBytes <= 1000) {
			if (iPacketLossRate >= 28) {
				return Protocol.CoAP;
			} else {
				return Protocol.MQTT;
			}
		} else {
			return Protocol.MQTT;
		}
	}

	private void output(String msg) {
		for (int i = 1; i <= 10; i++) {
			System.out.println(msg);
		}
	}

}
