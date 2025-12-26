package com.aetherlink.dexeditor;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import androidx.activity.result.ActivityResult;

import org.json.JSONObject;

/**
 * DEX编辑器 Capacitor 插件
 * 通用执行器模式 - 通过 action 参数分发到对应的 dexlib2 操作
 */
@CapacitorPlugin(name = "DexEditorPlugin")
public class DexEditorPluginPlugin extends Plugin {

    private static final String TAG = "DexEditorPlugin";
    private final DexManager dexManager = new DexManager();
    private final ApkManager apkManager = new ApkManager();

    @Override
    public void load() {
        super.load();
        apkManager.setContext(getContext());
        
        // 设置编译进度回调
        dexManager.setProgressCallback(new DexManager.CompileProgress() {
            @Override
            public void onProgress(int current, int total) {
                JSObject data = new JSObject();
                data.put("type", "progress");
                data.put("current", current);
                data.put("total", total);
                data.put("percent", total > 0 ? (current * 100 / total) : 0);
                notifyListeners("compileProgress", data);
            }
            
            @Override
            public void onMessage(String message) {
                JSObject data = new JSObject();
                data.put("type", "message");
                data.put("message", message);
                notifyListeners("compileProgress", data);
            }
            
            @Override
            public void onTitle(String title) {
                JSObject data = new JSObject();
                data.put("type", "title");
                data.put("title", title);
                notifyListeners("compileProgress", data);
            }
        });
    }

    /**
     * 通用执行器入口
     * 所有 dexlib2 操作都通过此方法分发
     */
    @PluginMethod
    public void execute(PluginCall call) {
        String action = call.getString("action");
        JSObject params = call.getObject("params");
        
        if (action == null || action.isEmpty()) {
            call.reject("Action is required");
            return;
        }

        Log.d(TAG, "Executing action: " + action);

        // 在后台线程执行，避免阻塞UI
        new Thread(() -> {
            try {
                JSObject result = dispatchAction(action, params);
                call.resolve(result);
            } catch (Exception e) {
                Log.e(TAG, "Action failed: " + action, e);
                JSObject error = new JSObject();
                error.put("success", false);
                error.put("error", e.getMessage());
                call.resolve(error);
            }
        }).start();
    }

