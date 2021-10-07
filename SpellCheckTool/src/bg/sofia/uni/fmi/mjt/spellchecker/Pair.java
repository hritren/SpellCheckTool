package bg.sofia.uni.fmi.mjt.spellchecker;

public record Pair(String word, Double cosineSimilarity) implements Comparable<Pair> {

    @Override
    public int compareTo(Pair o) {
        return (o.cosineSimilarity).compareTo(this.cosineSimilarity);
    }
}
