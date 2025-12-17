use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jbyteArray, jint, jstring};
use jni::JNIEnv;
use smali::dex::DexFile;
use smali::types::SmaliClass;
use std::panic;

/// 搜索 DEX 文件中的内容
/// 返回 JSON 格式的搜索结果
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_searchInDex(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    query: JString,
    search_type: JString,
    case_sensitive: jboolean,
    max_results: jint,
) -> jstring {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        search_in_dex_impl(&mut env, dex_bytes, query, search_type, case_sensitive, max_results)
    }));

    match result {
        Ok(Ok(json)) => env
            .new_string(&json)
            .map(|s| s.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(e)) => {
            let error_json = format!(r#"{{"error": "{}"}}"#, e);
            env.new_string(&error_json)
                .map(|s| s.into_raw())
                .unwrap_or(std::ptr::null_mut())
        }
        Err(_) => {
            let error_json = r#"{"error": "Rust panic occurred"}"#;
            env.new_string(error_json)
                .map(|s| s.into_raw())
                .unwrap_or(std::ptr::null_mut())
        }
    }
}

fn search_in_dex_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    query: JString,
    search_type: JString,
    case_sensitive: jboolean,
    max_results: jint,
) -> Result<String, String> {
    // 获取 DEX 字节数据
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    // 获取查询字符串
    let query_str: String = env
        .get_string(&query)
        .map_err(|e| format!("Failed to get query string: {}", e))?
        .into();

    // 获取搜索类型
    let search_type_str: String = env
        .get_string(&search_type)
        .map_err(|e| format!("Failed to get search type: {}", e))?
        .into();

    let case_sensitive = case_sensitive != 0;
    let max_results = max_results as usize;

    // 使用 smali crate 解析 DEX
    match smali::dex::DexFile::from_bytes(&dex_data) {
        Ok(dex) => {
            let results = search_dex(&dex, &query_str, &search_type_str, case_sensitive, max_results);
            Ok(results)
        }
        Err(e) => Err(format!("Failed to parse DEX: {:?}", e)),
    }
}

fn search_dex(
    dex: &smali::dex::DexFile,
    query: &str,
    search_type: &str,
    case_sensitive: bool,
    max_results: usize,
) -> String {
    let mut results = Vec::new();
    let query_match = if case_sensitive {
        query.to_string()
    } else {
        query.to_lowercase()
    };

    // 转换为 Smali 类
    let classes = match dex.to_smali() {
        Ok(c) => c,
        Err(_) => return r#"{"error": "Failed to disassemble DEX", "results": []}"#.to_string(),
    };

    for class in classes.iter() {
        if results.len() >= max_results {
            break;
        }

        let class_name = &class.name.as_java_type();
        let class_name_match = if case_sensitive {
            class_name.to_string()
        } else {
            class_name.to_lowercase()
        };

        match search_type {
            "class" | "package" => {
                if class_name_match.contains(&query_match) {
                    results.push(format!(
                        r#"{{"type": "class", "className": "{}"}}"#,
                        class_name
                    ));
                }
            }
            "method" => {
                for method in &class.methods {
                    if results.len() >= max_results {
                        break;
                    }
                    let method_name = &method.name;
                    let method_match = if case_sensitive {
                        method_name.to_string()
                    } else {
                        method_name.to_lowercase()
                    };
                    if method_match.contains(&query_match) {
                        results.push(format!(
                            r#"{{"type": "method", "className": "{}", "methodName": "{}"}}"#,
                            class_name, method_name
                        ));
                    }
                }
            }
            "field" => {
                for field in &class.fields {
                    if results.len() >= max_results {
                        break;
                    }
                    let field_name = &field.name;
                    let field_match = if case_sensitive {
                        field_name.to_string()
                    } else {
                        field_name.to_lowercase()
                    };
                    if field_match.contains(&query_match) {
                        results.push(format!(
                            r#"{{"type": "field", "className": "{}", "fieldName": "{}"}}"#,
                            class_name, field_name
                        ));
                    }
                }
            }
            "string" | "code" => {
                // 搜索方法中的字符串/代码
                for method in &class.methods {
                    if results.len() >= max_results {
                        break;
                    }
                    // 将方法转换为字符串表示并搜索
                    let method_str = format!("{:?}", method);
                    let method_match = if case_sensitive {
                        method_str.clone()
                    } else {
                        method_str.to_lowercase()
                    };
                    if method_match.contains(&query_match) {
                        results.push(format!(
                            r#"{{"type": "{}", "className": "{}", "methodName": "{}"}}"#,
                            search_type, class_name, method.name
                        ));
                    }
                }
            }
            _ => {}
        }
    }

    format!(
        r#"{{"query": "{}", "searchType": "{}", "total": {}, "results": [{}]}}"#,
        query,
        search_type,
        results.len(),
        results.join(", ")
    )
}

