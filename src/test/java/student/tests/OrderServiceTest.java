package student.tests;

import am.gortsup.testing.exception.IllegalOrderStateException;
import am.gortsup.testing.exception.PaymentFailedException;
import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.Address;
import am.gortsup.testing.model.CartItem;
import am.gortsup.testing.model.Coupon;
import am.gortsup.testing.model.DiscountType;
import am.gortsup.testing.model.Order;
import am.gortsup.testing.model.OrderStatus;
import am.gortsup.testing.model.Product;
import am.gortsup.testing.model.ProductCategory;
import am.gortsup.testing.service.OrderService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;

public class OrderServiceTest {

    private OrderService orders;
    private List<CartItem> items;
    private Address address;

    @BeforeMethod
    public void createOrderFixture() {
        orders = new OrderService();
        Product product = new Product("P-1", "Keyboard", ProductCategory.ELECTRONICS, 120.0);
        items = List.of(new CartItem(product, 1));
        address = new Address("45 Abovyan St", "Yerevan", "Kentron", "0001", "AM");
    }

    private Order newPendingOrder() {
        return orders.createOrder("user-1", items, address, 0, null);
    }

    private Order newPaidOrder() {
        Order order = newPendingOrder();
        orders.payOrder(order.getId(), "49927398716", order.getTotal());
        return order;
    }

    // Lifecycle start: a valid order is created with PENDING status and a generated ID.
    @Test
    public void testCreateOrder_WithValidData_ShouldStartPending() {
        Order order = orders.createOrder("user-1", items, address, 0, null);
        Assert.assertNotNull(order.getId());
        Assert.assertEquals(order.getStatus(), OrderStatus.PENDING);
    }

    // Financial calculation: tax is added to the item subtotal.
    @Test
    public void testCreateOrder_WithTax_ShouldCalculateSubtotalTaxAndTotal() {
        Order order = orders.createOrder("user-1", items, address, 20.0, null);
        Assert.assertEquals(order.getSubtotal(), 120.0, 0.0001);
        Assert.assertEquals(order.getTax(), 24.0, 0.0001);
        Assert.assertEquals(order.getTotal(), 144.0, 0.0001);
    }

    // Input boundary: an order must contain at least one item.
    @Test(expectedExceptions = ValidationException.class)
    public void testCreateOrder_WithNoItems_ShouldThrow() {
        orders.createOrder("user-1", List.of(), address, 0, null);
    }

    // Input boundary: the shipping address is required to create an order.
    @Test(expectedExceptions = ValidationException.class)
    public void testCreateOrder_WithNullAddress_ShouldThrow() {
        orders.createOrder("user-1", items, null, 0, null);
    }

    // Coupon calculation: a valid fixed coupon reduces the order total and increments its use count.
    @Test
    public void testCreateOrder_WithCoupon_ShouldApplyDiscountAndTrackUsage() {
        Coupon coupon = new Coupon("SAVE", DiscountType.FIXED_AMOUNT, 20, 0,
                LocalDate.of(2026, 12, 31), 10);
        Order order = orders.createOrder("user-1", items, address, 10, coupon);
        Assert.assertEquals(order.getDiscount(), 20.0, 0.0001);
        Assert.assertEquals(order.getTotal(), 110.0, 0.0001);
        Assert.assertEquals(coupon.getTimesUsed(), 1);
    }

    // Successful payment: a Luhn-valid card and exact total move the order to PAID.
    @Test
    public void testPayOrder_WithValidCardAndExactAmount_ShouldSetPaidStatus() {
        Order order = newPendingOrder();
        orders.payOrder(order.getId(), "49927398716", 120.0);
        Assert.assertEquals(order.getStatus(), OrderStatus.PAID);
    }

    // Payment validation: a card with an invalid Luhn checksum is rejected.
    @Test(expectedExceptions = PaymentFailedException.class)
    public void testPayOrder_WithInvalidLuhnCard_ShouldThrowPaymentFailed() {
        Order order = newPendingOrder();
        orders.payOrder(order.getId(), "1234567812345678", 120.0);
    }

    // Payment boundary: payment amount must match the total order amount.
    @Test(expectedExceptions = PaymentFailedException.class)
    public void testPayOrder_WithIncorrectAmount_ShouldThrowPaymentFailed() {
        Order order = newPendingOrder();
        orders.payOrder(order.getId(), "49927398716", 119.0);
    }

    // State-machine rule: only paid or processing orders can be shipped.
    @Test(expectedExceptions = IllegalOrderStateException.class)
    public void testShipOrder_WhilePending_ShouldThrowIllegalState() {
        Order order = newPendingOrder();
        orders.shipOrder(order.getId(), "TRACK-1");
    }

