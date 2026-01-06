# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 2.7.2 e-commerce backend following Domain-Driven Design (DDD) principles. The system implements an online store with products, orders, shopping carts, coupons, and member management with JWT authentication.

**Tech Stack:**
- Java 17
- Spring Boot 2.7.2 (Spring Data JPA, Spring Security)
- MySQL database
- QueryDSL 5.0.0 for advanced queries
- JWT authentication (JJWT 0.11.5)
- Gradle build system
- Lombok

## Build and Development Commands

### Build
```bash
# Build the project (skip tests)
./gradlew build -x test

# Build with tests
./gradlew build

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests com.example.ddd_start.application.order.CalculateDiscountServiceTest

# Run tests with logging output
./gradlew test --info
```

### Running the Application
```bash
# Run locally (requires MySQL running on localhost:3306)
./gradlew bootRun

# Build JAR and run
./gradlew build -x test
java -jar build/libs/*.jar
```

### Database Setup
The application expects MySQL running on `localhost:3306` with database `ddd_start`. Environment variables from `.env`:
- `MYSQL_ROOT_PASSWORD` - MySQL root password
- `JWT_SECRET` - JWT signing secret
- `JWT_EXPIRES` - JWT expiration time in milliseconds (default: 86400000 = 1 day)

### QueryDSL Code Generation
QueryDSL Q-classes are generated during compilation. If you modify entities and need to regenerate:
```bash
./gradlew compileQuerydsl
```
Generated files are in `build/generated/querydsl/`

### Docker
```bash
# Build Docker image
docker build -t store_backend:latest -f docker/Dockerfile_backend .

# Note: This project uses Jenkins for CI/CD (see docker/Jenkinsfile)
```

## Architecture Overview

### DDD Structure
The codebase is organized into bounded contexts (modules):
- **member** - User accounts, authentication (implements Spring Security UserDetails)
- **product** - Products, categories, images, browsing history
- **order** - Order aggregates, order lines, shipping, payment
- **coupon** - Coupon definitions and user coupons
- **cart** - Shopping cart
- **store** - Store entities
- **category** - Product categorization
- **common** - Shared domain (Money, Address value objects, exceptions)

### Package Structure
```
src/main/java/com/example/ddd_start/
├── application/          # Application services, commands, event handlers
│   ├── member/
│   ├── order/
│   └── ...
├── domain/              # Domain entities, value objects, repositories, domain services
│   ├── member/
│   ├── order/
│   ├── product/
│   └── common/          # Shared domain (Money, Address, exceptions)
├── infrastructure/      # Technical implementations, configurations
│   ├── auth/           # JWT, Security config
│   └── ...
└── ui/                 # REST controllers, DTOs
    ├── member/
    ├── order/
    └── ...
```

### Key Architectural Patterns

**1. Aggregates**
- `Order` is the root aggregate containing `OrderLine` entities, `ShippingInfo`, and domain events
- Each aggregate has clear transactional boundaries
- Aggregates enforce consistency rules (e.g., Order validates shipping info)

**2. Value Objects**
- `Money` - Embeddable value object with arithmetic operations (add, subtract, multiply)
- `Address` - Contains address, detailedAddress, zipCode
- `ShippingInfo`, `Orderer`, `PaymentInfo` - Embedded in Order aggregate
- Always use value objects instead of primitives for domain concepts

**3. Domain Events**
- `OrderCanceledEvent` - Published when order is canceled, triggers async refund processing
- `ShippingInfoChangedEvent` - Published when shipping changes
- Event handlers run asynchronously in separate transactions using `@Async` and `@TransactionalEventListener(phase = AFTER_COMMIT)`

**4. Repository Patterns**
- Standard: `MemberRepository extends JpaRepository<Member, Long>`
- QueryDSL custom: `OrderRepository extends CrudRepository<Order, Long>, OrderRepositoryCustom`
- Custom implementations in `*RepositoryCustomImpl` use `JPAQueryFactory` for complex queries

**5. Locking Strategies**
- Optimistic locking: Order has `@Version private Integer version`
- Pessimistic locking: `@Lock(LockModeType.PESSIMISTIC_WRITE)` on `findByIdPessimistic`
- Lock timeouts configured via `@QueryHints`

## Critical Business Logic

### Order Placement Flow
1. `PlaceOrderRequest` received by `OrderController`
2. `OrderService.placeOrderV2(PlaceOrderCommand)` orchestrates:
   - Creates `OrderLine` entities from product DTOs
   - Creates `Order` aggregate (validates shipping, orderer, lines)
   - Calculates discounts via `DiscountCalculationService` (applies coupons + member grade)
   - Sets `paymentAmounts = totalAmounts - discount`
3. Saves order to repository
4. Publishes domain events if applicable

### Discount Calculation
- `DiscountCalculationService` applies user coupons (ratio-based or fixed amount)
- Coupons are marked as used via `UserCoupon.redeemCoupon(Money)`
- Member grade discounts can be added (stub currently)
- Multiple coupons accumulate discounts

### Order Cancellation
1. `Order.cancel()` validates order state (must not be shipped)
2. Sets `orderState = CANCEL`
3. Publishes `OrderCanceledEvent`
4. Async event handler:
   - Deletes order lines via `@Modifying` query
   - Calls `RefundService.refund(paymentId)`
   - Updates refund state to `REFUND_COMPLETED`