/// 获取 DEX 中的类列表
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_listClasses(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    package_filter: JString,
    offset: jint,
    limit: jint,
) -> jstring {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        list_classes_impl(&mut env, dex_bytes, package_filter, offset, limit)
    }));

    match result {
        Ok(Ok(json)) => env
            .new_string(&json)
            .map(|s| s.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(e)) => {
            let error_json = format!(r#"{{"error": "{}"}}"#, e);
            env.new_string(&error_json)
                .map(|s| s.into_raw())
                .unwrap_or(std::ptr::null_mut())
        }
        Err(_) => {
            let error_json = r#"{"error": "Rust panic occurred"}"#;
            env.new_string(error_json)
                .map(|s| s.into_raw())
                .unwrap_or(std::ptr::null_mut())
        }
    }
}

fn list_classes_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    package_filter: JString,
    offset: jint,
    limit: jint,
) -> Result<String, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let filter: String = env
        .get_string(&package_filter)
        .map_err(|e| format!("Failed to get filter: {}", e))?
        .into();

    let dex = smali::dex::DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    let filtered: Vec<_> = classes
        .iter()
        .map(|c| c.name.as_java_type())
        .filter(|name| filter.is_empty() || name.starts_with(&filter))
        .collect();

    let total = filtered.len();
    let offset = offset as usize;
    let limit = limit as usize;

    let page: Vec<_> = filtered
        .into_iter()
        .skip(offset)
        .take(limit)
        .map(|name| format!(r#""{}""#, name))
        .collect();

    Ok(format!(
        r#"{{"total": {}, "offset": {}, "limit": {}, "classes": [{}], "hasMore": {}}}"#,
        total,
        offset,
        limit,
        page.join(", "),
        offset + page.len() < total
    ))
}

/// 获取类的 Smali 代码
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_getClassSmali(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    class_name: JString,
) -> jstring {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        get_class_smali_impl(&mut env, dex_bytes, class_name)
    }));

    match result {
        Ok(Ok(json)) => env
            .new_string(&json)
            .map(|s| s.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(e)) => {
            let error_json = format!(r#"{{"error": "{}"}}"#, e);
            env.new_string(&error_json)
                .map(|s| s.into_raw())
                .unwrap_or(std::ptr::null_mut())
        }
        Err(_) => {
            let error_json = r#"{"error": "Rust panic occurred"}"#;
            env.new_string(error_json)
                .map(|s| s.into_raw())
                .unwrap_or(std::ptr::null_mut())
        }
    }
}

fn get_class_smali_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    class_name: JString,
) -> Result<String, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let target_class: String = env
        .get_string(&class_name)
        .map_err(|e| format!("Failed to get class name: {}", e))?
        .into();

    let dex = smali::dex::DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    for class in classes.iter() {
        if class.name.as_java_type() == target_class {
            let smali_code = class.to_string();
            // Escape for JSON
            let escaped = smali_code
                .replace('\\', "\\\\")
                .replace('"', "\\\"")
                .replace('\n', "\\n")
                .replace('\r', "\\r")
                .replace('\t', "\\t");
            return Ok(format!(
                r#"{{"className": "{}", "smaliContent": "{}"}}"#,
                target_class, escaped
            ));
        }
    }

    Err(format!("Class not found: {}", target_class))
}

