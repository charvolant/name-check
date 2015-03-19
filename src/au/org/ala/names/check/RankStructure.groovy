package au.org.ala.names.check

import groovy.json.JsonSlurper

/**
 * A structure for taxonomic ranks.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * Copyright (c) 2015 CSIRO
 */
class RankStructure {
    Map<String, Rank> ranks = [:]

    /**
     * Construct from a JSON source
     *
     * @param source The JSON source
     */
    RankStructure(InputStream source) {
        JsonSlurper slurper = new JsonSlurper()
        def rs = slurper.parse(source)
        int level = 1

        for (rank in rs.ranks) {
            Rank r = new Rank(rank, level)
            add(r)
            if (r.level >= 0)
                level++
        }
    }

    /**
     * Get a rank corresponding to a name
     *
     * @param name The (case-insensitive) name
     *
     * @return The matching rank, or null for not found
     */

    Rank rank(String name) {
        return ranks[name.toLowerCase()]
    }

    /**
     * Add a new rank to the rank dictionary.
     * <p>
     *     All synonyms are added to the dictionary, as well
     *
     * @param rank The new rank
     *
     * @throws IllegalArgumentException if a name has already been used
     */
    def add(Rank rank) {
        def n = rank.name.toLowerCase()
        if (ranks.containsKey(n))
            throw new IllegalArgumentException("Rank with name ${rank.name} already exists")
        ranks.put(n, rank)
        for (synonym in rank.synonyms) {
            n = synonym.toLowerCase()
            if (ranks.containsKey(n))
                throw new IllegalArgumentException("Rank with name ${synonym} already exists")
            ranks.put(n, rank)
        }
    }

    /**
     * An individual rank.
     * <p>
     *     The name contains a human-readable version of the name.
     *     The synonyms contain additional terms and abbreviations
     *     for the rank.
     */
    class Rank {
        String name
        Set<String> synonyms
        int level

        Rank(String name, Set<String> synonyms) {
            this.synonyms = synonyms
            this.name = name
            this.level = Integer.MIN_VALUE
        }

        Rank(json, level) {
            this.name = json.name
            this.synonyms = (json.synonyms ?: []) as Set<String>
            this.level = json.level ?: level
        }
    }
}
