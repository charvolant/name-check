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
* Odd-looking numbers of common names and synonyms attached to a taxon
* Taxa with no parent that don't appear to be a common ancestor

To run it, you need to run the Check script with the name of a DwCA zip archive and (optionally) the name of
a CSV file to dump the error report to.

## Ranks

The rank structure files are JSON files that describe the taxonomic levels used by a particular field.
There are two files botany-ranks.json and zoology-ranks.json.
Levels are automatically allocated; explicitly specifying a level less than 0 indicates that the
rank doesn't fit very well into the ranking structure and should be ignored when trying to build
a view of whether the parent/child taxa are consistent.

## Stuff that is not so good

The CSV files in the archive are assumed to have a single, consistent structure.
This follows the structure of the TREE-DWC.zip files at https://biodiversity.org.au/dataexport/20150129T1100/
A proper DwCA mapping would be an improvement.