/// 修改 DEX 中的类（替换 Smali 代码）
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_modifyClass(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    class_name: JString,
    new_smali: JString,
) -> jbyteArray {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        modify_class_impl(&mut env, dex_bytes, class_name, new_smali)
    }));

    match result {
        Ok(Ok(bytes)) => env
            .byte_array_from_slice(&bytes)
            .map(|arr| arr.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(_)) | Err(_) => std::ptr::null_mut(),
    }
}

fn modify_class_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    class_name: JString,
    new_smali: JString,
) -> Result<Vec<u8>, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let target_class: String = env
        .get_string(&class_name)
        .map_err(|e| format!("Failed to get class name: {}", e))?
        .into();

    let new_smali_str: String = env
        .get_string(&new_smali)
        .map_err(|e| format!("Failed to get smali: {}", e))?
        .into();

    let dex = DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let mut classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    // 使用 SmaliClass::from_smali 解析新的 Smali
    let new_class = SmaliClass::from_smali(&new_smali_str)
        .map_err(|e| format!("Failed to parse smali: {:?}", e))?;

    // 找到并替换目标类
    let mut found = false;
    for i in 0..classes.len() {
        if classes[i].name.as_java_type() == target_class {
            classes[i] = new_class;
            found = true;
            break;
        }
    }

    if !found {
        return Err(format!("Class not found: {}", target_class));
    }

    // 重新编译为 DEX
    let new_dex = DexFile::from_smali(&classes)
        .map_err(|e| format!("Failed to compile DEX: {:?}", e))?;

    Ok(new_dex.to_bytes().to_vec())
}

/// 添加新类到 DEX
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_addClass(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    new_smali: JString,
) -> jbyteArray {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        add_class_impl(&mut env, dex_bytes, new_smali)
    }));

    match result {
        Ok(Ok(bytes)) => env
            .byte_array_from_slice(&bytes)
            .map(|arr| arr.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(_)) | Err(_) => std::ptr::null_mut(),
    }
}

fn add_class_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    new_smali: JString,
) -> Result<Vec<u8>, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let new_smali_str: String = env
        .get_string(&new_smali)
        .map_err(|e| format!("Failed to get smali: {}", e))?
        .into();

    let dex = DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let mut classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    // 解析新的 Smali 类
    let new_class = SmaliClass::from_smali(&new_smali_str)
        .map_err(|e| format!("Failed to parse smali: {:?}", e))?;

    classes.push(new_class);

    // 重新编译为 DEX
    let new_dex = DexFile::from_smali(&classes)
        .map_err(|e| format!("Failed to compile DEX: {:?}", e))?;

    Ok(new_dex.to_bytes().to_vec())
}

/// 从 DEX 中删除类
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_deleteClass(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    class_name: JString,
) -> jbyteArray {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        delete_class_impl(&mut env, dex_bytes, class_name)
    }));

    match result {
        Ok(Ok(bytes)) => env
            .byte_array_from_slice(&bytes)
            .map(|arr| arr.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(_)) | Err(_) => std::ptr::null_mut(),
    }
}

fn delete_class_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    class_name: JString,
) -> Result<Vec<u8>, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let target_class: String = env
        .get_string(&class_name)
        .map_err(|e| format!("Failed to get class name: {}", e))?
        .into();

    let dex = DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let mut classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    let original_len = classes.len();
    classes.retain(|c| c.name.as_java_type() != target_class);

    if classes.len() == original_len {
        return Err(format!("Class not found: {}", target_class));
    }

    // 重新编译为 DEX
    let new_dex = DexFile::from_smali(&classes)
        .map_err(|e| format!("Failed to compile DEX: {:?}", e))?;

    Ok(new_dex.to_bytes().to_vec())
}

