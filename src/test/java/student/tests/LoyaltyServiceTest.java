package student.tests;

import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.LoyaltyTier;
import am.gortsup.testing.service.LoyaltyService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LoyaltyServiceTest {

    private LoyaltyService loyalty;

    @BeforeMethod
    public void createIsolatedLoyaltyLedger() {
        loyalty = new LoyaltyService();
    }

    // Equivalence partition: positive earned points increase spendable and lifetime totals.
    @Test
    public void testAddPoints_WithPositiveAmount_ShouldIncreaseBalances() {
        loyalty.addPoints("user-1", 1200);
        Assert.assertEquals(loyalty.getPoints("user-1"), 1200);
        Assert.assertEquals(loyalty.getLifetimePoints("user-1"), 1200);
    }

    // Boundary: zero points cannot be earned.
    @Test(expectedExceptions = ValidationException.class)
    public void testAddPoints_WithZeroAmount_ShouldThrow() {
        loyalty.addPoints("user-1", 0);
    }

    // Equivalence partition: redeeming points decreases spendable balance.
    @Test
    public void testRedeemPoints_WithSufficientBalance_ShouldDecreaseSpendableBalance() {
        loyalty.addPoints("user-1", 1500);
        loyalty.redeemPoints("user-1", 300);
        Assert.assertEquals(loyalty.getPoints("user-1"), 1200);
    }

    // Business rule: redemption does not reduce cumulative lifetime points.
    @Test
    public void testRedeemPoints_ShouldPreserveLifetimeTotal() {
        loyalty.addPoints("user-1", 1500);
        loyalty.redeemPoints("user-1", 300);
        Assert.assertEquals(loyalty.getLifetimePoints("user-1"), 1500);
    }

    // Boundary: negative redemption amounts are invalid.
    @Test(expectedExceptions = ValidationException.class)
    public void testRedeemPoints_WithNegativeAmount_ShouldThrow() {
        loyalty.redeemPoints("user-1", -1);
    }

    // Balance boundary: a user cannot redeem more points than are available.
    @Test(expectedExceptions = ValidationException.class)
    public void testRedeemPoints_AboveAvailableBalance_ShouldThrow() {
        loyalty.redeemPoints("user-1", 1);
    }

    // Tier boundary: zero lifetime points belongs to BRONZE.
    @Test
    public void testCalculateTier_WithZeroPoints_ShouldReturnBronze() {
        Assert.assertEquals(loyalty.calculateTier(0), LoyaltyTier.BRONZE);
    }

    // Tier boundary: the last point before SILVER remains BRONZE.
    @Test
    public void testCalculateTier_At999Points_ShouldReturnBronze() {
        Assert.assertEquals(loyalty.calculateTier(999), LoyaltyTier.BRONZE);
    }

    // Tier boundary: exactly 1000 lifetime points starts SILVER.
    @Test
    public void testCalculateTier_At1000Points_ShouldReturnSilver() {
        Assert.assertEquals(loyalty.calculateTier(1000), LoyaltyTier.SILVER);
    }

    // Tier boundary: 4999 lifetime points remains SILVER.
    @Test
    public void testCalculateTier_At4999Points_ShouldReturnSilver() {
        Assert.assertEquals(loyalty.calculateTier(4999), LoyaltyTier.SILVER);
    }

    // Tier boundary: exactly 5000 lifetime points starts GOLD.
    @Test
    public void testCalculateTier_At5000Points_ShouldReturnGold() {
        Assert.assertEquals(loyalty.calculateTier(5000), LoyaltyTier.GOLD);
    }

    // Tier boundary: 9999 lifetime points remains GOLD.
    @Test
    public void testCalculateTier_At9999Points_ShouldReturnGold() {
        Assert.assertEquals(loyalty.calculateTier(9999), LoyaltyTier.GOLD);
    }

    // Tier boundary: exactly 10000 lifetime points starts PLATINUM.
    @Test
    public void testCalculateTier_At10000Points_ShouldReturnPlatinum() {
        Assert.assertEquals(loyalty.calculateTier(10000), LoyaltyTier.PLATINUM);
    }

    // Invalid boundary: negative lifetime points are rejected.
    @Test(expectedExceptions = ValidationException.class)
    public void testCalculateTier_WithNegativePoints_ShouldThrow() {
        loyalty.calculateTier(-1);
    }

    // Lifetime rule: a user's tier depends on lifetime earnings, not spendable balance.
    @Test
    public void testGetUserTier_AfterRedeemingPoints_ShouldUseLifetimePoints() {
        loyalty.addPoints("user-1", 1100);
        loyalty.redeemPoints("user-1", 1099);
        Assert.assertEquals(loyalty.getPoints("user-1"), 1);
        Assert.assertEquals(loyalty.getUserTier("user-1"), LoyaltyTier.SILVER);
    }

    // Tier benefit: BRONZE accounts receive no loyalty discount.
    @Test
    public void testGetTierDiscountPercentage_ForBronze_ShouldReturnZero() {
        Assert.assertEquals(loyalty.getTierDiscountPercentage(LoyaltyTier.BRONZE), 0.0, 0.0001);
    }

    // Tier benefit: SILVER accounts receive a five-percent discount.
    @Test
    public void testGetTierDiscountPercentage_ForSilver_ShouldReturnFivePercent() {
        Assert.assertEquals(loyalty.getTierDiscountPercentage(LoyaltyTier.SILVER), 5.0, 0.0001);
    }

    // Tier benefit: GOLD accounts receive a ten-percent discount.
    @Test
    public void testGetTierDiscountPercentage_ForGold_ShouldReturnTenPercent() {
        Assert.assertEquals(loyalty.getTierDiscountPercentage(LoyaltyTier.GOLD), 10.0, 0.0001);
    }

    // Tier benefit: PLATINUM accounts receive a fifteen-percent discount.
    @Test
    public void testGetTierDiscountPercentage_ForPlatinum_ShouldReturnFifteenPercent() {
        Assert.assertEquals(loyalty.getTierDiscountPercentage(LoyaltyTier.PLATINUM), 15.0, 0.0001);
    }

    // Transfer operation: sender balance decreases and recipient balance increases.
    @Test
    public void testTransferPoints_WithValidAccounts_ShouldMoveSpendableBalance() {
        loyalty.addPoints("sender", 500);
        loyalty.transferPoints("sender", "receiver", 200);
        Assert.assertEquals(loyalty.getPoints("sender"), 300);
        Assert.assertEquals(loyalty.getPoints("receiver"), 200);
    }

    // Lifetime rule: a transfer preserves sender lifetime earnings and adds recipient lifetime earnings.
    @Test
    public void testTransferPoints_ShouldUpdateRecipientLifetimeOnly() {
        loyalty.addPoints("sender", 500);
        loyalty.transferPoints("sender", "receiver", 200);
        Assert.assertEquals(loyalty.getLifetimePoints("sender"), 500);
        Assert.assertEquals(loyalty.getLifetimePoints("receiver"), 200);
    }

    // Invalid partition: transferring points to the same account is not allowed.
    @Test(expectedExceptions = ValidationException.class)
    public void testTransferPoints_ToSameUser_ShouldThrow() {
        loyalty.addPoints("sender", 100);
        loyalty.transferPoints("sender", "sender", 10);
    }

    // Boundary: a transfer must contain at least one point.
    @Test(expectedExceptions = ValidationException.class)
    public void testTransferPoints_WithZeroAmount_ShouldThrow() {
        loyalty.transferPoints("sender", "receiver", 0);
    }

    // Balance boundary: a transfer cannot exceed sender's spendable points.
    @Test(expectedExceptions = ValidationException.class)
    public void testTransferPoints_AboveSenderBalance_ShouldThrow() {
        loyalty.addPoints("sender", 100);
        loyalty.transferPoints("sender", "receiver", 101);
    }
}
