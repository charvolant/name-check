# Darwin Core Archive Taxonomy Checker

This program can be used to check a DwCA file containing taxonomy information.
The program scans the DwC archive and loads the core file (assumed to be a taxa file) and extra
files with common name and synonym information; recognised by having "common" or "synonym" in the
file name.

The program then checks the taxonomy data looking for

* Dangling common name and synonym references to taxa that are not there
* Dangling parent relationships for taxa
* Problems with taxon ranks not following the hierarchy
* Circular parent links

To run it, you need to run the Check script with the name of a DwCA zip archive and (optionally) the name of
a CSV file to dump the error report to.

## Stuff that is not so good

The CSV files in the archive are assumed to have a single, consistent structure.
This follows the structure of the TREE-DWC.zip files at https://biodiversity.org.au/dataexport/20150129T1100/
A proper DwCA mapping would be an improvement.