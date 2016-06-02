package org.bbop.apollo.security;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.HostAuthenticationToken;

/**
 * Created by nathandunn on 6/2/16.
 */
public class RemoteUserAuthenticationToken implements HostAuthenticationToken,AuthenticationToken{

    private String principal;

    public RemoteUserAuthenticationToken(String username){
        this.principal = username;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return "REMOTE_USER";
    }

    @Override
    public String getHost() {
        return "REMOTE_USER_HOST";
    }
}
