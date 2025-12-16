import { WebPlugin } from '@capacitor/core';

import type { DexEditorPluginPlugin, DexExecuteOptions, DexExecuteResult, OpenSmaliEditorOptions, OpenXmlEditorOptions, OpenCodeEditorOptions, OpenEditorResult } from './definitions';

/**
 * Web 平台实现（仅用于开发调试，实际功能需要 Android 原生）
 */
export class DexEditorPluginWeb extends WebPlugin implements DexEditorPluginPlugin {
  async execute(options: DexExecuteOptions): Promise<DexExecuteResult> {
    console.warn('DexEditorPlugin: Web platform is not supported for DEX editing');
    console.log('Action:', options.action, 'Params:', options.params);
    
    return {
      success: false,
      error: 'DEX editing is only available on Android platform'
    };
  }

  async openSmaliEditor(_options: OpenSmaliEditorOptions): Promise<OpenEditorResult> {
    console.warn('DexEditorPlugin: Native Smali editor is only available on Android');
    return {
      success: false,
      cancelled: true
    };
  }

  async openXmlEditor(_options: OpenXmlEditorOptions): Promise<OpenEditorResult> {
    console.warn('DexEditorPlugin: Native XML editor is only available on Android');
    return {
      success: false,
      cancelled: true
    };
  }

  async openCodeEditor(_options: OpenCodeEditorOptions): Promise<OpenEditorResult> {
    console.warn('DexEditorPlugin: Native code editor is only available on Android');
    return {
      success: false,
      cancelled: true
    };
  }
}
