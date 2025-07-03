package com.backend.blog.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.blog.entities.Comment;

public interface CommentRepo  extends JpaRepository<Comment	, Integer> {

}
