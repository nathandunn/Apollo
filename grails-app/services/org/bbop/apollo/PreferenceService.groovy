package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

@Transactional
class PreferenceService {

    def permissionService

    JSONObject getPreferences() {
        JSONObject permissionsObject = new JSONObject()
        JSONArray organisms = new JSONArray()
        permissionsObject.organisms = organisms
        if (permissionService.currentUser) {
            UserOrganismPreference.findAllByUser(permissionService.currentUser).each { pref ->
                JSONObject organismPermission = JSON.parse((pref as JSON).toString()) as JSONObject
                organisms.add(organismPermission)
            }
        }
        return permissionsObject
//        return getCurrentOrganism(permissionService.currentUser, clientToken)
    }

    JSONObject setOrganismPreference(Organism organism){

    }

    JSONObject setPreferences(String preferenceString) {
        JSONObject permissionsObject = JSON.parse(preferenceString)
        JSONArray organisms = permissionsObject.organisms


        Map<String, UserOrganismPreference> preferenceMap = UserOrganismPreference.findAllByUser(permissionService.currentUser).collectEntries() {
            [(it.organism.commonName): it]
        }

        // we add or update only .. no need to delete
        for (int i = 0; i < organisms.size(); i++) {
            JSONObject organismPreference = organisms.getJSONObject(i)
            UserOrganismPreference userOrganismPreference = preferenceMap.get(organismPreference.commonName)
            if (!userOrganismPreference) {
                userOrganismPreference = new UserOrganismPreference()
            }
            userOrganismPreference.currentOrganism = organismPreference.currentOrganism
            // TODO: do only if different
            userOrganismPreference.organism = Organism.findById(organismPreference.organismId)
            userOrganismPreference.nativeTrackList = organismPreference.nativeTrackList
            userOrganismPreference.sequence = Sequence.findById(organismPreference.sequenceId)
            userOrganismPreference.startbp = organismPreference.startbp
            userOrganismPreference.endbp = organismPreference.endbp
            // TOdO: double-check that it is the right type of user
            userOrganismPreference.user = User.findById(organismPreference.userId)
            assert userOrganismPreference.user == permissionService.currentUser
            userOrganismPreference.name = organismPreference.name
            userOrganismPreference.preferenceString = organismPreference.preferenceString
            if (i == organisms.size() - 1) {
                userOrganismPreference.save(flush: true)
            } else {
                userOrganismPreference.save()
            }
        }


        return permissionsObject
//        return getCurrentOrganism(permissionService.currentUser, clientToken)
    }

//    Organism getCurrentOrganismForCurrentUser(String clientToken,HttpServletRequest request) {
//        println "PS: getCurrentOrganismForCurrentUser ${clientToken}"
////        if (permissionService.currentUser == null) {
//        return getOrganismForToken(clientToken)
////        } else {
////            return getCurrentOrganism(permissionService.currentUser, clientToken)
////        }
////        return permissionService.currentUser == null ? null : getCurrentOrganism(permissionService.currentUser,clientToken);
//    }

//    Organism getOrganismForToken(String s) {
//        println "token for org ${s}"
//        if (s.isLong()) {
//            println "is long "
//            return Organism.findById(Long.parseLong(s))
//        } else {
//            println "is NOT long "
//            return Organism.findByCommonNameIlike(s)
//        }
//
//    }
/**
 * Get the current user preference.
 * If no preference, then set one
 * @param user
 * @return
 */
//    Organism getCurrentOrganism(User user, String clientToken) {
//        println "getting current organism for token: ${clientToken}"
//        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByCurrentOrganismAndUserAndClientToken(true, user, clientToken)
//
//        // if there is not a current one, we see if there is another one for the same token
//        if (!userOrganismPreference) {
//            userOrganismPreference = UserOrganismPreference.findByCurrentOrganismAndUserAndClientToken(false, user, clientToken)
//        }
//
//        // if there are none, then we have to create a new one
//        if (!userOrganismPreference) {
//            Iterator i = permissionService.getOrganisms(user).iterator();
//            if (i.hasNext()) {
//                Organism organism = i.next()
//                userOrganismPreference = new UserOrganismPreference(
//                        user: user
//                        , organism: organism
//                        , sequence: Sequence.findByOrganism(organism)
//                        , currentOrganism: true
////                        , clientToken: clientToken
//                ).save()
//            } else {
//                throw new PermissionException("User has no access to any organisms!")
//            }
//        }
//
//        userOrganismPreference.currentOrganism = true
//        userOrganismPreference.save(flush: true)
//
//        return userOrganismPreference.organism
//    }


//    def setCurrentOrganism(User user, Organism organism, String clientToken) {
//        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndClientToken(user, organism, clientToken)
//        if (!userOrganismPreference) {
//            userOrganismPreference = new UserOrganismPreference(
//                    user: user
//                    , organism: organism
//                    , currentOrganism: true
//                    , sequence: Sequence.findByOrganism(organism)
////                    , clientToken: clientToken
//            ).save(flush: true)
//            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
//        } else if (!userOrganismPreference.currentOrganism) {
//            userOrganismPreference.currentOrganism = true;
//            userOrganismPreference.save(flush: true)
//            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
//        }
//    }

//    protected static
//    def setOtherCurrentOrganismsFalse(UserOrganismPreference userOrganismPreference, User user, String clientToken) {
//        println "setting other orgs false for ${clientToken}"
//        UserOrganismPreference.executeUpdate(
//                "update UserOrganismPreference  pref set pref.currentOrganism = false " +
//                        "where pref.id != :prefId and pref.user = :user and pref.organism = :organism",
//                [prefId: userOrganismPreference.id, user: user, clientToken: clientToken])
//    }


//    def setCurrentSequence(User user, Sequence sequence, String clientToken) {
//        Organism organism = sequence.organism
//        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndClientTokenAndSequence(user, organism, clientToken, sequence)
//        if (!userOrganismPreference) {
//            userOrganismPreference = new UserOrganismPreference(
//                    user: user
//                    , organism: organism
//                    , currentOrganism: true
//                    , sequence: sequence
////                    , clientToken: clientToken
//            ).save(flush: true)
//            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
//        } else if (!userOrganismPreference.currentOrganism) {
//            userOrganismPreference.currentOrganism = true;
//            userOrganismPreference.sequence = sequence
//            userOrganismPreference.save()
//            setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
//        }
//    }

//    UserOrganismPreference setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
//        User currentUser = permissionService.currentUser
//        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(currentUser, true, clientToken)
//        if (!userOrganismPreference) {
//            userOrganismPreference = UserOrganismPreference.findByUser(currentUser)
//        }
//        if (!userOrganismPreference) {
//            throw new AnnotationException("Organism preference is not set for user")
//        }
//
//        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, userOrganismPreference.organism)
//        if (!sequence) {
//            throw new AnnotationException("Sequence name is invalid ${sequenceName}")
//        }
//
//        log.debug "version ${userOrganismPreference.version} for ${userOrganismPreference.organism.commonName} ${userOrganismPreference.currentOrganism}"
//
//        userOrganismPreference.refresh()
//
////        userOrganismPreference.clientToken = clientToken
//        userOrganismPreference.currentOrganism = true
//        userOrganismPreference.sequence = sequence
//        userOrganismPreference.setStartbp(startBp ?: 0)
//        userOrganismPreference.setEndbp(endBp ?: sequence.end)
//        userOrganismPreference.save()
//    }

//    Organism getOrganismFromPreferences(User user, String trackName, String clientToken) {
//        if (user != null) {
//            UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndCurrentOrganismAndClientToken(user, true, clientToken)
//            if (userOrganismPreference) {
//                return userOrganismPreference.organism
//            }
//
//            if (!userOrganismPreference) {
//                userOrganismPreference = UserOrganismPreference.findByUserAndClientTokenAndCurrentOrganism(user, clientToken, false)
//                if (userOrganismPreference) {
//                    setOtherCurrentOrganismsFalse(userOrganismPreference, user, clientToken)
//                    userOrganismPreference.currentOrganism = true
//                    userOrganismPreference.save(flush: true)
//                    return userOrganismPreference.organism
//                }
//            }
//
//            if (!userOrganismPreference) {
//                // find a random organism based on sequence
//                Sequence sequence = Sequence.findByName(trackName)
//                Organism organism = sequence.organism
//
//                userOrganismPreference = new UserOrganismPreference(
//                        user: user
//                        , organism: organism
//                        , currentOrganism: true
//                        , sequence: sequence
////                        , clientToken: clientToken
//                ).save(insert: true)
//                return userOrganismPreference.organism
//            }
//        }
//        log.warn("No organism preference if no user")
//        return null
//
//    }

