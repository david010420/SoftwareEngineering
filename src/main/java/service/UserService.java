package service;

import model.Role;
import model.UserAccount;

import java.util.List;

public interface UserService {

    /**
     * 새 계정을 등록한다. ADMIN 권한이 있는 계정만 호출할 수 있다.
     */
    void register(String requesterUsername, String newUsername, String password, Role role);

    UserAccount login(String username, String password);

    /** 전체 조회 */
    List<UserAccount> findAll();

    /** 역할 기준으로 조회 */
    List<UserAccount> findByRole(Role role);

    /**
     * username 으로 계정 조회.
     */
    UserAccount findByUsername(String username);

    /**
     * 계정 삭제 (ADMIN인 경우)
     */
    void delete(String requesterUsername, String targetUsername);
}
