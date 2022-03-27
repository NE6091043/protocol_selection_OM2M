/*******************************************************************************
 * Copyright (c) 2013-2015 LAAS-CNRS (www.laas.fr) 
 * 7 Colonel Roche 31077 Toulouse - France
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Thierry Monteil (Project co-founder) - Management and initial specification, 
 * 		conception and documentation.
 *     Mahdi Ben Alaya (Project co-founder) - Management and initial specification, 
 * 		conception, implementation, test and documentation.
 *     Christophe Chassot - Management and initial specification.
 *     Khalil Drira - Management and initial specification.
 *     Yassine Banouar - Initial specification, conception, implementation, test 
 * 		and documentation.
 *     Guillaume Garzone - Conception, implementation, test and documentation.
 *     Francois Aissaoui - Conception, implementation, test and documentation.
 ******************************************************************************/
package org.eclipse.om2m.core.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.persistence.EntityManager;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.om2m.commons.resource.Application;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.comm.RestClient;
import org.eclipse.om2m.core.dao.DAOFactory;
import org.eclipse.om2m.core.dao.DBAccess;

/** allows the choice of the controller based on the aPoC of the application**/
public class APocController extends Controller{
	
	//2021.10.5
	public static String gettime() {
		String cmd="sh /home/user/time.sh";
		Runtime run = Runtime.getRuntime();
		Process pr = null;
		try {
			pr = run.exec(cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			pr.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line = "";
		try {
			line+=buf.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line;
	}
	
	//2021.9.26
	public void caltime(RequestIndication requestIndication) {
//		if(requestIndication.getRepresentation()==null || requestIndication.getRepresentation().contains("DATA/contentInstances/subscriptions"))
//			return;
		String[] tmp0=requestIndication.getRepresentation().split("application/xml\">");
		String a=tmp0[1].split("</om2m:representation>")[0];
		byte[] decoded = Base64.decodeBase64(a);
		String b="";
		try {
			b = new String(decoded, "UTF-8");
			//System.out.println(b);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String[] tmp=b.split("<om2m:content xmime:contentType=\"application/xml\">");
		String output=tmp[1].split("</om2m:content>\n")[0];
		byte[] decoded2 = Base64.decodeBase64(output);
		String c="";
		try {
			c=new String(decoded2, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(c);
		String[] tmp2=c.split("<str name='timestamp' val='");
		
		long gsclTime=Long.parseLong(tmp2[1].split("'/></obj>")[0]);
		//set time into it
		long diff=Long.parseLong(gettime())-gsclTime;
		String newcontent=tmp2[0]+"<str name='timestamp' val='"+diff+"'/></obj>";
		
		//adding time then encode back
		//System.out.println(newcontent);
		byte[] encode = newcontent.getBytes(Charset.forName("UTF-8"));
		String enc=Base64.encodeBase64String(encode);
		//System.out.println(enc);
		String enc2=tmp[0]+"<om2m:content xmime:contentType=\"application/xml\">"+enc+"</om2m:content>\n" + 
				"</om2m:contentInstance>";
		byte[] encode2 = enc2.getBytes(Charset.forName("UTF-8"));
		String xyz=Base64.encodeBase64String(encode2);
		System.out.println(xyz);
		String input=tmp0[0]+"application/xml\">"+xyz+"</om2m:representation>"+tmp0[1].split("</om2m:representation>")[1];
		requestIndication.setRepresentation(input);
	}
	
    public ResponseConfirm doCreate (RequestIndication requestIndication) {
        String sclId = requestIndication.getTargetID().split("/")[0];
        String applicationId = requestIndication.getTargetID().split("/")[2];
        String applicationUri = sclId+"/applications/"+applicationId;
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        Application application= DAOFactory.getApplicationDAO().find(applicationUri, em);
        em.close();
        String aPoCPath = application.getAPoCPaths().getAPoCPath().get(0).getPath();
        if (aPoCPath.matches(".*://.*")){
            String targetID = requestIndication.getTargetID().split(applicationId)[1];
            requestIndication.setBase(aPoCPath);
            requestIndication.setTargetID(targetID);
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
              caltime(requestIndication);
//            System.out.println("create");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
            return new RestClient().sendRequest(requestIndication);
        }else{
            InterworkingProxyController IPUController= new InterworkingProxyController();
            return IPUController.doCreate(requestIndication);
        }
    }


    public ResponseConfirm doRetrieve (RequestIndication requestIndication) {


        String sclId = requestIndication.getTargetID().split("/")[0];
        String applicationId = requestIndication.getTargetID().split("/")[2];
        String applicationUri = sclId+"/applications/"+applicationId;

        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        Application application= DAOFactory.getApplicationDAO().find(applicationUri, em);
        em.close();
        String aPoCPath = application.getAPoCPaths().getAPoCPath().get(0).getPath();
        if (aPoCPath.matches(".*://.*")){
            String targetID = requestIndication.getTargetID().split(applicationId)[1];
            requestIndication.setBase(aPoCPath);
            requestIndication.setTargetID(targetID);
            LOGGER.info(targetID);
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            caltime(requestIndication);
//            System.out.println("retrieve");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
            return new RestClient().sendRequest(requestIndication);
        }else{
            Controller IPUController= new InterworkingProxyController();
            return IPUController.doRetrieve(requestIndication);
        }
    }
    
    
    public ResponseConfirm doUpdate (RequestIndication requestIndication) {

        String sclId = requestIndication.getTargetID().split("/")[0];
        String applicationId = requestIndication.getTargetID().split("/")[2];
        String applicationUri = sclId+"/applications/"+applicationId;
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        Application application= DAOFactory.getApplicationDAO().find(applicationUri, em);
        em.close();
        String aPoCPath = application.getAPoCPaths().getAPoCPath().get(0).getPath();
        if (aPoCPath.matches(".*://.*")){
            String targetID = requestIndication.getTargetID().split(applicationId)[1];
            requestIndication.setBase(aPoCPath);
            requestIndication.setTargetID(targetID);
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            caltime(requestIndication);
//            System.out.println("update");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
            return new RestClient().sendRequest(requestIndication);
        }else{
            Controller IPUController= new InterworkingProxyController();
            return IPUController.doUpdate(requestIndication);
        }
    }



    public ResponseConfirm doDelete (RequestIndication requestIndication) {
        String sclId = requestIndication.getTargetID().split("/")[0];
        String applicationId = requestIndication.getTargetID().split("/")[2];
        String applicationUri = sclId+"/applications/"+applicationId;
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        Application application= DAOFactory.getApplicationDAO().find(applicationUri, em);
        em.close();
        String aPoCPath = application.getAPoCPaths().getAPoCPath().get(0).getPath();
        if (aPoCPath.matches(".*://.*")){
            String targetID = requestIndication.getTargetID().split(applicationId)[1];
            requestIndication.setBase(aPoCPath);
            requestIndication.setTargetID(targetID);
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            caltime(requestIndication);
//            System.out.println("delete");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
            return new RestClient().sendRequest(requestIndication);
        }else{
            Controller IPUController= new InterworkingProxyController();
            return IPUController.doDelete(requestIndication);
        }
    }

    public ResponseConfirm doExecute (RequestIndication requestIndication) {

        String sclId = requestIndication.getTargetID().split("/")[0];
        String applicationId = requestIndication.getTargetID().split("/")[2];
        String applicationUri = sclId+"/applications/"+applicationId;
        EntityManager em = DBAccess.createEntityManager();
        em.getTransaction().begin();
        Application application= DAOFactory.getApplicationDAO().find(applicationUri, em);
        em.close();
        String aPoCPath = application.getAPoCPaths().getAPoCPath().get(0).getPath();
        if (aPoCPath.matches(".*://.*")){
            String targetID = requestIndication.getTargetID().split(applicationId)[1];
            requestIndication.setBase(aPoCPath);
            requestIndication.setTargetID(targetID);
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("--------------------------------------");
//            System.out.println("execute");
//            caltime(requestIndication);
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
//            System.out.println("**************************************");
            return new RestClient().sendRequest(requestIndication);
        }else{
            Controller IPUController= new InterworkingProxyController();
            return IPUController.doExecute(requestIndication);
        }

    }

}
