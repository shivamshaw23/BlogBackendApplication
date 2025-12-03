package com.backend.blog.bloom;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.backend.blog.entities.Post;
import com.backend.blog.entities.User;
import com.backend.blog.repositories.PostRepo;
import com.backend.blog.repositories.UserRepo;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

@Service
public class BloomFilterService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BloomFilterService.class);

	@Value("${bloom.email.expectedInsertions:10000}")
	private long emailExpectedInsertions;

	@Value("${bloom.email.fpp:0.0001}")
	private double emailFalsePositiveProbability;

	@Value("${bloom.post.expectedInsertions:10000}")
	private long postExpectedInsertions;

	@Value("${bloom.post.fpp:0.0001}")
	private double postFalsePositiveProbability;

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private PostRepo postRepo;

	private BloomFilter<CharSequence> emailFilter;
	private BloomFilter<CharSequence> postTitleFilter;

	@PostConstruct
	public void initFilters() {
		this.emailFilter = BloomFilter.create(Funnels.unencodedCharsFunnel(), emailExpectedInsertions,
				emailFalsePositiveProbability);
		this.postTitleFilter = BloomFilter.create(Funnels.unencodedCharsFunnel(), postExpectedInsertions,
				postFalsePositiveProbability);

		loadExistingEmails();
		loadExistingPostTitles();
	}

	private void loadExistingEmails() {
		List<User> users = userRepo.findAll();
		users.stream().map(User::getEmail).filter(StringUtils::hasText).map(String::toLowerCase)
				.forEach(emailFilter::put);
		LOGGER.info("Bloom filter primed with {} user emails", users.size());
	}

	private void loadExistingPostTitles() {
		List<Post> posts = postRepo.findAll();
		posts.stream().map(Post::getTitle).filter(StringUtils::hasText).map(String::toLowerCase)
				.forEach(postTitleFilter::put);
		LOGGER.info("Bloom filter primed with {} post titles", posts.size());
	}

	public boolean isEmailProbablyRegistered(String email) {
		if (!StringUtils.hasText(email)) {
			return false;
		}
		return emailFilter.mightContain(email.toLowerCase());
	}

	public void recordEmail(String email) {
		if (!StringUtils.hasText(email)) {
			return;
		}
		emailFilter.put(email.toLowerCase());
	}

	public boolean isPostTitleProbablyUsed(String title) {
		if (!StringUtils.hasText(title)) {
			return false;
		}
		return postTitleFilter.mightContain(title.toLowerCase());
	}

	public void recordPostTitle(String title) {
		if (!StringUtils.hasText(title)) {
			return;
		}
		postTitleFilter.put(title.toLowerCase());
	}
}

