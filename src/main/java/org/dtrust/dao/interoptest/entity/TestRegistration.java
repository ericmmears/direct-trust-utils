package org.dtrust.dao.interoptest.entity;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "testregistration")
public class TestRegistration
{
	private long registrationId;
	private String userName;
	private String reportAddress;
	private String reportAddressAllCaps;
	private String sourceDirectAddress;
	private String sourceDirectAddressAllCaps;
	private Calendar startDtTm;
	private Calendar updateDtTm;
	private int updateCnt;
	
	public TestRegistration()
	{
		
	}

    @Id
    @Column(name = "registrationId", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
	public long getRegistrationId()
	{
		return registrationId;
	}

	public void setRegistrationId(long registrationId)
	{
		this.registrationId = registrationId;
	}

    @Column(name = "userName")
	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

    @Column(name = "reportAddress")
	public String getReportAddress()
	{
		return reportAddress;
	}

	public void setReportAddress(String reportAddress)
	{
		this.reportAddress = reportAddress;
	}

    @Column(name = "reportAddressAllCaps")
	public String getReportAddressAllCaps()
	{
		return reportAddressAllCaps;
	}

	public void setReportAddressAllCaps(String reportAddressAllCaps)
	{
		this.reportAddressAllCaps = reportAddressAllCaps;
	}

    @Column(name = "sourceDirectAddress")
	public String getSourceDirectAddress()
	{
		return sourceDirectAddress;
	}

	public void setSourceDirectAddress(String sourceDirectAddress)
	{
		this.sourceDirectAddress = sourceDirectAddress;
	}

    @Column(name = "sourceDirectAddressAllCaps")
	public String getSourceDirectAddressAllCaps()
	{
		return sourceDirectAddressAllCaps;
	}

	public void setSourceDirectAddressAllCaps(String sourceDirectAddressAllCaps)
	{
		this.sourceDirectAddressAllCaps = sourceDirectAddressAllCaps;
	}
	
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "startDtTm")
	public Calendar getStartDtTm()
	{
		return startDtTm;
	}

	public void setStartDtTm(Calendar startDtTm)
	{
		this.startDtTm = startDtTm;
	}

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updateDtTm")
	public Calendar getUpdateDtTm()
	{
		return updateDtTm;
	}

	public void setUpdateDtTm(Calendar updateDtTm)
	{
		this.updateDtTm = updateDtTm;
	}

	
    @Column(name = "updateCnt")
	public int getUpdateCnt()
	{
		return updateCnt;
	}

	public void setUpdateCnt(int updateCnt)
	{
		this.updateCnt = updateCnt;
	}
}
