package com.opus.backend.controller;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opus.backend.config.JwtTokenUtil;
import com.opus.backend.features.JwtUserDetailsService;
import com.opus.backend.features.MailingService;
import com.opus.backend.model.DAOUser;
import com.opus.backend.model.JwtRequest;
import com.opus.backend.model.JwtResponse;
import com.opus.backend.model.UserDTO;
import com.opus.backend.repository.UserDao;

@RestController
@CrossOrigin
public class JwtAuthenticationController {
	@Autowired
    private Environment env;
	
	@Autowired
	private UserDao userDao;
	
	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	
	@Autowired
	private JwtUserDetailsService userDetailsService;
	
	@Autowired
	private UserDetailsService jwtInMemoryUserDetailsService;
	
	@Autowired
	private PasswordEncoder bcryptEncoder;

	@RequestMapping(value = "/authenticate", method = RequestMethod.POST)
	public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest)
			throws Exception {
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    	if(authenticationRequest.getUsername().contains("@")) { // the enduser send his/her email instead of username
	    		DAOUser user = userDao.findByEmail(authenticationRequest.getUsername());
	    		authenticationRequest.setUsername(user.getUsername());
	    	}
			authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
			final UserDetails userDetails = jwtInMemoryUserDetailsService
					.loadUserByUsername(authenticationRequest.getUsername());
			DAOUser user = userDetailsService.getUser(userDetails.getUsername());
			if(!user.getUser_type().equals("SAAS")) { // SAAS type can login without choosing his or her type
				if(!user.getUser_type().equals(authenticationRequest.getUser_type())) {
					return new ResponseEntity<String>("{\"status\": \"Invalid credentials or wrong account type\"}", headers , HttpStatus.UNAUTHORIZED); 
				}
			}
			if(!user.getVerification_status().equals("Verified")) {
				if(user.getUser_type().equals("Free")) { // re-send verification email
					String token = jwtTokenUtil.generateToken(userDetails,60*60*36); // 3 days expiration
					String body = "Verification Link  : " + env.getProperty("base_url") + "verify?token=" + token;
					String mailTo = user.getEmail();
					String status_message = String.format("User is not verified, We'v send new verification link to %s", mailTo);
					MailingService.SendMessage(env.getProperty("sendgrid_api_key"),env.getProperty("smtp_admin_email"),"Opus - Account Verification", body, mailTo);
					return new ResponseEntity<String>(String.format("{\"status\": \"%s\"}",status_message), headers , HttpStatus.NOT_ACCEPTABLE); 

				} // partner should contact admin
				return new ResponseEntity<String>("{\"status\": \"User Is Not Verified, Please Contact info@opusanalytics.ai\"}", headers , HttpStatus.NOT_ACCEPTABLE); 
			}
			final String token = jwtTokenUtil.generateToken(userDetails,60*60); // expires in one hour	
			return ResponseEntity.ok(new JwtResponse(token));

	}
	@RequestMapping(value = "/register", method = RequestMethod.POST)
	public ResponseEntity<?> saveUser(@RequestBody UserDTO user) throws Exception {
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);    
		int registeration_status = userDetailsService.user_status(user);
		if(registeration_status == -2) { // email already exists 
			ResponseEntity.status(409);
			return new ResponseEntity<String>(String.format("{\"status\": \"Email already taken\"}",""), headers , HttpStatus.OK);		
		}
		if(registeration_status == -1) { // user already exists 
			ResponseEntity.status(409);
			return new ResponseEntity<String>(String.format("{\"status\": \"User already exists\"}",""), headers , HttpStatus.OK);		
		}
		UserDetails userDetails = jwtInMemoryUserDetailsService
				.loadUserByUsername(user.getUsername());
		String token = jwtTokenUtil.generateToken(userDetails,60*60*36); // 3 days expiration
		String body = "Verification Link  : " + env.getProperty("base_url") + "verify?token=" + token;
		String mailTo = "";
		String status_message = "";
		if(user.getUser_type().equals("Free")) {
			mailTo = user.getEmail();
			status_message = String.format("We'v sent a verification link to %s", mailTo);
		}
		else{
			mailTo = env.getProperty("smtp_admin_email");
			body += String.format("\n\n Full Name : %s "
								+ "\n\n Email : %s",user.getFull_name(),user.getEmail());
			status_message = String.format("Thanks for the registration. We will contact you as soon as possible", mailTo);
		}
		MailingService.SendMessage(env.getProperty("sendgrid_api_key"),env.getProperty("smtp_admin_email"),"Opus - Account Verification", body, mailTo);
		return new ResponseEntity<String>(String.format("{\"status\": \"%s\"}",status_message), headers , HttpStatus.OK);		

		//return ResponseEntity.ok(userDetailsService.save(user));
	}
	@RequestMapping(value = "/forget_password", method = RequestMethod.POST)
	public ResponseEntity<?> ForgetPassword(@RequestParam("username") String username ) throws Exception {
		DAOUser user = new DAOUser();
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
		if(username.contains("@")) {
		    user = userDao.findByEmail(username);
		}
		else {
		    user = userDao.findByUsername(username);
		}
		UserDetails userDetails = jwtInMemoryUserDetailsService
				.loadUserByUsername(user.getUsername());
		String token = jwtTokenUtil.generateToken(userDetails,60*60); // 1 days expiration
		String body = "Reset Link : " + env.getProperty("base_frontend_url") + "resetpassword/" + token;
		String mailTo = user.getEmail();
		String status_message = String.format("We'v sent reset password link to %s",mailTo);
		MailingService.SendMessage(env.getProperty("sendgrid_api_key"),env.getProperty("smtp_admin_email"),"Opus - Reset Password", body, mailTo);
		return new ResponseEntity<String>(String.format("{\"status\": \"%s\"}",status_message), headers , HttpStatus.OK);
	}
	@RequestMapping(value = "/reset_password", method = RequestMethod.POST)
	public ResponseEntity<?> ResetPassword(@RequestParam("token") String token			  											
										  ,@RequestParam("password") String password ) throws Exception {
		String username = jwtTokenUtil.getUsernameFromToken(token);
	    DAOUser user = userDao.findByUsername(username);
		userDetailsService.setPassword(user,password);
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
		return new ResponseEntity<String>(String.format("{\"status\": \"%s\"}","Password has been changed successfully"), headers , HttpStatus.OK);
	}
	private void authenticate(String username, String password) throws Exception {
		Objects.requireNonNull(username);
		Objects.requireNonNull(password);
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
		} catch (DisabledException e) {
			throw new Exception("USER_DISABLED", e);
		} catch (BadCredentialsException e) {
			throw new Exception("INVALID_CREDENTIALS", e);
		}
	}
	@RequestMapping(value="/verify", method = RequestMethod.GET)
	public ResponseEntity<?>  hello(@RequestParam("token") String token) throws IOException {
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
		String username = jwtTokenUtil.getUsernameFromToken(token);
		String verification_status = "Verified";		
	    DAOUser user = userDao.findByUsername(username);
		user.setVerification_status(verification_status);
		userDetailsService.updateUser(user);
		if(user.getUser_type().equals("Free")) {
			return new ResponseEntity<String>(String.format("{\"status\": \"%s is %s\"}",username,verification_status), headers , HttpStatus.OK);
		}
		else { // partner account
			String body = "Your account has been successfully verified";
			String mailTo = user.getEmail();
			MailingService.SendMessage(env.getProperty("sendgrid_api_key"),env.getProperty("smtp_admin_email"),"Opus - Account Verification", body, mailTo);
			return new ResponseEntity<String>(String.format("{\"status\": \"%s is %s, Notifed to %s\"}",username,verification_status,user.getEmail()), headers , HttpStatus.OK);
		}
	}
	@RequestMapping(value = "/subscribe", method = RequestMethod.POST)
	public ResponseEntity<?> Subscribe(@RequestParam("username") String username 
									   ,@RequestParam("subscription_expiration_date") String subscription_expiration_date  ) throws Exception {
		DAOUser user = new DAOUser();
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
		if(username.contains("@")) {
		    user = userDao.findByEmail(username);
		}
		else {
		    user = userDao.findByUsername(username);
		}
		Date date_to = new SimpleDateFormat("yyyy-MM-dd").parse(subscription_expiration_date);  ;
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");  
		Date date_from = new Date();  
		user.setSubscription_from_date(date_from);
		user.setSubscription_expiration_date(date_to);
		userDetailsService.updateUser(user);
		String body = String.format("You Subscribed from %s to %s ",formatter.format(user.getSubscription_from_date()),formatter.format(date_to));
		String mailTo = user.getEmail();
		String status_message = String.format("Subscription Details sent to %s",mailTo);
		MailingService.SendMessage(env.getProperty("sendgrid_api_key"),env.getProperty("smtp_admin_email"),"Opus - Subscription Confirmation", body, mailTo);
		return new ResponseEntity<String>(String.format("{\"status\": \"%s\"}",status_message), headers , HttpStatus.OK);
	}
	@RequestMapping(value = "/user_details", method = RequestMethod.GET)
	public ResponseEntity<?> UserDetails(@RequestParam("username") String username) throws Exception {
		DAOUser user = new DAOUser();
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");  
	    String subscription_from_date = "";
	    String subscription_expiration_date = "";
		if(username.contains("@")) {
		    user = userDao.findByEmail(username);
		}
		else {
		    user = userDao.findByUsername(username);
		}
		if(user.getSubscription_from_date() != null){
			subscription_from_date = formatter.format(user.getSubscription_from_date());
		}
		if(user.getSubscription_expiration_date() != null){
			subscription_expiration_date = formatter.format(user.getSubscription_expiration_date());
		}
		return new ResponseEntity<String>(String.format("{\"username\": \"%s\","
													   + "\"email\": \"%s\" ,"
													   + "\"company_name\": \"%s\","
													   + "\"subscription_from_date\": \"%s\" , "
													   + "\"subscription_expiration_date\": \"%s\"}",user.getUsername()
													   												,user.getEmail()
													   												,user.getCompany_name()
													   												,subscription_from_date
													   												,subscription_expiration_date), headers , HttpStatus.OK);
	}
	@RequestMapping(value = "/update_user_details", method = RequestMethod.POST)
	public ResponseEntity<?> update_user_details(
												@RequestParam("full_name") String full_name										  		
										  		,@RequestParam("password") String password
										  		,@RequestParam("email") String email
										  		,@RequestParam("company_name") String company_name) throws Exception {
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    DAOUser user = userDao.findByEmail(email);
	    if(password != null) {
	    	user.setPassword(bcryptEncoder.encode(password));
	    }
	    if(company_name != null) {
	    	user.setCompany_name(company_name);
	    }
	    if(full_name != null) {
	    	user.setFull_name(full_name);
	    }
	    user.setVerification_status("Verified");
	    user.setUser_type("SAAS");
		userDetailsService.updateUser(user);
		return new ResponseEntity<String>(String.format("{\"status\": \"%s\"}","User details are updated , please login"), headers , HttpStatus.OK);
	}
}
