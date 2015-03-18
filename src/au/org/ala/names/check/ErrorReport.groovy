package au.org.ala.names.check

/**
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;

 * Copyright (c) 2015 CSIRO
 */
class ErrorReport {

    def NameEntry entry
    def error
    def ErrorClass errorClass

    ErrorReport(NameEntry entry, error, ErrorClass errorClass) {
        this.error = error
        this.entry = entry
        this.errorClass = errorClass
    }

    def report(PrintWriter writer) {
        entry.describe(writer)
        writer.print(",\"")
        writer.print(error)
        writer.print("\",\"")
        writer.print(errorClass)
        writer.print("\"")
        writer.println()
    }
}

enum ErrorClass {
    DUPLICATE,
    TAXON_MAPPING,
    SYNONYM_MAPPING,
    TAXON_STRUCTURE
}