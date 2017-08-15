package io.spring.api;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.spring.api.exception.InvalidRequestException;
import io.spring.application.user.UserQueryService;
import io.spring.core.user.EncryptService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class UsersApi {
    private UserRepository userRepository;
    private UserQueryService userQueryService;
    private String defaultImage;
    private EncryptService encryptService;

    @Autowired
    public UsersApi(UserRepository userRepository,
                    UserQueryService userQueryService,
                    EncryptService encryptService,
                    @Value("${image.default}") String defaultImage) {
        this.userRepository = userRepository;
        this.userQueryService = userQueryService;
        this.encryptService = encryptService;
        this.defaultImage = defaultImage;
    }

    @RequestMapping(path = "/users", method = POST)
    public ResponseEntity createUser(@Valid @RequestBody RegisterParam registerParam, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new InvalidRequestException(bindingResult);
        }
        if (userRepository.findByUsername(registerParam.getUsername()).isPresent()) {
            bindingResult.rejectValue("username", "DUPLICATED", "duplicated username");
            throw new InvalidRequestException(bindingResult);
        }

        if (userRepository.findByEmail(registerParam.getEmail()).isPresent()) {
            bindingResult.rejectValue("email", "DUPLICATED", "duplicated email");
            throw new InvalidRequestException(bindingResult);
        }
        User user = new User(
            registerParam.getEmail(),
            registerParam.getUsername(),
            encryptService.encrypt(registerParam.getPassword()),
            "",
            defaultImage);
        userRepository.save(user);
        return ResponseEntity.status(201).body(userQueryService.fetchNewAuthenticatedUser(user.getUsername()));
    }

    @RequestMapping(path = "/users/login", method = POST)
    public ResponseEntity userLogin(@Valid @RequestBody LoginParam loginParam, BindingResult bindingResult) {
        Optional<User> optional = userRepository.findByEmail(loginParam.getEmail());
        if (optional.isPresent() && encryptService.check(loginParam.getPassword(), optional.get().getPassword())) {
            return ResponseEntity.ok(userQueryService.fetchNewAuthenticatedUser(optional.get().getUsername()));
        } else {
            bindingResult.rejectValue("password", "INVALID", "invalid email or password");
            throw new InvalidRequestException(bindingResult);
        }
    }
}

@Getter
@JsonRootName("user")
@NoArgsConstructor
class LoginParam {
    @NotBlank(message = "can't be empty")
    @Email(message = "should be an email")
    private String email;
    @NotBlank(message = "can't be empty")
    private String password;
}

@Getter
@JsonRootName("user")
@NoArgsConstructor
class RegisterParam {
    @NotBlank(message = "can't be empty")
    @Email(message = "should be an email")
    private String email;
    @NotBlank(message = "can't be empty")
    private String username;
    @NotBlank(message = "can't be empty")
    private String password;
}