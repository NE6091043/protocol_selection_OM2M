package org.eclipse.om2m.sample.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.core.service.SclService;
import org.eclipse.om2m.ipu.service.IpuService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private static BundleContext context;

	/** Logger */
	private static Log logger = LogFactory.getLog(Activator.class);

	/** SCL service tracker */
	private ServiceTracker<Object, Object> sclServiceTracker;
	
	private testMonitor test_Monitor;
	
	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		
		RegisterService();
		
		trackSclService(getContext());
	}

	private void RegisterService() {

		// Register the Rest MqttProxyController
//		logger.info("Register MqttProxyController..");
//		getContext().registerService(IpuService.class.getName(), new MqttProxyController(), null);
//		logger.info("MqttProxyController is registered.");

	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		
		if (test_Monitor.getScl() != null) {
			test_Monitor.deleteMqttResources();
		}
		logger.info("test application stopped.");
	}

	private void trackSclService(BundleContext bundleContext) {
		/***************************************************************************
		 * track the SCL service
		 **************************************************************************/
		sclServiceTracker = new ServiceTracker<Object, Object>(bundleContext, SclService.class.getName(), null) {
			public void removedService(ServiceReference<Object> reference, Object service) {
				logger.info("SclService removed");
				try {
				} catch (IllegalArgumentException e) {
					logger.error("Error removing SclService", e);
				}
			}

			public Object addingService(ServiceReference<Object> reference) {
				logger.info("SclService discovered");
				SclService scl = (SclService) this.context.getService(reference);
				try {
					test_Monitor = new testMonitor(scl);
					test_Monitor.createMqttResources();

				} catch (Exception e) {
					logger.error("Error adding SclService", e);
				}
				return scl;
			}
		};
		// Open service trackers
		sclServiceTracker.open();
		logger.info("SclService opened");
	}

}
