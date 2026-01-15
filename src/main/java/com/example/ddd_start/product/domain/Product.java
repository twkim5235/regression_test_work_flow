package com.example.ddd_start.product.domain;

import com.example.ddd_start.common.domain.Money;
import com.example.ddd_start.store.domain.Store;
import java.time.Instant;
import java.util.List;
import javax.persistence.AttributeOverride;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product", indexes = @Index(name = "idx_title", columnList = "title"))
@Getter
@NoArgsConstructor
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String title;
  private String slug;
  @Embedded
  @AttributeOverride(name = "amount", column = @Column(name = "price"))
  private Money price;
  @Column(columnDefinition = "TEXT")
  private String description;
  private Long categoryId;
  @ManyToOne
  @JoinColumn(name = "store_id")
  private Store store;
  @ElementCollection
  @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
  @Column(name = "image_url", columnDefinition = "TEXT")
  List<String> images;
  private Instant createdAt;
  private Instant updatedAt;

  public Product(String title, String slug, Money price, String description, Long categoryId,
      List<String> images, Instant createdAt, Instant updatedAt) {
    this.title = title;
    this.slug = slug;
    this.price = price;
    this.description = description;
    this.categoryId = categoryId;
    this.images = images;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Product(String title, String slug, Money price, String description, Long categoryId,
      List<String> images, Store store) {
    validatePrice(price);
    validateCategoryId(categoryId);
    this.title = title;
    this.slug = slug;
    this.price = price;
    this.description = description;
    this.categoryId = categoryId;
    this.images = images;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
    this.store = store;
  }

  /**
   * 가격 유효성 검증
   * - null 체크
   * - 0원 이하 체크
   */
  private void validatePrice(Money price) {
    if (price == null || price.getAmount() == null) {
      throw new IllegalArgumentException("상품 가격은 필수입니다.");
    }
    if (price.getAmount() <= 0) {
      throw new IllegalArgumentException("상품 가격은 0원보다 커야 합니다.");
    }
  }

  /**
   * 카테고리 ID 유효성 검증
   */
  private void validateCategoryId(Long categoryId) {
    if (categoryId == null) {
      throw new IllegalArgumentException("카테고리 ID는 필수입니다.");
    }
  }

  public void updateProduct(String title, String slug, Money price,
      String description, Long categoryId,
      List<String> images, Store store) {
    validatePrice(price);
    validateCategoryId(categoryId);
    this.title = title;
    this.slug = slug;
    this.price = price;
    this.description = description;
    this.categoryId = categoryId;
    this.images = images;
    this.updatedAt = Instant.now();
    this.store = store;
  }
}
