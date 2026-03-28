package com.ecommerce.product.application.mapper;

import com.ecommerce.product.domain.Product;
import com.ecommerce.product.dto.product.ProductRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "name", source = "title")
    @Mapping(target = "stockQuantity", source = "quantity")
    @Mapping(target = "parentCategory", source = "parent")
    @Mapping(target = "childCategory", source = "children")
    @Mapping(target = "originalPrice", source = "price")
    @Mapping(target = "image", source = "image", qualifiedByName = "mapImage")
    Product toEntity(ProductRequest request);

    @Named("mapImage")
    default String mapImage(Object image) {
        if (image instanceof String) return (String) image;
        if (image instanceof Map) return (String) ((Map<?, ?>) image).get("url");
        return null;
    }
}