    /**
     * 分发操作到对应的处理方法
     */
    private JSObject dispatchAction(String action, JSObject params) throws Exception {
        JSObject result = new JSObject();
        result.put("success", true);

        switch (action) {
            // ============ DEX文件操作 ============
            case "loadDex":
                result.put("data", dexManager.loadDex(
                    params.getString("path"),
                    params.optString("sessionId", null)
                ));
                break;

            case "saveDex":
                dexManager.saveDex(
                    params.getString("sessionId"),
                    params.getString("outputPath")
                );
                break;

            case "closeDex":
                dexManager.closeDex(params.getString("sessionId"));
                break;

            case "getDexInfo":
                result.put("data", dexManager.getDexInfo(params.getString("sessionId")));
                break;

            // ============ 类操作 ============
            case "getClasses":
                result.put("data", dexManager.getClasses(params.getString("sessionId")));
                break;

            case "getClassInfo":
                result.put("data", dexManager.getClassInfo(
                    params.getString("sessionId"),
                    params.getString("className")
                ));
                break;

            case "addClass":
                dexManager.addClass(
                    params.getString("sessionId"),
                    params.getString("smaliCode")
                );
                break;

            case "removeClass":
                dexManager.removeClass(
                    params.getString("sessionId"),
                    params.getString("className")
                );
                break;

            case "renameClass":
                dexManager.renameClass(
                    params.getString("sessionId"),
                    params.getString("oldName"),
                    params.getString("newName")
                );
                break;

            // ============ 方法操作 ============
            case "getMethods":
                result.put("data", dexManager.getMethods(
                    params.getString("sessionId"),
                    params.getString("className")
                ));
                break;

            case "getMethodInfo":
                result.put("data", dexManager.getMethodInfo(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("methodName"),
                    params.getString("methodSignature")
                ));
                break;

            case "getMethodSmali":
                result.put("data", dexManager.getMethodSmali(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("methodName"),
                    params.getString("methodSignature")
                ));
                break;

            case "setMethodSmali":
                dexManager.setMethodSmali(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("methodName"),
                    params.getString("methodSignature"),
                    params.getString("smaliCode")
                );
                break;

            case "addMethod":
                dexManager.addMethod(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("smaliCode")
                );
                break;

            case "removeMethod":
                dexManager.removeMethod(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("methodName"),
                    params.getString("methodSignature")
                );
                break;

            // ============ 字段操作 ============
            case "getFields":
                result.put("data", dexManager.getFields(
                    params.getString("sessionId"),
                    params.getString("className")
                ));
                break;

            case "getFieldInfo":
                result.put("data", dexManager.getFieldInfo(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("fieldName")
                ));
                break;

            case "addField":
                dexManager.addField(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("fieldDef")
                );
                break;

            case "removeField":
                dexManager.removeField(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("fieldName")
                );
                break;

            // ============ Smali操作 ============
            case "classToSmali":
                result.put("data", dexManager.classToSmali(
                    params.getString("sessionId"),
                    params.getString("className")
                ));
                break;

            case "smaliToClass":
                dexManager.smaliToClass(
                    params.getString("sessionId"),
                    params.getString("smaliCode")
                );
                break;

            case "disassemble":
                dexManager.disassemble(
                    params.getString("sessionId"),
                    params.getString("outputDir")
                );
                break;

            case "assemble":
                result.put("data", dexManager.assemble(
                    params.getString("smaliDir"),
                    params.getString("outputPath")
                ));
                break;

            // ============ 搜索操作 ============
            case "searchString":
                result.put("data", dexManager.searchString(
                    params.getString("sessionId"),
                    params.getString("query"),
                    params.optBoolean("regex", false),
                    params.optBoolean("caseSensitive", false)
                ));
                break;

            case "searchCode":
                result.put("data", dexManager.searchCode(
                    params.getString("sessionId"),
                    params.getString("query"),
                    params.optBoolean("regex", false)
                ));
                break;

            case "searchMethod":
                result.put("data", dexManager.searchMethod(
                    params.getString("sessionId"),
                    params.getString("query")
                ));
                break;

            case "searchField":
                result.put("data", dexManager.searchField(
                    params.getString("sessionId"),
                    params.getString("query")
                ));
                break;

            // ============ 交叉引用分析（C++ 实现）============
            case "findMethodXrefs":
                result.put("data", dexManager.findMethodXrefs(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("methodName")
                ));
                break;

            case "findFieldXrefs":
                result.put("data", dexManager.findFieldXrefs(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("fieldName")
                ));
                break;

            // ============ Smali 转 Java（C++ 实现）============
            case "smaliToJava":
                result.put("data", dexManager.smaliToJava(
                    params.getString("sessionId"),
                    params.getString("className")
                ));
                break;

            // ============ 工具操作 ============
            case "fixDex":
                dexManager.fixDex(
                    params.getString("inputPath"),
                    params.getString("outputPath")
                );
                break;

            case "mergeDex":
                dexManager.mergeDex(
                    params.getJSONArray("inputPaths"),
                    params.getString("outputPath")
                );
                break;

            case "splitDex":
                result.put("data", dexManager.splitDex(
                    params.getString("sessionId"),
                    params.getInt("maxClasses")
                ));
                break;

            case "getStrings":
                result.put("data", dexManager.getStrings(params.getString("sessionId")));
                break;

            case "modifyString":
                dexManager.modifyString(
                    params.getString("sessionId"),
                    params.getString("oldString"),
                    params.getString("newString")
                );
                break;

            // ============ APK 操作 ============
            case "openApk":
                result.put("data", apkManager.openApk(
                    params.getString("apkPath"),
                    params.optString("extractDir", null)
                ));
                break;

            case "closeApk":
                apkManager.closeApk(
                    params.getString("sessionId"),
                    params.optBoolean("deleteExtracted", true)
                );
                break;

            case "getApkInfo":
                result.put("data", apkManager.getApkInfo(params.getString("apkPath")));
                break;

            case "listApkContents":
                result.put("data", apkManager.listApkContents(params.getString("apkPath")));
                break;

            case "extractFile":
                result.put("data", apkManager.extractFile(
                    params.getString("apkPath"),
                    params.getString("entryName"),
                    params.getString("outputPath")
                ));
                break;

            case "replaceFile":
                apkManager.replaceFile(
                    params.getString("sessionId"),
                    params.getString("entryName"),
                    params.getString("newFilePath")
                );
                break;

            case "addFile":
                apkManager.addFile(
                    params.getString("sessionId"),
                    params.getString("entryName"),
                    params.getString("filePath")
                );
                break;

            case "deleteFile":
                apkManager.deleteFile(
                    params.getString("sessionId"),
                    params.getString("entryName")
                );
                break;

            case "repackApk":
                result.put("data", apkManager.repackApk(
                    params.getString("sessionId"),
                    params.getString("outputPath")
                ));
                break;

            case "signApk":
                result.put("data", apkManager.signApk(
                    params.getString("apkPath"),
                    params.getString("outputPath"),
                    params.getString("keystorePath"),
                    params.getString("keystorePassword"),
                    params.getString("keyAlias"),
                    params.getString("keyPassword")
                ));
                break;

            case "signApkWithTestKey":
                result.put("data", apkManager.signApkWithTestKey(
                    params.getString("apkPath"),
                    params.getString("outputPath")
                ));
                break;

            case "getApkSignature":
                result.put("data", apkManager.getApkSignature(params.getString("apkPath")));
                break;

            case "getSessionDexFiles":
                result.put("data", apkManager.getSessionDexFiles(params.getString("sessionId")));
                break;

            case "installApk":
                apkManager.installApk(params.getString("apkPath"));
                break;

            case "listApkDirectory":
                result.put("data", apkManager.listApkDirectory(
                    params.getString("apkPath"),
                    params.optString("directory", "")
                ));
                break;

            // ==================== DEX 编辑器操作 ====================
            case "listDexClasses":
                result.put("data", dexManager.listDexClassesFromApk(
                    params.getString("apkPath"),
                    params.getString("dexPath")
                ));
                break;

            case "getDexStrings":
                result.put("data", dexManager.getDexStringsFromApk(
                    params.getString("apkPath"),
                    params.getString("dexPath")
                ));
                break;

            case "searchInDex":
                result.put("data", dexManager.searchInDexFromApk(
                    params.getString("apkPath"),
                    params.getString("dexPath"),
                    params.getString("query")
                ));
                break;

            case "getClassSmali":
                result.put("data", dexManager.getClassSmaliFromApk(
                    params.getString("apkPath"),
                    params.getString("dexPath"),
                    params.getString("className")
                ));
                break;

            case "saveClassSmali":
                result.put("data", dexManager.saveClassSmaliToApk(
                    params.getString("apkPath"),
                    params.getString("dexPath"),
                    params.getString("className"),
                    params.getString("smaliContent")
                ));
                break;

            // ==================== MCP 工作流操作 ====================
            case "listDexFiles":
                result.put("data", dexManager.listDexFilesInApk(
                    params.getString("apkPath")
                ));
                break;

            case "openDex":
                result.put("data", dexManager.openMultipleDex(
                    params.getString("apkPath"),
                    params.getJSONArray("dexFiles")
                ));
                break;

            case "listClasses":
                result.put("data", dexManager.getClassesFromMultiSession(
                    params.getString("sessionId"),
                    params.optString("packageFilter", ""),
                    params.optInt("offset", 0),
                    params.optInt("limit", 100)
                ));
                break;

            case "searchInDexSession":
                result.put("data", dexManager.searchInMultiSession(
                    params.getString("sessionId"),
                    params.getString("query"),
                    params.getString("searchType"),
                    params.optBoolean("caseSensitive", false),
                    params.optInt("maxResults", 50)
                ));
                break;

            case "getClassSmaliFromSession":
                result.put("data", dexManager.getClassSmaliFromSession(
                    params.getString("sessionId"),
                    params.getString("className")
                ));
                break;

            case "modifyClass":
                dexManager.modifyClassInSession(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("smaliContent")
                );
                break;

            case "saveDexToApk":
                result.put("data", dexManager.saveMultiDexSessionToApk(
                    params.getString("sessionId")
                ));
                break;

            case "closeMultiDexSession":
                dexManager.closeMultiDexSession(params.getString("sessionId"));
                break;

            case "addClassToSession":
                dexManager.addClassToSession(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("smaliContent")
                );
                break;

            case "deleteClassFromSession":
                dexManager.deleteClassFromSession(
                    params.getString("sessionId"),
                    params.getString("className")
                );
                break;

            case "getMethodFromSession":
                result.put("data", dexManager.getMethodFromSession(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("methodName"),
                    params.optString("methodSignature", "")
                ));
                break;

            case "modifyMethodInSession":
                dexManager.modifyMethodInSession(
                    params.getString("sessionId"),
                    params.getString("className"),
                    params.getString("methodName"),
                    params.optString("methodSignature", ""),
                    params.getString("newMethodCode")
                );
                break;

            case "listMethodsFromSession":
                result.put("data", dexManager.listMethodsFromSession(
                    params.getString("sessionId"),
                    params.getString("className")
                ));
                break;

            case "listFieldsFromSession":
                result.put("data", dexManager.listFieldsFromSession(
                    params.getString("sessionId"),
                    params.getString("className")
                ));
                break;

            case "renameClassInSession":
                dexManager.renameClassInSession(
                    params.getString("sessionId"),
                    params.getString("oldClassName"),
                    params.getString("newClassName")
                );
                break;

            case "modifyResource":
                result.put("data", dexManager.modifyResourceInApk(
                    params.getString("apkPath"),
                    params.getString("resourcePath"),
                    params.getString("newContent")
                ));
                break;

            case "deleteFileFromApk":
                result.put("data", dexManager.deleteFileFromApk(
                    params.getString("apkPath"),
                    params.getString("filePath")
                ));
                break;

            case "addFileToApk":
                result.put("data", dexManager.addFileToApk(
                    params.getString("apkPath"),
                    params.getString("filePath"),
                    params.getString("content"),
                    params.optBoolean("isBase64", false)
                ));
                break;

            case "listSessions":
                result.put("data", dexManager.listAllSessions());
                break;

            // ==================== XML/资源操作 ====================
            case "getManifest":
                result.put("data", dexManager.getManifestFromApk(
                    params.getString("apkPath")
                ));
                break;

            case "modifyManifest":
                result.put("data", dexManager.modifyManifestInApk(
                    params.getString("apkPath"),
                    params.getString("newManifest")
                ));
                break;

            case "listResources":
                result.put("data", dexManager.listResourcesInApk(
                    params.getString("apkPath"),
                    params.optString("filter", "")
                ));
                break;

            case "getResource":
                result.put("data", dexManager.getResourceFromApk(
                    params.getString("apkPath"),
                    params.getString("resourcePath")
                ));
                break;

            case "replaceInManifest":
                result.put("data", dexManager.replaceInManifest(
                    params.getString("apkPath"),
                    params.getJSONArray("replacements")
                ));
                break;

            case "listApkFiles":
                result.put("data", dexManager.listApkFiles(
                    params.getString("apkPath"),
                    params.optString("filter", ""),
                    params.optInt("limit", 100),
                    params.optInt("offset", 0)
                ));
                break;

            case "searchTextInApk":
                result.put("data", dexManager.searchTextInApk(
                    params.getString("apkPath"),
                    params.getString("pattern"),
                    params.optJSONArray("fileExtensions"),
                    params.optBoolean("caseSensitive", false),
                    params.optBoolean("isRegex", false),
                    params.optInt("maxResults", 50),
                    params.optInt("contextLines", 2)
                ));
                break;

            case "readApkFile":
                result.put("data", dexManager.readApkFile(
                    params.getString("apkPath"),
                    params.getString("filePath"),
                    params.optBoolean("asBase64", false),
                    params.optInt("maxBytes", 0),
                    params.optInt("offset", 0)
                ));
                break;

            default:
                result.put("success", false);
                result.put("error", "Unknown action: " + action);
        }

        return result;
    }

