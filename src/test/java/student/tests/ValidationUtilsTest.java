package student.tests;

import am.gortsup.testing.util.ValidationUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ValidationUtilsTest {

    // Equivalence partition: a valid Armenian-domain email is accepted.
    @Test
    public void testIsValidEmail_WithValidAddress_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidEmail("ana.petrossian@example.am"));
    }

    // Equivalence partition: plus aliases and subdomains are valid email parts.
    @Test
    public void testIsValidEmail_WithPlusAliasAndSubdomain_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidEmail("name+tag@sub.example.com"));
    }

    // Boundary: an empty address is not a valid email.
    @Test
    public void testIsValidEmail_WithEmptyValue_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidEmail(""));
    }

    // Invalid partition: consecutive dots are not permitted in the local part.
    @Test
    public void testIsValidEmail_WithConsecutiveDots_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidEmail("name..surname@example.com"));
    }

    // Invalid partition: the domain must contain a top-level domain.
    @Test
    public void testIsValidEmail_WithoutTopLevelDomain_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidEmail("name@example"));
    }

    // Invalid partition: whitespace inside an email address is forbidden.
    @Test
    public void testIsValidEmail_WithInternalWhitespace_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidEmail("name @example.com"));
    }

    // Null boundary: a missing email is invalid and does not throw.
    @Test
    public void testIsValidEmail_WithNull_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidEmail(null));
    }

    // Equivalence partition: password satisfying all complexity rules is accepted.
    @Test
    public void testIsValidPassword_WithAllRequiredCharacterClasses_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidPassword("StrongPass1!"));
    }

    // Boundary: the minimum supported password length is eight characters.
    @Test
    public void testIsValidPassword_AtMinimumLength_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidPassword("Ab1!xxxx"));
    }

    // Boundary: a password shorter than eight characters is rejected.
    @Test
    public void testIsValidPassword_BelowMinimumLength_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidPassword("short1!"));
    }

    // Invalid partition: a password must contain at least one digit.
    @Test
    public void testIsValidPassword_WithoutDigit_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidPassword("NoDigits!!"));
    }

    // Invalid partition: a password must contain at least one special character.
    @Test
    public void testIsValidPassword_WithoutSpecialCharacter_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidPassword("NoSpecial1"));
    }

    // Invalid partition: whitespace is forbidden even when other complexity rules pass.
    @Test
    public void testIsValidPassword_WithWhitespace_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidPassword("White space1!"));
    }

    // Null boundary: a missing password is invalid and does not throw.
    @Test
    public void testIsValidPassword_WithNull_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidPassword(null));
    }

    // Equivalence partition: international phone numbers may use a leading plus and spaces.
    @Test
    public void testIsValidPhoneNumber_WithInternationalFormatting_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidPhoneNumber("+374 10 123456"));
    }

    // Equivalence partition: parentheses and hyphens are allowed separators.
    @Test
    public void testIsValidPhoneNumber_WithParenthesesAndHyphens_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidPhoneNumber("(202) 555-0123"));
    }

    // Boundary: fewer than seven digits is rejected.
    @Test
    public void testIsValidPhoneNumber_BelowMinimumDigitCount_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidPhoneNumber("123456"));
    }

    // Boundary: more than fifteen digits is rejected.
    @Test
    public void testIsValidPhoneNumber_AboveMaximumDigitCount_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidPhoneNumber("1234567890123456"));
    }

    // Invalid partition: letters cannot substitute for digits in a phone number.
    @Test
    public void testIsValidPhoneNumber_WithLettersOnly_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidPhoneNumber("letters-only"));
    }

    // Country partition: a US ZIP+4 code is accepted.
    @Test
    public void testIsValidPostalCode_WithUsZipPlusFour_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidPostalCode("12345-6789", "US"));
    }

    // Country partition: Armenian postal codes contain exactly four digits.
    @Test
    public void testIsValidPostalCode_WithArmenianFourDigitCode_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidPostalCode("0001", "AM"));
    }

    // Country partition: a standard Canadian alphanumeric postal code is accepted.
    @Test
    public void testIsValidPostalCode_WithCanadianCode_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidPostalCode("K1A 0B1", "CA"));
    }

    // Invalid partition: Armenian postal codes containing letters are rejected.
    @Test
    public void testIsValidPostalCode_WithInvalidArmenianCode_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidPostalCode("12A", "AM"));
    }

    // Fallback partition: an unlisted country uses the generic alphanumeric rule.
    @Test
    public void testIsValidPostalCode_WithOtherCountryGenericCode_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidPostalCode("ABC123", "ZZ"));
    }

    // Equivalence partition: a well-formed IBAN with a valid mod-97 checksum is accepted.
    @Test
    public void testIsValidIban_WithValidChecksum_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidIban("GB82WEST12345698765432"));
    }

    // Invalid partition: changing one checksum digit invalidates the IBAN.
    @Test
    public void testIsValidIban_WithInvalidChecksum_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidIban("GB82WEST12345698765433"));
    }

    // Boundary: the maximum valid IPv4 octet value is accepted.
    @Test
    public void testIsValidIpAddress_WithMaximumOctets_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidIpAddress("255.255.255.255"));
    }

    // Equivalence partition: a typical private IPv4 address is accepted.
    @Test
    public void testIsValidIpAddress_WithPrivateAddress_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidIpAddress("192.168.0.1"));
    }

    // Boundary: an octet above 255 is rejected.
    @Test
    public void testIsValidIpAddress_WithOctetAboveMaximum_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidIpAddress("256.1.1.1"));
    }

    // Invalid partition: multi-digit IPv4 octets cannot start with zero.
    @Test
    public void testIsValidIpAddress_WithLeadingZero_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidIpAddress("01.2.3.4"));
    }

    // Invalid partition: IPv6 is outside this IPv4 validator's contract.
    @Test
    public void testIsValidIpAddress_WithIpv6Address_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidIpAddress("::1"));
    }

    // Equivalence partition: a secure HTTP URL with path and query is valid.
    @Test
    public void testIsValidUrl_WithHttpsPathAndQuery_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidUrl("https://example.com/path?q=1"));
    }

    // Equivalence partition: an HTTP URL with an explicit port is valid.
    @Test
    public void testIsValidUrl_WithHttpPort_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isValidUrl("http://example.org:8080"));
    }

    // Invalid partition: non-HTTP schemes are not accepted.
    @Test
    public void testIsValidUrl_WithFtpScheme_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidUrl("ftp://example.com"));
    }

    // Invalid partition: arbitrary text is not a URL.
    @Test
    public void testIsValidUrl_WithMalformedText_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isValidUrl("not a URL"));
    }

    // Security rule: markup delimiters and quotes are escaped before display.
    @Test
    public void testSanitizeInput_WithMarkupAndQuotes_ShouldEscapeHtmlCharacters() {
        String sanitized = ValidationUtils.sanitizeInput("<tag> & \"quoted\" 'text'");
        Assert.assertFalse(sanitized.contains("<"));
        Assert.assertFalse(sanitized.contains(">"));
        Assert.assertTrue(sanitized.contains("&amp;"));
        Assert.assertTrue(sanitized.contains("&quot;"));
        Assert.assertTrue(sanitized.contains("&#"));
    }

    // Null boundary: sanitizer returns an empty string for absent input.
    @Test
    public void testSanitizeInput_WithNull_ShouldReturnEmptyString() {
        Assert.assertEquals(ValidationUtils.sanitizeInput(null), "");
    }

    // Equivalence partition: a positive number is greater than zero.
    @Test
    public void testIsPositiveNumber_WithPositiveValue_ShouldReturnTrue() {
        Assert.assertTrue(ValidationUtils.isPositiveNumber(1));
    }

    // Boundary: zero is not a positive number.
    @Test
    public void testIsPositiveNumber_WithZero_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isPositiveNumber(0));
    }

    // Equivalence partition: negative values are not positive.
    @Test
    public void testIsPositiveNumber_WithNegativeValue_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isPositiveNumber(-1.5));
    }

    // Null boundary: absent numeric input is not positive.
    @Test
    public void testIsPositiveNumber_WithNull_ShouldReturnFalse() {
        Assert.assertFalse(ValidationUtils.isPositiveNumber(null));
    }
}
