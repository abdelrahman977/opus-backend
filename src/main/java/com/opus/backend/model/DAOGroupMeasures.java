package com.opus.backend.model;


import javax.persistence.*;

@Entity
@Table(name = "measures_group")
public class DAOGroupMeasures {

	@Id
	private String group_name;
	@Column
	private String measures;
	public String getGroup_name() {
		return group_name;
	}

	public void setGroup_name(String group_name) {
		this.group_name = group_name;
	}

	public String getMeasures() {
		return measures;
	}

	public void setMeasures(String measures) {
		this.measures = measures;
	}

	

}