/// 列出类中的所有方法
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_listMethods(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    class_name: JString,
) -> jstring {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        list_methods_impl(&mut env, dex_bytes, class_name)
    }));

    match result {
        Ok(Ok(json)) => env
            .new_string(&json)
            .map(|s| s.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(e)) => {
            let error_json = format!(r#"{{"error": "{}"}}"#, e);
            env.new_string(&error_json)
                .map(|s| s.into_raw())
                .unwrap_or(std::ptr::null_mut())
        }
        Err(_) => {
            let error_json = r#"{"error": "Rust panic occurred"}"#;
            env.new_string(error_json)
                .map(|s| s.into_raw())
                .unwrap_or(std::ptr::null_mut())
        }
    }
}

fn list_methods_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    class_name: JString,
) -> Result<String, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let target_class: String = env
        .get_string(&class_name)
        .map_err(|e| format!("Failed to get class name: {}", e))?
        .into();

    let dex = DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    for class in classes.iter() {
        if class.name.as_java_type() == target_class {
            let methods: Vec<String> = class.methods.iter().map(|m| {
                let modifiers: Vec<&str> = m.modifiers.iter().map(|mod_| mod_.to_str()).collect();
                let mod_str = modifiers.join(" ");
                format!(
                    r#"{{"name": "{}", "signature": "{:?}", "modifiers": "{}", "isConstructor": {}}}"#,
                    m.name,
                    m.signature,
                    mod_str,
                    m.constructor
                )
            }).collect();
            
            return Ok(format!(
                r#"{{"className": "{}", "methods": [{}], "total": {}}}"#,
                target_class,
                methods.join(", "),
                methods.len()
            ));
        }
    }

    Err(format!("Class not found: {}", target_class))
}

/// 获取单个方法的 Smali 代码
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_getMethod(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    class_name: JString,
    method_name: JString,
    method_signature: JString,
) -> jstring {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        get_method_impl(&mut env, dex_bytes, class_name, method_name, method_signature)
    }));

    match result {
        Ok(Ok(json)) => env
            .new_string(&json)
            .map(|s| s.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(e)) => {
            let error_json = format!(r#"{{"error": "{}"}}"#, e);
            env.new_string(&error_json)
                .map(|s| s.into_raw())
                .unwrap_or(std::ptr::null_mut())
        }
        Err(_) => {
            let error_json = r#"{"error": "Rust panic occurred"}"#;
            env.new_string(error_json)
                .map(|s| s.into_raw())
                .unwrap_or(std::ptr::null_mut())
        }
    }
}

fn get_method_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    class_name: JString,
    method_name: JString,
    method_signature: JString,
) -> Result<String, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let target_class: String = env
        .get_string(&class_name)
        .map_err(|e| format!("Failed to get class name: {}", e))?
        .into();

    let target_method: String = env
        .get_string(&method_name)
        .map_err(|e| format!("Failed to get method name: {}", e))?
        .into();

    let target_sig: String = env
        .get_string(&method_signature)
        .map_err(|e| format!("Failed to get method signature: {}", e))?
        .into();

    let dex = DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    for class in classes.iter() {
        if class.name.as_java_type() == target_class {
            for method in class.methods.iter() {
                let sig_match = target_sig.is_empty() || format!("{:?}", method.signature) == target_sig;
                if method.name == target_method && sig_match {
                    let smali_code = method.to_string();
                    let escaped = smali_code
                        .replace('\\', "\\\\")
                        .replace('"', "\\\"")
                        .replace('\n', "\\n")
                        .replace('\r', "\\r")
                        .replace('\t', "\\t");
                    
                    return Ok(format!(
                        r#"{{"className": "{}", "methodName": "{}", "signature": "{:?}", "smaliContent": "{}"}}"#,
                        target_class,
                        target_method,
                        method.signature,
                        escaped
                    ));
                }
            }
            return Err(format!("Method not found: {}", target_method));
        }
    }

    Err(format!("Class not found: {}", target_class))
}

