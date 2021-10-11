package org.dtrust.dao.interoptest.dao;

public class TestDAOException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2159725633905776467L;

	/**
	 * {@inheritDoc}
	 */
    public TestDAOException() 
    {
    }

	/**
	 * {@inheritDoc}
	 */
    public TestDAOException(String msg) 
    {
        super(msg);
    }

	/**
	 * {@inheritDoc}
	 */
    public TestDAOException(String msg, Throwable t) 
    {
        super(msg, t);
    }

	/**
	 * {@inheritDoc}
	 */
    public TestDAOException(Throwable t) 
    {
        super(t);
    }
}
