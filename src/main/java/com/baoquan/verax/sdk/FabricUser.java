package com.baoquan.verax.sdk;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

import java.io.Serializable;
import java.util.Set;

public class FabricUser implements User, Serializable {
    
	private static final long serialVersionUID = 8077132186383604355L;

    private String name;
    private Set<String> roles;
    private String account;
    private String affiliation;
    private String organization;
    private Enrollment enrollment = null; //need access in test env.
    private String mspId;
    
    public FabricUser() {}
    
    public FabricUser(String name) {
        this.name = name;
    }
    
    public FabricUser(String name, String org) {
        this.name = name;
        this.organization = org;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Set<String> getRoles() {
        return this.roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    @Override
    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    @Override
    public String getAffiliation() {
        return this.affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    @Override
    public Enrollment getEnrollment() {
        return this.enrollment;
    }


    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }


    @Override
    public String getMspId() {
        return mspId;
    }

    public void setMspId(String mspID) {
        this.mspId = mspID;
    }

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public void setName(String name) {
		this.name = name;
	}

}
