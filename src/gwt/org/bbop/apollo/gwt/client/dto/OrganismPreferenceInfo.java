package org.bbop.apollo.gwt.client.dto;

import java.util.List;

/**
 * Created by ndunn on 4/21/16.
 */
public class OrganismPreferenceInfo {

    private OrganismInfo organismInfo;
    private List<SequencePreferenceInfo> sequencePreferenceInfoList;

    public OrganismInfo getOrganismInfo() {
        return organismInfo;
    }

    public void setOrganismInfo(OrganismInfo organismInfo) {
        this.organismInfo = organismInfo;
    }

    public List<SequencePreferenceInfo> getSequencePreferenceInfoList() {
        return sequencePreferenceInfoList;
    }

    public void setSequencePreferenceInfoList(List<SequencePreferenceInfo> sequencePreferenceInfoList) {
        this.sequencePreferenceInfoList = sequencePreferenceInfoList;
    }
}
