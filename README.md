# capacitor-dex-editor

A powerful Capacitor plugin for editing DEX files in APK packages. Similar to MT Manager's DEX editor functionality.

## Features

- **DEX Operations**: Load, save, modify DEX files
- **Class Management**: Get, add, remove, rename classes
- **Method Editing**: View/modify Smali code, add/remove methods
- **Field Operations**: Get, add, remove fields
- **Smali Conversion**: Class ↔ Smali bidirectional conversion
- **Search**: Search strings, code, methods, fields
- **APK Operations**: Open, extract, repack, sign APK files
- **Utilities**: Fix DEX, merge DEX, split DEX

## Install

```bash
npm install capacitor-dex-editor
npx cap sync
```

## Quick Start

```typescript
import { DexEditorPlugin } from 'capacitor-dex-editor';

// 1. Open APK
const { data: apk } = await DexEditorPlugin.execute({
  action: 'openApk',
  params: { apkPath: '/sdcard/test.apk' }
});

// 2. Load DEX
const { data: dex } = await DexEditorPlugin.execute({
  action: 'loadDex',
  params: { path: apk.dexFiles[0] }
});

// 3. Get all classes
const { data: classes } = await DexEditorPlugin.execute({
  action: 'getClasses',
  params: { sessionId: dex.sessionId }
});

// 4. Modify method (change return value to true)
await DexEditorPlugin.execute({
  action: 'setMethodSmali',
  params: {
    sessionId: dex.sessionId,
    className: 'Lcom/example/App;',
    methodName: 'isVip',
    methodSignature: '()Z',
    smaliCode: `.method public isVip()Z
    .registers 1
    const/4 v0, 0x1
    return v0
.end method`
  }
});

// 5. Save DEX
await DexEditorPlugin.execute({
  action: 'saveDex',
  params: { sessionId: dex.sessionId, outputPath: apk.dexFiles[0] }
});

// 6. Repack APK
await DexEditorPlugin.execute({
  action: 'repackApk',
  params: { sessionId: apk.sessionId, outputPath: '/sdcard/modified.apk' }
});

// 7. Sign APK
await DexEditorPlugin.execute({
  action: 'signApkWithTestKey',
  params: { apkPath: '/sdcard/modified.apk', outputPath: '/sdcard/signed.apk' }
});
```

## Supported Actions

### DEX File Operations
| Action | Description |
|--------|-------------|
| `loadDex` | Load DEX file |
| `saveDex` | Save modified DEX |
| `closeDex` | Close DEX session |
| `getDexInfo` | Get DEX file info |

### Class Operations
| Action | Description |
|--------|-------------|
| `getClasses` | Get all classes |
| `getClassInfo` | Get class details |
| `addClass` | Add new class |
| `removeClass` | Remove class |
| `renameClass` | Rename class |

### Method Operations
| Action | Description |
|--------|-------------|
| `getMethods` | Get all methods of a class |
| `getMethodInfo` | Get method details |
| `getMethodSmali` | Get method Smali code |
| `setMethodSmali` | Modify method Smali code |
| `addMethod` | Add new method |
| `removeMethod` | Remove method |

### Field Operations
| Action | Description |
|--------|-------------|
| `getFields` | Get all fields of a class |
| `getFieldInfo` | Get field details |
| `addField` | Add new field |
| `removeField` | Remove field |

### Smali Operations
| Action | Description |
|--------|-------------|
| `classToSmali` | Convert class to Smali |
| `smaliToClass` | Compile Smali to class |
| `disassemble` | Disassemble entire DEX |
| `assemble` | Assemble Smali directory to DEX |

### Search Operations
| Action | Description |
|--------|-------------|
| `searchString` | Search strings (regex supported) |
| `searchCode` | Search in code |
| `searchMethod` | Search method names |
| `searchField` | Search field names |

### APK Operations
| Action | Description |
|--------|-------------|
| `openApk` | Open and extract APK |
| `closeApk` | Close APK session |
| `getApkInfo` | Get APK info |
| `listApkContents` | List APK contents |
| `extractFile` | Extract file from APK |
| `replaceFile` | Replace file in APK |
| `addFile` | Add file to APK |
| `deleteFile` | Delete file from APK |
| `repackApk` | Repack APK |
| `signApk` | Sign APK with custom key |
| `signApkWithTestKey` | Sign APK with test key |
| `getApkSignature` | Get APK signature info |

### Utility Operations
| Action | Description |
|--------|-------------|
| `fixDex` | Fix corrupted DEX |
| `mergeDex` | Merge multiple DEX files |
| `splitDex` | Split DEX by class count |
| `getStrings` | Get string pool |
| `modifyString` | Batch replace strings |

## Requirements

- Android only (iOS not supported)
- Capacitor 7.0+
- Android API 23+

## License

MIT

## API

<docgen-index>

