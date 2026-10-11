package com.sitemaster.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Custom Bean Validation constraints (polymorphism through ConstraintValidator). */
class ValidatorsTest {

    private final StrongPasswordValidator password = new StrongPasswordValidator();
    private final ValidNICValidator nic = new ValidNICValidator();
    private final ValidSriLankanPhoneValidator phone = new ValidSriLankanPhoneValidator();

    @ParameterizedTest
    @ValueSource(strings = {"Password@123", "Str0ng!Pass", "Aa1#aaaa"})
    void acceptsStrongPasswords(String value) {
        assertThat(password.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"password@123", "PASSWORD@123", "Password123", "Password@", "Pa@1", "Pass word1"})
    void rejectsWeakPasswords(String value) {
        assertThat(password.isValid(value, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"882345678V", "912345670v", "852345671X", "199023456789"})
    void acceptsOldAndNewNics(String value) {
        assertThat(nic.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"88234567V", "8823456789", "19902345678", "ABCDEFGHIJ", "85234567X"})
    void rejectsBadNics(String value) {
        assertThat(nic.isValid(value, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0771234567", "0112345678", "+94771234567"})
    void acceptsSriLankanPhones(String value) {
        assertThat(phone.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"771234567", "077123456", "+9477123456", "077-1234567", "+44771234567"})
    void rejectsOtherPhones(String value) {
        assertThat(phone.isValid(value, null)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    void optionalFieldsLeaveNullToNotNull(String value) {
        assertThat(password.isValid(value, null)).isTrue();
        assertThat(nic.isValid(value, null)).isTrue();
        assertThat(phone.isValid(value, null)).isTrue();
    }
}
