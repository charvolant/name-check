package au.org.ala.names.check

/**
 * Check a DWCA name file.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;

 * Copyright (c) 2015 CSIRO
 */

def zip = new File(args[0])
def db = new NameDatabase(zip)
def pw
def lim

if (args.length == 1) {
    pw = new PrintWriter(System.out)
    lim = 10
} else {
    pw = new PrintWriter(new FileOutputStream(args[1]))
    lim = -1
}
db.check()
db.report(pw, lim)
pw.close()