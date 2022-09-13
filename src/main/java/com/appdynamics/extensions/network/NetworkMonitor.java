/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.network;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.network.input.Stat;
import com.appdynamics.extensions.util.AssertUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.network.NetworkConstants.DEFAULT_METRIC_PREFIX;

/**
 * Monitors network related metrics
 * 
 * @author Florencio Sarmiento
 *
 */
public class NetworkMonitor extends ABaseMonitor {
	
	private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(NetworkMonitor.class);

	@Override
	protected String getDefaultMetricPrefix() {
		return DEFAULT_METRIC_PREFIX;
	}

	@Override
	public String getMonitorName() {
		return "Network Monitor";
	}

	@Override
	protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
		//this should be a task per server as the ExecServiceProvider fires per server
		List<Map<String, ?>> interfaceList = this.getServers();
		if (interfaceList.size() > 0) {
			for (Map<String, ?> interfaceMap : interfaceList) {

				NetworkMonitorTask networkMonitorTask = new NetworkMonitorTask(getContextConfiguration(), tasksExecutionServiceProvider.getMetricWriteHelper(), interfaceMap.get(0).toString());
				tasksExecutionServiceProvider.submit("NetworkTask", networkMonitorTask);
			}
		}
	}

	@Override
	protected List<Map<String, ?>> getServers() {
		//List<Map<String, ?>> networkInterfaces = (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get("networkInterfaces");
		//List<Map<String, ?>> anInterface = new ArrayList<>();
		//check to see if we have any before assuming with below, it throws index out of bounds before the assert check
		/*if(networkInterfaces == null || networkInterfaces.size() <= 0){
			LOGGER.error("The 'networkInterfaces' section in config.yml is not configured");
			return anInterface;
		}*/
		/*TODO the extension is running 2 threads obviously the idea was to pull an interface for a thread,
		* but both threads are building metrics for all interfaces
		*14:58:58,032 DEBUG [Monitor-Task-Thread2] WorkbenchMetricStore-Network Monitor - Adding the metric [Custom Metrics|Network|en0|TX Packets] with value [39402420]
		*14:58:58,032 DEBUG [Monitor-Task-Thread2] WorkbenchMetricStore-Network Monitor - Adding the metric [Custom Metrics|Network|en1|RX KB] with value [0]
		*15:05:28,021 DEBUG [Monitor-Task-Thread1] WorkbenchMetricStore-Network Monitor - Adding the metric [Custom Metrics|Network|en0|TX Packets] with value [39411983]
		*15:05:28,021 DEBUG [Monitor-Task-Thread1] WorkbenchMetricStore-Network Monitor - Adding the metric [Custom Metrics|Network|en1|RX KB] with value [0]
		 */
		//so we are just going to return the network interface list here, let the doRun manage properly the instantiation
		//of tasks per server
		return (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get("networkInterfaces");
		//anInterface.add(networkInterfaces.get(0));
		//the below assert checks null, this Utility has no ability to assume length of a list should
		//be greater than 0 and instantiation of an arraylist that is empty is not a null object
		//AssertUtils.assertNotNull(anInterface, "The 'networkInterfaces' section in config.yml is not configured");
		//return anInterface;
	}

	@Override
	protected void initializeMoreStuff(Map<String, String> args) {
		LOGGER.info("initializing metric.xml file");
		this.getContextConfiguration().setMetricXml(args.get("metric-file"), Stat.Stats.class);
	}


}
