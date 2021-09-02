package org.eclipse.om2m.ipu.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class OM2MMqttCallback implements MqttCallback{

	public OM2MMqttCallback() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void connectionLost(Throwable arg0) {
		System.out.println("connectionLost in OM2MMqttCallback");
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		System.out.println("deliveryComplete in OM2MMqttCallback");
		
	}

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		System.out.println("messageArrived in OM2MMqttCallback");
		
	}

}
