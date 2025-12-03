package com.backend.blog.security.oauth;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.backend.blog.entities.User;
import com.backend.blog.payloads.JwtAuthResponse;
import com.backend.blog.payloads.UserDto;
import com.backend.blog.repositories.UserRepo;
import com.backend.blog.security.JwtTokenHelper;
import com.backend.blog.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	@Autowired
	private JwtTokenHelper jwtTokenHelper;

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private ModelMapper modelMapper;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		if (!(authentication.getPrincipal() instanceof UserPrincipal)) {
			super.onAuthenticationSuccess(request, response, authentication);
			return;
		}

		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		User user = userRepo.findByEmail(principal.getUsername()).orElse(principal.getUser());

		String token = jwtTokenHelper.generateToken(principal);
		UserDto userDto = modelMapper.map(user, UserDto.class);

		JwtAuthResponse authResponse = new JwtAuthResponse();
		authResponse.setToken(token);
		authResponse.setUser(userDto);

		response.setStatus(HttpStatus.OK.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), authResponse);
		clearAuthenticationAttributes(request);
	}
}

