package com.example.ddd_start.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/shop")
    public String products() {
        return "products";
    }

    @GetMapping("/shop/{id}")
    public String productDetail(@PathVariable Long id) {
        // 상품 상세 페이지는 products.html에서 처리하거나 별도 페이지 생성 가능
        return "redirect:/shop";
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }

    @GetMapping("/my-orders")
    public String myOrders() {
        return "orders";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "checkout";
    }
}
