import { WebPlugin } from '@capacitor/core';

import type { DexEditorPluginPlugin } from './definitions';

export class DexEditorPluginWeb extends WebPlugin implements DexEditorPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
