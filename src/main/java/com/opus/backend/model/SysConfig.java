package com.opus.backend.model;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


import java.util.Date;
@Entity
@Table(name = "Sys_config")
public class SysConfig {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private String variable;
  private String value;
  private String set_by;
  
  
  @Temporal(TemporalType.TIMESTAMP)
  private Date set_time;
  
  // getters and setters
  
  public String getVariable() {
		return variable;
	}


	public void setVariable(String variable) {
		this.variable = variable;
	}


	public String getValue() {
		return value;
	}


	public void setValue(String value) {
		this.value = value;
	}


	public String getSet_by() {
		return set_by;
	}


	public void setSet_by(String set_by) {
		this.set_by = set_by;
	}


	public Date getSet_time() {
		return set_time;
	}


	public void setSet_time(Date set_time) {
		this.set_time = set_time;
	}
}
