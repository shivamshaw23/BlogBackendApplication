package com.backend.blog.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.blog.entities.Category;

public interface CategoryRepo extends JpaRepository<Category, Integer> {

}
