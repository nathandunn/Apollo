package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.session.Session
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.preference.OrganismDTO
import org.bbop.apollo.preference.SequenceDTO
import org.bbop.apollo.preference.UserDTO
import org.bbop.apollo.preference.UserOrganismPreferenceDTO
import org.bbop.apollo.sequence.SequenceLocationDTO
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class PreferenceService {

    def permissionService

    final Integer PREFERENCE_SAVE_DELAY_SECONDS = 30  // saves every 30 seconds
    // enqueue to store save
    private Map<SequenceLocationDTO, Date> saveSequenceLocationMap = [:]
    // set of client locations
    private Set<String> currentlySavingLocation = new HashSet<>()
    private Date lastSaveEvaluation = new Date()


    Organism getSessionOrganism(String clientToken) {
        JSONObject preferenceObject = getSessionPreferenceObject(clientToken)
        if (preferenceObject) {
            def organismId = preferenceObject.organism.id as Long
            return Organism.get(organismId) ?: Organism.findById(organismId)
        }
        return null
    }


    UserOrganismPreferenceDTO getSessionPreference(String clientToken) {
        JSONObject preferenceObject = getSessionPreferenceObject(clientToken)
        if (preferenceObject) {
            def preferenceId = preferenceObject.id as Long
            UserOrganismPreference userOrganismPreference = UserOrganismPreference.get(preferenceId) ?: UserOrganismPreference.findById(preferenceId)
            return userOrganismPreference ? getDTOFromPreference(userOrganismPreference) : null
        }
        return null
    }

    JSONObject getSessionPreferenceObject(String clientToken) {
        try {
            Session session = SecurityUtils.subject.getSession(false)
            if (session) {
                // should be client_token , JSONObject
                def keys = session.getAttributeKeys()
                String preferenceString = session.getAttribute(FeatureStringEnum.PREFERENCE.getValue() + "::" + clientToken)?.toString()
                if (!preferenceString) return null
                return JSON.parse(preferenceString) as JSONObject
            } else {
                log.debug "No session found"
            }
        } catch (e) {
            log.debug "faild to get the gession preference objec5 ${e}"
        }
        return null
    }

    UserOrganismPreferenceDTO setSessionPreference(String clientToken, UserOrganismPreferenceDTO userOrganismPreference) {
        Session session = SecurityUtils.subject.getSession(false)
        if (session) {
            // should be client_token , JSONObject
            String preferenceString = (userOrganismPreference as JSON).toString()
            session.setAttribute(FeatureStringEnum.PREFERENCE.getValue() + "::" + clientToken, preferenceString)
        } else {
            log.warn "No session found"
        }
        return userOrganismPreference
    }


    Organism getCurrentOrganismForCurrentUser(String clientToken) {
        log.debug "PS: getCurrentOrganismForCurrentUser ${clientToken}"
        Organism organism = getSessionOrganism(clientToken)
        log.debug "found organism in session ${organism} so returning"
        if (organism) return organism
        if (permissionService.currentUser == null) {
            log.warn "No user present, so using the client token"
            organism = getOrganismForTokenInDB(clientToken)
            return organism
        } else {
            UserOrganismPreferenceDTO userOrganismPreference = getCurrentOrganismPreference(permissionService.currentUser, null, clientToken)
            OrganismDTO organismDTO = setSessionPreference(clientToken, userOrganismPreference)?.organism
            return Organism.findById(organismDTO.id)
        }
    }

    Organism getOrganismForTokenInDB(String token) {
        log.debug "token for org ${token}"
        if (token.isLong()) {
            log.debug "is long "
            return Organism.findById(Long.parseLong(token))
        } else {
            log.debug "is NOT long "
            return Organism.findByCommonNameIlike(token)
        }
    }

    Organism getOrganismForToken(String token) {
        Organism organism = getSessionOrganism(token)
        if (organism) {
            return organism
        } else {
            return getOrganismForTokenInDB(token)
        }
    }


    UserOrganismPreferenceDTO setCurrentOrganism(User user, Organism organism, String clientToken) {
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = getCurrentOrganismPreference(user, null, clientToken)
        if (userOrganismPreferenceDTO.organism.id == organism.id) {
            log.info "Same organism so not changing preference"
            return userOrganismPreferenceDTO
        }
        // we have to go to the database to see if there was a prior sequence
        UserOrganismPreference userOrganismPreference = UserOrganismPreference.findByUserAndOrganismAndCurrentOrganism(user, organism, true, [sort: "lastUpdated", order: "desc"])
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndOrganism(user, organism, [sort: "lastUpdated", order: "desc"])
        SequenceDTO sequenceDTO = getDTOFromSequence(userOrganismPreference?.sequence ? userOrganismPreference.sequence : organism.sequences.first())
        OrganismDTO organismDTO = getDTOFromOrganism(organism)
        userOrganismPreferenceDTO.organism = organismDTO
        userOrganismPreferenceDTO.sequence = sequenceDTO

        setSessionPreference(clientToken, userOrganismPreferenceDTO)
        setCurrentOrganismInDB(userOrganismPreferenceDTO)
        return userOrganismPreferenceDTO
    }

    /**
     * We can save the preference locally.
     *
     * In the database we set this organism and sequence to true.
     *
     * 1. If there is none for this organism + sequence + client_token
     *    Then we generate it
     * 2. If there is one for this organism + sequence + client_token
     *    We retrieve it and update the start / stop
     *
     * Else . . .
     *
     * @param userOrganismPreferenceDTO
     * @return
     */
    UserOrganismPreference setCurrentOrganismInDB(UserOrganismPreferenceDTO userOrganismPreferenceDTO) {
//        UserOrganismPreference setCurrentOrganismInDB(User user, Organism organism, String clientToken) {
        UserOrganismPreference organismPreference = userOrganismPreferenceDTO.id ? UserOrganismPreference.findById(userOrganismPreferenceDTO.id) : null

        // 2. update it
        if(organismPreference
                && userOrganismPreferenceDTO.clientToken == organismPreference.clientToken
                && userOrganismPreferenceDTO.sequence.name == organismPreference.sequence.name
                && userOrganismPreferenceDTO.organism.id == organismPreference.organism.id
        ){
            organismPreference.startbp = userOrganismPreferenceDTO.startbp
            organismPreference.endbp = userOrganismPreferenceDTO.endbp
            organismPreference.save()
        }
        // 1. create a new one
        else{
            organismPreference = generateNewPreferenceFromDTO(userOrganismPreferenceDTO)
        }

        // if the preference already exists
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientToken(organismPreference.user, organismPreference.organism, organismPreference.clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first())
        }
//
//        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
//        if (!userOrganismPreference) {
//            userOrganismPreference = new UserOrganismPreference(
//                    user: userOrganismPreference.user
//                    , organism: u
//                    , currentOrganism: true
//                    , sequence: Sequence.findByOrganism(organism)
//                    , clientToken: clientToken
//            ).save(flush: true, insert: true)
//        } else if (!userOrganismPreference.currentOrganism) {
//            userOrganismPreference.currentOrganism = true;
//            userOrganismPreference.save(flush: true, insert: false)
//        }
        int affected = setOtherCurrentOrganismsFalseInDB(organismPreference, organismPreference.user, organismPreference.clientToken)
        println "others set to false: ${affected}"
        return organismPreference
    }

    protected static
    def setOtherCurrentOrganismsFalseInDB(UserOrganismPreference userOrganismPreference) {
        return setOtherCurrentOrganismsFalseInDB(userOrganismPreference, userOrganismPreference.user, userOrganismPreference.clientToken)
    }

    protected static
    def setOtherCurrentOrganismsFalseInDB(UserOrganismPreference userOrganismPreference, User user, String clientToken) {
        int affected = UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = false " +
                        "where pref.id != :prefId and pref.user = :user and pref.clientToken = :clientToken",
                [prefId: userOrganismPreference.id, user: user, clientToken: clientToken])

        Date now = new Date()
        Date newDate = new Date(now.getTime() - 8 * 1000 * 60 * 60)
        // no current organisms if they are 8 hours hold
        affected += UserOrganismPreference.executeUpdate(
                "update UserOrganismPreference  pref set pref.currentOrganism = false " +
                        "where pref.id != :prefId and pref.user = :user and pref.organism = :organism and pref.sequence = :sequence and pref.lastUpdated < :lastUpdated ",
                [prefId: userOrganismPreference.id, user: user, organism: userOrganismPreference.organism, sequence: userOrganismPreference.sequence, lastUpdated: newDate])
        return affected
    }

    UserOrganismPreferenceDTO setCurrentSequence(User user, Sequence sequence, String clientToken) {
        UserOrganismPreferenceDTO userOrganismPreference = getCurrentOrganismPreference(user, sequence.name, clientToken) ?: null
        println "found an organism for name ${userOrganismPreference} . . start /end ${userOrganismPreference?.startbp} - ${userOrganismPreference?.endbp} "
        if (!userOrganismPreference) {
            userOrganismPreference = getCurrentOrganismPreference(user, null, clientToken)
            SequenceDTO sequenceDTO = getDTOFromSequence(sequence)
            userOrganismPreference.sequence = sequenceDTO
        }
        setSessionPreference(clientToken, userOrganismPreference)
        return getDTOFromPreference(setCurrentSequenceInDB(user, sequence, clientToken))
    }

    UserOrganismPreference setCurrentSequenceInDB(User user, Sequence sequence, String clientToken) {
        Organism organism = sequence.organism
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndOrganismAndClientTokenAndSequence(user, organism, clientToken, sequence, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences for sequence and organism: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), user, clientToken)
        }

        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null

        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
                    , clientToken: clientToken
            ).save(flush: true, insert: true)
        } else if (!userOrganismPreference.currentOrganism) {
            userOrganismPreference.currentOrganism = true;
            userOrganismPreference.sequence = sequence
            userOrganismPreference.save(flush: true, insert: false)
        }
        setOtherCurrentOrganismsFalseInDB(userOrganismPreference, user, clientToken)
        return userOrganismPreference
    }

    UserOrganismPreferenceDTO setCurrentSequenceLocation(String sequenceName, Integer startBp, Integer endBp, String clientToken) {
        UserOrganismPreferenceDTO userOrganismPreference = getCurrentOrganismPreference(permissionService.currentUser, sequenceName, clientToken)
        if (userOrganismPreference.sequence.name != sequenceName || userOrganismPreference.sequence?.organism?.id != userOrganismPreference.organism.id) {
            Organism organism = Organism.findById(userOrganismPreference.organism.id)
            Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
            userOrganismPreference.sequence = getDTOFromSequence(sequence)
        }
        userOrganismPreference.startbp = startBp
        userOrganismPreference.endbp = endBp
        userOrganismPreference.currentOrganism = true
        setSessionPreference(clientToken, userOrganismPreference)

        SequenceLocationDTO sequenceLocationDTO = new SequenceLocationDTO(
                sequenceName: sequenceName,
                startBp: startBp,
                endBp: endBp,
                clientToken: clientToken
        )
        scheduleSaveSequenceLocationInDB(sequenceLocationDTO)

        return userOrganismPreference
    }

    def evaluateSaves(boolean forceSaves = false) {
        println "evaluating saves: ${forceSaves}"
        long timeDiff = (new Date()).getTime() - lastSaveEvaluation.getTime()
        if (!forceSaves && timeDiff / 1000.0 < PREFERENCE_SAVE_DELAY_SECONDS) {
            println "not yet: ${timeDiff}"
            return
        }
        println "saving this! : ${timeDiff}"
        lastSaveEvaluation = new Date()
        saveSequenceLocationMap.each {
            evaluateSave(it.value, it.key)
        }

    }

    def evaluateSave(Date date, SequenceLocationDTO sequenceLocationDTO) {
        try {
            currentlySavingLocation.add(sequenceLocationDTO.clientToken)
            Date now = new Date()
            println "trying to save it ${sequenceLocationDTO.clientToken}"
            def timeDiff = (now.getTime() - date.getTime()) / 1000
            if (timeDiff > PREFERENCE_SAVE_DELAY_SECONDS) {
                println "saving ${sequenceLocationDTO.clientToken} location to the database time: ${timeDiff}"
                setCurrentSequenceLocationInDB(sequenceLocationDTO)
            } else {
                println "not saving ${sequenceLocationDTO.clientToken} location to the database time: ${timeDiff}"
            }
        } catch (e) {
            log.error "Problem saving ${e}"
        } finally {
            currentlySavingLocation.remove(sequenceLocationDTO.clientToken)
        }
    }

    def scheduleDbSave(Date date, SequenceLocationDTO sequenceLocationDTO) {
        // these should replace, though
        if (!currentlySavingLocation.contains(sequenceLocationDTO.clientToken)) {
            // saves with the latest one now
            currentlySavingLocation.add(sequenceLocationDTO.clientToken)
        }
        saveSequenceLocationMap.put(sequenceLocationDTO, date)
    }

    def scheduleSaveSequenceLocationInDB(SequenceLocationDTO sequenceLocationDTO) {
        Date date = saveSequenceLocationMap.get(sequenceLocationDTO)
        println "date: ${date}"
        if (!date) {
            println "no last save found so saving ${sequenceLocationDTO.clientToken} location to the database"
            setCurrentSequenceLocationInDB(sequenceLocationDTO)
        } else {
            println "evaludate save : ${date}"
            scheduleDbSave(date, sequenceLocationDTO)
        }
        saveSequenceLocationMap.put(sequenceLocationDTO, new Date())
        saveOutstandingLocationPreferences(sequenceLocationDTO.clientToken)
    }

    def saveOutstandingLocationPreferences(String ignoreToken = "") {
        saveSequenceLocationMap.each {
            if (it.key.clientToken != ignoreToken) {
                scheduleDbSave(it.value, it.key)
            }
        }
    }

    UserOrganismPreference generateNewPreferenceFromDTO(UserOrganismPreferenceDTO userOrganismPreferenceDTO) {
        Organism organism = Organism.findById(userOrganismPreferenceDTO.organism.id)
        Sequence sequence = Sequence.findByNameAndOrganism(userOrganismPreferenceDTO.sequence.name, organism)
        User user = User.findByUsername(userOrganismPreferenceDTO.user.username)
        UserOrganismPreference userOrganismPreference = new UserOrganismPreference(
                organism: organism
                , user: user
                , sequence: sequence
                , currentOrganism: userOrganismPreferenceDTO.currentOrganism
                , nativeTrackList: userOrganismPreferenceDTO.nativeTrackList
                , startbp: userOrganismPreferenceDTO.startbp
                , endbp: userOrganismPreferenceDTO.endbp
                , clientToken: userOrganismPreferenceDTO.clientToken
        ).save(insert: true )
        return userOrganismPreference
    }

    SequenceDTO getDTOFromSequence(Sequence sequence) {
        OrganismDTO organismDTO = getDTOFromOrganism(sequence.organism)
        SequenceDTO sequenceDTO = new SequenceDTO(
                id: sequence.id
                , organism: organismDTO
                , name: sequence.name
                , start: sequence.start
                , end: sequence.end
                , length: sequence.length
        )
        return sequenceDTO
    }

    OrganismDTO getDTOFromOrganism(Organism organism) {
        OrganismDTO organismDTO = new OrganismDTO(
                id: organism.id
                , commonName: organism.commonName
                , directory: organism.directory
        )
        return organismDTO
    }

    UserDTO getDTOFromUser(User user) {
        UserDTO userDTO = new UserDTO(
                id: user.id
                , username: user.username
        )
        return userDTO
    }

    UserOrganismPreferenceDTO getDTOFromPreference(UserOrganismPreference userOrganismPreference) {
        Sequence sequence = userOrganismPreference.sequence
        SequenceDTO sequenceDTO = getDTOFromSequence(sequence)
        UserDTO userDTO = getDTOFromUser(userOrganismPreference.user)
        UserOrganismPreferenceDTO userOrganismPreferenceDTO = new UserOrganismPreferenceDTO(
                organism: sequenceDTO.organism
                , sequence: sequenceDTO
                , id: userOrganismPreference.id
                , user: userDTO
                , currentOrganism: userOrganismPreference.currentOrganism
                , nativeTrackList: userOrganismPreference.nativeTrackList
                , startbp: userOrganismPreference.startbp
                , endbp: userOrganismPreference.endbp
                , clientToken: userOrganismPreference.clientToken
        )
        return userOrganismPreferenceDTO
    }


    UserOrganismPreference setCurrentSequenceLocationInDB(SequenceLocationDTO sequenceLocationDTO) {
        println "saving location in DB"
        saveSequenceLocationMap.remove(saveSequenceLocationMap)
        User currentUser = permissionService.currentUser
        String sequenceName = sequenceLocationDTO.sequenceName
        String clientToken = sequenceLocationDTO.clientToken
        Integer startBp = sequenceLocationDTO.startBp
        Integer endBp = sequenceLocationDTO.endBp

        OrganismDTO currentOrganism = getCurrentOrganismPreference(currentUser, sequenceName, clientToken)?.organism
        if (!currentOrganism) {
            throw new AnnotationException("Organism preference is not set for user")
        }
        Organism organism = Organism.findById(currentOrganism.id)
        Sequence sequence = Sequence.findByNameAndOrganism(sequenceName, organism)
        if (!sequence) {
            throw new AnnotationException("Sequence name is invalid ${sequenceName}")
        }

//        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(currentUser, true, clientToken,[sort: "lastUpdated", order: "desc"])
        def userOrganismPreferences = UserOrganismPreference.createCriteria().list {
            createAlias('sequence', 'sequence', org.hibernate.criterion.CriteriaSpecification.LEFT_JOIN)
            and {
                eq("user", currentUser)
                eq("clientToken", clientToken)
                eq("sequence.name", sequenceName)
                eq("organism", organism)
            }
        }
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), currentUser, clientToken)
        }
        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (!userOrganismPreference) {
            userOrganismPreference = new UserOrganismPreference(
                    user: currentUser
                    , currentOrganism: true
                    , sequence: sequence
                    , organism: organism
                    , clientToken: clientToken
            ).save(insert: true)
        }

        log.debug "version ${userOrganismPreference.version} for ${userOrganismPreference.organism.commonName} ${userOrganismPreference.currentOrganism}"

        userOrganismPreference.refresh()

        userOrganismPreference.clientToken = clientToken
        userOrganismPreference.currentOrganism = true
        userOrganismPreference.sequence = sequence

        // use the current value if we aren't setting it
        if (userOrganismPreference.startbp) {
            userOrganismPreference.startbp = startBp ?: userOrganismPreference.startbp
        } else {
            userOrganismPreference.startbp = startBp ?: 0
        }

        if (userOrganismPreference.endbp) {
            userOrganismPreference.endbp = endBp ?: userOrganismPreference.endbp
        } else {
            userOrganismPreference.endbp = endBp ?: sequence.end
        }

        userOrganismPreference.save(flush: true, insert: false)
    }

    UserOrganismPreferenceDTO getCurrentOrganismPreference(User user, String sequenceName, String clientToken) {
        UserOrganismPreferenceDTO preference = getSessionPreference(clientToken)
        println "found in-memory preference: ${preference ? preference as JSON : null}"
        return preference ?: getDTOFromPreference(getCurrentOrganismPreferenceInDB(user, sequenceName, clientToken))
    }

    UserOrganismPreference getCurrentOrganismPreferenceInDB(User user, String sequenceName, String clientToken) {
        if (!user && !clientToken) {
            log.warn("No organism preference if no user ${user} or client token ${clientToken}")
            return null
        }

        // 1 - if a user exists, look up their client token and if they have a current organism.
        def userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(user, true, clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), user, clientToken)
        }
        UserOrganismPreference userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (userOrganismPreference) {
            return userOrganismPreference
        }

        // 2 - if there is not a current organism for that token, then grab the first non-current one (unlikely) and make it current
        userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganismAndClientToken(user, false, clientToken, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), user, clientToken)
        }
        userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null
        if (userOrganismPreference) {
            setOtherCurrentOrganismsFalseInDB(userOrganismPreference, user, clientToken)
            userOrganismPreference.currentOrganism = true
            userOrganismPreference.save(flush: true, insert: false)
            return userOrganismPreference
        }

        //3 - if none at all exist, we should ignore the client token and look it up by the user (missing), saving it for the current client token
        // we create a new one off of that, but for this client token
        userOrganismPreferences = UserOrganismPreference.findAllByUserAndCurrentOrganism(user, true, [sort: "lastUpdated", order: "desc"])
        if (userOrganismPreferences.size() > 1) {
            log.warn("Multiple preferences found: " + userOrganismPreferences.size())
            setOtherCurrentOrganismsFalseInDB(userOrganismPreferences.first(), user, clientToken)
        }
        userOrganismPreference = userOrganismPreferences ? userOrganismPreferences.first() : null

        // just grab an adjacent one for that user, that is not current
        userOrganismPreference = userOrganismPreference ?: UserOrganismPreference.findByUserAndCurrentOrganism(user, false, [max: 1, sort: "lastUpdated", order: "desc"])
        if (userOrganismPreference) {
            Organism organism = userOrganismPreference.organism
            Sequence sequence = sequenceName ? Sequence.findByNameAndOrganism(sequenceName, organism) : userOrganismPreference.sequence
            UserOrganismPreference newPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
                    , startbp: userOrganismPreference.startbp
                    , endbp: userOrganismPreference.endbp
                    , clientToken: clientToken
            ).save(insert: true, flush: true)
            return newPreference
        }

        // 4 - if none at all exist, then we create one
        if (!userOrganismPreference) {
            // find a random organism based on sequence
            Sequence sequence = sequenceName ? Sequence.findByName(sequenceName) : null
            Set<Organism> organisms = permissionService.getOrganisms(user)
//            Organism organism = sequence ? sequence.organism : organisms?.first()
            Organism organism
            if (sequence) {
                organism = sequence.organism
            }
            if (!organism && organisms) {
                organism = organisms.first()
            }
            if (!organism && permissionService.isAdmin()) {
                organism = Organism.first()
            }
            if (!organism) {
                throw new PermissionException("User does not have permission for any organisms.")
            }

            sequence = sequence ?: organism.sequences?.first()
            UserOrganismPreference newUserOrganismPreference = new UserOrganismPreference(
                    user: user
                    , organism: organism
                    , currentOrganism: true
                    , sequence: sequence
                    , clientToken: clientToken
            )
            if (sequence) {
                newUserOrganismPreference.startbp = sequence.start
                newUserOrganismPreference.endbp = sequence.end
            }
            newUserOrganismPreference.save(insert: true, flush: true)
            return newUserOrganismPreference
        }

        return userOrganismPreference
    }

    UserOrganismPreference getCurrentOrganismPreferenceInDB(String token) {
        getCurrentOrganismPreferenceInDB(permissionService.getCurrentUser(), null, token)
    }

    /**
     * 1. Find all preferences a month old or more
     * 2. For each client token shared by a user and more than one organism
     * 3. Delete the older set of client tokens for each organisms / user combination
     */
    def removeStalePreferences() {

        try {
            log.info "Removing stale preferences"
            Date lastMonth = new Date().minus(0)
            int removalCount = 0

            // get user client tokens
            Map<User, Map<Organism, UserOrganismPreference>> userClientTokens = [:]
            UserOrganismPreference.findAllByLastUpdatedLessThan(lastMonth, [sort: "lastUpdated", order: "desc"]).each {
                Map<Organism, UserOrganismPreference> organismPreferenceMap = userClientTokens.containsKey(it.user) ? userClientTokens.get((it.user)) : [:]
                if (organismPreferenceMap.containsKey(it.organism)) {
                    UserOrganismPreference preference = organismPreferenceMap.get(it.organism)
                    // since we are sorting from the newest to the oldest, so just delete the older one
                    preference.delete()
                    ++removalCount
                    organismPreferenceMap.remove(it.organism)
                } else {
                    organismPreferenceMap.put(it.organism, it)
                }
                userClientTokens.put(it.user, organismPreferenceMap)
            }

            log.info "Removed ${removalCount} stale preferences"
        } catch (e) {
            log.error("Error removing preferences ${e}")
        }

    }
}
