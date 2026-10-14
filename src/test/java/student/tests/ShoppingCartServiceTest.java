package student.tests;

import am.gortsup.testing.exception.InvalidCouponException;
import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.CartItem;
import am.gortsup.testing.model.Coupon;
import am.gortsup.testing.model.DiscountType;
import am.gortsup.testing.model.Product;
import am.gortsup.testing.model.ProductCategory;
import am.gortsup.testing.service.ShoppingCartService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;

public class ShoppingCartServiceTest {

    private ShoppingCartService carts;
    private Product product;
    private final String cartId = "cart-1";

    @BeforeMethod
    public void createCartAndProduct() {
        carts = new ShoppingCartService();
        product = new Product("P-1", "Coffee", ProductCategory.GROCERY, 20.00);
    }

    private Coupon coupon(String code, DiscountType type, double value, double minAmount,
                          LocalDate expiry, int usageLimit) {
        return new Coupon(code, type, value, minAmount, expiry, usageLimit);
    }

    // Equivalence partition: adding an existing product increments its quantity rather than duplicating it.
    @Test
    public void testAddItem_TwiceForSameProduct_ShouldCombineQuantity() {
        carts.addItem(cartId, product, 2);
        carts.addItem(cartId, product, 3);
        Assert.assertEquals(carts.getItemCount(cartId), 5);
        Assert.assertEquals(carts.getCartItems(cartId).size(), 1);
    }

    // Quantity boundary: zero items cannot be added to a cart.
    @Test(expectedExceptions = ValidationException.class)
    public void testAddItem_WithZeroQuantity_ShouldThrow() {
        carts.addItem(cartId, product, 0);
    }

    // State partition: inactive products cannot be added to a cart.
    @Test(expectedExceptions = ValidationException.class)
    public void testAddItem_WithInactiveProduct_ShouldThrow() {
        product.setActive(false);
        carts.addItem(cartId, product, 1);
    }

    // Cart arithmetic: subtotal is unit price multiplied by quantity.
    @Test
    public void testCalculateSubtotal_WithMultipleUnits_ShouldMultiplyPriceByQuantity() {
        carts.addItem(cartId, product, 5);
        Assert.assertEquals(carts.calculateSubtotal(cartId), 100.0, 0.0001);
    }

    // State transition: an existing product quantity can be updated.
    @Test
    public void testUpdateQuantity_WithExistingProduct_ShouldSetNewQuantity() {
        carts.addItem(cartId, product, 2);
        carts.updateQuantity(cartId, product.getId(), 4);
        Assert.assertEquals(carts.getItemCount(cartId), 4);
    }

    // Quantity boundary: the maximum permitted quantity is 99 per product.
    @Test
    public void testAddItem_AtMaximumQuantity_ShouldSucceed() {
        carts.addItem(cartId, product, 99);
        Assert.assertEquals(carts.getItemCount(cartId), 99);
    }

    // Quantity boundary: quantities above 99 are rejected.
    @Test(expectedExceptions = ValidationException.class)
    public void testAddItem_AboveMaximumQuantity_ShouldThrow() {
        carts.addItem(cartId, product, 100);
    }

    // State transition: removing an existing item removes it from the cart.
    @Test
    public void testRemoveItem_WithExistingProduct_ShouldEmptyCart() {
        carts.addItem(cartId, product, 2);
        carts.removeItem(cartId, product.getId());
        Assert.assertEquals(carts.getItemCount(cartId), 0);
    }

    // Resource boundary: removing an absent item reports a not-found error.
    @Test(expectedExceptions = ResourceNotFoundException.class)
    public void testRemoveItem_WithMissingProduct_ShouldThrow() {
        carts.removeItem(cartId, "missing");
    }

    // Coupon equivalence partition: percentage savings reduce the taxable total.
    @Test
    public void testCalculateTotal_WithPercentageCoupon_ShouldTaxDiscountedSubtotal() {
        carts.addItem(cartId, product, 5);
        Coupon tenPercent = coupon("TEN", DiscountType.PERCENTAGE, 10, 50,
                LocalDate.of(2026, 12, 31), 10);
        carts.applyCoupon(cartId, tenPercent, LocalDate.of(2026, 10, 14));

        Assert.assertEquals(carts.calculateDiscountAmount(cartId), 10.0, 0.0001);
        Assert.assertEquals(carts.calculateTotal(cartId, 10), 99.0, 0.0001);
    }

