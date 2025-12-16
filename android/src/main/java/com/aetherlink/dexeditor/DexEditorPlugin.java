package com.aetherlink.dexeditor;

import com.getcapacitor.Logger;

public class DexEditorPlugin {

    public String echo(String value) {
        Logger.info("Echo", value);
        return value;
    }
}
