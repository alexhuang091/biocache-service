/**************************************************************************
 *  Copyright (C) 2017 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.biocache.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AlaFileUtils}
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
class AlaFileUtilsTest {

	/**
	 * Test method for {@link au.org.ala.biocache.util.AlaFileUtils#reduceNameByVowels(java.lang.String, int)}.
	 */
	@Test
	final void testReduceNameByVowels() {
		assertEquals("scntfcNmp", AlaFileUtils.reduceNameByVowels("scientificName.p", 10));
		assertEquals("uoieaeioua", AlaFileUtils.reduceNameByVowels("uoieaeiouaeiou", 10));
	}

	/**
	 * Test method for {@link au.org.ala.biocache.util.AlaFileUtils#generateShapeHeader(java.lang.String[])}.
	 */
	@Test
	final void testGenerateShapeHeader() {
		Map<String, String> shapeHeader = AlaFileUtils.generateShapeHeader(
				new String[]{
						"scientificName.p",
						"order.p",
						"test-field.p",
						"uoieaeiouaeiou",
						"uoieaeiouaeiou",
						"uoiea",
						"uoiea",
						"uoieaeiouaeiou",
						"uoieaeiouaeiou",
						"abccdddffgghhj",
						"bccdddffgghhjk",
						"abccdddffgghhj",
						"bccdddffgghhjk"});
		assertEquals(13, shapeHeader.size());
		assertTrue(shapeHeader.containsKey("scntfcNmp"), () -> shapeHeader.keySet().toString());
		assertEquals("scientificName.p", shapeHeader.get("scntfcNmp"));
		assertTrue(shapeHeader.containsKey("orderp"), () -> shapeHeader.keySet().toString());
		assertEquals("order.p", shapeHeader.get("orderp"));
		assertTrue(shapeHeader.containsKey("testfieldp"), () -> shapeHeader.keySet().toString());
		assertEquals("test-field.p", shapeHeader.get("testfieldp"));
		assertTrue(shapeHeader.containsKey("uoieaeioua"), () -> shapeHeader.keySet().toString());
		assertEquals("uoieaeiouaeiou", shapeHeader.get("uoieaeioua"));
		assertTrue(shapeHeader.containsKey("uoieaeiou6"), () -> shapeHeader.keySet().toString());
		assertEquals("uoieaeiouaeiou", shapeHeader.get("uoieaeiou6"));
		assertTrue(shapeHeader.containsKey("uoiea"), () -> shapeHeader.keySet().toString());
		assertEquals("uoiea", shapeHeader.get("uoiea"));
		assertTrue(shapeHeader.containsKey("uoiea9"), () -> shapeHeader.keySet().toString());
		assertEquals("uoiea", shapeHeader.get("uoiea9"));
		assertTrue(shapeHeader.containsKey("uoieaeio11"), () -> shapeHeader.keySet().toString());
		assertEquals("uoieaeiouaeiou", shapeHeader.get("uoieaeio11"));
		assertTrue(shapeHeader.containsKey("uoieaeio13"), () -> shapeHeader.keySet().toString());
		assertEquals("uoieaeiouaeiou", shapeHeader.get("uoieaeio13"));
		assertTrue(shapeHeader.containsKey("abccdddffg"), () -> shapeHeader.keySet().toString());
		assertEquals("abccdddffgghhj", shapeHeader.get("abccdddffg"));
		assertTrue(shapeHeader.containsKey("bccdddffgg"), () -> shapeHeader.keySet().toString());
		assertEquals("bccdddffgghhjk", shapeHeader.get("bccdddffgg"));
		assertTrue(shapeHeader.containsKey("abccdddf17"), () -> shapeHeader.keySet().toString());
		assertEquals("abccdddffgghhj", shapeHeader.get("abccdddf17"));
		assertTrue(shapeHeader.containsKey("bccdddff19"), () -> shapeHeader.keySet().toString());
		assertEquals("bccdddffgghhjk", shapeHeader.get("bccdddff19"));

	}

	/**
	 * Test method for {@link au.org.ala.biocache.util.AlaFileUtils#removeNonAlphanumeric(String)}.
	 */
	@Test
	final void testRemoveNonAlphanumeric() {
		assertEquals("scientificNamep", AlaFileUtils.removeNonAlphanumeric("scientificName.p"));
		assertEquals("orderp", AlaFileUtils.removeNonAlphanumeric("order.p"));
		assertEquals("testfieldp", AlaFileUtils.removeNonAlphanumeric("test-field.p"));
		assertEquals("uoieaeiouaeiou", AlaFileUtils.removeNonAlphanumeric("uoieaeiouaeiou"));
	}
}
