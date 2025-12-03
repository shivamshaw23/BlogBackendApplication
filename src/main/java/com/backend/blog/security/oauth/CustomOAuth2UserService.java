package com.backend.blog.security.oauth;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import com.backend.blog.config.AppConstants;
import com.backend.blog.entities.AuthProvider;
import com.backend.blog.entities.Role;
import com.backend.blog.entities.User;
import com.backend.blog.repositories.RoleRepo;
import com.backend.blog.repositories.UserRepo;
import com.backend.blog.security.UserPrincipal;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomOAuth2UserService.class);

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private RoleRepo roleRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RestTemplateBuilder restTemplateBuilder;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);
		return processOAuth2User(userRequest, oAuth2User);
	}

	private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		Map<String, Object> attributes = oAuth2User.getAttributes();

		String email = extractEmail(userRequest, attributes);
		if (!StringUtils.hasText(email)) {
			throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
		}

		User user = userRepo.findByEmail(email).map(existing -> updateExistingUser(existing, registrationId, attributes))
				.orElseGet(() -> registerNewUser(registrationId, email, attributes));

		return UserPrincipal.create(user, attributes);
	}

	private User registerNewUser(String registrationId, String email, Map<String, Object> attributes) {
		User user = new User();
		user.setEmail(email);
		user.setName(extractName(registrationId, attributes, email));
		user.setAbout("Signed in with " + registrationId);
		user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
		user.setProvider(resolveProvider(registrationId));
		user.setProviderId(extractProviderId(registrationId, attributes));
		user.setEmailVerified(true);

		Role defaultRole = roleRepo.findById(AppConstants.NORMAL_USER)
				.orElseThrow(() -> new UsernameNotFoundException("Default role not configured"));
		user.getRoles().add(defaultRole);
		return userRepo.save(user);
	}

	private User updateExistingUser(User existing, String registrationId, Map<String, Object> attributes) {
		existing.setName(extractName(registrationId, attributes, existing.getEmail()));
		existing.setProvider(resolveProvider(registrationId));
		existing.setProviderId(extractProviderId(registrationId, attributes));
		existing.setEmailVerified(true);
		return userRepo.save(existing);
	}

	private String extractEmail(OAuth2UserRequest userRequest, Map<String, Object> attributes) {
		String registrationId = userRequest.getClientRegistration().getRegistrationId();
		if ("google".equalsIgnoreCase(registrationId)) {
			return (String) attributes.get("email");
		}
		if ("github".equalsIgnoreCase(registrationId)) {
			String email = (String) attributes.get("email");
			if (StringUtils.hasText(email)) {
				return email;
			}
			return fetchPrimaryGithubEmail(userRequest.getAccessToken().getTokenValue());
		}
		return (String) attributes.get("email");
	}

	private String extractName(String registrationId, Map<String, Object> attributes, String fallback) {
		if ("google".equalsIgnoreCase(registrationId)) {
			return Optional.ofNullable((String) attributes.get("name")).orElse(fallback);
		}
		if ("github".equalsIgnoreCase(registrationId)) {
			return Optional.ofNullable((String) attributes.get("name"))
					.orElse((String) attributes.getOrDefault("login", fallback));
		}
		return fallback;
	}

	private String extractProviderId(String registrationId, Map<String, Object> attributes) {
		if ("google".equalsIgnoreCase(registrationId)) {
			return (String) attributes.get("sub");
		}
		if ("github".equalsIgnoreCase(registrationId)) {
			return String.valueOf(attributes.get("id"));
		}
		return (String) attributes.get("id");
	}

	private AuthProvider resolveProvider(String registrationId) {
		try {
			return AuthProvider.valueOf(registrationId.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new OAuth2AuthenticationException("Provider not supported : " + registrationId);
		}
	}

	private String fetchPrimaryGithubEmail(String accessToken) {
		if (!StringUtils.hasText(accessToken)) {
			return null;
		}
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.add(HttpHeaders.ACCEPT, "application/vnd.github+json");
			HttpEntity<Void> entity = new HttpEntity<>(headers);

			ResponseEntity<List<Map<String, Object>>> response = restTemplateBuilder.build().exchange(
					"https://api.github.com/user/emails",
					HttpMethod.GET,
					entity,
					new ParameterizedTypeReference<List<Map<String, Object>>>() {});

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				return null;
			}

			return response.getBody().stream()
					.filter(entry -> Boolean.TRUE.equals(entry.get("primary")))
					.map(entry -> (String) entry.get("email"))
					.filter(StringUtils::hasText)
					.findFirst()
					.orElseGet(() -> response.getBody().stream()
							.map(entry -> (String) entry.get("email"))
							.filter(StringUtils::hasText)
							.findFirst()
							.orElse(null));
		} catch (RestClientException ex) {
			LOGGER.warn("Failed to fetch GitHub primary email", ex);
			return null;
		}
	}
}

