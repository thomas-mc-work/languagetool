/* LanguageTool, a natural language style checker
 * Copyright (C) 2012 Marcin Miłkowski
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules.pl;

import org.junit.Test;
import org.languagetool.JLanguageTool;
import org.languagetool.TestTools;
import org.languagetool.language.Polish;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MorfologikPolishSpellerRuleTest {

  @Test
  public void testMorfologikSpeller() throws IOException {
    final MorfologikPolishSpellerRule rule =
        new MorfologikPolishSpellerRule (TestTools.getMessages("Polish"), new Polish());

    final JLanguageTool langTool = new JLanguageTool(new Polish());

    // correct sentences:
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("To jest test bez jakiegokolwiek błędu.")).length);
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("Żółw na starość wydziela dziwną woń.")).length);
    //test for "LanguageTool":
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("LanguageTool jest świetny!")).length);

    //test for the ignored uppercase word "Gdym":
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("Gdym to zobaczył, zdębiałem.")).length);

    assertEquals(0, rule.match(langTool.getAnalyzedSentence(",")).length);
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("123454")).length);

    //compound word with ignored part "techniczno"
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("Bogactwo nie rośnie proporcjonalnie do jej rozwoju techniczno-terytorialnego.")).length);

    //compound word with one of the compound prefixes:
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("Antypostmodernistyczna batalia hiperfilozofów")).length);
   //compound words: "trzynastobitowy", "zgniłożółty"
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("Trzynastobitowe przystawki w kolorze zgniłożółtym")).length);

    //incorrect sentences:

    final RuleMatch[] matches = rule.match(langTool.getAnalyzedSentence("Zolw"));
    // check match positions:
    assertEquals(1, matches.length);
    assertEquals(0, matches[0].getFromPos());
    assertEquals(4, matches[0].getToPos());
    assertEquals("Żółw", matches[0].getSuggestedReplacements().get(0));

    assertEquals(1, rule.match(langTool.getAnalyzedSentence("aõh")).length);

    //tokenizing on prefixes niby- and quasi-
    assertEquals(0, rule.match(langTool.getAnalyzedSentence("Niby-artysta spotkał się z quasi-opiekunem i niby-Francuzem.")).length);
  }

}
