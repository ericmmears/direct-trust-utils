package org.dtrust.dao.interoptest.dao;

public class TestEntityNotFoundException extends TestDAOException
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 3363991543363314430L;

	public TestEntityNotFoundException() 
    {
    }

	/**
	 * {@inheritDoc}
	 */
    public TestEntityNotFoundException(String msg) 
    {
        super(msg);
    }

	/**
	 * {@inheritDoc}
	 */
    public TestEntityNotFoundException(String msg, Throwable t) 
    {
        super(msg, t);
    }

	/**
	 * {@inheritDoc}
	 */
    public TestEntityNotFoundException(Throwable t) 
    {
        super(t);
    }
}