/// 添加方法到类
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_addMethod(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    class_name: JString,
    method_smali: JString,
) -> jbyteArray {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        add_method_impl(&mut env, dex_bytes, class_name, method_smali)
    }));

    match result {
        Ok(Ok(bytes)) => env
            .byte_array_from_slice(&bytes)
            .map(|arr| arr.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(_)) | Err(_) => std::ptr::null_mut(),
    }
}

fn add_method_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    class_name: JString,
    method_smali: JString,
) -> Result<Vec<u8>, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let target_class: String = env
        .get_string(&class_name)
        .map_err(|e| format!("Failed to get class name: {}", e))?
        .into();

    let method_def: String = env
        .get_string(&method_smali)
        .map_err(|e| format!("Failed to get method smali: {}", e))?
        .into();

    let dex = DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let mut classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    // 构造临时类来解析方法
    let temp_class_smali = format!(
        ".class public LTemp;\n.super Ljava/lang/Object;\n\n{}",
        method_def
    );
    
    let temp_class = SmaliClass::from_smali(&temp_class_smali)
        .map_err(|e| format!("Failed to parse method: {:?}", e))?;
    
    if temp_class.methods.is_empty() {
        return Err("No method found in provided smali".to_string());
    }

    // 找到目标类并添加方法
    let mut found = false;
    for class in classes.iter_mut() {
        if class.name.as_java_type() == target_class {
            for new_method in temp_class.methods {
                class.methods.push(new_method);
            }
            found = true;
            break;
        }
    }

    if !found {
        return Err(format!("Class not found: {}", target_class));
    }

    let new_dex = DexFile::from_smali(&classes)
        .map_err(|e| format!("Failed to compile DEX: {:?}", e))?;

    Ok(new_dex.to_bytes().to_vec())
}

/// 删除类中的方法
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_deleteMethod(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    class_name: JString,
    method_name: JString,
    method_signature: JString,
) -> jbyteArray {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        delete_method_impl(&mut env, dex_bytes, class_name, method_name, method_signature)
    }));

    match result {
        Ok(Ok(bytes)) => env
            .byte_array_from_slice(&bytes)
            .map(|arr| arr.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(_)) | Err(_) => std::ptr::null_mut(),
    }
}

fn delete_method_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    class_name: JString,
    method_name: JString,
    method_signature: JString,
) -> Result<Vec<u8>, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let target_class: String = env
        .get_string(&class_name)
        .map_err(|e| format!("Failed to get class name: {}", e))?
        .into();

    let target_method: String = env
        .get_string(&method_name)
        .map_err(|e| format!("Failed to get method name: {}", e))?
        .into();

    let target_sig: String = env
        .get_string(&method_signature)
        .map_err(|e| format!("Failed to get method signature: {}", e))?
        .into();

    let dex = DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let mut classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    let mut found_class = false;
    let mut found_method = false;
    for class in classes.iter_mut() {
        if class.name.as_java_type() == target_class {
            found_class = true;
            let original_len = class.methods.len();
            class.methods.retain(|m| {
                let sig_match = target_sig.is_empty() || format!("{:?}", m.signature) == target_sig;
                !(m.name == target_method && sig_match)
            });
            if class.methods.len() < original_len {
                found_method = true;
            }
            break;
        }
    }

    if !found_class {
        return Err(format!("Class not found: {}", target_class));
    }
    if !found_method {
        return Err(format!("Method not found: {}", target_method));
    }

    let new_dex = DexFile::from_smali(&classes)
        .map_err(|e| format!("Failed to compile DEX: {:?}", e))?;

    Ok(new_dex.to_bytes().to_vec())
}

