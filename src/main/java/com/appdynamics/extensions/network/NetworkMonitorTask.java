package com.appdynamics.extensions.network;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.network.config.ScriptFile;
import com.appdynamics.extensions.network.input.Stat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.util.*;

import static com.appdynamics.extensions.network.NetworkConstants.DEFAULT_SCRIPT_TIMEOUT_IN_SEC;

public class NetworkMonitorTask implements AMonitorTaskRunnable {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(NetworkMonitorTask.class);
    ObjectMapper objectMapper = new ObjectMapper();
    private BigInteger heartBeatValue = BigInteger.ZERO;

    private MonitorContextConfiguration monitorContextConfiguration;
    private MetricWriteHelper metricWriteHelper;
    private String interfaceName;
    public NetworkMonitorTask(MonitorContextConfiguration contextConfiguration, MetricWriteHelper metricWriteHelper, String interfaceName) {
        this.monitorContextConfiguration = contextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.interfaceName = interfaceName;
    }

    @Override
    public void onTaskComplete() {
        LOGGER.info("Network Monitoring Extension Task completed");
    }

    @Override
    public void run() {
        Map<String, ?> config = monitorContextConfiguration.getConfigYml();
        collectAndPrintMetrics(config);
    }

    private void collectAndPrintMetrics(Map<String, ?> config) {
        List<Metric> metricsList = Lists.newArrayList();
        String metricPrefix = monitorContextConfiguration.getMetricPrefix();
        try {
            //Splitting out script and sigar metrics to their own
            //tasks because the task threading executes a task/thread
            //per interface and as such only network interface centric
            //metrics should run here but scripts can override any
            //metric
            ScriptMetrics scriptMetrics = getScriptMetrics(config);
            //this should not be pulling all servers, we should be handling one
            //interface below decided on by the task executioner up stream
            //Set<String> networkInterfaces = new HashSet<>((ArrayList<String>)config.get("networkInterfaces"));
            Set<String> networkInterfaces = new HashSet<>();
            //Easiest is to set the set to the single interface for the task/thread
            networkInterfaces.add(this.interfaceName);
            SigarMetrics sigarMetrics = new SigarMetrics(networkInterfaces);

            Stat.Stats statsFromMetricsXml = (Stat.Stats) monitorContextConfiguration.getMetricsXml();

            NetworkMetricsCollector metricsCollector = new NetworkMetricsCollector
                    (sigarMetrics, scriptMetrics, networkInterfaces, statsFromMetricsXml, metricPrefix);

            metricsList.addAll(metricsCollector.collectMetrics("interfaces"));
            heartBeatValue = BigInteger.ONE;
        } catch (Exception e) {
            LOGGER.error("Error while collecting metrics for Network Monitor", e);
        } finally {
            Metric heartBeat = new Metric("HeartBeat", String.valueOf(heartBeatValue), metricPrefix + "|" + "HeartBeat");
            metricsList.add(heartBeat);
            metricWriteHelper.transformAndPrintMetrics(metricsList);
        }
    }

    private ScriptMetrics getScriptMetrics(Map<String, ?> config) {
        ScriptMetrics scriptMetrics = null;

        if ((Boolean) config.get("overrideMetricsUsingScriptFile")) {
            LOGGER.debug("Override metrics using script is enabled, attempting to retrieve metrics from script...");

            try {
                ScriptMetricsExecutor scriptMetricsExecutor = new ScriptMetricsExecutor();
                List<ScriptFile> ScriptFiles = Arrays.asList(objectMapper.convertValue(config.get("scriptFiles"), ScriptFile[].class));
                scriptMetrics = scriptMetricsExecutor.executeAndCollectScriptMetrics(
                        ScriptFiles, getScriptTimeout(config));

            } catch (Exception ex) {
                LOGGER.error("Unfortunately an error has occurred while fetching metrics from script", ex);
            }
        }

        return scriptMetrics != null ? scriptMetrics : new ScriptMetrics();
    }

    private long getScriptTimeout(Map<String, ?> config) {
        int scriptTimeout = (Integer) config.get("scriptTimeoutInSec");
        return scriptTimeout > 0 ? scriptTimeout : DEFAULT_SCRIPT_TIMEOUT_IN_SEC;
    }
}
