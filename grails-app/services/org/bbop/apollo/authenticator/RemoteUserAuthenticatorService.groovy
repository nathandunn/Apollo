package org.bbop.apollo.authenticator

import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.crypto.hash.Sha256Hash
import org.apache.shiro.subject.Subject
import org.bbop.apollo.Role
import org.bbop.apollo.User
import org.bbop.apollo.UserService
import org.bbop.apollo.gwt.shared.ClientTokenGenerator
import org.bbop.apollo.gwt.shared.FeatureStringEnum

import javax.servlet.http.HttpServletRequest

@Transactional
class RemoteUserAuthenticatorService implements AuthenticatorService {

    def authenticate(HttpServletRequest request) {
        User user
        UsernamePasswordToken authToken = new UsernamePasswordToken()
        String randomPassword = ClientTokenGenerator.generateRandomString()
        String passwordHash = new Sha256Hash(randomPassword).toHex()
        Subject subject
        try {
            subject = SecurityUtils.getSubject();

            String remoteUser
            // for testing
//            if (!request.getHeader(FeatureStringEnum.REMOTE_USER.value)) {
//                remoteUser = "abcd@125.com"
//            } else {
            remoteUser = request.getHeader(FeatureStringEnum.REMOTE_USER.value)
//            }
//            remoteUser = request.getHeader(FeatureStringEnum.REMOTE_USER.value)
            log.warn "Remote user found [${remoteUser}]"
            if (!remoteUser) {
                log.warn("No remote user passed in header!")
                return false
            }
//            }
            authToken.username = remoteUser
            user = User.findByUsername(authToken.username)
            log.warn "User exists ${user} ? "
            log.warn "for username: ${authToken.username}"
            if (!user) {

                log.warn "User does not exist so creating new user."

                user = new User(
                        username: remoteUser,
                        passwordHash: passwordHash,
                        firstName: "REMOTE_USER",
                        lastName: "${remoteUser}",
                        metadata: randomPassword// reversible autogenerated password
                ).save(insert: true, flush: true, failOnError: true)

                Role role = Role.findByName(UserService.USER)
                log.debug "adding role: ${role}"
                user.addToRoles(role)
                role.addToUsers(user)
                role.save()
                user.save(flush: true)
                log.warn "User created ${user}"
            }

            authToken.password = user.metadata
            subject.login(authToken)

            return true
        } catch (AuthenticationException ae) {
            log.error("Problem authenticating: " + ae.fillInStackTrace())
            // force authentication
            log.error "Failed to authenticate user ${authToken.username}, resaving password and forcing"
            user.metadata = randomPassword
            user.passwordHash = passwordHash
            log.warn("reset password and saving: " + user.metadata)
            user.save(flush: true, failOnError: true, insert: false)
            authToken.password = user.metadata
            log.warn("logging in again")
            subject.login(authToken)
            if (subject.authenticated) {
                log.warn("success!")
                return true
            } else {
                log.warn("fail!")
                return false
            }
        }

    }

    //    @Override
    def authenticate(UsernamePasswordToken authToken, HttpServletRequest request) {
        // token is ignored
        return authToken(request)
    }
}
