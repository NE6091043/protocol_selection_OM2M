import java.io.IOException;

import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.Paths;

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

//	private static int max_idx=-1;

	private static double total_delay=0.0;

	private static double avg_delay=0.0;



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

			strRequestURI.substring(strRequestURI.lastIndexOf("/") + 1);

//			System.out.println("strRequestContext = " + strRequestContext);

//			System.out.println("strRequestCmd = " + strRequestCmd);

			if (strRequestContext.equalsIgnoreCase(DeviceForQueryDriven.readerContext)) {

				// setDoorState(strRequestCmd);

			}



			t.sendResponseHeaders(204, -1);

		} else {

			try {



				// regex

				String strTimestamp = "";

//				String strNumber = "";

				String patternStr_1 = "<str name='timestamp' val='(\\d+)'/>";

				String patternStr_2 = "<str name='data' val=";

				Pattern pattern = Pattern.compile(patternStr_1);

				Matcher matcher = pattern.matcher(body);

				

//				String[] tmp=body.split("<str name='index' val='");

//				String idx=tmp[1].split("'/><str name='timestamp'")[0];

				

				

				

				String[] data = body.split("<str name='data' val='");

				String payload = data[1].split("'/>")[0];

				

				if (matcher.find()) {

					strTimestamp = matcher.group(1);

					

					long previous_time = Long.parseLong(strTimestamp);

					long current_time = System.currentTimeMillis();

					long result = current_time - previous_time;





					System.out.println("Content Size:" + payload.length());



					// System.out.println("Content:\n" + content + "\n");



					System.out.println("body size = " + body.length());



					System.out.println("time =" + result);

					

					total_delay+=result;

					avg_delay=total_delay/number;

					System.out.println("average delay =" + avg_delay);

					

					String strNumber = body.substring(body.lastIndexOf(patternStr_2) + 22 , body.lastIndexOf(patternStr_2) + 25);

					//System.out.println(strNumber);

					int number = Integer.parseInt(strNumber);

					System.out.println("------------"+number+"-----------------");

									

					

//					double est_lossrate=1-number/max_idx;

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

					

					

					String url = "http://140.116.247.69:9000/receive_reward";



					HttpClient httpclient = new HttpClient();



					PostMethod httpMethod = new PostMethod(url);



					StringBuilder sb = new StringBuilder();



					sb.append(avg_delay);



					StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(),"application/xml", "UTF-8");



					httpMethod.setRequestEntity(requestEntity);



					httpclient.executeMethod(httpMethod);

//					

//					System.out.println("---------------"+result);

//					

//					int line_idx=Integer.parseInt(idx);

//					if(number>100) {

//						line_idx+=100*(number/100);

//					}

					Path path = Paths.get("test.csv");

					List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

					String s=String.valueOf(number)+","+String.valueOf(result);

					lines.set(number , ","+s);

					Files.write(path, lines, StandardCharsets.UTF_8);

					

					

					System.out.println("-----------------------------------------------");

				}		

				





				t.sendResponseHeaders(204, -1);

			} catch (IOException e) {

				e.printStackTrace();

				t.sendResponseHeaders(501, -1);

			}



			// 若收到訊息的次數已到達原先設定的迴圈次數就停止程式

//			if (number >= DeviceForQueryDriven.iLoopCount) {

//

//				// 送指令給NSCL，停止 NSCL裡的ping的程式

//				//int statusCode = ChangeWanem.start(0);

//				int statusCode = 200;

//				System.out.println("ChangeWanem(0) = " + statusCode);

//				stopPingOnNSCL("nscl/applications/Ping/process/stop");

//				

//				// 停止 HTTP Server

//				DeviceForQueryDriven.getServer().stop(0);

//				Toolkit.getDefaultToolkit().beep();

//			}



		}



	}





}

