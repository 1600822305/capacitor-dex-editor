/*
 * Based on Dex-Editor-Android by developer-krushna
 * Original Author @MikeAndrson
 */

package com.android.tools.smali.smali2;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import com.android.tools.smali.smali.smaliTreeWalker;

import java.util.ArrayList;
import java.util.List;

public class SmaliCatchErrTreeWalker extends smaliTreeWalker {

    private StringBuilder errors = new StringBuilder();
    private List<SyntaxError> syntaxErrors = new ArrayList<>();

    public SmaliCatchErrTreeWalker(CommonTreeNodeStream treeStream) {
        super(treeStream);
    }

    @Override
    public void emitErrorMessage(String msg) {
        errors.append(msg);
        errors.append("\n");
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
