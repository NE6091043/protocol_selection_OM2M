import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ReaderHandlerForQueryDriven implements HttpHandler {

	private static int number = 0;

	@Override
	public void handle(HttpExchange t) throws IOException {

		// region print HttpExchange information
		System.out.println();
		System.out.println("Received information :");
		// System.out.println("t.getRequestURI() = " + t.getRequestURI());
		// System.out.println("t.getRequestURI().getPath() = " +
		// t.getRequestURI().getPath());
		// System.out.println("t.getRequestURI().toString() = " +
		// t.getRequestURI().toString());
		// System.out.println("t.getRequestURI().getRawPath() = " +
		// t.getRequestURI().getRawPath());
		// System.out.println("t.getRequestURI().getHost() = " +
		// t.getRequestURI().getHost());
		// System.out.println("t.getRequestURI().getPort() = " +
		// t.getRequestURI().getPort());
		// System.out.println("t.getRequestMethod() = " + t.getRequestMethod());
		// System.out.println("t.getRequestHeaders().getFirst('Authorization') = "
		// + t.getRequestHeaders().getFirst("Authorization"));
		// System.out.println("t.getProtocol() = " + t.getProtocol());

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

		System.out.println("body size = " + body.length());
		System.out.println("body = " + body);
		// endregion

		number++;
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
				String patternStr = "<str name='timestamp' val='(\\d+)'/>";
				Pattern pattern = Pattern.compile(patternStr);
				Matcher matcher = pattern.matcher(body);
				if (matcher.find()) {
					strTimestamp = matcher.group(1);

					// 取得Device送出訊息時的時間
					long previous_time = Long.parseLong(strTimestamp);

					// 取得目前時間
					long current_time = System.currentTimeMillis();

					// 將時間相減，算出Delay(ms)
					long result = current_time - previous_time;

					System.out.println("number = " + number);
					System.out.println("previous_time = " + new Date(previous_time));
					System.out.println("current_time = " + new Date(current_time));
					System.out.println("time = " + result);

					// 將結果寫到文字檔裡
					StringBuilder sb = new StringBuilder();
					sb.append(String.valueOf(result));
					sb.append("\r\n");
					writeToFile("nacomand_delay.txt", sb.toString());
				}

				t.sendResponseHeaders(204, -1);
			} catch (IOException e) {
				e.printStackTrace();
				t.sendResponseHeaders(501, -1);
			}

			// 若收到訊息的次數已到達原先設定的迴圈次數就停止程式
			if (number >= DeviceForQueryDriven.iLoopCount) {

				// 送指令給NSCL，停止 NSCL裡的ping的程式
				int statusCode = ChangeWanem.start(0);
				System.out.println("ChangeWanem(0) = " + statusCode);
				stopPingOnNSCL("nscl/applications/Ping/process/stop");
				
				// 停止 HTTP Server
				DeviceForQueryDriven.getServer().stop(0);
				Toolkit.getDefaultToolkit().beep();
			}

		}

	}

	private boolean stopPingOnNSCL(String cmd) {
		try {
			// 停止NSCL的ping程式
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
	 * 將結果寫到文字檔裡
	 */
	private void writeToFile(String fileName, String body) throws IOException {
		FileWriter fw = new FileWriter(fileName, true); // True則表示用附加的方式寫到檔案原有內容之後
		BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
		bw.write(body);
		bw.flush();
		bw.close();
	}

}
