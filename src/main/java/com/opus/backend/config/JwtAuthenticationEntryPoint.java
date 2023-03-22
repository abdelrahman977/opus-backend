package com.opus.backend.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint, Serializable {

	private static final long serialVersionUID = -7858869558953243875L;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(401);
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.print(String.format("{\"status\": \"%s\"}","Invalid credentials or wrong account type"));
		out.flush();
		//response.sendError(HttpServletResponse.SC_UNAUTHORIZED, String.format("{\"status\": \"%s\"}","INVALID CREDENTIALS"));
	}
}
