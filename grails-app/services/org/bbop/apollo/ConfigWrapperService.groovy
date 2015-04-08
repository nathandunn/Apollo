package org.bbop.apollo

import grails.transaction.Transactional
import org.bbop.apollo.sequence.Overlapper
import org.bbop.apollo.sequence.SequenceTranslationHandler
import org.bbop.apollo.sequence.TranslationTable

/**
 * TODO:  move all of this stuff to a database
 */
@Transactional
class ConfigWrapperService {

    def grailsApplication

    /**
     * @deprecated
     * @return
     */
    String getRefSeqDirectory() {
        return getJBrowseDirectory()+"/seq/refSeqs.json"
    }


    /**
     * @deprecated
     * @return
     */
    String getJBrowseDirectory() {
        return grailsApplication.config.apollo.jbrowse.data.directory
    }

    Boolean useCDS() {
        return grailsApplication.config.apollo.use_cds_for_new_transcripts
    }

    TranslationTable getTranslationTable() {
        return SequenceTranslationHandler.getTranslationTableForGeneticCode(getTranslationCode())
    }

    int getTranslationCode(){
        return grailsApplication.config.apollo.get_translation_code
    }

    Overlapper getOverlapper(){
        (Overlapper) Class.forName(grailsApplication.config.apollo.overlapper_class).newInstance();
    }


    Boolean hasDbxrefs(){
        return true
    }
    Boolean hasAttributes(){
        return true
    }

    Boolean hasPubmedIds(){
        return true
    }
    Boolean hasGoIds(){
        return true
    }
    Boolean hasComments(){
        return true
    }
    Boolean hasStatus(){
        return true
    }

    List<String> getSpliceDonorSites(){
        return grailsApplication.config.apollo.splice_donor_sites
    }

    List<String> getSpliceAcceptorSites(){
        return grailsApplication.config.apollo.splice_acceptor_sites
    }

    int getDefaultMinimumIntronSize() {
        return grailsApplication.config.apollo.default_minimum_intron_size
    }

    String getBlatExecutable() {
        return grailsApplication.config.apollo.blat_executable
    }

    String getBlatDefaultParams() {
        return grailsApplication.config.apollo.blat_default_params
    }
}
