package its.repository;

import its.model.UserAccount;

import java.util.Optional;

public interface UserRepository {

    boolean existsByUsername(String username);

    void save(UserAccount userAccount);

    Optional<UserAccount> findByUsername(String name);
}
