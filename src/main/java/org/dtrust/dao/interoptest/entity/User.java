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
@Table(name = "interopuser")
public class User
{
	private long userId;
	private String firstName;
	private String lastName;
	private String addressCity;	
	private String addressState;		
	private String username;
	private String usernameAllCaps;	
	private String hashedPass;
	private String contactEmail;
	private String contactEmailAllCaps;	
	private String contactPhone;	
	private String organization;
	private UserRole role;
	private UserAccountStatus accountStatus;
	private Calendar startDtTm;
	private Calendar updateDtTm;
	private int updateCnt;	
	private String updateChallenge;
	private Calendar updateChallengeIssuedDtTm;
	
	public User()
	{
		
	}

    @Id
    @Column(name = "userId", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
	public long getUserId()
	{
		return userId;
	}

	public void setUserId(long userId)
	{
		this.userId = userId;
	}

    @Column(name = "firstName")
	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

    @Column(name = "lastName")
	public String getLastName()
	{
		return lastName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

    @Column(name = "addressCity")
	public String getAddressCity()
	{
		return addressCity;
	}

	public void setAddressCity(String addressCity)
	{
		this.addressCity = addressCity;
	}

    @Column(name = "addressState")
	public String getAddressState()
	{
		return addressState;
	}

	public void setAddressState(String addressState)
	{
		this.addressState = addressState;
	}

    @Column(name = "username")
	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

    @Column(name = "usernameAllCaps")
	public String getUsernameAllCaps()
	{
		return usernameAllCaps;
	}

	public void setUsernameAllCaps(String usernameAllCaps)
	{
		this.usernameAllCaps = usernameAllCaps;
	}
	
    @Column(name = "hashedPass")
	public String getHashedPass()
	{
		return hashedPass;
	}

	public void setHashedPass(String hashedPass)
	{
		this.hashedPass = hashedPass;
	}

    @Column(name = "contactEmail")
	public String getContactEmail()
	{
		return contactEmail;
	}

	public void setContactEmail(String contactEmail)
	{
		this.contactEmail = contactEmail;
	}

    @Column(name = "contactEmailAllCaps")
	public String getContactEmailAllCaps()
	{
		return contactEmailAllCaps;
	}

	public void setContactEmailAllCaps(String contactEmailAllCaps)
	{
		this.contactEmailAllCaps = contactEmailAllCaps;
	}
	
    @Column(name = "contactPhone")
	public String getContactPhone()
	{
		return contactPhone;
	}

	public void setContactPhone(String contactPhone)
	{
		this.contactPhone = contactPhone;
	}

	
    @Column(name = "organization")	
    public String getOrganization()
	{
		return organization;
	}

	public void setOrganization(String organization)
	{
		this.organization = organization;
	}

	@Column(name = "role")
    @Enumerated
	public UserRole getRole()
	{
		return role;
	}

	public void setRole(UserRole role)
	{
		this.role = role;
	}

    @Column(name = "accountStatus")
    @Enumerated
	public UserAccountStatus getAccountStatus()
	{
		return accountStatus;
	}

	public void setAccountStatus(UserAccountStatus accountStatus)
	{
		this.accountStatus = accountStatus;
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

    @Column(name = "updateChallenge")
	public String getUpdateChallenge()
	{
		return updateChallenge;
	}

	public void setUpdateChallenge(String updateChallenge)
	{
		this.updateChallenge = updateChallenge;
	}

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updateChallengeIssuedDtTm")
	public Calendar getUpdateChallengeIssuedDtTm()
	{
		return updateChallengeIssuedDtTm;
	}

	public void setUpdateChallengeIssuedDtTm(Calendar updateChallengeIssuedDtTm)
	{
		this.updateChallengeIssuedDtTm = updateChallengeIssuedDtTm;
	}
	
	
	
}
