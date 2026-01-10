package com.eaglebank.repository;

import com.eaglebank.domain.Address;
import com.eaglebank.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import com.eaglebank.config.JpaConfig;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void shouldSaveAndFindUserByUserId() {
        // Given
        User user = User.builder()
            .userId("usr-test123")
            .name("Test User")
            .email("test@example.com")
            .passwordHash("hashedpassword")
            .phoneNumber("+447123456789")
            .address(Address.builder()
                .line1("123 Main St")
                .town("London")
                .county("Greater London")
                .postcode("SW1A 1AA")
                .build())
            .build();

        // When
        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findByUserId("usr-test123");

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldFindUserByEmail() {
        // Given
        User user = createTestUser("usr-test456", "john@example.com");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmail("john@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("usr-test456");
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        // Given
        User user = createTestUser("usr-test789", "exists@example.com");
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByEmail("exists@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // When
        boolean exists = userRepository.existsByEmail("notexists@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldReturnTrueWhenUserIdExists() {
        // Given
        User user = createTestUser("usr-existing", "user@example.com");
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByUserId("usr-existing");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserIdDoesNotExist() {
        // When
        boolean exists = userRepository.existsByUserId("usr-notexists");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldSaveUserWithAllAddressFields() {
        // Given
        User user = User.builder()
            .userId("usr-fulladdress")
            .name("Test User")
            .email("fulladdress@example.com")
            .passwordHash("hashedpassword")
            .phoneNumber("+447123456789")
            .address(Address.builder()
                .line1("123 Main Street")
                .line2("Apartment 4B")
                .line3("Building C")
                .town("London")
                .county("Greater London")
                .postcode("SW1A 1AA")
                .build())
            .build();

        // When
        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findByUserId("usr-fulladdress");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAddress().getLine1()).isEqualTo("123 Main Street");
        assertThat(found.get().getAddress().getLine2()).isEqualTo("Apartment 4B");
        assertThat(found.get().getAddress().getLine3()).isEqualTo("Building C");
        assertThat(found.get().getAddress().getTown()).isEqualTo("London");
        assertThat(found.get().getAddress().getCounty()).isEqualTo("Greater London");
        assertThat(found.get().getAddress().getPostcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    void shouldUpdateUserAndIncrementVersion() {
        // Given
        User user = createTestUser("usr-version", "version@example.com");
        User saved = userRepository.save(user);
        testEntityManager.flush();
        testEntityManager.clear();
        Integer initialVersion = saved.getVersion();

        // When
        User managedUser = userRepository.findByUserId("usr-version").orElseThrow();
        managedUser.setName("Updated Name");
        User updated = userRepository.save(managedUser);
        testEntityManager.flush();

        // Then
        assertThat(updated.getVersion()).isNotNull();
        assertThat(initialVersion).isNotNull();
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
        assertThat(updated.getName()).isEqualTo("Updated Name");
    }

    private User createTestUser(String userId, String email) {
        return User.builder()
            .userId(userId)
            .name("Test User")
            .email(email)
            .passwordHash("hashedpassword")
            .phoneNumber("+447123456789")
            .address(Address.builder()
                .line1("123 Main St")
                .town("London")
                .county("Greater London")
                .postcode("SW1A 1AA")
                .build())
            .build();
    }
}

