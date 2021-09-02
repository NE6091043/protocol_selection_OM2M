package org.eclipse.om2m.comm.protocolmanager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapAddr;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Http;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ProtocolManager {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(ProtocolManager.class);

	/** Discovered SCL service */
	private static SclService SCL;

	/** installed bundle in OSGi */
	private static Map<String, Bundle> mapBundle = new HashMap<String, Bundle>();

	/** 存放目前正在下載中的Bundle名稱 */
	private static ArrayList<String> listDownloadingBundle = new ArrayList<String>();

	private static String GatewayPrivateIP = System.getProperty("org.eclipse.om2m.gatewayPrivateIP", "192.168.1.124");

	private final static List<Integer> whiteFilter = new ArrayList<Integer>();
	private final static List<Integer> blackFilter = new ArrayList<Integer>();

	private static Pcap pcap;

	public ProtocolManager() {
		getWhiteFilter();
		getBlackFilter();
		getCurrentBundles();
	}

	public void getWhiteFilter() {
		whiteFilter.add(1883);
		whiteFilter.add(5683);
	}

	public void getBlackFilter() {
		for (int i = 1; i <= 1000; i++) {
			blackFilter.add(i);
		}
	}

	/**
	 * 取得目前OM2M裡面有哪些Bundle
	 */
	private void getCurrentBundles() {
		Bundle[] bs = Activator.getContext().getBundles();
		for (int i = 0; i < bs.length; i++) {
			mapBundle.put(bs[i].getSymbolicName(), bs[i]);
		}
	}

	public static SclService getScl() {
		return SCL;
	}

	public static void setScl(SclService scl) {
		ProtocolManager.SCL = scl;
	}

	public void startServer() {
		new Thread() {
			public void run() {
				try {
					LOGGER.info("===========================================start capture packet=============================================================");
					capture();

				} catch (Exception e) {
					LOGGER.error("protocol manager error", e);
				}
			}
		}.start();
	}

	public void stopServer() throws IOException {
		if (pcap != null) {
			pcap.close();
		}
	}

	/**
	 * Capture live data from network interfaces.
	 * 
	 * @param outputFolder
	 */
	public void capture() {

		LinkedList<PcapIf> alldevs = new LinkedList<PcapIf>();
		StringBuilder errbuf = new StringBuilder();

		int r = Pcap.findAllDevs(alldevs, errbuf);
		if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
			System.err.printf("Can't read list of devices, error is %s", errbuf.toString());
			return;
		}

		LOGGER.info("Network devices found:");

		PcapIf selected_device = getNetworkDeviceByIP(alldevs);

		if (selected_device == null) {
			LOGGER.debug("network device not found ");
		} else {
			LOGGER.debug(String.format("Choosing '%s' on your behalf:", (selected_device.getDescription() != null) ? selected_device.getDescription() : selected_device.getName()));
			capture(selected_device);
		}
	}

	private PcapIf getNetworkDeviceByIP(LinkedList<PcapIf> alldevs) {
		for (PcapIf device : alldevs) {
			List<PcapAddr> listAddr = device.getAddresses();
			for (int k = 0; k < listAddr.size(); k++) {
				byte[] byteAddr = listAddr.get(k).getAddr().getData();
				StringBuilder sbAddr = new StringBuilder();
				for (int i = 0; i < byteAddr.length; i++) {
					byte b = byteAddr[i];
					sbAddr.append(b & 0xFF);
					if (i < byteAddr.length - 1) {
						sbAddr.append('.');
					}
				}

				if (sbAddr.toString().equals(GatewayPrivateIP)) {
					return device;
				}
			}
		}
		return null;
	}

	/**
	 * * Capture the bytes from a specified network interface, and stores the
	 * attachments to the specified output folder. @param netInf Network
	 * interface to be captured. @param outputFolder Folder for storing the
	 * attachments.
	 */
	public void capture(final PcapIf netInf) {
		LOGGER.info("Try to capture packets from network interface interface '" + netInf.getName() + "' (" + netInf.toString() + ").");

		/***************************************************************************
		 * * First we setup error buffer and name for our file
		 **************************************************************************/
		// For any error
		final StringBuilder errbuf = new StringBuilder();
		// Parser for parsing packet using HTTP protocol.
		final Http http = new Http();
		// Truncate packet at this size.
		int snaplen = 2048;
		// Set the interface in 'Promiscuous Mode' which capture all packets
		// with or without destination address points to it.
		int promiscous = Pcap.MODE_PROMISCUOUS;
		// In milliseconds
		int timeout = 10 * 1000;

		/***************************************************************************
		 * Second we open up the selected file using openOffline call
		 **************************************************************************/
		pcap = Pcap.openLive(netInf.getName(), snaplen, promiscous, timeout, errbuf);
		// Error occurred while accessing the specified *.pcap file.
		if (pcap == null) {
			LOGGER.error("Error while opening device for capture: " + errbuf.toString());
			return;
		}
		/***************************************************************************
		 * Third we create a packet handler which will receive packets from the
		 * libpcap loop.
		 **************************************************************************/

		// Collection for storing the Internet addresses of the specified
		// interface.
		final Set<String> ifAddrs = new HashSet<String>(netInf.getAddresses().size());
		for (PcapAddr addr : netInf.getAddresses()) {
			try {
				// Parse the Internet address and cache it in the memory.
				String capturedAddr = InetAddress.getByAddress(addr.getAddr().getData()).getHostAddress();
				ifAddrs.add(capturedAddr);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		LOGGER.info("ifAddrs = " + ifAddrs);
		PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
			/**
			 * Get the next packet from the captured file.
			 * 
			 * @param packet
			 *            Captured packet.
			 * @param outputPath
			 *            Path for outputting the captured result.
			 */

			public void nextPacket(PcapPacket packet, String user) {

				// --------------------------------------------------------- //
				// Filter packets. //
				// --------------------------------------------------------- //
				Ip4 ip4 = new Ip4();
				if (packet.hasHeader(ip4)) {
					try {
						byte[] dIP = packet.getHeader(ip4).destination();
						byte[] sIP = packet.getHeader(ip4).source();
						String sourceIP = org.jnetpcap.packet.format.FormatUtils.ip(sIP);
						String destinationIP = org.jnetpcap.packet.format.FormatUtils.ip(dIP);

						if (!destinationIP.equals(GatewayPrivateIP)) {
							return;
						}
						Tcp tcp = new Tcp();
						if (packet.hasHeader(tcp)) {
							/***************************************************************************
							 * parse packet
							 **************************************************************************/
							final int tcpPort = tcp.destination();
							if (whiteFilter.contains(tcpPort)) {
								new Thread() {
									public void run() {
										try {
											String strSymbolicName = getBundleSymbolicName(tcpPort);
											if (strSymbolicName != null) {
												checkProtocol(strSymbolicName); // SymbolicName
											}
										} catch (Exception e) {
											LOGGER.error("ProtocolManager error", e);
										}
									}

									private String getBundleSymbolicName(int tcpPort) {
										String appId = "ProtocolRepository";
										String targetId = Constants.NSCL_ID + Refs.APPLICATIONS_REF + "/" + appId + "/" + "protocol" + "/" + "port" + "/" + tcpPort;
										RequestIndication requestIndication = new RequestIndication(Constants.METHOD_RETREIVE, targetId, Constants.ADMIN_REQUESTING_ENTITY);
										ResponseConfirm resp = getScl().doRequest(requestIndication);
										if (resp.getStatusCode() == StatusCode.STATUS_NOT_FOUND) {
											return null;
										}
										return resp.getRepresentation().trim(); // return
																				// "org.eclipse.om2m.ipu.mqtt"
									}
								}.start();

								// Thread.sleep(1000);
							}
						}
						Udp udp = new Udp();
						if (packet.hasHeader(udp)) {
							/***************************************************************************
							 * parse packet
							 **************************************************************************/
							final int udpPort = udp.destination();
							if (whiteFilter.contains(udpPort)) {
								LOGGER.debug("sourceIP = " + sourceIP + ", destinationIP = " + destinationIP);
								LOGGER.debug("udp.source() = " + udp.source() + ", udp.destination() = " + udp.destination());

								// download CoAP bundle using Thread
								new Thread() {
									public void run() {
										try {
											String strSymbolicName = getBundleSymbolicName(udpPort);
											if (strSymbolicName != null) {
												checkProtocol(strSymbolicName); // SymbolicName
											}
										} catch (Exception e) {
											LOGGER.error("ProtocolManager error", e);
										}
									}

									private String getBundleSymbolicName(int port) {
										String appId = "ProtocolRepository";
										String targetId = Constants.NSCL_ID + Refs.APPLICATIONS_REF + "/" + appId + "/" + "protocol" + "/" + "port" + "/" + port;
										RequestIndication requestIndication = new RequestIndication(Constants.METHOD_RETREIVE, targetId, Constants.ADMIN_REQUESTING_ENTITY);
										ResponseConfirm resp = getScl().doRequest(requestIndication);
										if (resp.getStatusCode() == StatusCode.STATUS_NOT_FOUND) {
											return null;
										}
										return resp.getRepresentation().trim(); // return
																				// "org.eclipse.om2m.comm.coap"
									}

								}.start();
								// Thread.sleep(1000);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};

		try {
			pcap.loop(Pcap.LOOP_INFINITE, jpacketHandler, "jNetPcap rocks!");
		} catch (Exception | Error e) {
			e.printStackTrace();
		} finally {
			if (pcap != null) {
				pcap.close();
			}
		}
	}

	private void checkProtocol(String strSymbolicName) throws IOException {

		/***************************************************************************
		 * 檢查 protocol bundle是否存在
		 **************************************************************************/
		getCurrentBundles();
		if (mapBundle.containsKey(strSymbolicName)) {

			/***************************************************************************
			 * 檢查 protocol bundle是否已啟動
			 **************************************************************************/
			Bundle bundle = mapBundle.get(strSymbolicName);
			checkBundleState(bundle);

		} else {
			/***************************************************************************
			 * 檢查protocol bundle是否正在下載中
			 **************************************************************************/
			if (listDownloadingBundle.contains(strSymbolicName)) {
				output(strSymbolicName + " bundle is downloading....");
				return;
			}

			/***************************************************************************
			 * 去NSCL搜尋protocol bundle，並下載、安裝、啟動
			 **************************************************************************/
			// 搜尋bundle
			ResponseConfirm response = discoveryProtocolBundleURI(strSymbolicName); // nscl/applications/ProtocolRepository/containers/ProtocolContainer/contentInstances/MQTT
			if (response.getStatusCode().equals(StatusCode.STATUS_OK)) {
				String responseData = response.getRepresentation();
				LOGGER.info(responseData);
				ResponseConfirm response2 = retrieveProtocolBundleURI(responseData);
				if (response2 != null) {
					if (response2.getStatusCode().equals(StatusCode.STATUS_OK)) {
						String responseData2 = response2.getRepresentation();
						LOGGER.info(responseData2);
						String bundleAddress = getProtocolBundleAddress(responseData2);

						listDownloadingBundle.add(strSymbolicName);

						// 下載 bundle
						String strDest = downloadBundle(strSymbolicName, bundleAddress);

						// 安裝 bundle
						Bundle testBundle = installBundle(strSymbolicName, strDest);

						// 啟動 bundle
						checkBundleState(testBundle);

						listDownloadingBundle.remove(strSymbolicName);
					}
				} else {
					output(strSymbolicName + " bundle not found");
				}
			}
		}
	}

	private void checkBundleState(Bundle bundle) {
		/***************************************************************************
		 * 檢查 protocol bundle是否已啟動
		 **************************************************************************/
		if (bundle.getState() != Bundle.ACTIVE) {
			String strSymbolicName = bundle.getSymbolicName();
			try {
				bundle.start();
				output(strSymbolicName + " bundle started.");
			} catch (BundleException e) {
				output(strSymbolicName + "start error");
				e.printStackTrace();
			}
		}
	}

	private Bundle installBundle(String strSymbolicName, String strDest) {
		/***************************************************************************
		 * 安裝Bundle
		 **************************************************************************/
		String fileLocation = "file:///" + strDest;
		Bundle testBundle = Activator.getContext().getBundle(fileLocation);

		if (testBundle == null) {
			try {
				testBundle = Activator.getContext().installBundle(fileLocation);
				output(strSymbolicName + " bundle installed.");
			} catch (BundleException e) {
				output(strSymbolicName + " bundle install failed.");
				e.printStackTrace();
			}
		}
		return testBundle;
	}

	public void output(String msg) {
		for (int i = 1; i <= 3; i++) {
			System.out.println(msg);
		}
	}

	/**
	 * 下載 Protocol Bundle
	 * 
	 * @param url
	 */
	private String downloadBundle(String strSymbolicName, String url) {

		/***************************************************************************
		 * 下載Bundle
		 **************************************************************************/
		String fileName = getFileName(url);
		String FileNamePath = "./plugins/" + fileName;

		while (true) {
			try {
				URL source = new URL(url);
				File destination = new File(FileNamePath);
				LOGGER.info("Bundle Path = " + destination.getCanonicalPath());
				FileUtils.copyURLToFile(source, destination);
				output(strSymbolicName + " bundle download successful");
				return destination.getCanonicalPath().replace('\\', '/');
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String getFileName(String url) {
		int index = url.lastIndexOf("/");
		String fileName = url.subSequence(index + 1, url.length()).toString();
		return fileName;
	}

	/**
	 * 取得 Protocol Bundle 的下載地址
	 * 
	 * @param responseData
	 * @return
	 */
	private String getProtocolBundleAddress(String responseData) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbf.newDocumentBuilder();

			Document instanceDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(responseData.getBytes("utf-8"))));
			String content64 = instanceDoc.getElementsByTagName("om2m:content").item(0).getTextContent();

			LOGGER.info("Content (Base64-encoded):\n" + content64 + "\n");

			final String content = new String(DatatypeConverter.parseBase64Binary(content64));
			LOGGER.info("Content:\n" + content + "\n");

			String patternStr = "(https://.+.jar)";
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(content);
			if (matcher.find()) {
				String strURL = matcher.group(1);
				return strURL;
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		return null;
	}

	private ResponseConfirm retrieveProtocolBundleURI(String responseData) {
		// 擷取第一個URI
		String patternStr = "<reference>(.+)</reference>";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(responseData);
		if (matcher.find()) {
			String first_uri = matcher.group(1);
			RequestIndication requestIndication = new RequestIndication(Constants.METHOD_RETREIVE, first_uri, Constants.ADMIN_REQUESTING_ENTITY);
			ResponseConfirm responseURI = SCL.doRequest(requestIndication);
			return responseURI;
		}
		return null;
	}

	/**
	 * 到遠端的 NSCL 搜尋 Protocol Bundle 的所在位置
	 * 
	 * @param strProtocol
	 * @return
	 */
	private ResponseConfirm discoveryProtocolBundleURI(String strSymbolicName) {
		// ex. strSymbolicName = org.eclipse.om2m.comm.coap
		// String strProtocolName =
		// strSymbolicName.substring(strSymbolicName.lastIndexOf(".") + 1);
		String targetId = Constants.NSCL_ID + Refs.DISCOVERY_REF;

		// Create a request
		// http://127.0.0.1:8080/om2m/nscl/discovery?searchString=ResourceID/strProtocolName
		RequestIndication requestIndication = new RequestIndication(Constants.METHOD_RETREIVE, targetId, Constants.ADMIN_REQUESTING_ENTITY);

		// 設定 Request 的參數
		Map<String, List<String>> parameters = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		values.add("ResourceID/" + strSymbolicName);
		parameters.put("searchString", values);
		requestIndication.setParameters(parameters);

		// 送出 Request
		ResponseConfirm response = SCL.doRequest(requestIndication);
		return response;
	}
}
