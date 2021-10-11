package org.dtrust.dao.interoptest.entity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;

@Entity
@Table(name = "testsuite")
public class TestSuite
{
	private long testSuiteid = 0L;
	private String testSuiteName;
	private TestStatus testStatus;
	private Calendar startDtTm;
	private Calendar updateDtTm;
	private int updateCnt;
	private Set<Test> tests;
	private int testTimout;
	private String targetAddress;
	
	public TestSuite()
	{
		
	}

    @Id
    @Column(name = "testSuiteid", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
	public long getTestSuiteid()
	{
		return testSuiteid;
	}

	public void setTestSuiteid(long testSuiteid)
	{
		this.testSuiteid = testSuiteid;
	}

    @Column(name = "testSuiteName")
	public String getTestSuiteName()
	{
		return testSuiteName;
	}

	public void setTestSuiteName(String testSuiteName)
	{
		this.testSuiteName = testSuiteName;
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

	@OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "tests", joinColumns = @JoinColumn(name = "testSuiteId"), inverseJoinColumns = @JoinColumn(name = "testId"))
	public Set<Test> getTests()
	{
		if (tests == null)
			tests = new HashSet<Test>();
		
		return tests;
	}

	public void setTests(Set<Test> tests)
	{
		this.tests = tests;
	}

	public int getTestTimout()
	{
		return testTimout;
	}

	public void setTestTimout(int testTimout)
	{
		this.testTimout = testTimout;
	}

	public String getTargetAddress()
	{
		return targetAddress;
	}

	public void setTargetAddress(String targetAddress)
	{
		this.targetAddress = targetAddress;
	}
	
	@Override
	public String toString()
	{
		
		final SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
		final StringBuilder builder = new StringBuilder();
		
		builder.append(this.getTestSuiteName() + " Validation Report\r\n\r\n");
		
		
		builder.append("Test report id: " + this.getTestSuiteid() + "\r\n");
		
		if (this.getStartDtTm() != null)
			builder.append("Test start date time: " + sdf.format(this.getStartDtTm().getTime()) + "\r\n");
		
		if (this.getTestStatus() != null)
			builder.append("Test status: " + this.getTestStatus() + "\r\n");
		
		builder.append("Test timeout in minutes: " + this.getTestTimout() + "\r\n\r\n");
		
		if (!StringUtils.isEmpty(this.getTargetAddress()))	
		{
			builder.append("Direct Address Recipients:\r\n");

			builder.append("\t" + this.getTargetAddress() + "\r\n");
		}
		
		builder.append("\r\nNumber of tests executed: " + this.getTests().size() + "\r\n\r\n");
		
		if (this.getTests().size() > 0)
		{
			builder.append("Individual test results: " + "\r\n");
			
			int idx = 1;
			int numTests = this.getTests().size();
			for (Test test : this.getTests())
			{
				builder.append("\r\nTest " + idx++ + " of " + numTests + "\r\n");
				builder.append("\tTest name: " + test.getTestName() + "\r\n");
				builder.append("\tTest status: " + test.getTestStatus() + "\r\n");
				builder.append("\tTest comments: " + test.getComments() + "\r\n");
			}
		}
		
		return builder.toString();
	}
}
