package student.tests;

import am.gortsup.testing.exception.AccountLockedException;
import am.gortsup.testing.exception.AuthenticationException;
import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.Role;
import am.gortsup.testing.model.User;
import am.gortsup.testing.service.UserService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

public class UserServiceTest {

    private UserService users;

    @BeforeMethod
    public void createIsolatedUserService() {
        users = new UserService();
    }

    private User register(String username, Role role) {
        return users.registerUser(username, username.toLowerCase() + "@example.com",
                "StrongPass1!", role);
    }

    // Equivalence partition: registration creates an identified customer when role is omitted.
    @Test
    public void testRegisterUser_WithNullRole_ShouldDefaultToCustomer() {
        User user = users.registerUser("customer_1", "customer@example.com", "StrongPass1!", null);

        Assert.assertNotNull(user.getId());
        Assert.assertEquals(user.getRole(), Role.CUSTOMER);
    }

    // Case-insensitive uniqueness: username cannot be registered twice with different casing.
    @Test(expectedExceptions = ValidationException.class)
    public void testRegisterUser_WithDuplicateUsernameIgnoringCase_ShouldThrow() {
        register("same_name", Role.CUSTOMER);
        register("SAME_NAME", Role.ADMIN);
    }

    // Equivalence partition: email address must be unique across user accounts.
    @Test(expectedExceptions = ValidationException.class)
    public void testRegisterUser_WithDuplicateEmail_ShouldThrow() {
        users.registerUser("first_user", "shared@example.com", "StrongPass1!", Role.CUSTOMER);
        users.registerUser("second_user", "shared@example.com", "StrongPass1!", Role.CUSTOMER);
    }

    // Boundary: usernames shorter than three characters are invalid.
    @Test(expectedExceptions = ValidationException.class)
    public void testRegisterUser_WithUsernameBelowMinimumLength_ShouldThrow() {
        users.registerUser("ab", "valid@example.com", "StrongPass1!", Role.CUSTOMER);
    }

    // Invalid partition: a weak password cannot be used during registration.
    @Test(expectedExceptions = ValidationException.class)
    public void testRegisterUser_WithWeakPassword_ShouldThrow() {
        users.registerUser("valid_user", "valid@example.com", "weak", Role.CUSTOMER);
    }

    // Equivalence partition: username lookup ignores letter casing and trims surrounding spaces.
    @Test
    public void testGetUserByUsername_WithDifferentCase_ShouldReturnRegisteredUser() {
        User user = register("lookup_user", Role.CUSTOMER);
        Assert.assertEquals(users.getUserByUsername(" Lookup_User "), user);
    }

    // Equivalence partition: correct credentials authenticate and reset failed-attempt state.
    @Test
    public void testAuthenticate_WithValidCredentials_ShouldReturnUserAndResetFailures() {
        User user = register("login_user", Role.CUSTOMER);
        Assert.assertSame(users.authenticate(" LOGIN_USER ", "StrongPass1!"), user);
        Assert.assertEquals(user.getFailedLoginAttempts(), 0);
    }

    // Negative partition: incorrect credentials fail and increment the account's attempt counter.
    @Test
    public void testAuthenticate_WithWrongPassword_ShouldIncrementFailureCount() {
        User user = register("wrong_password", Role.CUSTOMER);
        Assert.assertThrows(AuthenticationException.class,
                () -> users.authenticate("wrong_password", "incorrect"));
        Assert.assertEquals(user.getFailedLoginAttempts(), 1);
    }

    // Threshold boundary: the first two consecutive failures do not lock the account.
    @Test
    public void testAuthenticate_AfterTwoFailedAttempts_ShouldRemainUnlocked() {
        User user = register("two_failures", Role.CUSTOMER);
        for (int attempt = 0; attempt < 2; attempt++) {
            Assert.assertThrows(AuthenticationException.class,
                    () -> users.authenticate("two_failures", "incorrect"));
        }
        Assert.assertFalse(user.isLocked());
        Assert.assertEquals(user.getFailedLoginAttempts(), 2);
    }

    // Threshold boundary: exactly the third consecutive failed attempt locks the account.
    @Test
    public void testAuthenticate_OnThirdFailedAttempt_ShouldLockAccount() {
        User user = register("three_failures", Role.CUSTOMER);
        for (int attempt = 0; attempt < 2; attempt++) {
            Assert.assertThrows(AuthenticationException.class,
                    () -> users.authenticate("three_failures", "incorrect"));
        }
        Assert.assertThrows(AccountLockedException.class,
                () -> users.authenticate("three_failures", "incorrect"));
        Assert.assertTrue(user.isLocked());
        Assert.assertEquals(user.getFailedLoginAttempts(), 3);
    }

    // Security rule: the previous password is required to update account credentials.
    @Test(expectedExceptions = AuthenticationException.class)
    public void testChangePassword_WithIncorrectOldPassword_ShouldThrow() {
        User user = register("change_password", Role.CUSTOMER);
        users.changePassword(user.getId(), "wrong", "NewStrong2@");
    }

