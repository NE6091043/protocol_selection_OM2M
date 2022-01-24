import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

public class shut_down_server {
	
	public static void main(String[] args) throws HttpException, IOException{

		String url = "http://140.116.247.69:9000/shutdown";

		HttpClient httpclient = new HttpClient();

		PostMethod httpMethod = new PostMethod(url);

		StringBuilder sb = new StringBuilder();

		StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(),"application/xml", "UTF-8");

		httpMethod.setRequestEntity(requestEntity);

		int statuscode = httpclient.executeMethod(httpMethod);
		
		String str=new String(httpMethod.getResponseBody());
		
		System.out.println(str);
		
	}
}
