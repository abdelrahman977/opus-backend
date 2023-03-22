package com.opus.backend.controller;



import java.io.File;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.opus.backend.config.JwtTokenUtil;
import com.opus.backend.features.PowerExcelReader;
import com.opus.backend.repository.GroupMeasuresDAO;
import com.opus.backend.security.SSLignore;


@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class APIController {
	@Autowired
	private GroupMeasuresDAO groupMeasuresDAO;
	String filename;
	@Autowired
	private JwtTokenUtil jwtTokenUtil;
	@Autowired
    private Environment env;

	  @PostMapping("/upload") 
	  @CrossOrigin(origins = "*", allowedHeaders = "*")
	  public ResponseEntity<?> handleFileUpload(@RequestParam("primary_key") String primary_key
			  									,@RequestParam("uploader") String uploader
			  									,@RequestParam("measure") String measures 
			  									,@RequestParam("file") MultipartFile[] files 
			  									, HttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
		String  uploader_name = "";
		//String primary_key = "email";
		//String uploader = "Hossam";
		String uuid = UUID.randomUUID().toString().replace("-", "");
		String output_path = env.getProperty("output_path")+ uuid + "/";
		//String output_path = env.getProperty("output_path") + "/";
		try {
			uploader_name = jwtTokenUtil.getUsernameFromToken(uploader);
		}
		catch(Exception e) {
			uploader_name = "Guest";
		}
		new File(output_path).mkdirs();
		String fileName = uuid + '.' + FilenameUtils.getExtension(files[0].getOriginalFilename());
	    ResponseEntity<String> response;
	    String output = "{" 
	    		   + "  \"root\": [";
	    try {
	      for(int i = 0; i < files.length; i++) {	  	      
		      files[i].transferTo(new File(output_path + i + "_" + fileName));
	      }
	      if(groupMeasuresDAO.measures(measures) != null) {
	    	  measures = groupMeasuresDAO.measures(measures);
	      }
	      String measures_list []= measures.split(","); // code accepting more than one measure
	      /* excel file transformation */
	      PowerExcelReader reader = new PowerExcelReader(primary_key,"0" + "_" + fileName ,output_path,env.getProperty("output_path_cleansed") + fileName,env.getProperty("dictionary_folder") + "combined_dict.txt");
	      int numberofSheets = reader.getNumberOfSheetAvaliable();
		  reader.excelCleansingTable();
	      reader.close();
	      	 
	      // Call External API
	      for (String measure : measures_list) {	      
	      String uri = env.getProperty("apache_karaf_url")+ "hr_measures";
	      String body = String.format("{root:{filename: %s , measure: %s , numberOfSheets : %2d , uploader: %s}}",fileName , measure , numberofSheets , uploader_name);
	      RestTemplate template = SSLignore.getRestTemplate();
	      headers = new HttpHeaders();
	      headers.setContentType(MediaType.APPLICATION_JSON);
	      HttpEntity<String> requestEntity = 
	           new HttpEntity<>(body, headers);
	      response = template.exchange(uri, HttpMethod.POST, requestEntity, String.class);
	      output += response.getBody() + ',';
	      }
	    } catch (Exception e) {
	      return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	    headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    output= output.substring(0,output.length()-1); // removing extra ','
	    output += "]}"; // closing JSON object
	    return new ResponseEntity<String>(output, headers, HttpStatus.OK);
	   	    
	  }
	  @PostMapping("/powerBIADDToken") 
	  @CrossOrigin(origins = "*", allowedHeaders = "*")
	  public ResponseEntity<String> powerBIADDToken(	@RequestParam("tenantid") String tenantid			  											
			  											,@RequestParam("grant_type") String grant_type 
			  											,@RequestParam("client_id") String client_id
			  											,@RequestParam("client_secret") String client_secret
			  											,@RequestParam("resource") String resource 
			  											,HttpServletRequest request) {
	    String output = "";
	    ResponseEntity<String> response;
	    String uri;
	    RestTemplate template;
	    HttpHeaders headers;
	    // Call AADToken
	    try {     	 
	      uri = "https://login.windows.net/" + tenantid + "/oauth2/token";
	      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
	      body.add("grant_type",grant_type);
	      body.add("client_id", client_id);
	      body.add("client_secret",client_secret);
	      body.add("resource",resource);
	      template = SSLignore.getRestTemplate();
	      headers = new HttpHeaders();
	      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		  headers.add("Access-Control-Allow-Origin", "*");
	      HttpEntity<MultiValueMap<String, String>> requestEntity = 
	           new HttpEntity<>(body, headers);
	      response = template.exchange(uri, HttpMethod.POST, requestEntity, String.class);
	      output = response.getBody();
	    } catch (Exception e) {
	      return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	    headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    return new ResponseEntity<String>(output, headers, HttpStatus.OK);	   	    
	  	} 
	  @PostMapping("/powerBIAllReports") 
	  @CrossOrigin(origins = "*", allowedHeaders = "*")
	  public ResponseEntity<String> powerBIAllReports(	@RequestParam("workspace") String workspace
			  											,@RequestParam("token") String token 
			  											,HttpServletRequest request) {
	    String output = "";
	    ResponseEntity<String> response;
	    String uri;
	    RestTemplate template;
	    HttpHeaders headers;
	    // Call Get All Reports
	    try {     	 
		  uri = "https://api.powerbi.com/v1.0/myorg/groups/"+ workspace + "/reports";
	      template = SSLignore.getRestTemplate();
	      headers = new HttpHeaders();
	      headers.add("Authorization", "Bearer " + token);
	      HttpEntity<MultiValueMap<String, String>> requestEntity = 
	           new HttpEntity<>(headers);
	      response = template.exchange(uri, HttpMethod.GET, requestEntity, String.class);
	      output = response.getBody();
	    } catch (Exception e) {
	      return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	    headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    return new ResponseEntity<String>(output, headers, HttpStatus.OK);	   	    
	  	} 
		@PostMapping("/powerBISingleReport") 
		@CrossOrigin(origins = "*", allowedHeaders = "*")
		public ResponseEntity<String> powerBISingleReport(	@RequestParam("workspace") String workspace
															,@RequestParam("report") String report
															,@RequestParam("token") String token 
															, HttpServletRequest request) {
			String output = "";
			ResponseEntity<String> response;
			String uri;
			RestTemplate template;
			HttpHeaders headers;
			// Call Get All Reports
			try {     	 
			  uri = String.format("https://api.powerbi.com/v1.0/myorg/groups/%s/reports/%s",workspace,report);
			  template = SSLignore.getRestTemplate();
			  headers = new HttpHeaders();
			  headers.add("Authorization", "Bearer " + token);
			  headers.add("Access-Control-Allow-Origin", "*");
			      HttpEntity<MultiValueMap<String, String>> requestEntity = 
			           new HttpEntity<>(headers);
			      response = template.exchange(uri, HttpMethod.GET, requestEntity, String.class);
			      output = response.getBody();
			    } catch (Exception e) {
			      return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
			    }
			    headers = new HttpHeaders();
			    headers.setContentType(MediaType.APPLICATION_JSON);
			    return new ResponseEntity<String>(output, headers, HttpStatus.OK);	   	  
		}
		@PostMapping("/powerBIGenerateToken") 
		@CrossOrigin(origins = "*", allowedHeaders = "*")
		public ResponseEntity<String> powerBIGenerateToken(	@RequestParam("workspace") String workspace
															,@RequestParam("report") String report
															,@RequestParam("token") String token 
															,@RequestParam("username") String username 
															,@RequestParam("datasets") String datasets 
															, HttpServletRequest request) {
			String output = "";
			ResponseEntity<String> response;
			String uri;
			RestTemplate template;
			HttpHeaders headers;
			// Call Get All Reports
			try {     	 
			  uri = String.format("https://api.powerbi.com/v1.0/myorg/groups/%s/reports/%s/GenerateToken",workspace,report);
			  template = SSLignore.getRestTemplate();
			  headers = new HttpHeaders();
			  headers.setContentType(MediaType.APPLICATION_JSON);
			  headers.add("Authorization", "Bearer " + token);
			  headers.add("Access-Control-Allow-Origin", "*");
			      HttpEntity<String> requestEntity = 
			           new HttpEntity<>(String.format("{\r\n"
			           		+ "    \"accessLevel\":\"view\",\r\n"
			           		+ "    \"identities\":[\r\n"
			           		+ "    {\r\n"
			           		+ "    \"username\": \"%s\",\r\n"
			           		+ "    \"roles\": [\r\n"
			           		+ "        \"Customer\"\r\n"
			           		+ "        ],\r\n"
			           		+ "     \"datasets\":[\r\n"
			           		+ "         \"%s\"\r\n"
			           		+ "     ]\r\n"
			           		+ "}\r\n"
			           		+ "    ]\r\n"
			           		+ "}",username,datasets),headers);
			      response = template.exchange(uri, HttpMethod.POST, requestEntity, String.class);
			      output = response.getBody();
			    } catch (Exception e) {
			      return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
			    }
			    headers = new HttpHeaders();
			    headers.setContentType(MediaType.APPLICATION_JSON);
			    return new ResponseEntity<String>(output, headers, HttpStatus.OK);	   	  
		}
		
		
}
