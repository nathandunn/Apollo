package org.bbop.apollo

/**
 * Inherited from here:
 * AbstractSingleLocationBioFeature
 */
class Gene extends SequenceFeature{


    static constraints = {
    }


    static String ontologyId = "SO:0000704"// XX:NNNNNNN
    static String cvTerm = "Gene"// may have a link
    static String alternateCvTerm = "gene"


}
