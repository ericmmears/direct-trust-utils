package org.dtrust.mailet;

import java.util.ArrayList;
import java.util.Collection;

public abstract class ValidationReportAttr
{
	public Collection<String> toAddrs;
	public String fromAddr;
	public String messageId;
	
	public ValidationReportAttr()
	{
		toAddrs = new ArrayList<String>();
		fromAddr = "";
		messageId = "";
	}
}
