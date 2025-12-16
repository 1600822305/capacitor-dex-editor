/*
 * Based on Dex-Editor-Android by developer-krushna
 * Original Author @MikeAndrson
 */

package com.android.tools.smali.smali2;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import com.android.tools.smali.smali.smaliParser;

import java.util.ArrayList;
import java.util.List;

public class SmaliCatchErrParser extends smaliParser {

    private StringBuilder errors = new StringBuilder();
    private List<SyntaxError> syntaxErrors = new ArrayList<>();

    public SmaliCatchErrParser(CommonTokenStream tokens) {
        super(tokens);
    }

    @Override
    public void emitErrorMessage(String msg) {
        errors.append(msg).append("\n");
    }

    public String getErrorsString() {
        return errors.toString();
    }

    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
        syntaxErrors.add(new SyntaxError(
                e.line,
                e.charPositionInLine,
                e.line,
                e.charPositionInLine + (e.token != null ? e.token.getText().length() : 1),
                getErrorMessage(e, tokenNames)
        ));

        super.displayRecognitionError(tokenNames, e);
    }

    public List<SyntaxError> getErrors() {
        return syntaxErrors;
    }
}
