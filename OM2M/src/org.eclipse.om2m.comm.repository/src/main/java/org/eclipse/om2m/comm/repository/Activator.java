/**
 * org.eclipse.om2m.comm.repository用來存放傳輸協定的Bundle，供GSCL下載。
 * 這個Bundle只放在NSCL
 */
package org.eclipse.om2m.comm.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.core.service.SclService;
import org.eclipse.om2m.ipu.service.IpuService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(Activator.class);

	/** SCL service tracker */
	private ServiceTracker<Object, Object> sclServiceTracker;

	private ProtocolRepository commMonitor;

	private static BundleContext context;
	
	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;

		RegisterService();
		
		trackSCL(bundleContext);
	}
	private void RegisterService() {
		LOGGER.info("Register ProtocolRepositoryController..");
		getContext().registerService(IpuService.class.getName(), new ProtocolRepositoryController(), null);
		LOGGER.info("ProtocolRepositoryController is registered.");
	}
	
	private void trackSCL(BundleContext bundleContext) {
		// track the SCL service
		sclServiceTracker = new ServiceTracker<Object, Object>(bundleContext, SclService.class.getName(), null) {
			public void removedService(ServiceReference<Object> reference, Object service) {
				LOGGER.info("SclService removed");
				try {
					ProtocolRepository.setScl((SclService) service);
				} catch (IllegalArgumentException e) {
					LOGGER.error("Error removing SclService", e);
				}
			}

			public Object addingService(ServiceReference<Object> reference) {
				LOGGER.info("SclService discovered");
				SclService scl = (SclService) this.context.getService(reference);

				commMonitor = new ProtocolRepository(scl);
				new Thread() {
					public void run() {
						try {
							commMonitor.start();
						} catch (Exception e) {
							LOGGER.error("Communication Monitor error", e);
						}
					}
				}.start();
				return scl;
			}
		};
		// Open service trackers
		sclServiceTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		if (commMonitor != null) {
			commMonitor.deleteCommResources();
		}
	}

}
