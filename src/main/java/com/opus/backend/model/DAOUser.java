package com.opus.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

import javax.persistence.*;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "users")
public class DAOUser {

	@Id
	private String username;
	@Column
	@JsonIgnore
	private String password;
	@Column
	private String email;
	@Column
	private String full_name;
	@Column
	private String company_name;
	@Column
	private String user_type;
	@Column
	private String verification_status;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Column
	private Date subscription_from_date;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Column
	private Date subscription_expiration_date;

	public String getVerification_status() {
		return verification_status;
	}

	public void setVerification_status(String verification_status) {
		this.verification_status = verification_status;
	}

	public String getUser_type() {
		return user_type;
	}

	public Date getSubscription_from_date() {
		return subscription_from_date;
	}

	public void setSubscription_from_date(Date subscription_from_date) {
		this.subscription_from_date = subscription_from_date;
	}

	public Date getSubscription_expiration_date() {
		return subscription_expiration_date;
	}

	public void setSubscription_expiration_date(Date subscription_expiration_date) {
		this.subscription_expiration_date = subscription_expiration_date;
	}

	public void setUser_type(String user_type) {
		this.user_type = user_type;
	}

	public String getCompany_name() {
		return company_name;
	}

	public void setCompany_name(String company_name) {
		this.company_name = company_name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFull_name() {
		return full_name;
	}

	public void setFull_name(String full_name) {
		this.full_name = full_name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
