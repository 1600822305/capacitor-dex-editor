/*
 * Based on Dex-Editor-Android by developer-krushna
 * Original Author @MikeAndrson
 */

package com.android.tools.smali.smali2;

import com.android.tools.smali.smali.InvalidToken;
import com.android.tools.smali.smali.smaliFlexLexer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class SmaliCatchErrFlexLexer extends smaliFlexLexer {

    private StringBuilder errors = new StringBuilder();
    private List<SyntaxError> syntaxErrors = new ArrayList<>();

    public SmaliCatchErrFlexLexer(Reader reader, int apiLevel) {
        super(reader, apiLevel);
    }

    @Override
    public String getErrorHeader(InvalidToken invalidToken) {
        errors.append("[")
                .append(invalidToken.getLine())
                .append(",")
                .append(invalidToken.getCharPositionInLine())
                .append("] Error for input '")
                .append(invalidToken.getText())
                .append("': ")
                .append(invalidToken.getMessage())
                .append("\n");

        syntaxErrors.add(new SyntaxError(
                invalidToken.getLine(),
                invalidToken.getCharPositionInLine(),
                invalidToken.getLine(),
                invalidToken.getCharPositionInLine() + invalidToken.getText().length(),
                invalidToken.getMessage()
        ));

        return super.getErrorHeader(invalidToken);
    }

    public String getErrorsString() {
        return errors.toString();
    }

    public List<SyntaxError> getErrors() {
        return syntaxErrors;
    }
}
