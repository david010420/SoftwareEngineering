package its.controller;

import its.service.UserService;

public class UserController {

    private final UserService userService;

    public UserController(UserService service) {
        this.userService = service;
    }


}
