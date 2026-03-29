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
@Order(2) // DataInitializer (admin user) çalıştıktan sonra
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Value("${app.seed-data:false}")
    private boolean seedData;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedData) return;

        log.info(">>> Veri temizleme başlıyor (users/invitations korunuyor)...");
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        log.info(">>> Tablolar temizlendi. Örnek veriler ekleniyor...");

        seedBrands();
        seedCategories();
        seedProducts();

        log.info(">>> Seed tamamlandı. app.seed-data=false yaparak bir daha çalışmamasını sağlayın.");
    }

    // ─────────────────────────────────────────────
    // BRANDS
    // ─────────────────────────────────────────────
    private void seedBrands() {
        List<Brand> brands = List.of(
            brand("Nike",       "nike@brand.com", "https://www.nike.com",    "USA",   "Dünyaca ünlü spor markası."),
            brand("Zara",       "zara@brand.com", "https://www.zara.com",    "Spain", "Hızlı moda perakende markası."),
            brand("Apple",      "apple@brand.com","https://www.apple.com",   "USA",   "Yenilikçi teknoloji ürünleri."),
            brand("L'Oréal",    "loreal@brand.com","https://www.loreal.com", "France","Kozmetik ve güzellik markası."),
            brand("Adidas",     "adidas@brand.com","https://www.adidas.com", "Germany","Küresel spor giyim markası.")
        );
        brandRepository.saveAll(brands);
        log.info(">>> {} marka eklendi.", brands.size());
    }

    // ─────────────────────────────────────────────
    // CATEGORIES
    // ─────────────────────────────────────────────
    private void seedCategories() {
        List<Category> categories = List.of(
            category("Giyim",       "Erkek ve kadın giyim ürünleri.",
                     "https://placehold.co/272x181/4f46e5/ffffff?text=Giyim",
                     List.of("T-Shirt", "Ceket", "Elbise", "Kazak", "Pantolon")),

            category("Kozmetik",    "Cilt bakım ve güzellik ürünleri.",
                     "https://placehold.co/272x181/ec4899/ffffff?text=Kozmetik",
                     List.of("Krem", "Serum", "Parfüm", "Makyaj", "Güneş Koruyucu")),

            category("Elektronik",  "Akıllı cihazlar ve teknoloji ürünleri.",
                     "https://placehold.co/272x181/0ea5e9/ffffff?text=Elektronik",
                     List.of("Telefon", "Laptop", "Tablet", "Kulaklık", "Akıllı Saat")),

            category("Spor",        "Spor ekipman ve giyim ürünleri.",
                     "https://placehold.co/272x181/22c55e/ffffff?text=Spor",
                     List.of("Spor Ayakkabı", "Spor Giyim", "Fitness", "Outdoor", "Bisiklet")),

            category("Ev & Yaşam",  "Ev dekorasyon ve yaşam ürünleri.",
                     "https://placehold.co/272x181/f59e0b/ffffff?text=Ev+%26+Yasam",
                     List.of("Dekor", "Aydınlatma", "Mutfak", "Banyo", "Tekstil"))
        );
        categoryRepository.saveAll(categories);
        log.info(">>> {} kategori eklendi.", categories.size());
    }

    // ─────────────────────────────────────────────
    // PRODUCTS
    // ─────────────────────────────────────────────
    private void seedProducts() {
        List<Product> products = List.of(

            // ── GİYİM ──
            product("Oversize Pamuklu T-Shirt",
                    "Yüksek kalite %100 pamuk kumaştan üretilmiş rahat kesim t-shirt.",
                    299.99, 299.99, 150, "Giyim", "Ceket",
                    "Zara", "https://placehold.co/960x1125/e5e7eb/374151?text=T-Shirt",
                    List.of("giyim", "t-shirt", "pamuk"), List.of("Beyaz", "Siyah", "Gri")),

            product("Deri Biker Ceket",
                    "Klasik biker tasarımlı suni deri ceket. Şık ve dayanıklı.",
                    1299.99, 1599.99, 45, "Giyim", "Ceket",
                    "Zara", "https://placehold.co/960x1125/1f2937/f9fafb?text=Ceket",
                    List.of("giyim", "ceket", "deri"), List.of("Siyah", "Kahverengi")),

            product("Midi Floral Elbise",
                    "Çiçek desenli midi boy elbise. Yaz sezonu için ideal.",
                    899.99, 1199.99, 60, "Giyim", "Elbise",
                    "Zara", "https://placehold.co/960x1125/fce7f3/be185d?text=Elbise",
                    List.of("giyim", "elbise", "yaz"), List.of("Pembe", "Mavi", "Sarı")),

            product("Yün Blend Kazak",
                    "Yumuşak yün karışımı örme kazak. Kış için mükemmel tercih.",
                    649.99, 649.99, 80, "Giyim", "Kazak",
                    "Zara", "https://placehold.co/960x1125/dbeafe/1e40af?text=Kazak",
                    List.of("giyim", "kazak", "kış"), List.of("Mavi", "Krem", "Haki")),

            // ── KOZMETİK ──
            product("Hyalüronik Asit Serumu",
                    "Yoğun nem içeren hyalüronik asit serumu. Cilt dolgunluğu için.",
                    399.99, 499.99, 200, "Kozmetik", "Serum",
                    "L'Oréal", "https://placehold.co/960x1125/fdf2f8/9d174d?text=Serum",
                    List.of("kozmetik", "serum", "nem"), List.of()),

            product("Nemlendirici Gündüz Kremi SPF30",
                    "SPF 30 koruma faktörlü günlük nemlendirici krem.",
                    259.99, 259.99, 300, "Kozmetik", "Krem",
                    "L'Oréal", "https://placehold.co/960x1125/fff7ed/92400e?text=Krem",
                    List.of("kozmetik", "krem", "spf"), List.of()),

            product("Çiçek Notası Eau de Parfum 50ml",
                    "Çiçek ve misk notaları içeren kadın parfümü.",
                    749.99, 899.99, 80, "Kozmetik", "Parfüm",
                    "L'Oréal", "https://placehold.co/960x1125/ede9fe/5b21b6?text=Parfüm",
                    List.of("kozmetik", "parfüm", "çiçek"), List.of()),

            // ── ELEKTRONİK ──
            product("Akıllı Telefon 128GB",
                    "6.7 inç AMOLED ekran, 128GB depolama, 5000mAh batarya.",
                    12999.99, 14999.99, 30, "Elektronik", "Telefon",
                    "Apple", "https://placehold.co/960x1125/f0f9ff/0369a1?text=Telefon",
                    List.of("elektronik", "telefon", "akıllı"), List.of("Siyah", "Beyaz", "Mor")),

            product("Kablosuz Bluetooth Kulaklık",
                    "Aktif gürültü engelleme özellikli premium kablosuz kulaklık.",
                    2499.99, 2999.99, 50, "Elektronik", "Kulaklık",
                    "Apple", "https://placehold.co/960x1125/f7f7f7/111827?text=Kulaklık",
                    List.of("elektronik", "kulaklık", "bluetooth"), List.of("Siyah", "Gümüş")),

            product("Ultra İnce Laptop 14\"",
                    "Intel Core i7, 16GB RAM, 512GB SSD. Taşınabilir iş laptopı.",
                    34999.99, 34999.99, 15, "Elektronik", "Laptop",
                    "Apple", "https://placehold.co/960x1125/ecfdf5/065f46?text=Laptop",
                    List.of("elektronik", "laptop", "bilgisayar"), List.of("Gümüş", "Uzay Grisi")),

            // ── SPOR ──
            product("Koşu Ayakkabısı",
                    "Hafif taban ve yüksek nefes alabilirlik özellikli koşu ayakkabısı.",
                    1899.99, 2299.99, 90, "Spor", "Spor Ayakkabı",
                    "Nike", "https://placehold.co/960x1125/fff1f2/9f1239?text=Ayakkabı",
                    List.of("spor", "ayakkabı", "koşu"), List.of("Siyah/Beyaz", "Lacivert/Turuncu", "Gri")),

            product("Dry-Fit Spor Tayt",
                    "Nem çekici teknoloji ile yüksek performanslı spor taytı.",
                    549.99, 549.99, 120, "Spor", "Spor Giyim",
                    "Nike", "https://placehold.co/960x1125/f0fdf4/166534?text=Tayt",
                    List.of("spor", "tayt", "fitness"), List.of("Siyah", "Lacivert", "Koyu Yeşil")),

            product("Yoga Matı 6mm",
                    "Anti-slip yüzeyli, 6mm kalınlığında TPE yoga matı.",
                    449.99, 599.99, 70, "Spor", "Fitness",
                    "Adidas", "https://placehold.co/960x1125/ecfdf5/14532d?text=Yoga+Mat",
                    List.of("spor", "yoga", "fitness"), List.of("Mor", "Mavi", "Pembe")),

            // ── EV & YAŞAM ──
            product("Dekoratif Seramik Vazo",
                    "El yapımı seramik vazo. Modern ev dekorasyonu için ideal.",
                    349.99, 349.99, 55, "Ev & Yaşam", "Dekor",
                    "Zara", "https://placehold.co/960x1125/fefce8/713f12?text=Vazo",
                    List.of("ev", "dekor", "seramik"), List.of("Bej", "Terrakota", "Beyaz")),

            product("LED Masa Lambası",
                    "Dokunmatik dim özellikli, 3 renk sıcaklığı ayarı olan LED lamba.",
                    699.99, 899.99, 40, "Ev & Yaşam", "Aydınlatma",
                    "Apple", "https://placehold.co/960x1125/fffbeb/78350f?text=Lamba",
                    List.of("ev", "aydınlatma", "led"), List.of("Beyaz", "Siyah"))
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
        p.setSku("SKU-" + name.substring(0, Math.min(4, name.length())).toUpperCase().replace(" ", "")
                 + "-" + (int)(Math.random() * 9000 + 1000));
        return p;
    }
}
