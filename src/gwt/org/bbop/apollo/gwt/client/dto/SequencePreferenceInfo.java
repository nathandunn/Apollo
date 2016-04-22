package org.bbop.apollo.gwt.client.dto;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import org.bbop.apollo.gwt.shared.FeatureStringEnum;

/**
 * Created by ndunn on 4/21/16.
 */
public class SequencePreferenceInfo {

    private Long id;
    private String name;
    private Integer startBp;
    private Integer endBp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStartBp() {
        return startBp;
    }

    public void setStartBp(Integer startBp) {
        this.startBp = startBp;
    }

    public Integer getEndBp() {
        return endBp;
    }

    public void setEndBp(Integer endBp) {
        this.endBp = endBp;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FeatureStringEnum.ID.getValue(),new JSONNumber(id));
        jsonObject.put(FeatureStringEnum.NAME.getValue(),new JSONString(name));
        jsonObject.put(FeatureStringEnum.START_BP.getValue(),new JSONNumber(startBp));
        jsonObject.put(FeatureStringEnum.END_BP.getValue(),new JSONNumber(endBp));
        return jsonObject;
    }
}
