package au.org.ala.names.check

/**
 * Generic name entry
 * <p>
 *     Ignores rank-code, rank, synionymy, cc_licence, cc_attributionURL, exluded
 *     until we know what to do with them
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *
 * Copyright (c) 2015 CSIRO
 */
abstract class NameEntry {
    String rankCode
    String rank
    String name
    String authority
    String fullName
    String nameLsid
    String taxonLsid
    String parentTaxonLsid
    String synonymOfLsid
    String nameUri
    String taxonUri
    String parentTaxonUri
    String synonymOfTaxonUri
    String source

    NameEntry(String[] line) {
        def nullcheck = { v -> v == null ? null : v.trim(); v == null || v.isEmpty() ? null : v }
        def tlc = { v -> v == null ? null : v.toLowerCase() }

        rankCode = tlc(nullcheck(line[0]))
        rank = tlc(nullcheck(line[1]))
        name = nullcheck line[3]
        authority = nullcheck line[4]
        fullName = nullcheck line[5]
        nameLsid = nullcheck line[6]
        taxonLsid = nullcheck line[7]
        parentTaxonLsid = nullcheck line[8]
        synonymOfLsid = nullcheck line[9]
        nameUri = nullcheck line[10]
        taxonUri = nullcheck line[11]
        parentTaxonUri = nullcheck line[12]
        synonymOfTaxonUri = nullcheck line[13]
        source = nullcheck line[14]
    }

    abstract String key()

    abstract String classifier()

    abstract void check(NameDatabase database)

    void describe(PrintWriter writer) {
        writer.print("\"")
        writer.print(classifier())
        writer.print("\",\"")
        writer.print(rankCode)
        writer.print("\",\"")
        writer.print(fullName)
        writer.print("\",\"")
        writer.print(key())
        writer.print("\"")
    }

    /**
     * Allocate statistical counts to other elements.
     * <p>
     *     By default, this does nothing.
     *     Subclasses can override this
     *
     *  @param database The database
     */
    void allocateStatistics(NameDatabase database) {
    }
}

class CommonName extends NameEntry {
    CommonName(String[] line) { super(line) }

    String key() { return nameLsid }

    void check(NameDatabase database) {
        if (synonymOfLsid == null)
            database.error(this, "No synonym LSID", ErrorClass.SYNONYM_MAPPING)
        else if (!database.taxa.containsKey(synonymOfLsid))
            database.error(this, "Missing synonym LSID ${synonymOfLsid}", ErrorClass.SYNONYM_MAPPING)
    }

    String classifier() { return "Common Name" }

    void allocateStatistics(NameDatabase database) {
        if (synonymOfLsid) {
            def taxa = database.taxa[synonymOfLsid] ?: []
            for (t in taxa)
                t.commonNameCount++
        }
    }
}

class Synonym extends NameEntry {
    Synonym(String[] line) { super(line) }

    String key() { return nameLsid }

    void check(NameDatabase database) {
        /*
        if (taxonLsid == null)
            database.error(this, "No taxon LSID", ErrorClass.TAXON_MAPPING)
        else if (!database.taxa.containsKey(taxonLsid))
            database.error(this, "Missing taxon LSID ${taxonLsid}", ErrorClass.TAXON_MAPPING)
        */
        if (synonymOfLsid == null)
            database.error(this, "No synonym LSID", ErrorClass.SYNONYM_MAPPING)
        else if (!database.taxa.containsKey(synonymOfLsid))
            database.error(this, "Missing synonym taxon LSID ${synonymOfLsid}", ErrorClass.SYNONYM_MAPPING)

    }

    String classifier() { return "Synonym" }

    void allocateStatistics(NameDatabase database) {
        if (synonymOfLsid) {
            def taxa = database.taxa[synonymOfLsid] ?: []
            for (t in taxa)
                t.synonymCount++
        }
    }
}

class Taxon extends NameEntry {
    int commonNameCount
    int synonymCount

    Taxon(String[] line) {
        super(line)
        commonNameCount = 0
        synonymCount = 0
    }

    String key() { return taxonLsid }

    void check(NameDatabase database) {
        def rk = database.ranks.rank(rank ?: rankCode)
        def rkc = database.ranks.rank(rankCode)

        // Rank errors
        if (rk == null)
            database.error(this, "Unknown rank code ${rankCode}", ErrorClass.TAXON_STRUCTURE)
        else if (rkc != null && rkc != rk)
            database.error(this, "Rank code ${rankCode} does not match rank name ${rank}", ErrorClass.TAXON_MAPPING)

        // Name statistics (catch outliers)
        if (commonNameCount > database.commonNameLimit)
            database.error(this, "Taxon has ${commonNameCount} common names", ErrorClass.TAXON_MAPPING)
        if (synonymCount > database.synonymLimit)
            database.error(this, "Taxon has ${synonymCount} synonyms", ErrorClass.TAXON_MAPPING)

        if (database.topRank != null && rk != null && rk.level > database.topRank.level && parentTaxonLsid == null)
            database.error(this, "Top rank is ${database.topRank.name}/${database.topRank.level} but taxon with rank ${rk.name}/${rk.level} has no parent", ErrorClass.TAXON_STRUCTURE)

        // Parent structure errors
        if (parentTaxonLsid != null) {
            def parents = database.taxa[parentTaxonLsid]
            def parent = null

            if (parents == null || parents.isEmpty())
                database.error(this, "Missing parent taxon LSID ${parentTaxonLsid}", ErrorClass.TAXON_STRUCTURE)
            else
                parent = parents[0]
            if (parent != null && rk != null) {
                def prank = database.ranks.rank(parent.rank ?: parent.rankCode).level ?: Integer.MAX_VALUE
                if (prank > 0 && rk.level > 0 && prank >= rk.level)
                    database.error(this, "Parent taxon ${parentTaxonLsid} rank ${parent.rank}/${prank} is not above ${rank}/${rk.level}", ErrorClass.TAXON_STRUCTURE)
            }
            // Check for cycles
            if (parent != null) {
                def seen = [taxonLsid] as Set<String>

                while (parent != null) {
                    if (seen.contains(parent.taxonLsid)) {
                        database.error(this, "Ancestor taxon ${parent.taxonLsid} contains cycle", ErrorClass.TAXON_STRUCTURE)
                        parent = null
                    } else {
                        seen << parent.taxonLsid
                        parents = database.taxa[parent.parentTaxonLsid]
                        parent = parents == null || parents.isEmpty() ? null : parents[0]
                    }
                }
            }
        }
    }

    String classifier() { return "Taxon" }
}
