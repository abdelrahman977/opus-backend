package com.opus.backend.features;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.opus.backend.model.DAOUser;
import com.opus.backend.model.UserDTO;
import com.opus.backend.repository.UserDao;

@Service
public class JwtUserDetailsService implements UserDetailsService {

	@Autowired
	private UserDao userDao;

	@Autowired
	private PasswordEncoder bcryptEncoder;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		DAOUser user = userDao.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("User not found with username: " + username);
		}
		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				new ArrayList<>());
	}
	public int user_status(UserDTO user) {
		if(userDao.existsByEmail(user.getEmail())) {
			return -2;
		}
		if(userDao.existsByUsername(user.getUsername())) {
			//user_details = userDao.findByUsername(user.getUsername());
			//if(user_details.getVerification_status().equals("Pending")) {
				//return 0;
			//}
			return -1;
		}
		this.save(user);
		return 1;
	}
	public DAOUser save(UserDTO user) {
		DAOUser newUser = new DAOUser();
		newUser.setUsername(user.getUsername());
		newUser.setPassword(bcryptEncoder.encode(user.getPassword()));
		newUser.setEmail(user.getEmail());
		newUser.setFull_name(user.getFull_name());
		newUser.setCompany_name(user.getCompany_name());
		newUser.setUser_type(user.getUser_type());
		newUser.setVerification_status("Pending");
		
		return userDao.save(newUser);
		
	}
	
	public DAOUser updateUser(DAOUser user) {	
		return userDao.save(user);
	}
	public DAOUser getUser(String username) {	
		return userDao.findByUsername(username);
	}
	public void setPassword(DAOUser user,String password) {	
		user.setPassword(bcryptEncoder.encode(password));
		userDao.save(user);
		return;
	}
}