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
    RankStructure ranks
    Map<String, List<CommonName>> commonNames = [:]
    Map<String, List<Synonym>> synonyms = [:]
    Map<String, List<Taxon>> taxa = [:]
    List<ErrorReport> errors = []
    /** Where to get worried about the number of common names */
    int commonNameLimit = 10
    /** Where to get worried about the number of synonyms */
    int synonymLimit = 100
    /** The total number of common names */
    int countCommonNames
    /** The total number of synonyms */
    int countSynonyms
    /** The number of common names with mutliple taxa */
    int multipleCommonNames
    /** The number of synonyms with mutliple taxa */
    int multipleSynonyms


    NameDatabase(File dwca, RankStructure ranks) {
        this.ranks = ranks;
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

    /**
     * Allocate common name and synonym counts to taxa
     */
    def computeStatistics() {
        for (t in taxa.values()) {
            t[0].commonNameCount = 0
            t[0].synonymCount = 0
        }
        countCommonNames = 0
        countSynonyms = 0
        multipleCommonNames = 0
        multipleSynonyms = 0
        for (n in commonNames.values()) {
            countCommonNames += n.size()
            if (n.size() > 1)
                multipleCommonNames++
            for (name in n)
                name.allocateStatistics(this)
        }
        for (s in synonyms.values()) {
            countSynonyms += s.size()
            if (s.size() > 1)
                multipleSynonyms++
            for (synonym in s)
                synonym.allocateStatistics(this)
        }
    }

    /**
     * Run a check on the database
     */
    def check() {
        computeStatistics()
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
        writer.println("\"Statistics\",\"\",\"\",\"\",\"\",\"\"")
        writer.println("\"\",\"Total Taxa\",\"\",\"\",\"${taxa.size()}\",\"\"")
        writer.println("\"\",\"Total Common Names\",\"\",\"\",\"${countCommonNames}\",\"\"")
        writer.println("\"\",\"Total Synonyms\",\"\",\"\",\"${countSynonyms}\",\"\"")
        writer.println("\"\",\"Multi Common Names\",\"\",\"\",\"${multipleCommonNames}\",\"\"")
        writer.println("\"\",\"Multi Synonyms\",\"\",\"\",\"${multipleSynonyms}\",\"\"")
        writer.println("\"Issues\",\"\",\"\",\"\",\"\",\"\"")
        writer.println("\"Origin\",\"Rank\",\"Name\",\"LSID\",\"Issue\",\"Class\"")
        for (ec in ErrorClass.values()) {
            def errs = errors.findAll { err -> err.errorClass == ec }
            def lim = limit < 0 ? errs.size() : Math.min(limit, errs.size())
            writer.println("\"Count\",\"\",\"\",\"\",\"${errs.size()} issues (first ${lim} shown)\",\"${ec}\"")
            for (int i = 0; i < lim; i++)
                errs[i].report(writer)
        }
        writer.println("\"Total\",\"\",\"\",\"\",\"${errors.size()} issues\",\"\"")
    }
}
