package repository;

import model.Role;
import model.UserAccount;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    boolean existsByUsername(String username);

    void save(UserAccount userAccount);

    Optional<UserAccount> findByUsername(String username);

    List<UserAccount> findAll();

    List<UserAccount> findByRole(Role role);

    UserAccount delete(String username);
}
