package org.eclipse.om2m.comm.protocolmanager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.core.service.SclService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	/** Logger */
	private static Log LOGGER = LogFactory.getLog(Activator.class);

	/** SCL service tracker */
	private ServiceTracker<Object, Object> sclServiceTracker;

	ProtocolManager protocolManager;

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

		// Start the Config server
		protocolManager = new ProtocolManager();
		protocolManager.startServer();
		LOGGER.info("protocol Manager started.");

		trackService(bundleContext);
	}

	private void trackService(BundleContext bundleContext) {
		/***************************************************************************
		 * track the SCL service
		 **************************************************************************/
		sclServiceTracker = new ServiceTracker<Object, Object>(bundleContext, SclService.class.getName(), null) {
			public void removedService(ServiceReference<Object> reference, Object service) {
				LOGGER.info("SclService removed");
				try {
					ProtocolManager.setScl((SclService) service);
					LifeTimeTask.setScl((SclService) service);

				} catch (IllegalArgumentException e) {
					LOGGER.error("Error removing SclService", e);
				}
			}

			public Object addingService(ServiceReference<Object> reference) {
				LOGGER.info("SclService discovered");
				SclService scl = (SclService) this.context.getService(reference);
				try {

					ProtocolManager.setScl(scl);
					LifeTimeTask.setScl(scl);

					new Thread() {
						public void run() {
							try {
								verifyLifeTime();
							} catch (Exception e) {
								LOGGER.error("protocolSelection error", e);
							}
						}
					}.start();

				} catch (Exception e) {
					LOGGER.error("Error adding SclService", e);
				}
				return scl;
			}
		};
		// Open service trackers
		sclServiceTracker.open();

	}

	private void RegisterService() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;

		protocolManager.stopServer();
		LOGGER.info("protocol Manager stopped.");
	}

	/**
	 * 每天隨機檢查Bundle的lifetime是否到期
	 */
	void verifyLifeTime() {
//		long period_day = 24 * 60 * 60 * 1000; // 每天執行一次
//		ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
//		LifeTimeTask lifetimeTask = new LifeTimeTask();
//		pool.scheduleWithFixedDelay(lifetimeTask, 3000, period_day, TimeUnit.MILLISECONDS);
		
		ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
		LifeTimeTask lifetimeTask = new LifeTimeTask(pool);
		pool.schedule(lifetimeTask, 3000, TimeUnit.MILLISECONDS);
	}
}