    def setOrganismPreferencesForSession(HttpSession session, Organism organism) {
//        def session = request.getSession(true)
        session.setAttribute(FeatureStringEnum.ORGANISM_JBROWSE_DIRECTORY.value, organism.directory)
        session.setAttribute(FeatureStringEnum.ORGANISM_ID.value, organism.id)
        session.setAttribute(FeatureStringEnum.ORGANISM_NAME.value, organism.commonName)
        session.setAttribute(FeatureStringEnum.ORGANISM.value, organism.id)
    }

    Organism getOrganismByClientToken(String clientToken) {
        getOrganismByClientToken(clientToken, null)
    }

    Organism getOrganismByClientToken(String clientToken, HttpServletRequest request) {
        Organism organism
        if (clientToken.isLong()) {
            organism = Organism.findById(clientToken as Long)
        } else {
            organism = Organism.findByCommonNameIlike(clientToken)
        }
        // if there is no organism
        if (!organism) {
            log.error("Unable to find organism for key ${clientToken}")
        } else if (request) {
            setOrganismPreferencesForSession(request.getSession(true), organism)
        }
        return organism
    }

    def setCurrentSequence(HttpSession session, Sequence sequenceInstance) {
        session.setAttribute(FeatureStringEnum.DEFAULT_SEQUENCE_NAME.value, sequenceInstance.name)
        session.setAttribute(FeatureStringEnum.SEQUENCE_NAME.value, sequenceInstance.name)
        setOrganismPreferencesForSession(session,sequenceInstance?.organism)
    }
}
