package com.opus.backend.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.opus.backend.model.DAOUser;

@Repository
public interface UserDao extends CrudRepository<DAOUser, Integer> {
	boolean existsByUsername(String username);
	boolean existsByEmail(String email);
	//@Query(value = "SELECT * FROM users u WHERE u.status = ?1",nativeQuery = true)
	DAOUser findByEmail(String email);
	DAOUser findByUsername(String username);
}