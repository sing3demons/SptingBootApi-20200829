package com.sing3demons.springbootapi.business;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;

import com.sing3demons.springbootapi.entity.User;
import com.sing3demons.springbootapi.exception.BaseException;
import com.sing3demons.springbootapi.exception.FileException;
import com.sing3demons.springbootapi.exception.UserException;
import com.sing3demons.springbootapi.model.LoginRequest;
import com.sing3demons.springbootapi.model.LoginResponse;
import com.sing3demons.springbootapi.model.MRegisterRequest;
import com.sing3demons.springbootapi.service.TokenService;
import com.sing3demons.springbootapi.service.UserService;
import com.sing3demons.springbootapi.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserBusiness {
    private final UserService userService;
    private final TokenService tokenService;
    private final EmailBusiness emailBusiness;

    public User register(MRegisterRequest request) throws UserException {
        String token = SecurityUtil.generateToken();
        User user = userService.create(request.getEmail(), request.getPassword(), request.getName(), token);
        sendEmail(user);
        return user;
    }

    private void sendEmail(User user) {

        String token = user.getToken();
        try {
            emailBusiness.sendActivateUserEmail(user.getEmail(), user.getName(), token);
        } catch (BaseException e) {
            e.printStackTrace();
        }

    }

    public LoginResponse login(LoginRequest request) throws UserException {
        Optional<User> opt = userService.findByEmail(request.getEmail());
        if (opt.isEmpty()) {
            // throw login fail, email not found
            throw UserException.loginFailEmailNotFound();
        }

        User user = opt.get();

        if (!userService.matchPassword(request.getPassword(), user.getPassword())) {
            // throw login fail, password incorrect
            throw UserException.loginFailPasswordIncorrect();
        }

        LoginResponse response = new LoginResponse();

        String token = tokenService.tokenize(user);
        response.setToken(token);

        return response;
    }

    public String refreshToken() throws UserException {
        Optional<String> opt = SecurityUtil.getCurrentUserId();
        if (opt.isEmpty()) {
            throw UserException.unauthorized();
        }

        String userId = opt.get();

        Optional<User> optUser = userService.findByID(userId);
        if (optUser.isEmpty()) {
            throw UserException.notFound();
        }
        User user = optUser.get();
        return tokenService.tokenize(user);
    }

    public String uploadProfilePicture(MultipartFile file) throws BaseException {
        if (file == null) {
            throw FileException.fileNull();
        }

        if (file.getSize() > 1048576 * 2) {
            throw FileException.fileMaxSize();
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw FileException.fileUnsupported();
        }

        List<String> supportedTypes = Arrays.asList("image/jpeg", "image/png");
        if (!supportedTypes.contains(contentType)) {
            throw FileException.fileUnsupported();
        }

        return "";
    }

}
