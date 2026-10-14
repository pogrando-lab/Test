package student.tests;

import am.gortsup.testing.util.MathAndFinanceUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MathAndFinanceUtilsTest {

    // Equivalence partition: tax is calculated as a percentage and rounded to cents.
    @Test
    public void testCalculateTax_WithFractionalAmount_ShouldRoundToCents() {
        Assert.assertEquals(MathAndFinanceUtils.calculateTax(19.99, 20), 4.0, 0.0001);
    }

    // Boundary: a zero tax rate produces no tax.
    @Test
    public void testCalculateTax_WithZeroRate_ShouldReturnZero() {
        Assert.assertEquals(MathAndFinanceUtils.calculateTax(100, 0), 0.0, 0.0001);
    }

    // Invalid partition: negative taxable amounts are not supported.
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCalculateTax_WithNegativeAmount_ShouldThrow() {
        MathAndFinanceUtils.calculateTax(-1, 10);
    }

    // Equivalence partition: percentage discount reduces the original price.
    @Test
    public void testCalculateDiscount_WithPartialRate_ShouldReduceAmount() {
        Assert.assertEquals(MathAndFinanceUtils.calculateDiscount(80, 12.5), 70.0, 0.0001);
    }

    // Boundary: a 100% discount reduces the final amount to zero.
    @Test
    public void testCalculateDiscount_WithFullRate_ShouldReturnZero() {
        Assert.assertEquals(MathAndFinanceUtils.calculateDiscount(80, 100), 0.0, 0.0001);
    }

    // Rounding boundary: HALF_UP rounds an exact half away from zero.
    @Test
    public void testRoundToDecimalPlaces_AtHalfBoundary_ShouldRoundUp() {
        Assert.assertEquals(MathAndFinanceUtils.roundToDecimalPlaces(2.345, 2), 2.35, 0.0001);
    }

    // Equivalence partition: a value below the rounding midpoint rounds down.
    @Test
    public void testRoundToDecimalPlaces_BelowHalfBoundary_ShouldRoundDown() {
        Assert.assertEquals(MathAndFinanceUtils.roundToDecimalPlaces(2.344, 2), 2.34, 0.0001);
    }

    // Invalid boundary: decimal places cannot be negative.
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRoundToDecimalPlaces_WithNegativePlaces_ShouldThrow() {
        MathAndFinanceUtils.roundToDecimalPlaces(1.23, -1);
    }

    // Equivalence partition: a known Luhn-valid card number is accepted.
    @Test
    public void testIsValidLuhn_WithValidCardNumber_ShouldReturnTrue() {
        Assert.assertTrue(MathAndFinanceUtils.isValidLuhn("49927398716"));
    }

    // Invalid partition: a card number with an invalid checksum is rejected.
    @Test
    public void testIsValidLuhn_WithInvalidChecksum_ShouldReturnFalse() {
        Assert.assertFalse(MathAndFinanceUtils.isValidLuhn("1234567812345678"));
    }

    // Boundary: card numbers shorter than eight digits are rejected.
    @Test
    public void testIsValidLuhn_BelowMinimumLength_ShouldReturnFalse() {
        Assert.assertFalse(MathAndFinanceUtils.isValidLuhn("1234567"));
    }

    // Input format: the Luhn validator accepts digits only, not embedded spaces.
    @Test
    public void testIsValidLuhn_WithEmbeddedSpaces_ShouldReturnFalse() {
        Assert.assertFalse(MathAndFinanceUtils.isValidLuhn("4111 1111 1111 1112"));
    }

    // Equivalence partition: currency conversion uses the supplied positive exchange rate.
    @Test
    public void testConvertCurrency_WithPositiveRate_ShouldConvertAmount() {
        Assert.assertEquals(MathAndFinanceUtils.convertCurrency(10.25, 2), 20.5, 0.0001);
    }

    // Invalid boundary: exchange rate must be greater than zero.
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConvertCurrency_WithZeroRate_ShouldThrow() {
        MathAndFinanceUtils.convertCurrency(10, 0);
    }

    // Equivalence partition: compound interest uses monthly compounding at a 12% rate.
    @Test
    public void testCalculateCompoundInterest_WithMonthlyCompounding_ShouldReturnFutureValue() {
        Assert.assertEquals(MathAndFinanceUtils.calculateCompoundInterest(1000, 12, 12, 1),
                1126.83, 0.01);
    }

    // Boundary: a zero-interest loan divides principal evenly over its term.
    @Test
    public void testCalculateMonthlyInstallment_WithZeroInterest_ShouldDividePrincipalEvenly() {
        Assert.assertEquals(MathAndFinanceUtils.calculateMonthlyInstallment(1200, 0, 12),
                100.0, 0.0001);
    }

    // Equivalence partition: percentage change uses the magnitude of a negative starting value.
    @Test
    public void testCalculatePercentageChange_WithNegativeBaseline_ShouldUseAbsoluteBaseline() {
        Assert.assertEquals(MathAndFinanceUtils.calculatePercentageChange(-80, -100), -25.0, 0.0001);
    }

    // Invalid boundary: percentage change from zero has no defined denominator.
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCalculatePercentageChange_WithZeroBaseline_ShouldThrow() {
        MathAndFinanceUtils.calculatePercentageChange(0, 10);
    }
}