## Security and Authentication

### JWT Flow
1. User signs in via `/members/sign-in` with username/password
2. `JwtTokenProvider.generateToken()` creates access + refresh tokens
3. Client includes token in `Authorization: Bearer {token}` header
4. `JwtAuthenticationFilter` validates and extracts `Authentication` from token
5. `SecurityContextHolder` stores authentication for request

### Key Components
- `JwtTokenProvider` - Token generation/validation (HMAC-SHA256)
- `JwtAuthenticationFilter` - Runs before UsernamePasswordAuthenticationFilter
- `SecurityConfig` - Stateless sessions, BCrypt password encoding
- `Member` implements `UserDetails` with roles stored in `@ElementCollection List<String> roles`

### Public Endpoints (No Auth)
- `/members/join`, `/members/sign-in`
- `/products/**` (GET operations)
- `/categories`
- Swagger UI endpoints

## Database Conventions

### Entity Design
- Use `@Embeddable` for value objects (Money, Address, ShippingInfo)
- Use `@ElementCollection` for simple collections (roles, images)
- Use `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)` for aggregate children
- Always use `@Enumerated(EnumType.STRING)` for enums

### Money Value Object Usage
```java
// Always use Money for amounts
private Money totalAmounts;
private Money paymentAmounts;

// Use Money operations
Money discounted = totalAmounts.subtract(discountAmount);
Money lineTotal = price.multiply(quantity);
```

### QueryDSL Custom Queries
When writing complex queries:
1. Define interface `*RepositoryCustom` with query methods
2. Implement in `*RepositoryCustomImpl` with `JPAQueryFactory`
3. Use `Projections.constructor()` for DTO mapping
4. Use `BooleanBuilder` for dynamic predicates
5. Main repository extends both `JpaRepository` and custom interface

Example:
```java
public List<OrderDto> searchMyOrders(OrderSearchCondition condition) {
    return queryFactory
        .select(Projections.constructor(OrderDto.class,
            order.orderNumber,
            order.orderState,
            order.shippingInfo,
            order.totalAmounts,
            order.orderer.name,
            order.createdAt))
        .from(order)
        .where(ordererIdEq(condition.getOrdererId()))
        .fetch();
}
```

## Important Implementation Notes

### Transaction Management
- Application services use `@Transactional` at method level
- Event handlers use `@Transactional(propagation = TxType.REQUIRES_NEW)` for separate transactions
- Read-only operations should use `@Transactional(readOnly = true)`

### Domain Event Publishing
```java
// In service method
@Transactional
public void cancelOrder(Long orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    order.cancel(); // This publishes OrderCanceledEvent
    // Event handler executes AFTER transaction commits
}
```

### Cart Same-Item Handling
When adding items to cart, if the same product exists, increase quantity instead of creating duplicate entries. This is handled in cart business logic.

### Member Deletion Cascade
When deleting a member, related entities (orders, coupons, cart) are deleted via `@Modifying @Query` to avoid N+1 issues:
```java
@Modifying
@Query("delete from orders o where o.orderer.memberId = :memberId")
void deleteByMemberId(Long memberId);
```

### Registration Coupon
On member registration, automatically issue a welcome coupon via `CouponService.insertCouponDefinition()`.

### Amount Calculations
Backend always calculates order amounts. Frontend should NOT send pre-calculated totals - send product IDs and quantities only.

## API Conventions

### Response Format
Most endpoints return:
- Success: HTTP 200 with response DTO
- Error: HTTP 4xx/5xx with error details

### Pagination
List endpoints accept `Pageable` parameters:
```
GET /products?page=0&size=20&sort=price.amount,asc
```

### Key Controller Endpoints
- `POST /members/sign-in` - Returns JWT tokens
- `POST /orders/place-order` - Creates order (auth required)
- `GET /orders/my-order` - Gets user's orders (auth required)
- `GET /products` - Lists products with pagination
- `POST /carts` - Adds item to cart
- `DELETE /user-coupons` - Uses (redeems) a coupon

## Testing Notes

- Main test directory: `src/test/java/com/example/ddd_start/`
- Tests use JUnit 5 (Jupiter)
- Spring Boot Test provides `@SpringBootTest` for integration tests
- Minimal test coverage currently - focus on service layer when adding tests
- Mock external dependencies (RefundService, etc.) in unit tests

## Common Gotchas

1. **QueryDSL Q-classes not found**: Run `./gradlew compileQuerydsl` to generate
2. **Version conflict on order update**: Use optimistic locking properly, handle `VersionConflictException`
3. **JWT expired**: Default expiration is 1 day, configure via `JWT_EXPIRES` env var
4. **MySQL connection refused**: Ensure MySQL is running and .env variables are set
5. **Cascade operations**: Order deletion cascades to OrderLines via `orphanRemoval = true`
6. **Event handler not firing**: Check `@EnableAsync` is configured and method is `public`

## Git Commit Conventions

Based on recent commits, use Korean messages with prefixes:
- `[HOT-FIX]` - Production hotfixes
- `[FEATURE]` - New features
- `[FIX]` - Bug fixes
- `[REFACTOR]` - Code refactoring

Example: `[HOT-FIX] 프론트엔드 연동을 위한 hotfix - 주문 생성시 amount 계산은 백엔드가 하므로, 요청에서 제거`