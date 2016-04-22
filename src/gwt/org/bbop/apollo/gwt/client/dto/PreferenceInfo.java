package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONObject;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

import java.util.Map;

//import org.bbop.apollo.Organism;

/**
 * Created by ndunn on 4/21/16.
 */
public class PreferenceInfo {

    private OrganismInfo currentOrganism;
    private Map<OrganismInfo, OrganismPreferenceInfo> organismPreferenceInfos;

    public OrganismInfo getCurrentOrganism() {
        return currentOrganism;
    }

    public void setCurrentOrganism(OrganismInfo currentOrganism) {
        this.currentOrganism = currentOrganism;
    }

    public Map<OrganismInfo, OrganismPreferenceInfo> getOrganismPreferenceInfos() {
        return organismPreferenceInfos;
    }

    public void setOrganismPreferenceInfos(Map<OrganismInfo, OrganismPreferenceInfo> organismPreferenceInfos) {
        this.organismPreferenceInfos = organismPreferenceInfos;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.ORGANISM.getValue(),currentOrganism.toJSON());

        // each organism will have multiple sequence preferences
        JSONObject sequenceArrayObject = new JSONObject();
        if(organismPreferenceInfos!=null){
            for(OrganismInfo organismInfo : organismPreferenceInfos.keySet()){
                for(SequenceInfo sequencePreferenceInfo : organismPreferenceInfos.get(organismInfo).getSequenceInfoList()){
                    sequenceArrayObject.put(sequencePreferenceInfo.getName(),sequencePreferenceInfo.toJSON());
                }
            }
        }
        jsonObject.put(FeatureStringEnum.SEQUENCES.getValue(),sequenceArrayObject);

        return jsonObject ;
    }
}
