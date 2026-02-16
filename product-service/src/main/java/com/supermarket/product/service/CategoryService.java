package com.supermarket.product.service;

import com.supermarket.product.entity.Category;
import com.supermarket.product.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createCategory(Category category, String tenantId) {
        if (categoryRepository.existsByCodeAndTenantId(category.getCode(), tenantId)) {
            throw new RuntimeException("Category with code " + category.getCode() + " already exists");
        }

        category.setTenantId(tenantId);
        
        if (category.getParent() != null && category.getParent().getId() != null) {
            Category parent = categoryRepository.findById(category.getParent().getId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParent(parent);
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setParent(null);
            category.setLevel(0);
        }

        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category category, String tenantId) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!existing.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to category");
        }

        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setImageUrl(category.getImageUrl());
        existing.setActive(category.isActive());
        existing.setSortOrder(category.getSortOrder());

        if (category.getParent() != null && category.getParent().getId() != null) {
            Category parent = categoryRepository.findById(category.getParent().getId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            existing.setParent(parent);
            existing.setLevel(parent.getLevel() + 1);
        }

        return categoryRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Long id, String tenantId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        if (!category.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to category");
        }
        
        return category;
    }

    @Transactional(readOnly = true)
    public List<Category> getRootCategories(String tenantId) {
        return categoryRepository.findByTenantIdAndParentIsNull(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Category> getChildCategories(String tenantId, Long parentId) {
        return categoryRepository.findByTenantIdAndParentId(tenantId, parentId);
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories(String tenantId) {
        return categoryRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesByLevel(String tenantId, Integer level) {
        return categoryRepository.findByTenantIdAndLevel(tenantId, level);
    }
}