    // Input boundary: shipping requires a nonblank tracking number.
    @Test(expectedExceptions = ValidationException.class)
    public void testShipOrder_WithBlankTrackingNumber_ShouldThrow() {
        Order order = newPaidOrder();
        orders.shipOrder(order.getId(), " ");
    }

    // State transition: a paid order can be shipped and receives a normalized tracking number.
    @Test
    public void testShipOrder_AfterPayment_ShouldSetShippedStatusAndTrackingNumber() {
        Order order = newPaidOrder();
        orders.shipOrder(order.getId(), " TRACK-1 ");
        Assert.assertEquals(order.getStatus(), OrderStatus.SHIPPED);
        Assert.assertEquals(order.getTrackingNumber(), "TRACK-1");
    }

    // State-machine rule: delivery is only allowed after shipment.
    @Test(expectedExceptions = IllegalOrderStateException.class)
    public void testDeliverOrder_BeforeShipment_ShouldThrowIllegalState() {
        orders.deliverOrder(newPendingOrder().getId());
    }

    // State transition: a shipped order can be marked delivered.
    @Test
    public void testDeliverOrder_AfterShipment_ShouldSetDeliveredStatus() {
        Order order = newPaidOrder();
        orders.shipOrder(order.getId(), "TRACK-2");
        orders.deliverOrder(order.getId());
        Assert.assertEquals(order.getStatus(), OrderStatus.DELIVERED);
    }

    // State transition: a pending order may be cancelled before payment.
    @Test
    public void testCancelOrder_WhenPending_ShouldSetCancelledStatus() {
        Order order = newPendingOrder();
        orders.cancelOrder(order.getId());
        Assert.assertEquals(order.getStatus(), OrderStatus.CANCELLED);
    }

    // State transition: a paid order may be cancelled before shipment.
    @Test
    public void testCancelOrder_WhenPaid_ShouldSetCancelledStatus() {
        Order order = newPaidOrder();
        orders.cancelOrder(order.getId());
        Assert.assertEquals(order.getStatus(), OrderStatus.CANCELLED);
    }

    // State-machine rule: a delivered order cannot be cancelled.
    @Test(expectedExceptions = IllegalOrderStateException.class)
    public void testCancelOrder_WhenDelivered_ShouldThrowIllegalState() {
        Order order = newPaidOrder();
        orders.shipOrder(order.getId(), "TRACK-3");
        orders.deliverOrder(order.getId());
        orders.cancelOrder(order.getId());
    }

    // Refund transition: a positive refund not exceeding total marks a paid order refunded.
    @Test
    public void testRefundOrder_WithFullOrderAmount_ShouldSetRefundedStatus() {
        Order order = newPaidOrder();
        orders.refundOrder(order.getId(), order.getTotal());
        Assert.assertEquals(order.getStatus(), OrderStatus.REFUNDED);
    }

    // Refund boundary: zero refund amounts are invalid.
    @Test(expectedExceptions = ValidationException.class)
    public void testRefundOrder_WithZeroAmount_ShouldThrow() {
        orders.refundOrder(newPaidOrder().getId(), 0);
    }

    // Refund boundary: refunds cannot exceed the order's total.
    @Test(expectedExceptions = ValidationException.class)
    public void testRefundOrder_AboveOrderTotal_ShouldThrow() {
        orders.refundOrder(newPaidOrder().getId(), 121);
    }

    // State-machine rule: an unpaid pending order cannot be refunded.
    @Test(expectedExceptions = IllegalOrderStateException.class)
    public void testRefundOrder_WhilePending_ShouldThrowIllegalState() {
        orders.refundOrder(newPendingOrder().getId(), 10);
    }

    // Resource lookup: orders are retrieved by their generated ID.
    @Test
    public void testGetOrder_WithExistingId_ShouldReturnOrder() {
        Order order = newPendingOrder();
        Assert.assertEquals(orders.getOrder(order.getId()), order);
    }

    // Query behavior: user and status filters return only matching orders.
    @Test
    public void testOrderQueries_ShouldFilterByUserAndStatus() {
        Order pending = newPendingOrder();
        Order paid = newPendingOrder();
        orders.payOrder(paid.getId(), "49927398716", 120);

        Assert.assertEquals(orders.getOrdersByUser("user-1").size(), 2);
        Assert.assertEquals(orders.getOrdersByStatus(OrderStatus.PAID), List.of(paid));
        Assert.assertTrue(orders.getOrdersByStatus(OrderStatus.PENDING).contains(pending));
    }

    // Resource boundary: unknown order identifiers raise the domain not-found exception.
    @Test(expectedExceptions = ResourceNotFoundException.class)
    public void testGetOrder_WithUnknownId_ShouldThrow() {
        orders.getOrder("unknown");
    }
}