* [`execute(...)`](#execute)
* [`openSmaliEditor(...)`](#opensmalieditor)
* [`openXmlEditor(...)`](#openxmleditor)
* [`openCodeEditor(...)`](#opencodeeditor)
* [`addListener('compileProgress', ...)`](#addlistenercompileprogress-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

DEX编辑器插件 - 通用执行器接口
通过 action 参数调用 dexlib2 的全部功能

### execute(...)

```typescript
execute(options: DexExecuteOptions) => Promise<DexExecuteResult>
```

通用执行器 - 调用任意 dexlib2 功能

| Param         | Type                                                            |
| ------------- | --------------------------------------------------------------- |
| **`options`** | <code><a href="#dexexecuteoptions">DexExecuteOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#dexexecuteresult">DexExecuteResult</a>&gt;</code>

--------------------


### openSmaliEditor(...)

```typescript
openSmaliEditor(options: OpenSmaliEditorOptions) => Promise<OpenEditorResult>
```

打开原生 Smali 编辑器

| Param         | Type                                                                      |
| ------------- | ------------------------------------------------------------------------- |
| **`options`** | <code><a href="#opensmalieditoroptions">OpenSmaliEditorOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#openeditorresult">OpenEditorResult</a>&gt;</code>

--------------------


### openXmlEditor(...)

```typescript
openXmlEditor(options: OpenXmlEditorOptions) => Promise<OpenEditorResult>
```

打开原生 XML 编辑器

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#openxmleditoroptions">OpenXmlEditorOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#openeditorresult">OpenEditorResult</a>&gt;</code>

--------------------


### openCodeEditor(...)

```typescript
openCodeEditor(options: OpenCodeEditorOptions) => Promise<OpenEditorResult>
```

打开通用代码编辑器

| Param         | Type                                                                    |
| ------------- | ----------------------------------------------------------------------- |
| **`options`** | <code><a href="#opencodeeditoroptions">OpenCodeEditorOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#openeditorresult">OpenEditorResult</a>&gt;</code>

--------------------


### addListener('compileProgress', ...)

```typescript
addListener(eventName: 'compileProgress', listenerFunc: (event: CompileProgressEvent) => void) => Promise<PluginListenerHandle>
```

监听编译进度事件

| Param              | Type                                                                                      |
| ------------------ | ----------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'compileProgress'</code>                                                            |
| **`listenerFunc`** | <code>(event: <a href="#compileprogressevent">CompileProgressEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

移除所有监听器

--------------------


### Interfaces


#### DexExecuteResult

| Prop          | Type                 |
| ------------- | -------------------- |
| **`success`** | <code>boolean</code> |
| **`data`**    | <code>any</code>     |
| **`error`**   | <code>string</code>  |


#### DexExecuteOptions

| Prop         | Type                                                         |
| ------------ | ------------------------------------------------------------ |
| **`action`** | <code><a href="#dexaction">DexAction</a></code>              |
| **`params`** | <code><a href="#record">Record</a>&lt;string, any&gt;</code> |


#### OpenEditorResult

| Prop            | Type                 |
| --------------- | -------------------- |
| **`success`**   | <code>boolean</code> |
| **`content`**   | <code>string</code>  |
| **`modified`**  | <code>boolean</code> |
| **`cancelled`** | <code>boolean</code> |


#### OpenSmaliEditorOptions

| Prop            | Type                 |
| --------------- | -------------------- |
| **`content`**   | <code>string</code>  |
| **`title`**     | <code>string</code>  |
| **`className`** | <code>string</code>  |
| **`readOnly`**  | <code>boolean</code> |


#### OpenXmlEditorOptions

| Prop           | Type                 |
| -------------- | -------------------- |
| **`content`**  | <code>string</code>  |
| **`title`**    | <code>string</code>  |
| **`filePath`** | <code>string</code>  |
| **`readOnly`** | <code>boolean</code> |


#### OpenCodeEditorOptions

| Prop             | Type                 |
| ---------------- | -------------------- |
| **`content`**    | <code>string</code>  |
| **`title`**      | <code>string</code>  |
| **`filePath`**   | <code>string</code>  |
| **`readOnly`**   | <code>boolean</code> |
| **`syntaxFile`** | <code>string</code>  |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### CompileProgressEvent

编译进度事件数据

| Prop          | Type                                            |
| ------------- | ----------------------------------------------- |
| **`type`**    | <code>'message' \| 'progress' \| 'title'</code> |
| **`current`** | <code>number</code>                             |
| **`total`**   | <code>number</code>                             |
| **`percent`** | <code>number</code>                             |
| **`message`** | <code>string</code>                             |
| **`title`**   | <code>string</code>                             |


### Type Aliases


#### DexAction

支持的操作类型

<code>'loadDex' | 'saveDex' | 'closeDex' | 'getDexInfo' | 'getClasses' | 'getClassInfo' | 'addClass' | 'removeClass' | 'renameClass' | 'getMethods' | 'getMethodInfo' | 'getMethodSmali' | 'setMethodSmali' | 'addMethod' | 'removeMethod' | 'getFields' | 'getFieldInfo' | 'addField' | 'removeField' | 'classToSmali' | 'smaliToClass' | 'disassemble' | 'assemble' | 'searchString' | 'searchCode' | 'searchMethod' | 'searchField' | 'fixDex' | 'mergeDex' | 'splitDex' | 'getStrings' | 'modifyString' | 'openApk' | 'closeApk' | 'getApkInfo' | 'listApkContents' | 'extractFile' | 'replaceFile' | 'addFile' | 'deleteFile' | 'repackApk' | 'signApk' | 'signApkWithTestKey' | 'getApkSignature' | 'getSessionDexFiles'</code>


#### Record

Construct a type with a set of properties K of type T

<code>{ [P in K]: T; }</code>

</docgen-api>
