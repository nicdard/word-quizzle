package protocol.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for a ranking list item.
 */
public class RankingListItem implements Comparable<RankingListItem> {
    @JsonProperty ("n")     public String name;
    @JsonProperty ("s")     public int score;

    public RankingListItem() {}

    public RankingListItem(String name, int score) {
        this.name = name;
        this.score = score;
    }

    @Override
    public int compareTo(RankingListItem rankingListItem) {
        return this.score - rankingListItem.score;
    }
}
