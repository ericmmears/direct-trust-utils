package org.dtrust.resources.utils;

import java.util.Collection;

import javax.mail.internet.InternetAddress;

import org.dtrust.dao.interoptest.entity.TestSuite;

public interface InteropTestMonitorFactory 
{
	public Runnable createTestMonitor(TestSuite suite, Collection<InternetAddress> reportAddresses);
}
