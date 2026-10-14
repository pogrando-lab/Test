package student.tests;

import am.gortsup.testing.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class StringUtilsTest {

    // Equivalence partition: slugs are lowercase and remove accents and punctuation.
    @Test
    public void testToSlug_WithAccentsAndPunctuation_ShouldNormalizeText() {
        Assert.assertEquals(StringUtils.toSlug(" Crème  brûlée & Tea! "), "creme-brulee-tea");
    }

    // Boundary: consecutive separators collapse to a single hyphen.
    @Test
    public void testToSlug_WithRepeatedHyphens_ShouldCollapseSeparators() {
        Assert.assertEquals(StringUtils.toSlug("a---b"), "a-b");
    }

    // Equivalence partition: each word is converted to title case.
    @Test
    public void testCapitalizeWords_WithMixedCase_ShouldCapitalizeEachWord() {
        Assert.assertEquals(StringUtils.capitalizeWords("jOhN dOE"), "John Doe");
    }

    // Boundary: truncation accounts for suffix characters in the maximum length.
    @Test
    public void testTruncate_WhenTextExceedsLimit_ShouldIncludeSuffixWithinLimit() {
        Assert.assertEquals(StringUtils.truncate("abcdefgh", 6, "..."), "abc...");
    }

    // Equivalence partition: text already shorter than the limit remains unchanged.
    @Test
    public void testTruncate_WhenTextIsShorterThanLimit_ShouldRemainUnchanged() {
        Assert.assertEquals(StringUtils.truncate("short", 10, "..."), "short");
    }

    // Invalid boundary: the requested maximum cannot be shorter than the suffix.
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTruncate_WhenLimitIsShorterThanSuffix_ShouldThrow() {
        StringUtils.truncate("text", 2, "...");
    }

    // Equivalence partition: repeated non-overlapping substrings are counted.
    @Test
    public void testCountOccurrences_WithRepeatedSubstring_ShouldCountNonOverlappingMatches() {
        Assert.assertEquals(StringUtils.countOccurrences("aaaa", "aa"), 2);
    }

    // Case-sensitivity rule: matching distinguishes uppercase from lowercase.
    @Test
    public void testCountOccurrences_WithDifferentCase_ShouldRemainCaseSensitive() {
        Assert.assertEquals(StringUtils.countOccurrences("Ababa", "aba"), 1);
    }

    // Boundary: empty search text returns zero occurrences.
    @Test
    public void testCountOccurrences_WithEmptySubstring_ShouldReturnZero() {
        Assert.assertEquals(StringUtils.countOccurrences("text", ""), 0);
    }

    // Null boundary: null source text returns zero occurrences.
    @Test
    public void testCountOccurrences_WithNullText_ShouldReturnZero() {
        Assert.assertEquals(StringUtils.countOccurrences(null, "x"), 0);
    }

    // Equivalence partition: punctuation and spaces do not prevent palindrome recognition.
    @Test
    public void testIsPalindrome_WithPhraseAndPunctuation_ShouldReturnTrue() {
        Assert.assertTrue(StringUtils.isPalindrome("A man, a plan, a canal: Panama"));
    }

    // Negative partition: a phrase with different normalized ends is not a palindrome.
    @Test
    public void testIsPalindrome_WithNonPalindrome_ShouldReturnFalse() {
        Assert.assertFalse(StringUtils.isPalindrome("Not a palindrome"));
    }

    // Equivalence partition: non-digit card characters are stripped before masking.
    @Test
    public void testMaskCreditCard_WithSeparators_ShouldMaskAllButLastFourDigits() {
        Assert.assertEquals(StringUtils.maskCreditCard("4111-1111 1111-1234"), "************1234");
    }

    // Boundary: a card value of four digits or fewer has no digits to mask.
    @Test
    public void testMaskCreditCard_WithFourDigits_ShouldLeaveAllDigitsVisible() {
        Assert.assertEquals(StringUtils.maskCreditCard("1234"), "1234");
    }

    // Equivalence partition: word order reverses while single spaces separate words.
    @Test
    public void testReverseWords_WithThreeWords_ShouldReverseTheirOrder() {
        Assert.assertEquals(StringUtils.reverseWords("one two three"), "three two one");
    }
}
