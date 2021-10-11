package org.dtrust.dao.interoptest.entity;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "test")
public class Test
{
	private long testId;
	private String testName;
	private TestType type;
	private String correlationId;
	private TestStatus testStatus;
	private String comments;
	private Calendar startDtTm;
	private Calendar updateDtTm;
	private int updateCnt;
	
	public Test()
	{
		
	}

    @Id
    @Column(name = "testId", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
	public long getTestId()
	{
		return testId;
	}

	public void setTestId(long testId)
	{
		this.testId = testId;
	}

    @Column(name = "testName")
	public String getTestName()
	{
		return testName;
	}

	public void setTestName(String testName)
	{
		this.testName = testName;
	}

    @Column(name = "testType")
    @Enumerated
    public TestType getType()
	{
		return type;
	}

	public void setType(TestType type)
	{
		this.type = type;
	}

    @Column(name = "correlationId")
	public String getCorrelationId()
	{
		return correlationId;
	}

	public void setCorrelationId(String correlationId)
	{
		this.correlationId = correlationId;
	}

	@Column(name = "testStatus")
    @Enumerated
	public TestStatus getTestStatus()
	{
		return testStatus;
	}

	public void setTestStatus(TestStatus testStatus)
	{
		this.testStatus = testStatus;
	}

    @Column(name = "comments", length = 10000)
	public String getComments()
	{
		return comments;
	}

	public void setComments(String comments)
	{
		this.comments = comments;
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
