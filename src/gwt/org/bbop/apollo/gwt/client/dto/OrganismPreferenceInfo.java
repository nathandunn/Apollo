package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.List;
import java.util.Map;

/**
 * Created by ndunn on 4/21/16.
 */
public class OrganismPreferenceInfo {

    private OrganismInfo organismInfo;
    private List<SequenceInfo> sequenceInfoList;

    public OrganismInfo getOrganismInfo() {
        return organismInfo;
    }

    public void setOrganismInfo(OrganismInfo organismInfo) {
        this.organismInfo = organismInfo;
    }

    public List<SequenceInfo> getSequenceInfoList() {
        return sequenceInfoList;
    }

    public void setSequenceInfoList(List<SequenceInfo> sequenceInfoList) {
        this.sequenceInfoList = sequenceInfoList;
    }

    public JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.ORGANISM.getValue(),organismInfo.toJSON());
        JSONArray sequencesArray = new JSONArray();
        jsonObject.put(FeatureStringEnum.SEQUENCES.getValue(),sequencesArray);
        for(SequenceInfo sequenceInfo : sequenceInfoList){
            sequencesArray.set(sequencesArray.size(),sequenceInfo.toJSON());
        }
        return jsonObject ;
    }
}
