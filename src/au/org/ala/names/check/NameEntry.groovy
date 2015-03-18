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
}

class Taxon extends NameEntry {
    Taxon(String[] line) { super(line) }

    String key() { return taxonLsid }

    void check(NameDatabase database) {
        def rank = NameDatabase.RANKS[rankCode]

        if (rank == null)
            database.error(this, "Unknown rank code ${rankCode}", ErrorClass.TAXON_STRUCTURE)
        if (parentTaxonLsid != null) {
            def parents = database.taxa[parentTaxonLsid]
            def parent = null

            if (parents == null || parents.isEmpty())
                database.error(this, "Missing parent taxon LSID ${parentTaxonLsid}", ErrorClass.TAXON_STRUCTURE)
                else
                parent = parents[0]
            if (parent != null && rank != null) {
                def prank = NameDatabase.RANKS[parent.rankCode] ?: Integer.MAX_VALUE
                if (prank > 0 && rank > 0 && prank >= rank)
                    database.error(this, "Parent taxon ${parentTaxonLsid} rank ${parent.rankCode}/${prank} is not above ${rankCode}/${rank}", ErrorClass.TAXON_STRUCTURE)
            }
            // Check for cycles
            if (parent != null) {
                def seen = [taxonLsid] as Set<String>

                while (parent != null) {
                    if (seen.contains(parent.taxonLsid)) {
                        database.error(this, "Ancestor taxon ${parent.taxonLsid} contains cycle", ErrorClass.TAXON_STRUCTURE)
                        parent = null
                    } else {
                        parents = database.taxa[parent.parentTaxonLsid]
                        parent = parents == null || parents.isEmpty() ? null : parents[0]
                    }
                }
            }
        }
    }

    String classifier() { return "Taxon" }
}
