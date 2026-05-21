package its.service;

import its.model.Role;
import its.model.UserAccount;
import its.repository.UserRepository;
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
        UserAccount admin = new UserAccount("admin", "pw", Role.ADMIN);
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
        UserAccount pl1 = new UserAccount("pl1", "pw", Role.PL);
        when(userRepository.findByUsername("pl1")).thenReturn(Optional.of(pl1));

        assertThrows(RuntimeException.class,
                () -> userService.register("pl1", "newUser", "pass", Role.DEV));
    }

    // 중복 username이면 예외
    @Test
    void register_중복username이면_예외() {
        when(userRepository.existsByUsername("dev1")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> userService.register("admin", "dev1", "pass2", Role.DEV));
    }


}