/// 添加字段到类
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_addField(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    class_name: JString,
    field_smali: JString,
) -> jbyteArray {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        add_field_impl(&mut env, dex_bytes, class_name, field_smali)
    }));

    match result {
        Ok(Ok(bytes)) => env
            .byte_array_from_slice(&bytes)
            .map(|arr| arr.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(_)) | Err(_) => std::ptr::null_mut(),
    }
}

fn add_field_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    class_name: JString,
    field_smali: JString,
) -> Result<Vec<u8>, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let target_class: String = env
        .get_string(&class_name)
        .map_err(|e| format!("Failed to get class name: {}", e))?
        .into();

    let field_def: String = env
        .get_string(&field_smali)
        .map_err(|e| format!("Failed to get field smali: {}", e))?
        .into();

    let dex = DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let mut classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    // 构造临时类来解析字段
    let temp_class_smali = format!(
        ".class public LTemp;\n.super Ljava/lang/Object;\n\n{}\n",
        field_def
    );
    
    let temp_class = SmaliClass::from_smali(&temp_class_smali)
        .map_err(|e| format!("Failed to parse field: {:?}", e))?;
    
    if temp_class.fields.is_empty() {
        return Err("No field found in provided smali".to_string());
    }

    let mut found = false;
    for class in classes.iter_mut() {
        if class.name.as_java_type() == target_class {
            for new_field in temp_class.fields {
                class.fields.push(new_field);
            }
            found = true;
            break;
        }
    }

    if !found {
        return Err(format!("Class not found: {}", target_class));
    }

    let new_dex = DexFile::from_smali(&classes)
        .map_err(|e| format!("Failed to compile DEX: {:?}", e))?;

    Ok(new_dex.to_bytes().to_vec())
}

/// 删除类中的字段
#[no_mangle]
pub extern "C" fn Java_com_aetherlink_dexeditor_RustDex_deleteField(
    mut env: JNIEnv,
    _class: JClass,
    dex_bytes: JByteArray,
    class_name: JString,
    field_name: JString,
) -> jbyteArray {
    let result = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        delete_field_impl(&mut env, dex_bytes, class_name, field_name)
    }));

    match result {
        Ok(Ok(bytes)) => env
            .byte_array_from_slice(&bytes)
            .map(|arr| arr.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Ok(Err(_)) | Err(_) => std::ptr::null_mut(),
    }
}

fn delete_field_impl(
    env: &mut JNIEnv,
    dex_bytes: JByteArray,
    class_name: JString,
    field_name: JString,
) -> Result<Vec<u8>, String> {
    let dex_data = env
        .convert_byte_array(dex_bytes)
        .map_err(|e| format!("Failed to convert byte array: {}", e))?;

    let target_class: String = env
        .get_string(&class_name)
        .map_err(|e| format!("Failed to get class name: {}", e))?
        .into();

    let target_field: String = env
        .get_string(&field_name)
        .map_err(|e| format!("Failed to get field name: {}", e))?
        .into();

    let dex = DexFile::from_bytes(&dex_data)
        .map_err(|e| format!("Failed to parse DEX: {:?}", e))?;

    let mut classes = dex
        .to_smali()
        .map_err(|e| format!("Failed to disassemble: {:?}", e))?;

    let mut found_class = false;
    let mut found_field = false;
    for class in classes.iter_mut() {
        if class.name.as_java_type() == target_class {
            found_class = true;
            let original_len = class.fields.len();
            class.fields.retain(|f| f.name != target_field);
            if class.fields.len() < original_len {
                found_field = true;
            }
            break;
        }
    }

    if !found_class {
        return Err(format!("Class not found: {}", target_class));
    }
    if !found_field {
        return Err(format!("Field not found: {}", target_field));
    }

    let new_dex = DexFile::from_smali(&classes)
        .map_err(|e| format!("Failed to compile DEX: {:?}", e))?;

    Ok(new_dex.to_bytes().to_vec())
}
