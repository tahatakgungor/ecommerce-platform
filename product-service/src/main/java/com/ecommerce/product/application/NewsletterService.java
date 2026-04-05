package com.ecommerce.product.application;

import com.ecommerce.product.domain.NewsletterEmail;
import com.ecommerce.product.repository.NewsletterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterRepository newsletterRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getNewsletterSubscribers(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size), Sort.by(Sort.Direction.DESC, "subscribedAt"));
        Page<NewsletterEmail> subscribers = newsletterRepository.findAll(pageable);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("subscribers", subscribers.getContent().stream().map(this::toItem).toList());
        response.put("page", subscribers.getNumber());
        response.put("size", subscribers.getSize());
        response.put("totalPages", subscribers.getTotalPages());
        response.put("totalElements", subscribers.getTotalElements());
        return response;
    }

    @Transactional
    public void deleteSubscriber(Long id) {
        NewsletterEmail subscriber = newsletterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Abone bulunamadı."));
        newsletterRepository.delete(subscriber);
    }

    private Map<String, Object> toItem(NewsletterEmail subscriber) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", subscriber.getId());
        item.put("email", subscriber.getEmail());
        item.put("subscribedAt", subscriber.getSubscribedAt());
        return item;
    }

    private int normalizeSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, 100);
    }
}