    // Coupon boundary: a fixed discount cannot exceed the cart subtotal.
    @Test
    public void testCalculateDiscountAmount_WithFixedDiscountAboveSubtotal_ShouldCapAtSubtotal() {
        carts.addItem(cartId, product, 1);
        Coupon fixed = coupon("BIG", DiscountType.FIXED_AMOUNT, 100, 0,
                LocalDate.of(2026, 12, 31), 1);
        carts.applyCoupon(cartId, fixed, LocalDate.of(2026, 10, 14));
        Assert.assertEquals(carts.calculateDiscountAmount(cartId), 20.0, 0.0001);
    }

    // Coupon state: a coupon expiring before the current date is rejected.
    @Test(expectedExceptions = InvalidCouponException.class)
    public void testApplyCoupon_WithExpiredCoupon_ShouldThrow() {
        carts.addItem(cartId, product, 1);
        Coupon expired = coupon("OLD", DiscountType.PERCENTAGE, 10, 0,
                LocalDate.of(2026, 10, 13), 10);
        carts.applyCoupon(cartId, expired, LocalDate.of(2026, 10, 14));
    }

    // Usage boundary: a coupon that has reached its redemption limit is rejected.
    @Test(expectedExceptions = InvalidCouponException.class)
    public void testApplyCoupon_AtUsageLimit_ShouldThrow() {
        carts.addItem(cartId, product, 1);
        Coupon used = coupon("USED", DiscountType.PERCENTAGE, 10, 0,
                LocalDate.of(2026, 12, 31), 1);
        used.incrementTimesUsed();
        carts.applyCoupon(cartId, used, LocalDate.of(2026, 10, 14));
    }

    // Minimum-spend boundary: a cart below the coupon threshold cannot apply the coupon.
    @Test(expectedExceptions = InvalidCouponException.class)
    public void testApplyCoupon_BelowMinimumSpend_ShouldThrow() {
        carts.addItem(cartId, product, 1);
        Coupon coupon = coupon("MIN", DiscountType.PERCENTAGE, 10, 100,
                LocalDate.of(2026, 12, 31), 10);
        carts.applyCoupon(cartId, coupon, LocalDate.of(2026, 10, 14));
    }

    // State transition: removing a coupon clears the cart's applied-coupon value.
    @Test
    public void testRemoveCoupon_AfterApplyingCoupon_ShouldClearCoupon() {
        carts.addItem(cartId, product, 1);
        Coupon coupon = coupon("TEN", DiscountType.PERCENTAGE, 10, 0,
                LocalDate.of(2026, 12, 31), 10);
        carts.applyCoupon(cartId, coupon, LocalDate.of(2026, 10, 14));
        carts.removeCoupon(cartId);
        Assert.assertTrue(carts.getAppliedCoupon(cartId).isEmpty());
    }

    // State transition: clearing a cart removes all items and its applied coupon.
    @Test
    public void testClearCart_ShouldRemoveItemsAndAppliedCoupon() {
        carts.addItem(cartId, product, 2);
        carts.applyCoupon(cartId, coupon("TEN", DiscountType.PERCENTAGE, 10, 0,
                LocalDate.of(2026, 12, 31), 10), LocalDate.of(2026, 10, 14));
        carts.clearCart(cartId);
        Assert.assertTrue(carts.getCartItems(cartId).isEmpty());
        Assert.assertTrue(carts.getAppliedCoupon(cartId).isEmpty());
    }

    // Empty-cart boundary: subtotal and total are zero when no items remain.
    @Test
    public void testCalculateTotal_ForEmptyCart_ShouldReturnZero() {
        Assert.assertEquals(carts.calculateSubtotal(cartId), 0.0, 0.0001);
        Assert.assertEquals(carts.calculateTotal(cartId, 5), 0.0, 0.0001);
    }

    // Collection contract: callers cannot mutate the cart through its returned item list.
    @Test
    public void testGetCartItems_ShouldReturnUnmodifiableList() {
        carts.addItem(cartId, product, 1);
        List<CartItem> items = carts.getCartItems(cartId);
        Assert.assertThrows(UnsupportedOperationException.class, items::clear);
    }

    // Input boundary: cart identifiers cannot be blank.
    @Test(expectedExceptions = ValidationException.class)
    public void testGetItemCount_WithBlankCartId_ShouldThrow() {
        carts.getItemCount(" ");
    }
}
