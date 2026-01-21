package com.example.ddd_start.product.application.service;

import com.example.ddd_start.category.domain.CategoryRepository;
import com.example.ddd_start.common.domain.Money;
import com.example.ddd_start.product.application.service.model.NewProductRequest;
import com.example.ddd_start.product.application.service.model.UpdateProductRequest;
import com.example.ddd_start.product.domain.Product;
import com.example.ddd_start.product.domain.ProductRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterProductService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  /**
   * 상품 등록
   * - Category 존재 여부 검증
   * - Product 도메인 엔티티에서 가격 및 카테고리 ID 유효성 검증
   */
  @Transactional
  public Long registerProduct(NewProductRequest req) {
    // Category 존재 여부 검증
    validateCategoryExists(req.getCategoryId());

    // Product 생성 (도메인 레벨에서 가격 및 카테고리 ID 검증)
    Product product = new Product(
        req.getTitle(),
        req.getSlug(),
        new Money(req.getPrice()),
        req.getDescription(),
        req.getCategoryId(),
        req.getImages(),
        null
    );

    productRepository.save(product);
    return product.getId();
  }

  /**
   * 상품 수정
   * - Category 존재 여부 검증
   * - Product 도메인 엔티티에서 가격 및 카테고리 ID 유효성 검증
   * - JPA dirty checking을 활용하므로 save() 호출 불필요
   */
  @Transactional
  public Long updateProduct(UpdateProductRequest req) {
    Product product = productRepository.findById(req.getProductId())
        .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다."));

    // Category 존재 여부 검증
    validateCategoryExists(req.getCategoryId());

    // Product 수정 (도메인 레벨에서 가격 및 카테고리 ID 검증)
    product.updateProduct(
        req.getTitle(),
        req.getSlug(),
        new Money(req.getPrice()),
        req.getDescription(),
        req.getCategoryId(),
        req.getImages(),
        null
    );

    // JPA dirty checking으로 자동 업데이트되므로 save() 불필요
    return product.getId();
  }

  /**
   * 카테고리 존재 여부 검증
   */
  private void validateCategoryExists(Long categoryId) {
    if (categoryId == null) {
      return; // null은 Product 도메인 레벨에서 검증
    }
    if (!categoryRepository.existsById(categoryId)) {
      throw new IllegalArgumentException("존재하지 않는 카테고리입니다.");
    }
  }
}
