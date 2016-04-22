package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

@Transactional
class AnnotatorService {

    def permissionService
    def preferenceService
    def requestHandlingService

    def getAppState(String token) {
        JSONObject appStateObject = new JSONObject()
        try {
            appStateObject.preferences = preferenceService.getPreferences()
            def organismList = permissionService.getOrganismsForCurrentUser()

            JSONObject defaultJsonOrganism = appStateObject.preferences.organism
            Organism currentOrganism = null
            currentOrganism = token ? preferenceService.getOrganismByClientToken(token) : null
            if (!currentOrganism && defaultJsonOrganism) {
                currentOrganism = Organism.findById(defaultJsonOrganism.id as Long)
            }
            // if the is no default organism just choose the first one
            if (!currentOrganism && !defaultJsonOrganism && organismList) {
                currentOrganism = organismList.first()
            }


            JSONArray organismArray = new JSONArray()
            for (Organism organism in organismList) {
                Integer annotationCount = Feature.executeQuery("select count(distinct f) from Feature f left join f.parentFeatureRelationships pfr  join f.featureLocations fl join fl.sequence s join s.organism o  where f.childFeatureRelationships is empty and o = :organism and f.class in (:viewableTypes)", [organism: organism, viewableTypes: requestHandlingService.viewableAnnotationList])[0] as Integer
                Integer sequenceCount = Sequence.countByOrganism(organism)
                JSONObject jsonObject = [
                        id             : organism.id as Long,
                        commonName     : organism.commonName,
                        blatdb         : organism.blatdb,
                        directory      : organism.directory,
                        annotationCount: annotationCount,
                        sequences      : sequenceCount,
                        genus          : organism.genus,
                        species        : organism.species,
                        valid          : organism.valid,
                        publicMode     : organism.publicMode,
                        currentOrganism: organism.id == currentOrganism?.id,
                        editable       : permissionService.userHasOrganismPermission(organism, PermissionEnum.ADMINISTRATE)

                ] as JSONObject
                organismArray.add(jsonObject)
            }
            appStateObject.organismList = organismArray
//            UserOrganismPreference currentUserOrganismPreference = permissionService.getCurrentOrganismPreference(token)
            if (!appStateObject.preferences) {
                appStateObject.preferences = new JSONObject()
            }
            appStateObject.preferences.organism = currentOrganism

            if (!appStateObject.preferences.sequence) {
                Sequence sequence = Sequence.findByOrganism(currentOrganism)
                appStateObject.preferences.sequence = sequence
//                    currentUserOrganismPreference.sequence = sequence
//                    currentUserOrganismPreference.save()
            }
//                appStateObject.preferences.sequence = currentUserOrganismPreference.sequence)
            appStateObject.preferences.sequence.start = appStateObject.preferences.sequence.start ?: -1
            appStateObject.preferences.sequence.end = appStateObject.preferences.sequence.end ?: -1

            def userPreference = UserPreference.findByUser(permissionService.currentUser)
            if(!userPreference){
                userPreference = new UserPreference(
                        user: permissionService.currentUser
                ).save(insert: true)
            }

            println "preferences count ${UserOrganismPreference.countByUser(permissionService.currentUser)}"
            userPreference.preferenceString = appStateObject.toString()
            userPreference.save(flush:true)

        }
        catch (PermissionException e) {
            def error = [error: "Error: " + e]
            log.error(error.error)
            return error
        }



        return appStateObject
    }
}
