/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
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
package org.languagetool.rules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.UserConfig;
import org.languagetool.rules.Category.Location;

/**
 * A rule that gives hints when a defined condition is fulfilled 
 * and the percentage of hints in the text exceeds the given limit.
 * (For office extension: Works on the level of chapters)
 * A limit of 0 shows all hints. 
 * Direct speech or citation can be excluded.
 * A second condition per sentences can be defined.
 * The rule detects no grammar error but gives stylistic hints in a statistic way (default off).
 * @author Fred Kruse
 * @since 5.3
 */
public abstract class AbstractStatisticStyleRule extends TextLevelRule {
  private static final Pattern OPENING_QUOTES = Pattern.compile("[\"“„»«]");
  private static final Pattern ENDING_QUOTES = Pattern.compile("[\"“”»«]");
  private static final boolean DEFAULT_ACTIVATION = false;

  private final int minPercent;
  private final int defaultMinPercent;
  private int wordCount = 0;
  private int numMatches = 0;
  private boolean withoutDirectSpeech = false;
  private boolean excludeDirectSpeech;

  /**
   * Condition to generate a hint (possibly including all exceptions)
   * Returns:
   *  &lt; nAnalysedToken, if condition is not fulfilled
   *  &gt;= nAnalysedToken, if condition is not fulfilled; integer is number of token which is the end hint 
   */
  protected abstract int conditionFulfilled(AnalyzedTokenReadings[] tokens, int nAnalysedToken);
  
  /**
   * Condition to generate a hint related to the sentence (possibly including all exceptions)
   */
  protected abstract boolean sentenceConditionFulfilled(AnalyzedTokenReadings[] tokens, int nAnalysedToken);
  
  /**
   * Condition to generate a hint related to the sentence (possibly including all exceptions)
   */
  protected abstract boolean excludeDirectSpeech();
  
  /**
   * Defines the message for hints which exceed the limit
   */
  protected abstract String getLimitMessage(int limit, double percent);
  
  /**
   * Defines the message for sentence related hints
   */
  protected abstract String getSentenceMessage();
  
  public abstract String getConfigurePercentText();

  public abstract String getConfigureWithoutDirectSpeachText();

  public AbstractStatisticStyleRule(ResourceBundle messages, Language lang, UserConfig userConfig, int minPercent, boolean defaultActive) {
    super(messages);
    super.setCategory(new Category(new CategoryId("CREATIVE_WRITING"), 
        messages.getString("category_creative_writing"), Location.INTERNAL, false));
    if (!defaultActive) {
      setDefaultOff();
    }
    defaultMinPercent = minPercent;
    this.minPercent = getMinPercent(userConfig, minPercent);
    excludeDirectSpeech = getExcludeDirectSpeech(userConfig);
    setLocQualityIssueType(ITSIssueType.Style);
  }

  private int getMinPercent(UserConfig userConfig, int minPercentDefault) {
    if (userConfig != null) {
      Object[] cf = userConfig.getConfigValueByID(getId());
      if (cf != null && cf.length > 0 && cf[0] != null && cf[0] instanceof Integer) {
        return (int) cf[0];
      }
    }
    return minPercentDefault;
  }

  private boolean getExcludeDirectSpeech(UserConfig userConfig) {
    if (userConfig != null) {
      Object[] cf = userConfig.getConfigValueByID(getId());
      if (cf != null && cf.length > 1 && cf[1] != null && cf[1] instanceof Boolean) {
        return (boolean) cf[1];
      }
    }
    return excludeDirectSpeech();
  }

  public AbstractStatisticStyleRule(ResourceBundle messages, Language lang, UserConfig userConfig, int minPercent) {
    this(messages, lang, userConfig, minPercent, DEFAULT_ACTIVATION);
  }

  /**
   * Override, if value should be given in an other unity than percent
   */
  public double denominator() {
    return 100.0;
  }
  
  /**
   *  give the user the possibility to configure the function
   */
  @Override
  public RuleOption[] getRuleOptions() {
    RuleOption[] ruleOptions = { 
        new RuleOption(defaultMinPercent, getConfigurePercentText(), 0, 100),
        new RuleOption(excludeDirectSpeech, getConfigureWithoutDirectSpeachText())
        };
    return ruleOptions;
  }

  public int getWordCount() {
    return wordCount;
  }

  public int getNumberOfMatches() {
    return numMatches;
  }

  public void setWithoutDirectSpeech(boolean withoutDirectSpeech) {
    this.withoutDirectSpeech = withoutDirectSpeech;
  }

  /* (non-Javadoc)
   * @see org.languagetool.rules.TextLevelRule#match(java.util.List)
   */
  @Override
  public RuleMatch[] match(List<AnalyzedSentence> sentences) throws IOException {
    List<RuleMatch> ruleMatches = new ArrayList<>();
    List<Integer> startPos = new ArrayList<>();
    List<Integer> endPos = new ArrayList<>();
    List<AnalyzedSentence> relevantSentences = new ArrayList<>();
    double percent;
    int pos = 0;
    wordCount = 0;
    boolean isDirectSpeech = false;
    for (AnalyzedSentence sentence : sentences) {
      AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();
      for (int n = 1; n < tokens.length; n++) {
        AnalyzedTokenReadings token = tokens[n];
        String sToken = token.getToken();
        if (excludeDirectSpeech && !isDirectSpeech && OPENING_QUOTES.matcher(sToken).matches() && n < tokens.length - 1 && !tokens[n + 1].isWhitespaceBefore()) {
          isDirectSpeech = true;
        } else if (excludeDirectSpeech && isDirectSpeech && ENDING_QUOTES.matcher(sToken).matches() && n > 1 && !tokens[n].isWhitespaceBefore()) {
          isDirectSpeech = false;
        } else if ((!isDirectSpeech || (minPercent == 0 && !withoutDirectSpeech)) && !token.isWhitespace() && !token.isNonWord()) {
          wordCount++;
          int nEnd = conditionFulfilled(tokens, n);
          if (nEnd >= n) {
            if (sentenceConditionFulfilled(tokens, n)) {
              RuleMatch ruleMatch = new RuleMatch(this, sentence, token.getStartPos() + pos, token.getEndPos() + pos,
                      getSentenceMessage());
              ruleMatches.add(ruleMatch);
            } else {
              startPos.add(token.getStartPos() + pos);
              endPos.add(tokens[nEnd].getEndPos() + pos);
              relevantSentences.add(sentence);
            }
          }
        }
      }
      pos += sentence.getCorrectedTextLength();
    }
    numMatches = startPos.size() + ruleMatches.size();
    if (wordCount > 0) {
      percent = (numMatches * denominator()) / wordCount;
    } else {
      percent = 0;
    }
    if (percent > minPercent) {
      for (int i = 0; i < startPos.size(); i++) {
        RuleMatch ruleMatch = new RuleMatch(this, relevantSentences.get(i), startPos.get(i), endPos.get(i), 
            getLimitMessage(minPercent, percent));
        ruleMatches.add(ruleMatch);
      }
    }
    return toRuleMatchArray(ruleMatches);
  }
  
  @Override
  public int minToCheckParagraph() {
    return -1;
  }
 
}
