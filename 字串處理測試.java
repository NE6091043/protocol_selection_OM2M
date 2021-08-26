package org.eclipse.om2m.core.notifier;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

public class asrasrasras {
	
	static String ascii_letters() {
		StringBuffer out = new StringBuffer();
		for (char c = 'a'; c <= 'z'; c++) {
			out.append(c);
		}

		for (char c = 'A'; c <= 'Z'; c++) {
			out.append(c);
		}

		return out.toString();
	}

	static String digits() {
		StringBuffer out = new StringBuffer();
		for (int c = 0; c <= 9; c++) {
			out.append(c);
		}
		return out.toString();
	}
	
	static String str_generator(int size, int order) {
		StringBuffer out = new StringBuffer();
		String strChars = ascii_letters() + digits();
		//append order to value
		if(order < 10)
			out.append("00" + Integer.toString(order));
		else if(order < 100)
			out.append("0" + Integer.toString(order));
		else
			out.append(Integer.toString(order));
		
		for (int i = 0; i < size - 3; i++) {
			int idx = (int) (Math.random() * strChars.length());
			String str = strChars.substring(idx, idx + 1);
			out.append(str);
		}
		return out.toString();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String ss="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<om2m:notify xmlns:om2m=\"http://uri.etsi.org/m2m\" xmlns:xmime=\"http://www.w3.org/2005/05/xmlmime\">\n" + 
				"    <om2m:statusCode>STATUS_CREATED</om2m:statusCode>\n" + 
				"    <om2m:representation xmime:contentType=\"application/xml\">PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/Pgo8b20ybTpjb250ZW50SW5zdGFuY2UgeG1sbnM6b20ybT0iaHR0cDovL3VyaS5ldHNpLm9yZy9tMm0iIHhtbG5zOnhtaW1lPSJodHRwOi8vd3d3LnczLm9yZy8yMDA1LzA1L3htbG1pbWUiIG9tMm06aWQ9IkNJXzE1NzQ3NDI3NCIgaHJlZj0iZ3NjbC9hcHBsaWNhdGlvbnMvRDJfVGVtcC9jb250YWluZXJzL0RBVEEvY29udGVudEluc3RhbmNlcy9DSV8xNTc0NzQyNzQiPgogICAgPG9tMm06Y3JlYXRpb25UaW1lPjIwMjEtMDgtMjRUMDY6MjU6MDIuMDgwLTA3OjAwPC9vbTJtOmNyZWF0aW9uVGltZT4KICAgIDxvbTJtOmxhc3RNb2RpZmllZFRpbWU+MjAyMS0wOC0yNFQwNjoyNTowMi4wODAtMDc6MDA8L29tMm06bGFzdE1vZGlmaWVkVGltZT4KICAgIDxvbTJtOmRlbGF5VG9sZXJhbmNlPjIwMjEtMDgtMjRUMDk6NDU6MDIuMDgwLTA3OjAwPC9vbTJtOmRlbGF5VG9sZXJhbmNlPgogICAgPG9tMm06Y29udGVudFNpemU+Mzk0PC9vbTJtOmNvbnRlbnRTaXplPgogICAgPG9tMm06Y29udGVudCB4bWltZTpjb250ZW50VHlwZT0iYXBwbGljYXRpb24veG1sIj5QRzlpYWo0OGMzUnlJRzVoYldVOUoyUmhkR0VuSUhaaGJEMG5NREV3ZFRkT2NHdFhNR2cxVUhsYWVHMHpXVWsyVEd0cmR6Tk5PV3BMWWxOUU1Wa3pPR1E1ZW5aUmMweEpaMDVIYVRkTFZUaE9TRTFRY21Oa1FsRnRSV3gyU1UxaE56QlNkblF6ZUVGUk5tZzVXVUZoUTNWS2FIWm9WV2szWm1zMk5FMVhTa0U0ZHpoaFJUZHBUMlZUUWpCeU5HNW1hMUJqZWs5SWFsQTBhVUZUVmtaWmQyUjNSV2hXUTBWWVVHTm9WRVpMUzBKeE1raG9TM1pRU1dSalRqTXljRzVNYkd4WE9GZDJWelZxTm1wdGVFMVBObTF2UldsM1FtTnBia2hOT0VOVlIwSjNiMHhRVkZwb1owUkhZbW8zVEVFeGFrRlNOVEl4U0dFMWJHcElhVGhQT1ZaVFExRnJOVnB4YUdwb05uaE9ORlJ2VUhwM2MySTBNMkl4YkVWSWNuWk5jVXRqU0RSdFpHMVJhRUZRTWxKVE9UaFhZMDV0VUdwV2NFSlJSMGswTUZWbk1td3pkakZOTTBwQmVGVmpjV1pXZEV0cFFUTmlKeTgrUEhOMGNpQnVZVzFsUFNkMGFXMWxjM1JoYlhBbklIWmhiRDBuTVRZeU9UZ3hNVFV3TWpBMU15Y3ZQand2YjJKcVBnPT08L29tMm06Y29udGVudD4KPC9vbTJtOmNvbnRlbnRJbnN0YW5jZT4K</om2m:representation>\n" + 
				"    <om2m:subscriptionReference>gscl/applications/D2_Temp/containers/DATA/contentInstances/subscriptions/SUB_947550733</om2m:subscriptionReference>\n" + 
				"</om2m:notify>";
//		System.out.println(ss.length());
////		String st = str_generator(100, 0);
//		String st = "MDAwQWRpdEcwNDY2M0M3TDBVZGdaWm81OENHYThVUGNRanF5VkJwQld0SHJXeHdYb1N6M2c4Y1J1Mk5FNXlDSGt0SEF0MUF1bmhzZEpvRVVMSU5yYXBXWEFJN1FWbU1Ma0ZLTw==";
//		System.out.println(st);
//		System.out.println(st.length());
		
		HttpClient httpclientsend = new HttpClient();
		PostMethod senddata=new PostMethod("140.116.247.69:9000");
		StringBuilder sz = new StringBuilder();
		sz.append(ss.length());
		StringRequestEntity sendentity = new StringRequestEntity(sz.toString());
		senddata.setRequestEntity(sendentity);
		//int response = httpclientsend.executeMethod(senddata);
		//string entity = response.getEntity();
		
		
	}

}
