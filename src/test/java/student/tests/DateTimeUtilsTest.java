package student.tests;

import am.gortsup.testing.util.DateTimeUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;

public class DateTimeUtilsTest {

    // Gregorian rule: years divisible by four are leap years unless excluded by the century rule.
    @Test
    public void testIsLeapYear_WithOrdinaryLeapYear_ShouldReturnTrue() {
        Assert.assertTrue(DateTimeUtils.isLeapYear(2024));
    }

    // Century boundary: a year divisible by 100 but not 400 is not a leap year.
    @Test
    public void testIsLeapYear_WithNonFourHundredthCentury_ShouldReturnFalse() {
        Assert.assertFalse(DateTimeUtils.isLeapYear(1900));
    }

    // Gregorian exception: a century divisible by 400 is a leap year.
    @Test
    public void testIsLeapYear_WithFourHundredthCentury_ShouldReturnTrue() {
        Assert.assertTrue(DateTimeUtils.isLeapYear(2000));
    }

    // Equivalence partition: day difference includes the leap day between the endpoints.
    @Test
    public void testCalculateDaysBetween_CrossingLeapDay_ShouldCountBothDays() {
        Assert.assertEquals(DateTimeUtils.calculateDaysBetween(
                LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1)), 2L);
    }

    // Boundary: a Saturday is a weekend day.
    @Test
    public void testIsWeekend_WithSaturday_ShouldReturnTrue() {
        Assert.assertTrue(DateTimeUtils.isWeekend(LocalDate.of(2026, 10, 10)));
    }

    // Equivalence partition: a Monday is a business day.
    @Test
    public void testIsWeekend_WithMonday_ShouldReturnFalse() {
        Assert.assertFalse(DateTimeUtils.isWeekend(LocalDate.of(2026, 10, 12)));
    }

    // Range rule: count weekdays in [start, end), excluding Saturday and Sunday.
    @Test
    public void testCalculateBusinessDaysBetween_SpanningWeekend_ShouldCountWeekdaysOnly() {
        Assert.assertEquals(DateTimeUtils.calculateBusinessDaysBetween(
                LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 13)), 2);
    }

    // Boundary: an empty date interval contains no business days.
    @Test
    public void testCalculateBusinessDaysBetween_EqualDates_ShouldReturnZero() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        Assert.assertEquals(DateTimeUtils.calculateBusinessDaysBetween(date, date), 0);
    }

    // Equivalence partition: adding one business day on Friday skips the weekend.
    @Test
    public void testAddBusinessDays_FromFridayByOne_ShouldReturnMonday() {
        Assert.assertEquals(DateTimeUtils.addBusinessDays(
                LocalDate.of(2026, 10, 9), 1), LocalDate.of(2026, 10, 12));
    }

    // Boundary: adding zero business days leaves the starting date unchanged.
    @Test
    public void testAddBusinessDays_WithZeroDays_ShouldReturnStartDate() {
        LocalDate start = LocalDate.of(2026, 10, 9);
        Assert.assertEquals(DateTimeUtils.addBusinessDays(start, 0), start);
    }

    // Closed-interval boundary: intervals sharing an endpoint overlap.
    @Test
    public void testDoIntervalsOverlap_WithSharedEndpoint_ShouldReturnTrue() {
        LocalDate start = LocalDate.of(2026, 1, 10);
        Assert.assertTrue(DateTimeUtils.doIntervalsOverlap(start, start.plusDays(2),
                start.plusDays(2), start.plusDays(4)));
    }

    // Equivalence partition: separated closed intervals do not overlap.
    @Test
    public void testDoIntervalsOverlap_WithGapBetweenIntervals_ShouldReturnFalse() {
        LocalDate start = LocalDate.of(2026, 1, 10);
        Assert.assertFalse(DateTimeUtils.doIntervalsOverlap(start, start.plusDays(1),
                start.plusDays(2), start.plusDays(3)));
    }

    // Inclusive range boundary: the minimum endpoint belongs to the range.
    @Test
    public void testIsDateInRange_WithDateAtMinimum_ShouldReturnTrue() {
        LocalDate min = LocalDate.of(2026, 1, 1);
        Assert.assertTrue(DateTimeUtils.isDateInRange(min, min, LocalDate.of(2026, 1, 31)));
    }

    // Inclusive range boundary: the maximum endpoint belongs to the range.
    @Test
    public void testIsDateInRange_WithDateAtMaximum_ShouldReturnTrue() {
        LocalDate max = LocalDate.of(2026, 1, 31);
        Assert.assertTrue(DateTimeUtils.isDateInRange(max, LocalDate.of(2026, 1, 1), max));
    }

    // Invalid partition: a date after the inclusive maximum is outside the range.
    @Test
    public void testIsDateInRange_AfterMaximum_ShouldReturnFalse() {
        LocalDate min = LocalDate.of(2026, 1, 1);
        LocalDate max = LocalDate.of(2026, 1, 31);
        Assert.assertFalse(DateTimeUtils.isDateInRange(max.plusDays(1), min, max));
    }

    // Null boundary: date difference rejects a missing start date explicitly.
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCalculateDaysBetween_WithNullStart_ShouldThrow() {
        DateTimeUtils.calculateDaysBetween(null, LocalDate.now());
    }
}
