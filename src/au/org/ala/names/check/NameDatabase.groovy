package au.org.ala.names.check

import com.opencsv.CSVReader

import java.lang.invoke.MethodHandleImpl
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;

 * Copyright (c) 2015 CSIRO
 */
class NameDatabase {
    static RANKS = [
            "reg"        : 1,
            "phyl_div"   : 2,
            "division"   : 2,
            "div"        : 2,
            "subphyl_div": 3,
            "subdivision": 3,
            "supercl"    : 4,
            "cl"         : 5,
            "subcl"      : 6,
            "infracl"    : 7,
            "superord"   : 8,
            "superordo"  : 8,
            "cohort"     : 9,
            "ord"        : 9,
            "ordo"       : 9,
            "subord"     : 10,
            "subordo"    : 10,
            "infraord"   : 11,
            "section"    : 12,
            "superfam"   : 14,
            "fam"        : 15,
            "subfam"     : 16,
            "supertrib"  : 17,
            "trib"       : 18,
            "subtrib"    : 19,
            "gen"        : 20,
            "subgen"     : 21,
            "subg"       : 21,
            "sect"       : 22,
            "subsect"    : 23,
            "subsp_aggr" : 24,
            "sp"         : 25,
            "ssp"        : 26,
            "subsp"      : 27,
            "var"        : 28,
            "f"          : 29,
            "cv"         : 29,
            "taxsupragen": -1
    ]

    Map<String, List<CommonName>> commonNames = [:]
    Map<String, List<Synonym>> synonyms = [:]
    Map<String, List<Taxon>> taxa = [:]
    List<ErrorReport> errors = []

    NameDatabase(File dwca) {
        load(dwca)
    }

    protected <T extends NameEntry> void loadNames(InputStream is, Class<T> clazz, Map<String, List<T>> dictionary, boolean allowDuplicates) {
        Reader reader = null
        CSVReader csv
        String[] line
        T entry
        List<T> entries

        try {
            reader = new InputStreamReader(is)
            csv = new CSVReader(reader, (char) ',', (char) '\'', 1)
            while ((line = csv.readNext()) != null) {
                entry = clazz.newInstance(line)
                entries = dictionary[entry.key()]
                if (entries != null && !allowDuplicates)
                    error(entry, "Duplicate entry at ${entry.key()}", ErrorClass.DUPLICATE)
                if (entries == null) {
                    entries = []
                    dictionary.put(entry.key(), entries)
                }
                entries << entry
            }
        } finally {
            if (is != null)
                is.close()
        }
    }

    protected void load(File zip) {
        ZipFile zf = new ZipFile(zip)
        XmlSlurper slurper = new XmlSlurper()
        ZipEntry entry
        def meta

        meta = slurper.parse(zf.getInputStream(zf.getEntry("meta.xml")))
        entry = zf.getEntry(meta.core.files.location.text())
        loadNames(zf.getInputStream(entry), Taxon.class, taxa, false)
        for (ext in meta.extension) {
            entry = zf.getEntry(ext.files.location.text())
            if (entry.name.toLowerCase().contains("common"))
                loadNames(zf.getInputStream(entry), CommonName.class, commonNames, true)
            if (entry.name.toLowerCase().contains("synonym"))
                loadNames(zf.getInputStream(entry), Synonym.class, synonyms, true)
        }
    }

    def error(NameEntry entry, error, ErrorClass errorClass) {
        errors << new ErrorReport(entry, error, errorClass)
    }

    def check() {
        for (n in commonNames.values())
            for (name in n)
                name.check(this)
        for (s in synonyms.values())
            for (synonym in s)
                synonym.check(this)
        for (t in taxa.values())
            for (taxon in t)
                taxon.check(this)
    }

    def report(PrintWriter writer, int limit) {
        writer.println("\"Origin\",\"Rank\",\"Name\",\"LSID\",\"Error\",\"Error Class\"")
        for (ec in ErrorClass.values()) {
            def errs = errors.findAll { err -> err.errorClass == ec }
            def lim = limit < 0 ? errs.size() : Math.min(limit, errs.size())
            writer.println("\"Count\",\"\",\"\",\"\",\"${errs.size()} errors (first ${lim} shown)\",\"${ec}\"")
            for (int i = 0; i < lim; i++)
                errs[i].report(writer)
        }
        writer.println("\"Total\",\"\",\"\",\"\",\"${errors.size()} errors\",\"\"")
    }
}
