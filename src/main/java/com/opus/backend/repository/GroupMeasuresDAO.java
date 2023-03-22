package com.opus.backend.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import com.opus.backend.model.DAOGroupMeasures;

@Repository
public interface GroupMeasuresDAO extends CrudRepository<DAOGroupMeasures, Integer> {
	@Query(value = "SELECT measures FROM measures_group u WHERE u.group_name = ?1",nativeQuery = true)
	String measures(String group_name);
}