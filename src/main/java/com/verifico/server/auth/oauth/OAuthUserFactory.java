package com.verifico.server.auth.oauth;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class OAuthUserFactory {

    public OAuthUserInfo create(OAuth2User oAuth2User, String registrationId){
        String email = oAuth2User.getAttribute("email");
        // TO DO: Handle retreival of required OAuth2 information...
        String fullName = oAuth2User.getAttribute("name") != null ? oAuth2User.getAttribute("name") : null;

        return null;
    }
}
