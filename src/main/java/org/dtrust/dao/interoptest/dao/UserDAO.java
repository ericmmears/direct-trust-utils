package org.dtrust.dao.interoptest.dao;

import java.util.Collection;

import org.dtrust.dao.interoptest.entity.User;

public interface UserDAO
{
	public Collection<User> getUsers() throws TestDAOException;
	
	public User getUser(long id) throws TestDAOException;
	
	public User getUserByUsername(String username) throws TestDAOException;
	
	public User getUserByContactEmailAddress(String contactAddress) throws TestDAOException;
	
	public User addUser(User user) throws TestDAOException;
	
	public void deleteUser(long id) throws TestDAOException;
	
	public User updateUser(User user) throws TestDAOException;
	
	public void cleanUsers() throws TestDAOException;
}
