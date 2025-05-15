package app.api.diagnosticruntime.auth;

import app.api.diagnosticruntime.config.JwtUtil;
import app.api.diagnosticruntime.userdetails.model.User;
import app.api.diagnosticruntime.userdetails.service.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.api.diagnosticruntime.auth.AuthenticationMapper.toAuthenticationMapper;
import static app.api.diagnosticruntime.userdetails.mapper.UserMapper.toUserInfoDTO;

@RestController
@RequestMapping("/api")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MyUserDetailsService userDetailsService;

    @PostMapping("/user/login")
    public AuthenticationResponse createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
        );

        final User userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final String token = jwtUtil.generateToken(userDetails);
        AuthenticationResponse response = toAuthenticationMapper(token, toUserInfoDTO(userDetails));
        return response;
    }
}

