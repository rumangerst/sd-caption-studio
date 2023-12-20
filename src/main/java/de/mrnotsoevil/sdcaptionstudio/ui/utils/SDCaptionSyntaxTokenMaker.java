package de.mrnotsoevil.sdcaptionstudio.ui.utils;

import com.fathzer.soft.javaluator.Constant;
import com.fathzer.soft.javaluator.Function;
import com.fathzer.soft.javaluator.Operator;
import org.apache.commons.lang3.math.NumberUtils;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import javax.swing.text.Segment;
import java.util.HashSet;
import java.util.Set;

public class SDCaptionSyntaxTokenMaker extends AbstractTokenMaker {
    private final Set<String> knownNonAlphanumericOperatorTokens = new HashSet<>();

    public SDCaptionSyntaxTokenMaker() {
        knownNonAlphanumericOperatorTokens.add(",");
    }

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap tokenMap = new TokenMap();
        tokenMap.put(",", Token.OPERATOR);
        return tokenMap;
    }

    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
        resetTokenList();
        int offset = text.offset;
        int newStartOffset = startOffset - offset;
        int count = text.count;
        int end = offset + count;
        char[] array = text.array;

        StringBuilder buffer = new StringBuilder();
        int currentTokenStart = offset;

        for (int index = offset; index < end; index++) {
            char c = array[index];
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                addToken(text, buffer.toString(), currentTokenStart, newStartOffset + currentTokenStart);
                buffer.setLength(0);
                currentTokenStart = index;
                addToken(text, index, index, Token.WHITESPACE, newStartOffset + currentTokenStart);
                currentTokenStart = index + 1;
                continue;
            }
            if (c == '(' || c == ')' || c == '[' || c == ']') {
                addToken(text, buffer.toString(), currentTokenStart, newStartOffset + currentTokenStart);
                buffer.setLength(0);
                currentTokenStart = index;
                addToken(text, index, index, Token.SEPARATOR, newStartOffset + currentTokenStart);
                currentTokenStart = index + 1;
                continue;
            }
            buffer.append(c);

            if (buffer.length() > 0) {
                String s1 = buffer.toString();
                for (String s : knownNonAlphanumericOperatorTokens) {
                    int i1 = s1.indexOf(s);
                    if (i1 != -1) {
                        if (i1 > 0) {
                            addToken(text, s1.substring(0, i1), currentTokenStart, newStartOffset + currentTokenStart);
                        }
                        addToken(text, s, Token.OPERATOR, currentTokenStart + i1, newStartOffset + currentTokenStart + i1);
                        buffer.setLength(0);
                        currentTokenStart = index + 1;
                        break;
                    }
                }
            }
        }
        if (buffer.length() > 0) {
            addToken(text, buffer.toString(), currentTokenStart, newStartOffset + currentTokenStart);
        }
        if (firstToken == null) {
            addNullToken();
        }
        return firstToken;
    }

    private void addToken(Segment segment, String text, int start, int startOffset) {
        if (text.isEmpty())
            return;
        int end = start + text.length() - 1;
        int tokenType = getWordsToHighlight().get(segment, start, end);
        if (text.startsWith("@"))
            tokenType = Token.VARIABLE;
        if (tokenType == -1)
            tokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
        int shift = 0;
        for (int i = start; i <= end; i++) {
            addToken(segment, i, i, tokenType, startOffset + shift++);
        }
    }

    private void addToken(Segment segment, String text, int tokenType, int start, int startOffset) {
        if (text.isEmpty())
            return;
        int end = start + text.length() - 1;
        int shift = 0;
        for (int i = start; i <= end; i++) {
            addToken(segment, i, i, tokenType, startOffset + shift++);
        }
    }
}