    // Equivalence partition: a valid new password replaces the old password.
    @Test
    public void testChangePassword_WithValidOldAndNewPasswords_ShouldAllowNewLogin() {
        User user = register("password_user", Role.CUSTOMER);
        users.changePassword(user.getId(), "StrongPass1!", "NewStrong2@");
        Assert.assertSame(users.authenticate("password_user", "NewStrong2@"), user);
    }

    // Boundary: replacing a password with the identical value is rejected.
    @Test(expectedExceptions = ValidationException.class)
    public void testChangePassword_WithUnchangedPassword_ShouldThrow() {
        User user = register("unchanged_user", Role.CUSTOMER);
        users.changePassword(user.getId(), "StrongPass1!", "StrongPass1!");
    }

    // State transition: locking a user prevents authentication.
    @Test
    public void testLockUser_ShouldPreventAuthentication() {
        User user = register("manual_lock", Role.CUSTOMER);
        users.lockUser(user.getId());
        Assert.assertTrue(user.isLocked());
        Assert.assertThrows(AccountLockedException.class,
                () -> users.authenticate("manual_lock", "StrongPass1!"));
    }

    // State transition: unlocking restores access and resets the failed-attempt counter.
    @Test
    public void testUnlockUser_ShouldResetLockAndFailureCounter() {
        User user = register("manual_unlock", Role.CUSTOMER);
        users.lockUser(user.getId());
        users.unlockUser(user.getId());

        Assert.assertFalse(user.isLocked());
        Assert.assertEquals(user.getFailedLoginAttempts(), 0);
    }

    // Resource boundary: looking up a nonexistent ID reports a domain not-found error.
    @Test(expectedExceptions = ResourceNotFoundException.class)
    public void testGetUserById_WithUnknownId_ShouldThrow() {
        users.getUserById("unknown-id");
    }

    // Equivalence partition: a unique, valid email can be assigned to a user.
    @Test
    public void testUpdateEmail_WithUniqueValidAddress_ShouldUpdateUser() {
        User user = register("email_update", Role.CUSTOMER);
        users.updateEmail(user.getId(), "new@example.com");
        Assert.assertEquals(user.getEmail(), "new@example.com");
    }

    // Uniqueness rule: an email already assigned to another account cannot be reused.
    @Test(expectedExceptions = ValidationException.class)
    public void testUpdateEmail_WithAnotherUsersAddress_ShouldThrow() {
        User first = users.registerUser("email_first", "first@example.com", "StrongPass1!", Role.CUSTOMER);
        User second = users.registerUser("email_second", "second@example.com", "StrongPass1!", Role.CUSTOMER);
        users.updateEmail(first.getId(), second.getEmail());
    }

    // Authorization partition: a GUEST receives READ access only.
    @Test
    public void testHasPermission_ForGuest_ShouldAllowReadAndDenyWrite() {
        User guest = register("perm_guest", Role.GUEST);
        Assert.assertTrue(users.hasPermission(guest.getId(), "READ"));
        Assert.assertFalse(users.hasPermission(guest.getId(), "WRITE"));
    }

    // Authorization partition: a CUSTOMER can create orders and view their own profile.
    @Test
    public void testHasPermission_ForCustomer_ShouldAllowCustomerOperations() {
        User customer = register("perm_customer", Role.CUSTOMER);
        Assert.assertTrue(users.hasPermission(customer.getId(), "CREATE_ORDER"));
        Assert.assertTrue(users.hasPermission(customer.getId(), "VIEW_OWN_PROFILE"));
        Assert.assertFalse(users.hasPermission(customer.getId(), "MANAGE_INVENTORY"));
    }

    // Authorization partition: a MANAGER can manage inventory and orders.
    @Test
    public void testHasPermission_ForManager_ShouldAllowManagementOperations() {
        User manager = register("perm_manager", Role.MANAGER);
        Assert.assertTrue(users.hasPermission(manager.getId(), "MANAGE_INVENTORY"));
        Assert.assertTrue(users.hasPermission(manager.getId(), "MANAGE_ORDERS"));
        Assert.assertFalse(users.hasPermission(manager.getId(), "DELETE_SYSTEM"));
    }

    // Authorization partition: an ADMIN is allowed every named permission.
    @Test
    public void testHasPermission_ForAdmin_ShouldAllowAnyPermission() {
        User admin = register("perm_admin", Role.ADMIN);
        Assert.assertTrue(users.hasPermission(admin.getId(), "any-permission"));
    }

    // Collection contract: callers cannot mutate the registered-user snapshot.
    @Test
    public void testGetAllUsers_ShouldReturnUnmodifiableSnapshot() {
        register("list_user", Role.CUSTOMER);
        List<User> allUsers = users.getAllUsers();

        Assert.assertEquals(allUsers.size(), 1);
        Assert.assertThrows(UnsupportedOperationException.class, allUsers::clear);
    }

    // Account state: deactivated accounts cannot authenticate successfully.
    @Test
    public void testDeactivateUser_ShouldPreventAuthentication() {
        User user = register("inactive_user", Role.CUSTOMER);
        users.deactivateUser(user.getId());

        Assert.assertFalse(user.isActive());
        Assert.assertThrows(AuthenticationException.class,
                () -> users.authenticate("inactive_user", "StrongPass1!"));
    }
}
