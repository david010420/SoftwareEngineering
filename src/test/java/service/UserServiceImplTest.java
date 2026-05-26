package service;

import model.Role;
import model.UserAccount;
import repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // ADMIN 계정 조회 stubbing
        UserAccount admin = new UserAccount(1L, "admin", "pw", Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.existsByUsername("admin")).thenReturn(true);
    }

    // register 성공
    @Test
    void register_성공() {
        when(userRepository.existsByUsername("dev1")).thenReturn(false);

        userService.register("admin", "dev1", "pass", Role.DEV);

        verify(userRepository).save(argThat(u ->
                u.getUsername().equals("dev1") && u.getRole() == Role.DEV
        ));
    }

    // ADMIN_아니면 예외
    @Test
    void register_ADMIN_아니면_예외() {
        UserAccount pl1 = new UserAccount(2L, "pl1", "pw", Role.PL);
        when(userRepository.findByUsername("pl1")).thenReturn(Optional.of(pl1));

        assertThrows(RuntimeException.class,
                () -> userService.register("pl1", "newUser", "pass", Role.DEV));
    }

    // 중복 username이면 예외
    @Test
    void register_중복_username이면_예외() {
        when(userRepository.existsByUsername("dev1")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> userService.register("admin", "dev1", "pass2", Role.DEV));
    }

    // login 성공
    @Test
    void login_성공() {
        UserAccount dev1 = new UserAccount(3L, "dev1", "mypass", Role.DEV);
        when(userRepository.findByUsername("dev1")).thenReturn(Optional.of(dev1));

        UserAccount result = userService.login("dev1", "mypass");

        assertEquals("dev1", result.getUsername());
    }

    // login 실패 - 없는 유저
    @Test
    void login_없는유저이면_예외() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.login("nobody", "pw"));
    }

    // login 실패 - 틀린 비밀번호
    @Test
    void login_틀린비밀번호이면_예외() {
        UserAccount dev1 = new UserAccount(4L, "dev1", "correctpw", Role.DEV);
        when(userRepository.findByUsername("dev1")).thenReturn(Optional.of(dev1));

        assertThrows(RuntimeException.class,
                () -> userService.login("dev1", "wrongpw"));
    }

    // delete 성공
    @Test
    void delete_성공() {
        UserAccount dev1 = new UserAccount(5L, "dev1", "pw", Role.DEV);
        when(userRepository.findByUsername("dev1")).thenReturn(Optional.of(dev1));
        when(userRepository.delete("dev1")).thenReturn(dev1);

        userService.delete("admin", "dev1");

        verify(userRepository).delete("dev1");
    }

    // delete 실패 - 없는 대상
    @Test
    void delete_없는대상이면_예외() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.delete("admin", "nobody"));
    }

    // delete - 비ADMIN
    @Test
    void delete_비ADMIN이면_예외() {
        UserAccount dev1 = new UserAccount(6L, "dev1", "pw", Role.DEV);
        when(userRepository.findByUsername("dev1")).thenReturn(Optional.of(dev1));

        assertThrows(RuntimeException.class,
                () -> userService.delete("dev1", "dev2"));
    }
}
