package org.bbop.apollo.gwt.shared.projection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nathandunn on 11/10/15.
 *
 * This is made for mapping lf-X . . in a set of sequences where the prior sequences
 * could be made of un non-chunked data.
 */

public class ProjectionChunkList {

    Integer currentProjectedChunkIndex = 1 ;
    public List<ProjectionChunk> projectionChunkList = new ArrayList<>();

    /**
     * Add this chunk
     * @param projectionChunk
     * @return
     */
    public Integer addThisChunk(ProjectionChunk projectionChunk){
        projectionChunkList.add(projectionChunk);
        return projectionChunk.getOriginalChunkIndex();
    }

    public Integer addChunk(ProjectionChunk projectionChunk){
        ProjectionChunk duplicateChunk = projectionChunk.copy();
        duplicateChunk.setProjectedChunkIndex(currentProjectedChunkIndex);
        ++currentProjectedChunkIndex ;
        duplicateChunk.setOriginalChunkIndex(projectionChunk.getOriginalChunkIndex());
        projectionChunkList.add(duplicateChunk);
        return duplicateChunk.getOriginalChunkIndex();
    }

    /**
     * If a chunk lf-{index}.json is requested, get a chunk to request the actual chunks on the back end.
     *
     * @param index
     * @return a ProjectionChunk that represents the chunk being requested.
     */
    public ProjectionChunk findProjectChunkForIndex(Integer index){
        return projectionChunkList.get(index);
    }


    ProjectionChunk findFirstProjectChunkForSequenceName(String sequenceName) {
        for(ProjectionChunk projectionChunk : projectionChunkList){
            if(projectionChunk.getSequenceName().equals(sequenceName)){
                return projectionChunk;
            }
        }
        return null;
    }
}
