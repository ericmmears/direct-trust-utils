package org.dtrust.dao.interoptest.dao;

public class TestConflictException extends TestDAOException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5345384115958933728L;

	/**
	 * {@inheritDoc}
	 */
    public TestConflictException() 
    {
    }

	/**
	 * {@inheritDoc}
	 */
    public TestConflictException(String msg) 
    {
        super(msg);
    }

	/**
	 * {@inheritDoc}
	 */
    public TestConflictException(String msg, Throwable t) 
    {
        super(msg, t);
    }

	/**
	 * {@inheritDoc}
	 */
    public TestConflictException(Throwable t) 
    {
        super(t);
    }
}
