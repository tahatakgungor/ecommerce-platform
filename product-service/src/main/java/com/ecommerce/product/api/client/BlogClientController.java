package com.ecommerce.product.api.client;

import com.ecommerce.product.application.BlogPostService;
import com.ecommerce.product.domain.BlogPost;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogClientController {

    private final BlogPostService blogPostService;

    @GetMapping
    public Map<String, Object> getPublishedPosts() {
        List<BlogPost> posts = blogPostService.getPublishedPosts();
        return Map.of("posts", posts);
    }

    @GetMapping("/{slug}")
    public Map<String, Object> getPostBySlug(@PathVariable String slug) {
        BlogPost post = blogPostService.getPublishedPostBySlug(slug);
        return Map.of("post", post);
    }
}

