package io.test.security.repository;


import io.test.security.entity.UserSubstitution;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TODO Comment
 *
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since TODO
 */
public interface JpaUserSubstitutionRepository extends JpaRepository<UserSubstitution, UUID> {
}