    /**
     * 打开原生 Smali 编辑器
     */
    @PluginMethod
    public void openSmaliEditor(PluginCall call) {
        String content = call.getString("content", "");
        String title = call.getString("title", "Smali Editor");
        String className = call.getString("className", "");
        boolean readOnly = call.getBoolean("readOnly", false);

        Intent intent = new Intent(getContext(), SmaliEditorActivity.class);
        intent.putExtra(SmaliEditorActivity.EXTRA_CONTENT, content);
        intent.putExtra(SmaliEditorActivity.EXTRA_TITLE, title);
        intent.putExtra(SmaliEditorActivity.EXTRA_CLASS_NAME, className);
        intent.putExtra(SmaliEditorActivity.EXTRA_READ_ONLY, readOnly);
        intent.putExtra(SmaliEditorActivity.EXTRA_SYNTAX_FILE, "smali.json");

        startActivityForResult(call, intent, "handleEditorResult");
    }

    /**
     * 打开原生 XML 编辑器
     */
    @PluginMethod
    public void openXmlEditor(PluginCall call) {
        String content = call.getString("content", "");
        String title = call.getString("title", "XML Editor");
        String filePath = call.getString("filePath", "");
        boolean readOnly = call.getBoolean("readOnly", false);

        Intent intent = new Intent(getContext(), SmaliEditorActivity.class);
        intent.putExtra(SmaliEditorActivity.EXTRA_CONTENT, content);
        intent.putExtra(SmaliEditorActivity.EXTRA_TITLE, title);
        intent.putExtra(SmaliEditorActivity.EXTRA_CLASS_NAME, filePath);
        intent.putExtra(SmaliEditorActivity.EXTRA_READ_ONLY, readOnly);
        intent.putExtra(SmaliEditorActivity.EXTRA_SYNTAX_FILE, "xml.json");

        startActivityForResult(call, intent, "handleEditorResult");
    }

