package com.backend.blog.security;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.backend.blog.entities.User;

public class UserPrincipal implements OAuth2User, UserDetails {

	private final User user;
	private Map<String, Object> attributes = new HashMap<>();

	private UserPrincipal(User user) {
		this.user = user;
	}

	public static UserPrincipal create(User user) {
		return new UserPrincipal(user);
	}

	public static UserPrincipal create(User user, Map<String, Object> attributes) {
		UserPrincipal principal = create(user);
		principal.setAttributes(attributes);
		return principal;
	}

	public User getUser() {
		return this.user;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes != null ? attributes : new HashMap<>();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return user.getAuthorities();
	}

	@Override
	public String getPassword() {
		return user.getPassword();
	}

	@Override
	public String getUsername() {
		return user.getUsername();
	}

	@Override
	public boolean isAccountNonExpired() {
		return user.isAccountNonExpired();
	}

	@Override
	public boolean isAccountNonLocked() {
		return user.isAccountNonLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return user.isCredentialsNonExpired();
	}

	@Override
	public boolean isEnabled() {
		return user.isEnabled();
	}

	@Override
	public String getName() {
		return user.getName();
	}
}

