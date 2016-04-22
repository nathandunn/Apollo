package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional

@Transactional
class DomainMarshallerService {

    def registerObjects() {

        JSON.registerObjectMarshaller(User) {
            def returnArray = [:]
            returnArray['userId'] = it.id
            returnArray['username'] = it.username
            returnArray['firstName'] = it.firstName
            returnArray['lastName'] = it.lastName
            return returnArray
        }

        JSON.registerObjectMarshaller(Organism) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['commonName'] = it.commonName
            returnArray['genus'] = it?.genus
            returnArray['species'] = it?.species
            returnArray['directory'] = it.directory
            return returnArray
        }

        JSON.registerObjectMarshaller(Sequence) {
            def returnArray = [:]
            returnArray['id'] = it.id
            returnArray['name'] = it.name
            returnArray['length'] = it?.length
            returnArray['start'] = it?.start
            returnArray['end'] = it.end
            return returnArray
        }

        JSON.registerObjectMarshaller(UserOrganismPreference) {
            def returnArray = [:]
            returnArray['organismId'] = it.organism.id
            returnArray['organismName'] = it.organism.commonName
            returnArray['nativeTrackList'] = it.nativeTrackList
            returnArray['sequenceId'] = it.sequence?.id
            returnArray['sequenceName'] = it.sequence?.name
            returnArray['startbp'] = it.startbp
            returnArray['endbp'] = it.endbp
            returnArray['currentOrganism'] = it.currentOrganism
            returnArray['userName'] = it.user.username
            returnArray['userId'] = it.user.id
            returnArray['lastUpdated'] = it.lastUpdated
            returnArray['dateCreated'] = it.dateCreated
            returnArray['name'] = it.name
            returnArray['preferenceString'] = it.preferenceString
            return returnArray
        }
    }
}
