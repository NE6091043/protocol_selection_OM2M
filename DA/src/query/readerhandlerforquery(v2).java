import java.awt.Toolkit;

import java.awt.event.ActionEvent;

import java.awt.event.ActionListener;

import java.io.BufferedWriter;

import java.io.FileWriter;

import java.io.IOException;

import java.io.InputStream;

import java.io.UnsupportedEncodingException;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.Paths;

import java.util.Date;

import java.util.List;

import java.util.regex.Matcher;

import java.util.regex.Pattern;



import org.apache.commons.httpclient.HttpClient;

import org.apache.commons.httpclient.methods.PostMethod;

import org.apache.commons.httpclient.methods.StringRequestEntity;



import com.sun.net.httpserver.HttpExchange;

import com.sun.net.httpserver.HttpHandler;



public class ReaderHandlerForQueryDriven implements HttpHandler {



	private static int number = 0;

	private static int max_idx=-1;



	@Override

	public void handle(HttpExchange t) throws IOException {



//		System.out.println("Received information :");

//		System.out.println("----------------------------------------------- :");

		String body = "";

		int i;

		char c;

		try {

			InputStream is = t.getRequestBody();



			while ((i = is.read()) != -1) {

				c = (char) i;

				body = (String) (body + c);

			}

		} catch (Exception e) {

			e.printStackTrace();

		}



		// mod

		// 2021.12.9

//		System.out.println("body = " + body);

		// endregion



		++number;



		System.out.println("Received command:" +number+" times");

		

		if (body.isEmpty()) {

			String strRequestURI = t.getRequestURI().getPath();

			String strRequestContext = strRequestURI.substring(0, strRequestURI.lastIndexOf("/"));

			String strRequestCmd = strRequestURI.substring(strRequestURI.lastIndexOf("/") + 1);

			System.out.println("strRequestContext = " + strRequestContext);

			System.out.println("strRequestCmd = " + strRequestCmd);

			if (strRequestContext.equalsIgnoreCase(DeviceForQueryDriven.readerContext)) {

				// setDoorState(strRequestCmd);

			}



			t.sendResponseHeaders(204, -1);

		} else {

			try {



				// regex

				String strTimestamp = "";

				String strNumber = "";

				String patternStr_1 = "<str name='timestamp' val='(\\d+)'/>";

				String patternStr_2 = "<str name='data' val=";

				Pattern pattern = Pattern.compile(patternStr_1);

				Matcher matcher = pattern.matcher(body);

				

				String[] tmp=body.split("<str name='index' val='");

				String idx=tmp[1].split("'/><str name='timestamp'")[0];

				

				

				

				String[] data = body.split("<str name='data' val='");

				String payload = data[1].split("'/>")[0];

				

				if (matcher.find()) {

					strTimestamp = matcher.group(1);

					

					long result = Long.parseLong(strTimestamp);



						

					System.out.println("Order: " + idx);



					System.out.println("Content Size:" + payload.length());



					// System.out.println("Content:\n" + content + "\n");



					System.out.println("body size = " + body.length());



					System.out.println("time =" + result);

					

					if(max_idx<Integer.parseInt(idx)) {

						max_idx=Integer.parseInt(idx);

					}

					

					

					double est_lossrate=1-number/max_idx;

//					System.out.println("ssssssssssssssssssssssssssssssssss");

//					System.out.println("ssssssssssssssssssssssssssssssssss");

//					System.out.println("ssssssssssssssssssssssssssssssssss");

//					System.out.println(est_lossrate);

//					System.out.println("ssssssssssssssssssssssssssssssssss");

//					System.out.println("ssssssssssssssssssssssssssssssssss");

//					System.out.println("ssssssssssssssssssssssssssssssssss");

					

					

					if(result<=0) {

						System.out.println("ssssssssssssssssssssssssssssssssss");

						System.out.println("ssssssssssssssssssssssssssssssssss");

						System.out.println("ssssssssssssssssssssssssssssssssss");

						System.out.println("ssssssssssssssssssssssssssssssssss");

						System.out.println("ssssssssssssssssssssssssssssssssss");

						System.out.println("ssssssssssssssssssssssssssssssssss");

					}

					

					

					String url = "http://140.116.247.69:9000/receive_delay_as_reward";



					HttpClient httpclient = new HttpClient();



					PostMethod httpMethod = new PostMethod(url);



					StringBuilder sb = new StringBuilder();



					sb.append(result);



					//2021.9.14 mod



					sb.append("//");



					sb.append(idx);

					

					sb.append("//");

					

					sb.append(est_lossrate);

					

					sb.append("//");

					

					sb.append(payload.length());



					StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(),"application/xml", "UTF-8");



					httpMethod.setRequestEntity(requestEntity);



					int statuscode = httpclient.executeMethod(httpMethod);

					

//					System.out.println("---------------"+result);

					

					int line_idx=Integer.parseInt(idx);

//					if(number>100) {

//						line_idx+=100*(number/100);

//					}

					Path path = Paths.get("test.csv");

					List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

					String s=idx+","+String.valueOf(result);

					lines.set(line_idx , ","+s);

					Files.write(path, lines, StandardCharsets.UTF_8);

					

					

					System.out.println("-----------------------------------------------");

				}		

				





				t.sendResponseHeaders(204, -1);

			} catch (IOException e) {

				e.printStackTrace();

				t.sendResponseHeaders(501, -1);

			}



			// ???????????????????????????????????????????????????????????????????????????

//			if (number >= DeviceForQueryDriven.iLoopCount) {

//

//				// ????????????NSCL????????? NSCL??????ping?????????

//				//int statusCode = ChangeWanem.start(0);

//				int statusCode = 200;

//				System.out.println("ChangeWanem(0) = " + statusCode);

//				stopPingOnNSCL("nscl/applications/Ping/process/stop");

//				

//				// ?????? HTTP Server

//				DeviceForQueryDriven.getServer().stop(0);

//				Toolkit.getDefaultToolkit().beep();

//			}



		}



	}



	private boolean stopPingOnNSCL(String cmd) {

		try {

			// ??????NSCL???ping??????

			String url = "http://" + DeviceForQueryDriven.GSCL_IP + ":" + DeviceForQueryDriven.GSCL_Port + "/om2m/" + cmd;

			HttpClient httpclient = new HttpClient();

			PostMethod httpMethod = new PostMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");



			int statusCode = httpclient.executeMethod(httpMethod);

			System.out.println("stopPingOnNSCL() = " + statusCode);

			return true;

		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}

		return false;

	}



	/**

	 * ???????????????????????????

	 */

	private void writeToFile(String fileName, String body) throws IOException {

		FileWriter fw = new FileWriter(fileName, true); // True?????????????????????????????????????????????????????????

		BufferedWriter bw = new BufferedWriter(fw); // ???BufferedWeiter???FileWrite???????????????

		bw.write(body);

		bw.flush();

		bw.close();

	}



}

