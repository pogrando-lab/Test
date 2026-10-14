package student.tests;

import am.gortsup.testing.exception.InsufficientStockException;
import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.Product;
import am.gortsup.testing.model.ProductCategory;
import am.gortsup.testing.service.InventoryService;
import am.gortsup.testing.service.ProductCatalogService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ProductCatalogAndInventoryTest {

    private ProductCatalogService catalog;
    private InventoryService inventory;

    @BeforeMethod
    public void createIsolatedServices() {
        catalog = new ProductCatalogService();
        inventory = new InventoryService();
    }

    private Product add(String id, String name, ProductCategory category, double price) {
        return catalog.addProduct(new Product(id, name, category, price));
    }

    // Equivalence partition: a valid product can be added and retrieved by its ID.
    @Test
    public void testAddProduct_WithValidProduct_ShouldBeRetrievable() {
        Product product = add("P-1", "Keyboard", ProductCategory.ELECTRONICS, 49.99);
        Assert.assertEquals(catalog.getProductById("P-1"), product);
    }

    // Uniqueness rule: product identifiers cannot be registered twice.
    @Test(expectedExceptions = ValidationException.class)
    public void testAddProduct_WithDuplicateId_ShouldThrow() {
        add("P-1", "First", ProductCategory.BOOKS, 10);
        add("P-1", "Second", ProductCategory.BOOKS, 20);
    }

    // Boundary: negative product prices are invalid.
    @Test(expectedExceptions = ValidationException.class)
    public void testAddProduct_WithNegativePrice_ShouldThrow() {
        add("P-1", "Invalid", ProductCategory.BOOKS, -1);
    }

    // Boundary: zero is a valid non-negative product price.
    @Test
    public void testAddProduct_WithZeroPrice_ShouldSucceed() {
        Product product = add("P-1", "Free Sample", ProductCategory.BEAUTY, 0);
        Assert.assertEquals(product.getPrice(), 0.0, 0.0001);
    }

    // Monetary boundary: updated prices are rounded to two decimal places.
    @Test
    public void testUpdateProductPrice_WithFractionalCents_ShouldRoundHalfUp() {
        add("P-1", "Book", ProductCategory.BOOKS, 10);
        catalog.updateProductPrice("P-1", 12.345);
        Assert.assertEquals(catalog.getProductById("P-1").getPrice(), 12.35, 0.0001);
    }

    // Invalid partition: a negative updated price is rejected.
    @Test(expectedExceptions = ValidationException.class)
    public void testUpdateProductPrice_WithNegativePrice_ShouldThrow() {
        add("P-1", "Book", ProductCategory.BOOKS, 10);
        catalog.updateProductPrice("P-1", -0.01);
    }

    // Category partition: category queries include products in the requested category.
    @Test
    public void testGetProductsByCategory_WithMatchingProduct_ShouldReturnProduct() {
        Product book = add("P-1", "Novel", ProductCategory.BOOKS, 15);
        add("P-2", "Lamp", ProductCategory.HOME, 20);
        Assert.assertEquals(catalog.getProductsByCategory(ProductCategory.BOOKS), List.of(book));
    }

    // Search partition: name matching is partial and case-insensitive.
    @Test
    public void testSearchProductsByName_WithPartialDifferentCase_ShouldFindProduct() {
        Product product = add("P-1", "Mechanical Keyboard", ProductCategory.ELECTRONICS, 50);
        Assert.assertTrue(catalog.searchProductsByName("KEYBOARD").contains(product));
    }

    // State partition: inactive products are omitted from catalog name searches.
    @Test
    public void testSearchProductsByName_WithInactiveProduct_ShouldNotFindProduct() {
        Product product = add("P-1", "Mechanical Keyboard", ProductCategory.ELECTRONICS, 50);
        catalog.deactivateProduct(product.getId());
        Assert.assertFalse(catalog.searchProductsByName("keyboard").contains(product));
    }

    // Price-range boundary: products at either endpoint are included.
    @Test
    public void testGetProductsInPriceRange_WithEndpointPrices_ShouldIncludeBoth() {
        Product low = add("P-1", "Low", ProductCategory.HOME, 10);
        Product high = add("P-2", "High", ProductCategory.HOME, 20);
        add("P-3", "Outside", ProductCategory.HOME, 21);

        List<Product> matches = catalog.getProductsInPriceRange(10, 20);
        Assert.assertTrue(matches.contains(low));
        Assert.assertTrue(matches.contains(high));
        Assert.assertEquals(matches.size(), 2);
    }

    // Invalid range: a minimum price greater than the maximum is rejected.
    @Test(expectedExceptions = ValidationException.class)
    public void testGetProductsInPriceRange_WithInvertedBounds_ShouldThrow() {
        catalog.getProductsInPriceRange(20, 10);
    }

    // Category discount: only matching products receive the percentage reduction.
    @Test
    public void testApplyCategoryDiscount_ShouldAffectOnlyProductsInCategory() {
        Product book = add("P-1", "Book", ProductCategory.BOOKS, 19.99);
        Product lamp = add("P-2", "Lamp", ProductCategory.HOME, 20);

        Assert.assertEquals(catalog.applyCategoryDiscount(ProductCategory.BOOKS, 10), 1);
        Assert.assertEquals(book.getPrice(), 17.99, 0.0001);
        Assert.assertEquals(lamp.getPrice(), 20.0, 0.0001);
    }

    // State transition: an activated product appears in the active-product list.
    @Test
    public void testActivateProduct_ShouldRestoreProductToActiveCatalog() {
        Product product = add("P-1", "Item", ProductCategory.HOME, 10);
        catalog.deactivateProduct(product.getId());
        catalog.activateProduct(product.getId());
        Assert.assertTrue(catalog.getAllActiveProducts().contains(product));
    }

    // Collection contract: the active catalog omits deactivated products.
    @Test
    public void testGetAllActiveProducts_ShouldExcludeInactiveProducts() {
        Product active = add("P-1", "Active", ProductCategory.HOME, 10);
        Product inactive = add("P-2", "Inactive", ProductCategory.HOME, 10);
        catalog.deactivateProduct(inactive.getId());
        Assert.assertEquals(catalog.getAllActiveProducts(), List.of(active));
    }

    // Resource boundary: an unknown product ID raises the domain not-found exception.
    @Test(expectedExceptions = ResourceNotFoundException.class)
    public void testGetProductById_WithUnknownId_ShouldThrow() {
        catalog.getProductById("missing");
    }

    // Inventory invariant: reserving stock moves units from available to reserved.
    @Test
    public void testReserveStock_ShouldMoveQuantityToReservedBalance() {
        inventory.setStock("P-1", 10);
        inventory.reserveStock("P-1", 4);
        Assert.assertEquals(inventory.getStock("P-1"), 6);
        Assert.assertEquals(inventory.getReservedStock("P-1"), 4);
    }

    // Availability boundary: exactly the remaining quantity is in stock; one extra is not.
    @Test
    public void testIsInStock_ShouldCompareRequestedQuantityToAvailableBalance() {
        inventory.setStock("P-1", 10);
        inventory.reserveStock("P-1", 4);
        Assert.assertTrue(inventory.isInStock("P-1", 6));
        Assert.assertFalse(inventory.isInStock("P-1", 7));
    }

    // Inventory transition: deduction permanently removes units from reserved stock.
    @Test
    public void testDeductStock_ShouldDecreaseReservedBalance() {
        inventory.setStock("P-1", 10);
        inventory.reserveStock("P-1", 4);
        inventory.deductStock("P-1", 3);
        Assert.assertEquals(inventory.getReservedStock("P-1"), 1);
    }

    // Insufficient-stock partition: reservation cannot exceed available quantity.
    @Test(expectedExceptions = InsufficientStockException.class)
    public void testReserveStock_AboveAvailableQuantity_ShouldThrowInsufficientStock() {
        inventory.setStock("P-1", 2);
        inventory.reserveStock("P-1", 3);
    }

    // Atomicity: simultaneous reservations for the final unit allow exactly one caller to succeed.
    @Test
    public void testReserveStock_ConcurrentRequestsForLastUnit_ShouldAllowOnlyOneReservation()
            throws Exception {
        inventory.setStock("P-1", 1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            java.util.concurrent.Callable<Boolean> reservation = () -> {
                ready.countDown();
                start.await();
                try {
                    inventory.reserveStock("P-1", 1);
                    return true;
                } catch (InsufficientStockException expected) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(reservation);
            Future<Boolean> second = executor.submit(reservation);

            Assert.assertTrue(ready.await(5, TimeUnit.SECONDS), "Both reservation attempts should be ready");
            start.countDown();
            Assert.assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            Assert.assertEquals(inventory.getStock("P-1"), 0);
            Assert.assertEquals(inventory.getReservedStock("P-1"), 1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    // Inventory transition: releasing reserved units returns them to available stock.
    @Test
    public void testReleaseStock_ShouldReturnUnitsToAvailableBalance() {
        inventory.setStock("P-1", 4);
        inventory.reserveStock("P-1", 3);
        inventory.releaseStock("P-1", 2);
        Assert.assertEquals(inventory.getStock("P-1"), 3);
        Assert.assertEquals(inventory.getReservedStock("P-1"), 1);
    }

    // Inventory transition: restocking adds units to available stock.
    @Test
    public void testRestock_ShouldIncreaseAvailableBalance() {
        inventory.setStock("P-1", 4);
        inventory.restock("P-1", 5);
        Assert.assertEquals(inventory.getStock("P-1"), 9);
    }

    // Default behavior: a product not listed in inventory has no available units.
    @Test
    public void testGetStock_ForUnlistedProduct_ShouldReturnZero() {
        Assert.assertEquals(inventory.getStock("unknown"), 0);
    }

    // Boundary: negative stock counts are rejected.
    @Test(expectedExceptions = ValidationException.class)
    public void testSetStock_WithNegativeQuantity_ShouldThrow() {
        inventory.setStock("P-1", -1);
    }
}
