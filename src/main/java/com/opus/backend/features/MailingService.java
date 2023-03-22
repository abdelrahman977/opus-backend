package com.opus.backend.features;


import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;


public class MailingService {
	public static void SendMessage(String key,String smtp_from ,String subject,String body,String mailTo) throws IOException {
		Email from = new Email(smtp_from);
	    Email to = new Email(mailTo);
	    Content content = new Content("text/plain", body);
	    Mail mail = new Mail(from, subject, to, content);
	    SendGrid sg = new SendGrid(key);
	    Request request = new Request();
	    try {
	      request.setMethod(Method.POST);
	      request.setEndpoint("mail/send");
	      request.setBody(mail.build());
	      Response response = sg.api(request);
	      System.out.println(response.getStatusCode());
	      System.out.println(response.getBody());
	      System.out.println(response.getHeaders());
	    } catch (IOException ex) {
	      throw ex;
	    }
	  }
}