    /**
     * 打开通用代码编辑器
     */
    @PluginMethod
    public void openCodeEditor(PluginCall call) {
        String content = call.getString("content", "");
        String title = call.getString("title", "Code Editor");
        String filePath = call.getString("filePath", "");
        boolean readOnly = call.getBoolean("readOnly", false);
        String syntaxFile = call.getString("syntaxFile", "json.json");

        Intent intent = new Intent(getContext(), SmaliEditorActivity.class);
        intent.putExtra(SmaliEditorActivity.EXTRA_CONTENT, content);
        intent.putExtra(SmaliEditorActivity.EXTRA_TITLE, title);
        intent.putExtra(SmaliEditorActivity.EXTRA_CLASS_NAME, filePath);
        intent.putExtra(SmaliEditorActivity.EXTRA_READ_ONLY, readOnly);
        intent.putExtra(SmaliEditorActivity.EXTRA_SYNTAX_FILE, syntaxFile);

        startActivityForResult(call, intent, "handleEditorResult");
    }

    @ActivityCallback
    private void handleEditorResult(PluginCall call, ActivityResult activityResult) {
        if (call == null) {
            return;
        }
        
        JSObject result = new JSObject();
        if (activityResult.getResultCode() == Activity.RESULT_OK) {
            Intent data = activityResult.getData();
            if (data != null) {
                result.put("success", true);
                result.put("content", data.getStringExtra(SmaliEditorActivity.RESULT_CONTENT));
                result.put("modified", data.getBooleanExtra(SmaliEditorActivity.RESULT_MODIFIED, false));
            } else {
                result.put("success", false);
                result.put("cancelled", true);
            }
        } else {
            result.put("success", false);
            result.put("cancelled", true);
        }
        call.resolve(result);
    }
}
