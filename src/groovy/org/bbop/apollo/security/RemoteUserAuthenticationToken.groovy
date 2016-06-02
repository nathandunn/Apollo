package org.bbop.apollo.security

import org.apache.shiro.authc.AuthenticationToken

/**
 * Created by nathandunn on 6/2/16.
 */
class RemoteUserAuthenticationToken implements AuthenticationToken{

    String principal

    public RemoteUserAuthenticationToken(String username){
        this.principal = username
    }

    @Override
    Object getPrincipal() {
        return principal
    }

    @Override
    Object getCredentials() {
        return "REMOTE_USER"
    }
}
