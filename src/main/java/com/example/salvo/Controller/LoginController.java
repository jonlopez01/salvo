package com.example.salvo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api")
public class LoginController {
    /*@Autowired
    UserDetails userDetails;

    @PostMapping("/login")
    String login(
            @RequestParam("username") final String username,
            @RequestParam("password") final String password) {
        return userDetails.
                .login(username, password)
                .orElseThrow(() -> new RuntimeException("invalid login and/or password"));
    }*/
}
