package org.bbop.apollo.gwt.shared.projection;

/**
 * This an object that maps the projected chunk requested to the actual chunk.
 *
 * The index within the ChunkList is the projected chunk.  
 *
 * Created by nathandunn on 11/10/15.
 */
public class ProjectionChunk {

    /**
     * what is the sequence name
     */
    private String sequenceName = null;

    /**
     * The original ID.
     */
    private Integer originalChunkIndex = null ;

    private Integer projectedChunkIndex = null ;

    /**
     * what is the LAST bp of the prior sequence.
     */
    private Long sequenceOffset = 0L;

    /**
     *  if I have chunks 50 and 52 . . . they are probably 1 and 3 . . but have to map to the right sequence
     */
//    private Integer chunkArrayOffset = 0;

    public Integer getOriginalChunkIndex() {
        return originalChunkIndex;
    }

    public void setOriginalChunkIndex(Integer originalChunkIndex) {
        this.originalChunkIndex = originalChunkIndex;
    }

    public Integer getProjectedChunkIndex() {
        return projectedChunkIndex;
    }

    public void setProjectedChunkIndex(Integer projectedChunkIndex) {
        this.projectedChunkIndex = projectedChunkIndex;
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public Long getSequenceOffset() {
        return sequenceOffset;
    }

    public void setSequenceOffset(Long sequenceOffset) {
        this.sequenceOffset = sequenceOffset;
    }

    public ProjectionChunk copy() {
        ProjectionChunk projectionChunk = new ProjectionChunk();
        projectionChunk.sequenceName = this.sequenceName ;
        projectionChunk.originalChunkIndex = this.projectedChunkIndex;
        projectionChunk.projectedChunkIndex = this.projectedChunkIndex;
        projectionChunk.sequenceOffset = this.sequenceOffset ;
        return projectionChunk;
    }
}
