package io.test.security.repository;


import io.test.security.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TODO Comment
 *
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since TODO
 */
public interface JpaUserRepository extends JpaRepository<User, UUID> {
    User findByUsername(String username);
}
