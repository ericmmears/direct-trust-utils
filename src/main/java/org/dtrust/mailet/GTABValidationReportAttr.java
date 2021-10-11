package org.dtrust.mailet;

public class GTABValidationReportAttr extends ValidationReportAttr
{
	public EncryReport encrReport;
	public DigSigReport digSigReport;
	public WrappedReport wrappedReport;
	
	public GTABValidationReportAttr()
	{
		super();
		encrReport = null;
		digSigReport = null;
	}
	
	public static class EncryReport
	{
		
		public boolean encrValid;
		public String encouteredOID;
		public String comment;
		
		public EncryReport()
		{
			encrValid = false;
			encouteredOID = "";
			comment = "";
		}
	}
	
	public static class DigSigReport
	{
		
		public boolean digSigValid;
		public String encouteredOID;
		public String comment;
		
		public DigSigReport()
		{
			digSigValid = false;
			encouteredOID = "";
			comment = "";
		}
	}	
	
	public static class WrappedReport
	{
		
		public boolean isWrapped;
		
		public WrappedReport()
		{
			isWrapped = false;
		}
	}
	
	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder();
		
		builder.append("GTAB Message Send Validation Report:\r\n\r\n");
		
		builder.append("From: " + fromAddr + "\r\n");
		builder.append("Recipients:\r\n");
		for (String recip : toAddrs)
		{
			builder.append("\t" + recip + "\r\n");
		}
		builder.append("Message Id: " + messageId + "\r\n\r\n");
		
		builder.append("Encryption Validation:\r\n");
		if (encrReport != null)
		{
			builder.append("\tEnryption Validation Status: " + encrReport.encrValid + "\r\n");
			builder.append("\tEncryption Alorithm OID: " + encrReport.encouteredOID + "\r\n");
			builder.append("\tComments: " + encrReport.comment + "\r\n\r\n");
		}
		else
		{
			builder.append("\tN/A\r\n\r\n");
		}
		
		builder.append("Digital Signature Validation:\r\n");
		if (digSigReport != null)
		{
			builder.append("\tDigital Signature Validation Status: " + digSigReport.digSigValid + "\r\n");
			builder.append("\tDigest Alorithm OID: " + digSigReport.encouteredOID + "\r\n");
			builder.append("\tComments: " + digSigReport.comment + "\r\n\r\n");
		}
		else
		{
			builder.append("\tN/A\r\n\r\n");
		}
		
		
		builder.append("Message Wrapping Validation:\r\n");
		if (wrappedReport != null)
		{
			builder.append("\tMessage Wrapping Validation Status: " + wrappedReport.isWrapped + "\r\n\r\n");
		}
		else
		{
			builder.append("\tN/A\r\n\r\n");
		}
		
		builder.append("Final Validation Status: ");
		
		if (wrappedReport != null && wrappedReport.isWrapped && digSigReport != null &&
				digSigReport.digSigValid && encrReport != null && encrReport.encrValid)
			builder.append("SUCCESS");
		else
			builder.append("FAILED");
		
		return builder.toString();
	}
}
