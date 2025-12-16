/*
 * Based on Dex-Editor-Android by developer-krushna
 * Original Author @MikeAndrson
 */

package com.android.tools.smali.smali2;

public class SyntaxError {
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;
    private final String message;

    public SyntaxError(int startLine, int startColumn, int endLine, int endColumn, String message) {
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.message = message;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public String getMessage() {
        return message;
    }
}
