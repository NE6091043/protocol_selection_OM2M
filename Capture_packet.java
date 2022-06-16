package org.eclipse.om2m.core.redirector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Capture_packet {
	
//	private static String cmd="tshark -i ens38 -f \"tcp port 1883 and not ether src 00:0c:29:be:33:56\" -l";
	
	private static Process p=null;

	public static void main(String[] args) throws IOException, InterruptedException {
//		ArrayList<String> command = new ArrayList<String>();
//		command.add("tshark");
//		command.add("-l");
//		command.add("-i ens38");
//		command.add("-f \\\"tcp port 1883 and not ether src 00:0c:29:be:33:56\\\"");
		ProcessBuilder pb = new ProcessBuilder("tshark", "-l", "-i", "ens38", "-f", "tcp port 1883 and not ether src 00:0c:29:be:33:56");
		Process process = pb.start();

		BufferedReader br = null;
		    //tried different numbers for BufferedReader's last parameter
		    br = new BufferedReader(new InputStreamReader(process.getInputStream()), 1);
		    String line = null;
		    while ((line = br.readLine()) != null) {
//line = line.trim().replace(" ","@");
//1@0.000000000@192.168.101.136@→@192.168.101.134@MQTT@1725@Publish@Message@(id=3086)@[nscl/applications/NA/monitor]
		    	String[] data = line.trim().replace(" ","@").split("@");
		    	System.out.println(data[6]);
		    }
	}

}
