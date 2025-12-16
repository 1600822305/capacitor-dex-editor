export interface DexEditorPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
