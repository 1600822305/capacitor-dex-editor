import type { PluginListenerHandle } from '@capacitor/core';

/**
 * 编译进度事件数据
 */
export interface CompileProgressEvent {
  type: 'progress' | 'message' | 'title';
  current?: number;
  total?: number;
  percent?: number;
  message?: string;
  title?: string;
}

/**
 * DEX编辑器插件 - 通用执行器接口
 * 通过 action 参数调用 dexlib2 的全部功能
 */
export interface DexEditorPluginPlugin {
  /**
   * 通用执行器 - 调用任意 dexlib2 功能
   * @param options.action 操作名称
   * @param options.params 操作参数
   */
  execute(options: DexExecuteOptions): Promise<DexExecuteResult>;

  /**
   * 打开原生 Smali 编辑器
   */
  openSmaliEditor(options: OpenSmaliEditorOptions): Promise<OpenEditorResult>;

  /**
   * 打开原生 XML 编辑器
   */
  openXmlEditor(options: OpenXmlEditorOptions): Promise<OpenEditorResult>;

  /**
   * 打开通用代码编辑器
   */
  openCodeEditor(options: OpenCodeEditorOptions): Promise<OpenEditorResult>;

  /**
   * 监听编译进度事件
   */
  addListener(
    eventName: 'compileProgress',
    listenerFunc: (event: CompileProgressEvent) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * 移除所有监听器
   */
  removeAllListeners(): Promise<void>;
}

export interface OpenSmaliEditorOptions {
  content: string;
  title?: string;
  className?: string;
  readOnly?: boolean;
}

export interface OpenXmlEditorOptions {
  content: string;
  title?: string;
  filePath?: string;
  readOnly?: boolean;
}

export interface OpenCodeEditorOptions {
  content: string;
  title?: string;
  filePath?: string;
  readOnly?: boolean;
  syntaxFile?: string; // 语法文件: smali.json, xml.json, java.json, json.json 等
}

export interface OpenEditorResult {
  success: boolean;
  content?: string;
  modified?: boolean;
  cancelled?: boolean;
}

// 保持向后兼容
export type OpenSmaliEditorResult = OpenEditorResult;

export interface DexExecuteOptions {
  action: DexAction;
  params?: Record<string, any>;
}

export interface DexExecuteResult {
  success: boolean;
  data?: any;
  error?: string;
}

/**
 * 支持的操作类型
 */
export type DexAction =
  // DEX文件操作
  | 'loadDex'           // 加载DEX文件
  | 'saveDex'           // 保存DEX文件
  | 'closeDex'          // 关闭DEX文件
  | 'getDexInfo'        // 获取DEX文件信息
  
  // 类操作
  | 'getClasses'        // 获取所有类列表
  | 'getClassInfo'      // 获取类详细信息
  | 'addClass'          // 添加类
  | 'removeClass'       // 删除类
  | 'renameClass'       // 重命名类
  
  // 方法操作
  | 'getMethods'        // 获取类的所有方法
  | 'getMethodInfo'     // 获取方法详细信息
  | 'getMethodSmali'    // 获取方法的Smali代码
  | 'setMethodSmali'    // 设置方法的Smali代码
  | 'addMethod'         // 添加方法
  | 'removeMethod'      // 删除方法
  
  // 字段操作
  | 'getFields'         // 获取类的所有字段
  | 'getFieldInfo'      // 获取字段详细信息
  | 'addField'          // 添加字段
  | 'removeField'       // 删除字段
  
  // Smali操作
  | 'classToSmali'      // 将类转换为Smali代码
  | 'smaliToClass'      // 将Smali代码编译为类
  | 'disassemble'       // 反汇编整个DEX
  | 'assemble'          // 汇编Smali目录为DEX
  
  // 搜索操作
  | 'searchString'      // 搜索字符串
  | 'searchCode'        // 搜索代码
  | 'searchMethod'      // 搜索方法
  | 'searchField'       // 搜索字段
  
  // 工具操作
  | 'fixDex'            // 修复DEX文件
  | 'mergeDex'          // 合并多个DEX
  | 'splitDex'          // 拆分DEX
  | 'getStrings'        // 获取字符串常量池
  | 'modifyString'      // 修改字符串
  
  // APK操作
  | 'openApk'           // 打开APK文件（解压）
  | 'closeApk'          // 关闭APK会话
  | 'getApkInfo'        // 获取APK信息
  | 'listApkContents'   // 列出APK内容
  | 'extractFile'       // 提取APK中的文件
  | 'replaceFile'       // 替换APK中的文件
  | 'addFile'           // 添加文件到APK
  | 'deleteFile'        // 删除APK中的文件
  | 'repackApk'         // 重新打包APK
  | 'signApk'           // 签名APK
  | 'signApkWithTestKey'// 使用测试密钥签名
  | 'getApkSignature'   // 获取APK签名信息
  | 'getSessionDexFiles';// 获取会话中的DEX文件

/**
 * 常用参数类型定义
 */
export interface LoadDexParams {
  path: string;           // DEX文件路径
  sessionId?: string;     // 可选的会话ID
}

export interface SaveDexParams {
  sessionId: string;      // 会话ID
  outputPath: string;     // 输出路径
}

export interface ClassInfoParams {
  sessionId: string;
  className: string;      // 类名 (如 Lcom/example/Test;)
}

export interface MethodSmaliParams {
  sessionId: string;
  className: string;
  methodName: string;
  methodSignature: string; // 方法签名 (如 (I)V)
}

export interface SetMethodSmaliParams extends MethodSmaliParams {
  smaliCode: string;      // 新的Smali代码
}

export interface SearchParams {
  sessionId: string;
  query: string;
  regex?: boolean;        // 是否使用正则表达式
  caseSensitive?: boolean;
}

export interface DisassembleParams {
  sessionId: string;
  outputDir: string;      // 输出Smali目录
}

export interface AssembleParams {
  smaliDir: string;       // Smali目录
  outputPath: string;     // 输出DEX路径
}

// ============ APK 操作参数 ============

export interface OpenApkParams {
  apkPath: string;        // APK文件路径
  extractDir?: string;    // 解压目录（可选）
}

export interface ApkInfoParams {
  apkPath: string;
}

export interface ExtractFileParams {
  apkPath: string;
  entryName: string;      // ZIP内文件路径
  outputPath: string;
}

export interface ReplaceFileParams {
  sessionId: string;
  entryName: string;      // ZIP内文件路径
  newFilePath: string;    // 新文件路径
}

export interface RepackApkParams {
  sessionId: string;
  outputPath: string;     // 输出APK路径
}

export interface SignApkParams {
  apkPath: string;
  outputPath: string;
  keystorePath: string;   // 密钥库路径
  keystorePassword: string;
  keyAlias: string;       // 密钥别名
  keyPassword: string;
}

export interface SignApkTestParams {
  apkPath: string;
  outputPath: string;
}
