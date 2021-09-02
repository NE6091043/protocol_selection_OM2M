package org.eclipse.om2m.ipu.protocolselection;

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

	private ProtocolSelectionMonitor protocolSelection;
	
	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;

		RegisterService();

		trackSclService(getContext());
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;

		if (protocolSelection != null) {
			protocolSelection.deleteResources();
		}
		logger.info("org.eclipse.om2m.ipu.protocolselection stopped.");
	}

	private void RegisterService() {
		logger.info("Register ProtocolSelectionController..");
		getContext().registerService(IpuService.class.getName(), new ProtocolSelectionController(), null);
		logger.info("ProtocolSelectionController is registered.");
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
					writePacketLossRateTask.setScl(scl);
					protocolSelection = new ProtocolSelectionMonitor(scl);
					
					new Thread(){
	                    public void run(){
	                        try {
	                        	protocolSelection.start();
	                        } catch (Exception e) {
	                            logger.error("protocolSelection error", e);
	                        }
	                    }
	                }.start();
	                
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
