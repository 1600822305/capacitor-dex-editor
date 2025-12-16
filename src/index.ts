import { registerPlugin } from '@capacitor/core';

import type { DexEditorPluginPlugin } from './definitions';

const DexEditorPlugin = registerPlugin<DexEditorPluginPlugin>('DexEditorPlugin', {
  web: () => import('./web').then((m) => new m.DexEditorPluginWeb()),
});

export * from './definitions';
export { DexEditorPlugin };
