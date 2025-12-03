package com.backend.blog.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.backend.blog.bloom.BloomFilterService;
import com.backend.blog.entities.AuthProvider;
import com.backend.blog.entities.Role;
import com.backend.blog.entities.User;
import com.backend.blog.exceptions.ApiException;
import com.backend.blog.exceptions.ResourceNotFoundException;
import com.backend.blog.repositories.RoleRepo;
import com.backend.blog.repositories.UserRepo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.backend.blog.payloads.UserDto;
import com.backend.blog.services.UserService;
import com.backend.blog.config.AppConstants;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RoleRepo roleRepo;

	@Autowired
	private BloomFilterService bloomFilterService;

	@Override
	public UserDto createUser(UserDto userDto) {
		validateEmailUniqueness(userDto.getEmail());
		User user = this.dtoToUser(userDto);
		user.setProvider(AuthProvider.LOCAL);
		user.setEmailVerified(false);
		applyPasswordIfPresent(user, userDto.getPassword());
		assignDefaultRole(user);
		User savedUser = this.userRepo.save(user);
		bloomFilterService.recordEmail(savedUser.getEmail());
		return this.userToDto(savedUser);
	}

	@Override
	public UserDto updateUser(UserDto userDto, Integer userId) {

		User user = this.userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", " Id ", userId));

		boolean emailChanged = userDto.getEmail() != null
				&& !userDto.getEmail().equalsIgnoreCase(user.getEmail());
		if (emailChanged) {
			validateEmailUniqueness(userDto.getEmail());
		}

		user.setName(userDto.getName());
		user.setEmail(userDto.getEmail());
		applyPasswordIfPresent(user, userDto.getPassword());
		user.setAbout(userDto.getAbout());

		User updatedUser = this.userRepo.save(user);
		if (emailChanged) {
			bloomFilterService.recordEmail(updatedUser.getEmail());
		}
		UserDto userDto1 = this.userToDto(updatedUser);
		return userDto1;
	}

	@Override
	public UserDto getUserById(Integer userId) {

		User user = this.userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", " Id ", userId));

		return this.userToDto(user);
	}

	@Override
	public List<UserDto> getAllUsers() {

		List<User> users = this.userRepo.findAll();
		List<UserDto> userDtos = users.stream().map(user -> this.userToDto(user)).collect(Collectors.toList());

		return userDtos;
	}

	@Override
	public void deleteUser(Integer userId) {
		User user = this.userRepo.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", "Id", userId));
		this.userRepo.delete(user);

	}

	public User dtoToUser(UserDto userDto) {
		User user = this.modelMapper.map(userDto, User.class);
		if (user.getProvider() == null) {
			user.setProvider(AuthProvider.LOCAL);
		}
		user.setEmailVerified(false);

		// user.setId(userDto.getId());
		// user.setName(userDto.getName());
		// user.setEmail(userDto.getEmail());
		// user.setAbout(userDto.getAbout());
		// user.setPassword(userDto.getPassword());
		return user;
	}

	public UserDto userToDto(User user) {
		UserDto userDto = this.modelMapper.map(user, UserDto.class);
		return userDto;
	}

	@Override
	public UserDto registerNewUser(UserDto userDto) {

		validateEmailUniqueness(userDto.getEmail());

		User user = this.modelMapper.map(userDto, User.class);

		// encoded the password
		applyPasswordIfPresent(user, userDto.getPassword());
		user.setProvider(AuthProvider.LOCAL);
		user.setEmailVerified(false);

		assignDefaultRole(user);

		User newUser = this.userRepo.save(user);
		bloomFilterService.recordEmail(newUser.getEmail());

		return this.modelMapper.map(newUser, UserDto.class);
	}

	private void validateEmailUniqueness(String email) {
		if (bloomFilterService.isEmailProbablyRegistered(email)) {
			throw new ApiException("Email already registered. Please sign in instead.");
		}
	}

	private void applyPasswordIfPresent(User user, String rawPassword) {
		if (StringUtils.hasText(rawPassword)) {
			user.setPassword(this.passwordEncoder.encode(rawPassword));
		}
	}

	private void assignDefaultRole(User user) {
		if (user.getRoles() == null || user.getRoles().isEmpty()) {
			Role role = this.roleRepo.findById(AppConstants.NORMAL_USER)
					.orElseThrow(() -> new ResourceNotFoundException("Role", "Id", AppConstants.NORMAL_USER));
			user.getRoles().add(role);
		}
	}

}
