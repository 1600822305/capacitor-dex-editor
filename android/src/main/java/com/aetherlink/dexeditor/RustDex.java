package com.aetherlink.dexeditor;

import android.util.Log;

/**
 * RustDex - Rust 实现的 DEX 解析器 JNI 接口
 * 使用 azw413/smali crate 实现高性能 DEX 搜索
 */
public class RustDex {
    private static final String TAG = "RustDex";
    private static boolean libraryLoaded = false;

    static {
        try {
            System.loadLibrary("dex_rust");
            libraryLoaded = true;
            Log.d(TAG, "Rust library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Failed to load Rust library: " + e.getMessage());
            libraryLoaded = false;
        }
    }

    /**
     * 检查 Rust 库是否可用
     */
    public static boolean isAvailable() {
        return libraryLoaded;
    }

    /**
     * 在 DEX 中搜索
     * @param dexBytes DEX 文件字节数组
     * @param query 搜索查询
     * @param searchType 搜索类型: class, method, field, string, code
     * @param caseSensitive 是否区分大小写
     * @param maxResults 最大结果数
     * @return JSON 格式的搜索结果
     */
    public static native String searchInDex(
        byte[] dexBytes,
        String query,
        String searchType,
        boolean caseSensitive,
        int maxResults
    );

    /**
     * 列出 DEX 中的类
     * @param dexBytes DEX 文件字节数组
     * @param packageFilter 包名过滤器
     * @param offset 偏移量
     * @param limit 限制数量
     * @return JSON 格式的类列表
     */
    public static native String listClasses(
        byte[] dexBytes,
        String packageFilter,
        int offset,
        int limit
    );

    /**
     * 获取类的 Smali 代码
     * @param dexBytes DEX 文件字节数组
     * @param className 类名
     * @return JSON 格式的 Smali 代码
     */
    public static native String getClassSmali(
        byte[] dexBytes,
        String className
    );

    /**
     * 修改 DEX 中的类
     * @param dexBytes DEX 文件字节数组
     * @param className 类名
     * @param newSmali 新的 Smali 代码
     * @return 修改后的 DEX 字节数组，失败返回 null
     */
    public static native byte[] modifyClass(
        byte[] dexBytes,
        String className,
        String newSmali
    );

    /**
     * 添加新类到 DEX
     * @param dexBytes DEX 文件字节数组
     * @param newSmali 新类的 Smali 代码
     * @return 修改后的 DEX 字节数组，失败返回 null
     */
    public static native byte[] addClass(
        byte[] dexBytes,
        String newSmali
    );

    /**
     * 从 DEX 中删除类
     * @param dexBytes DEX 文件字节数组
     * @param className 要删除的类名
     * @return 修改后的 DEX 字节数组，失败返回 null
     */
    public static native byte[] deleteClass(
        byte[] dexBytes,
        String className
    );

    // ==================== 方法级操作 ====================

    /**
     * 列出类中的所有方法
     * @param dexBytes DEX 文件字节数组
     * @param className 类名
     * @return JSON 格式的方法列表
     */
    public static native String listMethods(
        byte[] dexBytes,
        String className
    );

    /**
     * 获取单个方法的 Smali 代码
     * @param dexBytes DEX 文件字节数组
     * @param className 类名
     * @param methodName 方法名
     * @param methodSignature 方法签名（可为空字符串表示不限制）
     * @return JSON 格式的方法 Smali 代码
     */
    public static native String getMethod(
        byte[] dexBytes,
        String className,
        String methodName,
        String methodSignature
    );

    /**
     * 添加方法到类
     * @param dexBytes DEX 文件字节数组
     * @param className 类名
     * @param methodSmali 方法的 Smali 代码
     * @return 修改后的 DEX 字节数组，失败返回 null
     */
    public static native byte[] addMethod(
        byte[] dexBytes,
        String className,
        String methodSmali
    );

    /**
     * 删除类中的方法
     * @param dexBytes DEX 文件字节数组
     * @param className 类名
     * @param methodName 方法名
     * @param methodSignature 方法签名（可为空字符串表示不限制）
     * @return 修改后的 DEX 字节数组，失败返回 null
     */
    public static native byte[] deleteMethod(
        byte[] dexBytes,
        String className,
        String methodName,
        String methodSignature
    );

    // ==================== 字段级操作 ====================

    /**
     * 列出类中的所有字段
     * @param dexBytes DEX 文件字节数组
     * @param className 类名
     * @return JSON 格式的字段列表
     */
    public static native String listFields(
        byte[] dexBytes,
        String className
    );

    /**
     * 添加字段到类
     * @param dexBytes DEX 文件字节数组
     * @param className 类名
     * @param fieldSmali 字段的 Smali 定义（如 ".field public myField:I"）
     * @return 修改后的 DEX 字节数组，失败返回 null
     */
    public static native byte[] addField(
        byte[] dexBytes,
        String className,
        String fieldSmali
    );

    /**
     * 删除类中的字段
     * @param dexBytes DEX 文件字节数组
     * @param className 类名
     * @param fieldName 字段名
     * @return 修改后的 DEX 字节数组，失败返回 null
     */
    public static native byte[] deleteField(
        byte[] dexBytes,
        String className,
        String fieldName
    );
}
