package com.ecommerce.product.api.admin;

import com.ecommerce.product.application.BlogPostService;
import com.ecommerce.product.domain.BlogPost;
import com.ecommerce.product.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/blog")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('Admin','Staff')")
public class BlogAdminController {

    private final BlogPostService blogPostService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BlogPost>>> getAllPosts() {
        List<BlogPost> posts = blogPostService.getAllForAdmin();
        return ResponseEntity.ok(ApiResponse.ok(posts, (long) posts.size()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlogPost>> createPost(@RequestBody BlogPost request) {
        BlogPost post = blogPostService.create(request);
        return ResponseEntity.ok(ApiResponse.ok(post, 1L));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogPost>> updatePost(
            @PathVariable UUID id,
            @RequestBody BlogPost request
    ) {
        BlogPost post = blogPostService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok(post, 1L));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BlogPost>> updatePostStatus(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String status = body != null ? body.get("status") : null;
        BlogPost post = blogPostService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok(post, 1L));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePost(@PathVariable UUID id) {
        blogPostService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Blog yazısı silindi.", 1L));
    }
}

