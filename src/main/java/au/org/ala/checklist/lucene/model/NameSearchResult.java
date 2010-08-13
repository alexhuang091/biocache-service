
package au.org.ala.checklist.lucene.model;

import org.apache.lucene.document.Document;

import au.org.ala.checklist.lucene.CBCreateLuceneIndex.IndexField;
import au.org.ala.data.model.LinnaeanRankClassification;
import au.org.ala.data.util.RankType;
import org.apache.commons.lang.StringUtils;

/**
 * A model to store the required information in a search result
 *
 * This includes the type of match that was used to get the result
 * 
 * @author Natasha
 */
public class NameSearchResult {
    private long id;
    private String lsid;
    private String cleanName;
    private boolean isHomonym;
    private String acceptedLsid;
    private long acceptedId = -1;
    private String kingdom;
    private LinnaeanRankClassification rankClass;
    private RankType rank;
    public enum MatchType{
        DIRECT,
        ALTERNATE,
        SEARCHABLE
    }
    private MatchType matchType;
    public NameSearchResult(String id, String lsid, MatchType type){
        this.id = Long.parseLong(id);
        this.lsid = StringUtils.trimToNull(lsid)==null ? id : StringUtils.trimToNull(lsid);
        matchType = type;
        isHomonym = false;
    }
    public NameSearchResult(Document doc, MatchType type){
        this(doc.get(IndexField.ID.toString()), doc.get(IndexField.LSID.toString()), type);
        kingdom = doc.get(RankType.KINGDOM.getRank());
        //System.out.println("Rank to use : " +doc.get(IndexField.RANK.toString()));
        rank = RankType.getForId(Integer.parseInt(doc.get(IndexField.RANK.toString())));
        rankClass = new LinnaeanRankClassification(doc.get(RankType.KINGDOM.getRank()),
                                                   doc.get(RankType.PHYLUM.getRank()),
                                                   doc.get(RankType.CLASS.getRank()),
                                                   doc.get(RankType.ORDER.getRank()),
                                                   doc.get(RankType.FAMILY.getRank()),
                                                   doc.get(RankType.GENUS.getRank()),
                                                   null);
        rankClass.setSpecies(doc.get(RankType.SPECIES.getRank()));
        String syn = doc.get(IndexField.ACCEPTED.toString());
        if(syn != null){
            String[] synDetails = syn.split("\t",2);
            acceptedLsid = StringUtils.trimToNull(synDetails[1]) == null?synDetails[0]:synDetails[1];
            try{
                acceptedId = Long.parseLong(synDetails[0]);
            }
            catch(NumberFormatException e){
                acceptedId = -1;
            }
        }
    }
    public LinnaeanRankClassification getRankClassification(){
        return rankClass;
    }
    public String getKingdom(){
        return kingdom;
    }
    /**
     * Return the LSID for the result if it is not null otherwise return the id.
     * @return
     */
    public String getLsid(){
        if(lsid !=null || id <1)
            return lsid;
        return Long.toString(id);
    }
    public long getId(){
        return id;
    }
    public MatchType getMatchType(){
        return matchType;
    }
    public void setMatchType(MatchType type){
        matchType = type;
    }
    public String getCleanName(){
        return cleanName;
    }
    public void setCleanName(String name){
        cleanName = name;
    }
    public boolean hasBeenCleaned(){
        return cleanName != null;
    }
    public boolean isHomonym(){
        return isHomonym;
    }
    public boolean isSynonym(){
        return acceptedLsid != null || acceptedId >0;
    }
    /**
     *
     * @return
     * @deprecated Use {@link #getAcceptedId()} instead;
     */
    @Deprecated
    public long getSynonymId(){
        return getAcceptedId();
    }
    public long getAcceptedId(){
        return acceptedId;
    }
    /**
     * When the LSID for the synonym is null return the ID for the synonym
     * @return
     * @deprecated Use {@link #getAcceptedLsid()} instead
     */
    @Deprecated
    public String getSynonymLsid(){
        return getAcceptedLsid();
    }
    /**
     *
     * @return The accepted LSID for this name.  When the
     * name is not a synonym null is returned
     */
    public String getAcceptedLsid(){
        if(acceptedLsid != null || acceptedId<1)
            return acceptedLsid;
        else
            return Long.toString(acceptedId);
    }
    @Override
    public String toString(){
        return "Match: " + matchType + " id: " + id+ " lsid: "+ lsid+ " classification: " +rankClass +" synonym: "+ acceptedLsid;
    }

    public RankType getRank() {
        return rank;
    }

    public void setRank(RankType rank) {
        this.rank = rank;
    }
}
