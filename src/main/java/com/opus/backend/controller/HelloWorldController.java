package com.opus.backend.controller;

import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opus.backend.config.JwtTokenUtil;
import com.opus.backend.features.MailingService;
import com.opus.backend.model.DAOGroupMeasures;
import com.opus.backend.repository.GroupMeasuresDAO;
import com.opus.backend.repository.UserDao;



@RestController
@CrossOrigin()
public class HelloWorldController {
	@Autowired
	private GroupMeasuresDAO groupMeasuresDAO;
	@Autowired
    private Environment env;
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	@RequestMapping({ "/hello" })
	public String hello() {
		
		//jwtTokenUtil.getUsernameFromToken("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJlenphcjIiLCJleHAiOjE2NzU4ODI3NjEsImlhdCI6MTY3NTg2NDc2MX0.8eBEzNN6X-DQ_u0v7TD7x4yMx4LDEd7jqzbbqCK2wJXp6xBsNlEGoAEiYms1u8XYCafFAKDjf6aZidOUTGfR6Q")
		return "Authentication Is working";

	}
	@RequestMapping({ "/hello1" })
	public String hello1() throws IOException {
		//MailingService.SendMessage(env.getProperty("sendgrid_api_key"),env.getProperty("sendgrid_api_key"),"Opus - Account Verification", "Test", "abdelrahman97@live.com");
		//jwtTokenUtil.getUsernameFromToken("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJlenphcjIiLCJleHAiOjE2NzU4ODI3NjEsImlhdCI6MTY3NTg2NDc2MX0.8eBEzNN6X-DQ_u0v7TD7x4yMx4LDEd7jqzbbqCK2wJXp6xBsNlEGoAEiYms1u8XYCafFAKDjf6aZidOUTGfR6Q")
		String x = groupMeasuresDAO.measures("Engagemssent & Retention");
		return x;

	}

}
