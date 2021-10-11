package org.dtrust.resources.utils;

import java.util.Collection;

import javax.mail.internet.InternetAddress;

import org.dtrust.dao.interoptest.dao.TestSuiteDAO;
import org.dtrust.dao.interoptest.entity.TestSuite;
import org.dtrust.util.MessageRetrieverFactory;
import org.dtrust.util.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class DAOAndPop3InteropTestMonitorFactory implements InteropTestMonitorFactory
{
	@Autowired
	protected TestSuiteDAO dao;
	
	@Autowired
	protected MessageRetrieverFactory retrieverFactory;
	
	@Autowired
	@Qualifier("msgSender")
	protected MessageSender msgSender;
	
	@Autowired
	@Qualifier("localSender")
	protected InternetAddress localSender;
	
	public DAOAndPop3InteropTestMonitorFactory()
	{
	}
	
	public void setDao(TestSuiteDAO dao)
	{
		this.dao = dao;
	}

	@Override
	public Runnable createTestMonitor(TestSuite suite, Collection<InternetAddress> reportAddresses)
	{
		return new DAOAndPop3InteropTestMonitorImpl(suite, dao, reportAddresses, retrieverFactory.createRetriver(), msgSender, localSender);
	}
}
