package com.ecommerce.product.config;

import com.ecommerce.product.domain.Brand;
import com.ecommerce.product.domain.Category;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.repository.BrandRepository;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Value("${app.seed-data:false}")
    private boolean seedData;
    @Value("${app.seed-data-reset-existing:false}")
    private boolean seedDataResetExisting;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedData) return;

        long productCount = productRepository.count();
        long categoryCount = categoryRepository.count();
        long brandCount = brandRepository.count();
        boolean hasExistingData = productCount > 0 || categoryCount > 0 || brandCount > 0;

        if (hasExistingData && !seedDataResetExisting) {
            log.warn(">>> Seed atlandı: mevcut veriler korunuyor. (products={}, categories={}, brands={})",
                    productCount, categoryCount, brandCount);
            log.warn(">>> Mevcut veriyi resetleyip seedlemek isterseniz app.seed-data-reset-existing=true yapın.");
            return;
        }

        if (seedDataResetExisting) {
            log.warn(">>> Seed reset modu aktif. Veri temizleme başlıyor (users/invitations korunuyor)...");
            productRepository.deleteAll();
            categoryRepository.deleteAll();
            brandRepository.deleteAll();
            log.warn(">>> Tablolar temizlendi. Serravit verileri ekleniyor...");
        } else {
            log.info(">>> Seed başlıyor. Veriler boş olduğu için ilk kurulum verisi eklenecek.");
        }

        seedBrands();
        seedCategories();
        seedProducts();

        log.info(">>> Seed tamamlandı.");
    }

    // ─────────────────────────────────────────────
    // BRANDS
    // ─────────────────────────────────────────────
    private void seedBrands() {
        List<Brand> brands = List.of(
            brand("SERRAVİT",
                  "info@serravit.com.tr",
                  "https://serravit.com.tr",
                  "Kocaeli, Türkiye",
                  "Humik asit bazlı gıda takviyesi alanında Türkiye'nin öncü markası."),
            brand("Humidone",
                  "info@serravit.com.tr",
                  "https://serravit.com.tr",
                  "Kocaeli, Türkiye",
                  "Humik asit içerikli doğal cilt bakım ve kişisel bakım ürünleri."),
            brand("Humat Kimya",
                  "info@serravit.com.tr",
                  "https://serravit.com.tr",
                  "Kandıra, Kocaeli",
                  "1997 yılından bu yana humik asit ve leonardit bazlı ürünler üretmektedir.")
        );
        brandRepository.saveAll(brands);
        log.info(">>> {} marka eklendi.", brands.size());
    }

    // ─────────────────────────────────────────────
    // CATEGORIES
    // ─────────────────────────────────────────────
    private void seedCategories() {
        List<Category> categories = List.of(
            category("Gıda Takviyesi",
                     "Humik asit bazlı doğal gıda takviyeleri.",
                     "https://placehold.co/272x181/74aa4c/ffffff?text=G%C4%B1da+Takviyesi",
                     List.of("Tablet", "Kapsül", "Sıvı", "Saşe", "Prebiyotik")),

            category("Cilt Bakımı",
                     "Humik asit içerikli doğal cilt bakım ürünleri.",
                     "https://placehold.co/272x181/5a8a38/ffffff?text=Cilt+Bak%C4%B1m%C4%B1",
                     List.of("Krem", "Maske", "Sabun", "Şampuan", "Losyon")),

            category("Saç Bakımı",
                     "Saç dökülmesine karşı etkili doğal saç bakım ürünleri.",
                     "https://placehold.co/272x181/3d6225/ffffff?text=Sa%C3%A7+Bak%C4%B1m%C4%B1",
                     List.of("Şampuan", "Saç Maskesi", "Saç Serumu")),

            category("Detoks",
                     "Vücudu arındıran ve bağışıklık sistemini destekleyen detoks ürünleri.",
                     "https://placehold.co/272x181/cd830e/ffffff?text=Detoks",
                     List.of("Sıvı Detoks", "Tablet Detoks", "Paket Programlar"))
        );
        categoryRepository.saveAll(categories);
        log.info(">>> {} kategori eklendi.", categories.size());
    }

    // ─────────────────────────────────────────────
    // PRODUCTS
    // ─────────────────────────────────────────────
    private void seedProducts() {
        List<Product> products = List.of(

            // ── GIDA TAKVİYESİ ──
            product("SERRAVİT Humik Asit Tablet 700mg",
                    "60 tablet/kutu. Güçlü antioksidan özelliğiyle bağışıklık sistemini destekler, " +
                    "ağır metal detoksu sağlar ve enerji metabolizmasını düzenler. " +
                    "AB üretim standartlarında üretilmiştir.",
                    400.0, 500.0, 200,
                    "Gıda Takviyesi", "Tablet",
                    "SERRAVİT",
                    "https://placehold.co/960x1125/74aa4c/ffffff?text=Humik+Asit+Tablet",
                    List.of("detoks", "antioksidan", "bağışıklık", "humik asit", "tablet"),
                    List.of()),

            product("SERRAVİT Humik Asit Kapsül 400mg",
                    "60 kapsül/kutu. Humik asit bazlı doğal gıda takviyesi. " +
                    "Sindirim sistemini destekler, hücre yenilenmesini hızlandırır " +
                    "ve vücudu toksinlerden arındırır.",
                    380.0, 480.0, 180,
                    "Gıda Takviyesi", "Kapsül",
                    "SERRAVİT",
                    "https://placehold.co/960x1125/5a8a38/ffffff?text=Humik+Asit+Kaps%C3%BCl",
                    List.of("detoks", "antioksidan", "bağışıklık", "humik asit", "kapsül"),
                    List.of()),

            product("SERRAVİT Humik Asit Sıvı 100mL",
                    "100mL sıvı gıda takviyesi. Hızlı emilim için sıvı form. " +
                    "Tablet ve kapsülle eşdeğer terapötik özellikler sunar. " +
                    "Günlük detoks programları için idealdir.",
                    450.0, 450.0, 150,
                    "Gıda Takviyesi", "Sıvı",
                    "SERRAVİT",
                    "https://placehold.co/960x1125/3d6225/ffffff?text=Humik+Asit+S%C4%B1v%C4%B1",
                    List.of("detoks", "sıvı", "hızlı emilim", "humik asit"),
                    List.of()),

            product("SERRAVİT Humik Asitli Prebiyotik Saşe 400mg",
                    "20 saşe/kutu. Humik asit ve prebiyotik formülü bir arada. " +
                    "Bağırsak florasını düzenler, sindirim sağlığını iyileştirir " +
                    "ve bağışıklık sistemini güçlendirir.",
                    520.0, 650.0, 120,
                    "Gıda Takviyesi", "Saşe",
                    "SERRAVİT",
                    "https://placehold.co/960x1125/cd830e/ffffff?text=Prebiyotik+Sa%C5%9Fe",
                    List.of("prebiyotik", "sindirim", "bağırsak", "humik asit", "saşe"),
                    List.of()),

            // ── CİLT BAKIMI ──
            product("Humidone Cilt Onarıcı Krem 75mL",
                    "75mL cilt onarıcı krem. Humik asit içeriğiyle cildi besler ve yeniler. " +
                    "Kuru ve hassas ciltler için özel formül. " +
                    "Doğal içerikler, kimyasal katkısız.",
                    300.0, 375.0, 250,
                    "Cilt Bakımı", "Krem",
                    "Humidone",
                    "https://placehold.co/960x1125/74aa4c/ffffff?text=Cilt+Onar%C4%B1c%C4%B1+Krem",
                    List.of("krem", "cilt bakımı", "onarıcı", "humik asit", "doğal"),
                    List.of()),

            product("Humidone Kil Maskesi 150mL",
                    "150mL humik asit katkılı kil maskesi. " +
                    "Gözenekleri temizler, sebum kontrolü sağlar ve cildi arındırır. " +
                    "Tüm cilt tipleri için uygundur.",
                    280.0, 350.0, 180,
                    "Cilt Bakımı", "Maske",
                    "Humidone",
                    "https://placehold.co/960x1125/5a8a38/ffffff?text=Kil+Maskesi",
                    List.of("maske", "kil", "arındırıcı", "humik asit", "gözenek"),
                    List.of()),

            product("Humidone Zeytinyağlı Sabun 100gr",
                    "100gr zeytinyağı ve humik asit içerikli doğal sabun. " +
                    "Cildi nemlendirir ve korur. " +
                    "Sentetik katkı maddesi içermez.",
                    120.0, 120.0, 300,
                    "Cilt Bakımı", "Sabun",
                    "Humidone",
                    "https://placehold.co/960x1125/3d6225/ffffff?text=Do%C4%9Fal+Sabun",
                    List.of("sabun", "zeytinyağı", "doğal", "humik asit", "nemlendirici"),
                    List.of()),

            // ── SAÇ BAKIMI ──
            product("Humidone Siyah Şampuan 250mL",
                    "250mL humik asit içerikli saç şampuanı. " +
                    "Saç dökülmesine karşı etkili formül. " +
                    "Saç köklerini güçlendirir ve saç derisini besler. " +
                    "Sülfat içermez.",
                    200.0, 250.0, 200,
                    "Saç Bakımı", "Şampuan",
                    "Humidone",
                    "https://placehold.co/960x1125/cd830e/ffffff?text=Siyah+%C5%9Eampuan",
                    List.of("şampuan", "saç dökülmesi", "saç bakımı", "humik asit", "sülfatsız"),
                    List.of()),

            // ── DETOKS PAKET ──
            product("SERRAVİT 30 Günlük Detoks Paketi",
                    "1 kutu Tablet (60 tablet) + 1 şişe Sıvı (100mL). " +
                    "Kombine detoks programı. Ağır metalleri uzaklaştırır, " +
                    "bağışıklığı güçlendirir ve enerji seviyesini artırır. " +
                    "Uzman önerisiyle hazırlanmış 30 günlük program.",
                    799.0, 950.0, 80,
                    "Detoks", "Paket Programlar",
                    "SERRAVİT",
                    "https://placehold.co/960x1125/74aa4c/ffffff?text=30+G%C3%BCnl%C3%BCk+Detoks",
                    List.of("detoks", "paket", "program", "humik asit", "bağışıklık"),
                    List.of())
        );

        productRepository.saveAll(products);
        log.info(">>> {} ürün eklendi.", products.size());
    }

    // ─────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────
    private Brand brand(String name, String email, String website, String location, String desc) {
        Brand b = new Brand();
        b.setName(name);
        b.setEmail(email);
        b.setWebsite(website);
        b.setLocation(location);
        b.setDescription(desc);
        b.setStatus("Active");
        return b;
    }

    private Category category(String name, String desc, String image, List<String> children) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(desc);
        c.setImage(image);
        c.setChildren(children);
        return c;
    }

    private Product product(String name, String desc, double price, double originalPrice,
                            int stock, String parentCat, String childCat,
                            String brandName, String image,
                            List<String> tags, List<String> colors) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(price);
        p.setOriginalPrice(originalPrice);
        p.setStockQuantity(stock);
        p.setStatus("Active");
        p.setParentCategory(parentCat);
        p.setChildCategory(childCat);
        p.setCategoryName(parentCat);
        p.setBrandName(brandName);
        p.setImage(image);
        p.setTags(tags);
        p.setColors(colors);
        p.setSku("SRV-" + name.substring(0, Math.min(4, name.length())).toUpperCase()
                             .replace(" ", "").replace("İ", "I")
                 + "-" + (int)(Math.random() * 9000 + 1000));
        return p;
    }
}
