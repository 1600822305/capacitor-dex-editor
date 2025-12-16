/*
 * Based on Dex-Editor-Android by developer-krushna
 * Original Author @MikeAndrson
 * 
 * 直接将 Smali 代码编译成 ClassDef，无需临时文件
 */

package com.android.tools.smali.smali2;

import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.smali.SmaliOptions;

import java.io.StringReader;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

public class Smali {

    public static ClassDef assemble(String smaliCode, SmaliOptions options, int dexVer)
            throws Exception {
        DexBuilder dexBuilder = new DexBuilder(Opcodes.forDexVersion(dexVer));

        SmaliCatchErrFlexLexer lexer = new SmaliCatchErrFlexLexer(new StringReader(smaliCode), options.apiLevel);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        SmaliCatchErrParser parser = new SmaliCatchErrParser(tokens);
        parser.setVerboseErrors(options.verboseErrors);
        parser.setAllowOdex(options.allowOdexOpcodes);
        parser.setApiLevel(dexVer);

        SmaliCatchErrParser.smali_file_return result = parser.smali_file();

        if (lexer.getNumberOfSyntaxErrors() > 0) {
            throw new Exception(lexer.getErrorsString());
        }

        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new Exception(parser.getErrorsString());
        }

        CommonTree t = result.getTree();

        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
        treeStream.setTokenStream(tokens);

        SmaliCatchErrTreeWalker treeWalker = new SmaliCatchErrTreeWalker(treeStream);
        treeWalker.setApiLevel(dexVer);
        treeWalker.setVerboseErrors(options.verboseErrors);
        treeWalker.setDexBuilder(dexBuilder);

        ClassDef classDef = treeWalker.smali_file();

        if (treeWalker.getNumberOfSyntaxErrors() > 0) {
            throw new Exception(treeWalker.getErrorsString());
        }

        return classDef;
    }

    public static ClassDef assemble(String smaliCode, SmaliOptions options) throws Exception {
        DexBuilder dexBuilder = new DexBuilder(Opcodes.forApi(options.apiLevel));

        SmaliCatchErrFlexLexer lexer = new SmaliCatchErrFlexLexer(new StringReader(smaliCode), options.apiLevel);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        SmaliCatchErrParser parser = new SmaliCatchErrParser(tokens);
        parser.setVerboseErrors(options.verboseErrors);
        parser.setAllowOdex(options.allowOdexOpcodes);
        parser.setApiLevel(options.apiLevel);

        SmaliCatchErrParser.smali_file_return result = parser.smali_file();

        if (lexer.getNumberOfSyntaxErrors() > 0) {
            throw new Exception(lexer.getErrorsString());
        }

        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new Exception(parser.getErrorsString());
        }

        CommonTree t = result.getTree();

        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
        treeStream.setTokenStream(tokens);

        SmaliCatchErrTreeWalker treeWalker = new SmaliCatchErrTreeWalker(treeStream);
        treeWalker.setApiLevel(options.apiLevel);
        treeWalker.setVerboseErrors(options.verboseErrors);
        treeWalker.setDexBuilder(dexBuilder);

        ClassDef classDef = treeWalker.smali_file();

        if (treeWalker.getNumberOfSyntaxErrors() > 0) {
            throw new Exception(treeWalker.getErrorsString());
        }

        return classDef;
    }
}
