package com.example.ddd_start.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ddd_start.category.domain.CategoryRepository;
import com.example.ddd_start.product.application.service.model.NewProductRequest;
import com.example.ddd_start.product.application.service.model.UpdateProductRequest;
import com.example.ddd_start.product.domain.Product;
import com.example.ddd_start.product.domain.ProductRepository;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterProductService 단위 테스트")
class RegisterProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @InjectMocks
  private RegisterProductService registerProductService;

  @Nested
  @DisplayName("상품 등록 (registerProduct)")
  class RegisterProductTests {

    private NewProductRequest validRequest;

    @BeforeEach
    void setUp() {
      List<String> images = Arrays.asList(
          "https://example.com/image1.jpg",
          "https://example.com/image2.jpg"
      );

      validRequest = NewProductRequest.builder()
          .title("테스트 상품")
          .slug("test-product")
          .price(10000)
          .description("테스트 상품 설명")
          .categoryId(1L)
          .images(images)
          .build();
    }

    @Test
    @DisplayName("정상적인 상품 등록")
    void registerProduct_Success() {
      // given
      when(categoryRepository.existsById(1L)).thenReturn(true);
      when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
        Product product = invocation.getArgument(0);
        // ID를 설정하여 반환 (실제 저장 시뮬레이션)
        return product;
      });

      // when
      Long productId = registerProductService.registerProduct(validRequest);

      // then
      verify(categoryRepository).existsById(1L);
      verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 상품 등록 시 예외 발생")
    void registerProduct_CategoryNotExists_ThrowsException() {
      // given
      when(categoryRepository.existsById(1L)).thenReturn(false);

      // when & then
      assertThatThrownBy(() -> registerProductService.registerProduct(validRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("존재하지 않는 카테고리입니다.");

      verify(categoryRepository).existsById(1L);
      verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("0원 이하 가격으로 상품 등록 시 예외 발생")
    void registerProduct_InvalidPrice_ThrowsException() {
      // given
      NewProductRequest invalidPriceRequest = NewProductRequest.builder()
          .title("테스트 상품")
          .slug("test-product")
          .price(0)
          .description("테스트 상품 설명")
          .categoryId(1L)
          .images(Arrays.asList("https://example.com/image1.jpg"))
          .build();

      when(categoryRepository.existsById(1L)).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> registerProductService.registerProduct(invalidPriceRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("상품 가격은 0원보다 커야 합니다.");

      verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("음수 가격으로 상품 등록 시 예외 발생")
    void registerProduct_NegativePrice_ThrowsException() {
      // given
      NewProductRequest negativePriceRequest = NewProductRequest.builder()
          .title("테스트 상품")
          .slug("test-product")
          .price(-1000)
          .description("테스트 상품 설명")
          .categoryId(1L)
          .images(Arrays.asList("https://example.com/image1.jpg"))
          .build();

      when(categoryRepository.existsById(1L)).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> registerProductService.registerProduct(negativePriceRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("상품 가격은 0원보다 커야 합니다.");

      verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("null 가격으로 상품 등록 시 예외 발생")
    void registerProduct_NullPrice_ThrowsException() {
      // given
      NewProductRequest nullPriceRequest = NewProductRequest.builder()
          .title("테스트 상품")
          .slug("test-product")
          .price(null)
          .description("테스트 상품 설명")
          .categoryId(1L)
          .images(Arrays.asList("https://example.com/image1.jpg"))
          .build();

      when(categoryRepository.existsById(1L)).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> registerProductService.registerProduct(nullPriceRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("상품 가격은 필수입니다.");

      verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("null 카테고리 ID로 상품 등록 시 예외 발생")
    void registerProduct_NullCategoryId_ThrowsException() {
      // given
      NewProductRequest nullCategoryRequest = NewProductRequest.builder()
          .title("테스트 상품")
          .slug("test-product")
          .price(10000)
          .description("테스트 상품 설명")
          .categoryId(null)
          .images(Arrays.asList("https://example.com/image1.jpg"))
          .build();

      // when & then
      assertThatThrownBy(() -> registerProductService.registerProduct(nullCategoryRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("카테고리 ID는 필수입니다.");

      verify(productRepository, never()).save(any(Product.class));
    }
  }

  @Nested
  @DisplayName("상품 수정 (updateProduct)")
  class UpdateProductTests {

    private UpdateProductRequest validUpdateRequest;
    private Product existingProduct;

    @BeforeEach
    void setUp() {
      List<String> images = Arrays.asList(
          "https://example.com/image1.jpg",
          "https://example.com/image2.jpg"
      );

      validUpdateRequest = UpdateProductRequest.builder()
          .productId(1L)
          .title("수정된 상품")
          .slug("updated-product")
          .price(20000)
          .description("수정된 상품 설명")
          .categoryId(2L)
          .images(images)
          .build();

      // 기존 상품 생성 (Mock)
      existingProduct = new Product(
          "기존 상품",
          "existing-product",
          new com.example.ddd_start.common.domain.Money(10000),
          "기존 상품 설명",
          1L,
          Arrays.asList("https://example.com/old-image.jpg"),
          null
      );
    }

    @Test
    @DisplayName("정상적인 상품 수정 (JPA dirty checking 활용)")
    void updateProduct_Success() {
      // given
      when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
      when(categoryRepository.existsById(2L)).thenReturn(true);

      // when
      registerProductService.updateProduct(validUpdateRequest);

      // then
      verify(productRepository).findById(1L);
      verify(categoryRepository).existsById(2L);
      // save()가 호출되지 않음을 확인 (JPA dirty checking)
      verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("존재하지 않는 상품 수정 시 예외 발생")
    void updateProduct_ProductNotExists_ThrowsException() {
      // given
      when(productRepository.findById(1L)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> registerProductService.updateProduct(validUpdateRequest))
          .isInstanceOf(NoSuchElementException.class)
          .hasMessage("상품을 찾을 수 없습니다.");

      verify(productRepository).findById(1L);
      verify(categoryRepository, never()).existsById(any());
      verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 상품 수정 시 예외 발생")
    void updateProduct_CategoryNotExists_ThrowsException() {
      // given
      when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
      when(categoryRepository.existsById(2L)).thenReturn(false);

      // when & then
      assertThatThrownBy(() -> registerProductService.updateProduct(validUpdateRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("존재하지 않는 카테고리입니다.");

      verify(productRepository).findById(1L);
      verify(categoryRepository).existsById(2L);
      verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("0원 이하 가격으로 상품 수정 시 예외 발생")
    void updateProduct_InvalidPrice_ThrowsException() {
      // given
      UpdateProductRequest invalidPriceRequest = UpdateProductRequest.builder()
          .productId(1L)
          .title("수정된 상품")
          .slug("updated-product")
          .price(0)
          .description("수정된 상품 설명")
          .categoryId(2L)
          .images(Arrays.asList("https://example.com/image1.jpg"))
          .build();

      when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
      when(categoryRepository.existsById(2L)).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> registerProductService.updateProduct(invalidPriceRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("상품 가격은 0원보다 커야 합니다.");

      verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("음수 가격으로 상품 수정 시 예외 발생")
    void updateProduct_NegativePrice_ThrowsException() {
      // given
      UpdateProductRequest negativePriceRequest = UpdateProductRequest.builder()
          .productId(1L)
          .title("수정된 상품")
          .slug("updated-product")
          .price(-5000)
          .description("수정된 상품 설명")
          .categoryId(2L)
          .images(Arrays.asList("https://example.com/image1.jpg"))
          .build();

      when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
      when(categoryRepository.existsById(2L)).thenReturn(true);

      // when & then
      assertThatThrownBy(() -> registerProductService.updateProduct(negativePriceRequest))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("상품 가격은 0원보다 커야 합니다.");

      verify(productRepository, never()).save(any(Product.class));
    }
  }
}
