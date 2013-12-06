Boundary Monitoring Extension
============================

This extension works only with the standalone machine agent.

## Use Case

Boundary is a consolidated operations management platform built for today’s IT environments. Delivered as a SaaS offering, Boundary enables customers to monitor their entire IT environment from a single point of control and is uniquely designed to deal with challenges of modern, highly distributed applications. The boundary-monitoring-extension gathers aggregate meter metrics exposed through Boundary's Historical Data REST API and sends them to the AppDynamics Metric Browser.

## Installation
<ol>
	<li>Type 'ant package' in the command line from the boundary-monitoring-extension directory.
	</li>
	<li>Deploy the file BoundaryMonitor.zip found in the 'dist' directory into the &lt;machineagent install dir&gt;/monitors/ directory.
	</li>
	<li>Unzip the deployed file.
	</li>
	<li>Open &lt;machineagent install dir&gt;/monitors/BoundaryMonitor/monitor.xml and configure the Boundary parameters.
<p></p>
<pre>
	&lt;argument name="api-key" is-required="true" default-value="" /&gt;          
	&lt;argument name="org-id" is-required="true" default-value="" /&gt;
</pre>
	</li>	
	<li> Restart the machine agent.
	</li>
	<li>In the AppDynamics Metric Browser, look for: Application Infrastructure Performance | &lt;Tier&gt; | Custom Metrics | Boundary
	</li>
</ol>

## Directory Structure

| Directory/File | Description |
|----------------|-------------|
|conf            | Contains the monitor.xml |
|lib             | Contains third-party project references |
|src             | Contains source code of the Boundary monitoring extension |
|dist            | Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file |
|build.xml       | Ant build script to package the project (required only if changing Java code) |

## Metrics

|Metric Name           | Description     |
|----------------------|-----------------|
|ingressPackets    	   | Number packets entering the server |
|ingressOctets             | Number of octets entering the server |
|egressPackets         | Number of packets leaving the server |
|egressOctets         | Number of octets leaving the server |
|appRTTUsec          | Application round trip time in microseconds |
|handshakeRTTUsec       | Handshake round trip time in microseconds |
|retransmits                | Number of packets retransmitted|
|outOfOrder            | Number of out of order packets |
|activeFlows                  | N/A |

## Custom Dashboard

![](https://raw.github.com/Appdynamics/boundary-monitoring-extension/master/Boundary%20Dashboard.png?token=2880440__eyJzY29wZSI6IlJhd0Jsb2I6QXBwZHluYW1pY3MvYm91bmRhcnktbW9uaXRvcmluZy1leHRlbnNpb24vbWFzdGVyL0JvdW5kYXJ5IERhc2hib2FyZC5wbmciLCJleHBpcmVzIjoxMzg2ODAyNTAyfQ%3D%3D--dfa89e2e3461bbd8ef09d5db317bb02c430da0b9)

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/boundary-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/eXchange/Boundary-Monitoring-Extension/idi-p/4851) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).
