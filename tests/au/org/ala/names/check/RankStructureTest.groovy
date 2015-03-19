package au.org.ala.names.check

import org.junit.Test
import static org.junit.Assert.*

/**
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;

 * Copyright (c) 2015 CSIRO
 */
class RankStructureTest  {
    @Test
    void testConstructor1() {
        def rs = new RankStructure(this.class.getResourceAsStream("rank-structure-1.json"))
        def rank

        assertNotNull(rs.ranks)
        assertEquals(1, rs.ranks.size())
        rank = rs.ranks["kingdom"]
        assertNotNull(rank)
        assertEquals("Kingdom", rank.name)
        assertEquals(0, rank.level)
        assertNotNull(rank.synonyms)
        assertEquals(0, rank.synonyms.size())
    }

    @Test
    void testConstructor2() {
        def rs = new RankStructure(this.class.getResourceAsStream("rank-structure-2.json"))
        def rank

        assertNotNull(rs.ranks)
        assertEquals(3, rs.ranks.size())
        rank = rs.ranks["kingdom"]
        assertNotNull(rank)
        assertEquals("Kingdom", rank.name)
        assertEquals(0, rank.level)
        assertNotNull(rank.synonyms)
        assertEquals(2, rank.synonyms.size())
        assertTrue(rank.synonyms.contains("reg"))
        assertTrue(rank.synonyms.contains("kng"))
    }

    @Test
    void testConstructor3() {
        def rs = new RankStructure(this.class.getResourceAsStream("rank-structure-3.json"))
        def rank

        assertNotNull(rs.ranks)
        assertEquals(8, rs.ranks.size())
        rank = rs.ranks["division"]
        assertNotNull(rank)
        assertEquals("Phylum", rank.name)
        assertEquals(1, rank.level)
        assertNotNull(rank.synonyms)
        assertEquals(3, rank.synonyms.size())
        assertTrue(rank.synonyms.contains("div"))
        rank = rs.ranks["cl"]
        assertNotNull(rank)
        assertEquals("Class", rank.name)
        assertEquals(2, rank.level)
        assertNotNull(rank.synonyms)
        assertEquals(1, rank.synonyms.size())
        assertTrue(rank.synonyms.contains("cl"))
     }

    @Test
    void testConstructor4() {
        try {
            def rs = new RankStructure(this.class.getResourceAsStream("rank-structure-4.json"))
            fail("Expecting exception from duplicate name")
        } catch (IllegalArgumentException ex) {
        }
     }

    @Test
    void testConstructor5() {
        try {
            def rs = new RankStructure(this.class.getResourceAsStream("rank-structure-5.json"))
            fail("Expecting exception from duplicate name")
        } catch (IllegalArgumentException ex) {
        }
    }


    @Test
    void testConstructor6() {
        def rs = new RankStructure(this.class.getResourceAsStream("rank-structure-6.json"))
        def rank

        assertNotNull(rs.ranks)
        assertEquals(3, rs.ranks.size())
        rank = rs.ranks["kingdom"]
        assertNotNull(rank)
        assertEquals(0, rank.level)
        rank = rs.ranks["phylum"]
        assertNotNull(rank)
        assertEquals(-1, rank.level)
        rank = rs.ranks["class"]
        assertNotNull(rank)
        assertEquals(1, rank.level)
    }


    @Test
    void testRank1() {
        def rs = new RankStructure(this.class.getResourceAsStream("rank-structure-3.json"))
        def rank

        rank = rs.rank("nothing")
        assertNull(rank)
    }

    @Test
    void testRank2() {
        def rs = new RankStructure(this.class.getResourceAsStream("rank-structure-3.json"))
        def rank

        rank = rs.rank("class")
        assertNotNull(rank)
        assertEquals("Class", rank.name)
    }

    @Test
    void testRank3() {
        def rs = new RankStructure(this.class.getResourceAsStream("rank-structure-3.json"))
        def rank

        rank = rs.rank("CLASS")
        assertNotNull(rank)
        assertEquals("Class", rank.name)
    }

    @Test
    void testRank4() {
        def rs = new RankStructure(this.class.getResourceAsStream("rank-structure-3.json"))
        def rank

        rank = rs.rank("Reg")
        assertNotNull(rank)
        assertEquals("Kingdom", rank.name)
    }

}
