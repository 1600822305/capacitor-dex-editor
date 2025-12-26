#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#include "dex/dex_parser.h"
#include "dex/dex_builder.h"
#include "dex/smali_disasm.h"
#include "dex/smali_to_java.h"
#include "xml/axml_parser.h"
#include "arsc/arsc_parser.h"
#include "apk/apk_handler.h"

#include <nlohmann/json.hpp>

#define LOG_TAG "CppDex"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using json = nlohmann::json;

// Helper: Convert jbyteArray to std::vector<uint8_t>
static std::vector<uint8_t> jbyteArray_to_vector(JNIEnv* env, jbyteArray array) {
    if (!array) return {};
    jsize len = env->GetArrayLength(array);
    std::vector<uint8_t> result(len);
    env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte*>(result.data()));
    return result;
}

// Helper: Convert std::vector<uint8_t> to jbyteArray
static jbyteArray vector_to_jbyteArray(JNIEnv* env, const std::vector<uint8_t>& data) {
    jbyteArray result = env->NewByteArray(static_cast<jsize>(data.size()));
    if (result) {
        env->SetByteArrayRegion(result, 0, static_cast<jsize>(data.size()),
                                reinterpret_cast<const jbyte*>(data.data()));
    }
    return result;
}

// Helper: Convert jstring to std::string
static std::string jstring_to_string(JNIEnv* env, jstring str) {
    if (!str) return "";
    const char* chars = env->GetStringUTFChars(str, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(str, chars);
    return result;
}

// Helper: Convert std::string to jstring
static jstring string_to_jstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

extern "C" {

// ==================== DEX 解析操作 ====================

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_getDexInfo(JNIEnv* env, jclass, jbyteArray dexBytes) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    const auto& header = parser.header();
    json result = {
        {"version", std::string(reinterpret_cast<const char*>(header.magic + 4), 3)},
        {"file_size", header.file_size},
        {"strings_count", header.string_ids_size},
        {"types_count", header.type_ids_size},
        {"protos_count", header.proto_ids_size},
        {"fields_count", header.field_ids_size},
        {"methods_count", header.method_ids_size},
        {"classes_count", header.class_defs_size}
    };
    
    return string_to_jstring(env, result.dump());
}

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_listClasses(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                  jstring packageFilter, jint offset, jint limit) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string filter = jstring_to_string(env, packageFilter);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    json class_list = json::array();
    const auto& classes = parser.classes();
    int count = 0;
    int matched = 0;
    
    for (const auto& cls : classes) {
        std::string class_name = parser.get_class_name(cls.class_idx);
        
        if (!filter.empty() && class_name.find(filter) == std::string::npos) {
            continue;
        }
        
        matched++;
        if (matched > offset && count < limit) {
            class_list.push_back(class_name);
            count++;
        }
    }
    
    json result = {
        {"classes", class_list},
        {"shown", class_list.size()},
        {"total", matched}
    };
    
    return string_to_jstring(env, result.dump());
}

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_searchInDex(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                  jstring query, jstring searchType,
                                                  jboolean caseSensitive, jint maxResults) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string q = jstring_to_string(env, query);
    std::string type = jstring_to_string(env, searchType);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    json results = json::array();
    int count = 0;
    
    // 转换为小写用于不区分大小写搜索
    std::string q_lower = q;
    if (!caseSensitive) {
        std::transform(q_lower.begin(), q_lower.end(), q_lower.begin(), ::tolower);
    }
    
    if (type == "string") {
        for (const auto& s : parser.strings()) {
            if (count >= maxResults) break;
            
            std::string s_check = caseSensitive ? s : s;
            if (!caseSensitive) {
                s_check.resize(s.size());
                std::transform(s.begin(), s.end(), s_check.begin(), ::tolower);
            }
            
            if (s_check.find(q_lower) != std::string::npos) {
                results.push_back({{"type", "string"}, {"value", s}});
                count++;
            }
        }
    } else if (type == "class") {
        for (const auto& cls : parser.classes()) {
            if (count >= maxResults) break;
            std::string class_name = parser.get_class_name(cls.class_idx);
            
            std::string check = caseSensitive ? class_name : class_name;
            if (!caseSensitive) {
                check.resize(class_name.size());
                std::transform(class_name.begin(), class_name.end(), check.begin(), ::tolower);
            }
            
            if (check.find(q_lower) != std::string::npos) {
                results.push_back({{"type", "class"}, {"name", class_name}});
                count++;
            }
        }
    } else if (type == "method") {
        auto methods = parser.get_methods();
        for (const auto& m : methods) {
            if (count >= maxResults) break;
            
            std::string check = caseSensitive ? m.method_name : m.method_name;
            if (!caseSensitive) {
                check.resize(m.method_name.size());
                std::transform(m.method_name.begin(), m.method_name.end(), check.begin(), ::tolower);
            }
            
            if (check.find(q_lower) != std::string::npos) {
                results.push_back({
                    {"type", "method"},
                    {"class", m.class_name},
                    {"name", m.method_name},
                    {"prototype", m.prototype}
                });
                count++;
            }
        }
    } else if (type == "field") {
        auto fields = parser.get_fields();
        for (const auto& f : fields) {
            if (count >= maxResults) break;
            
            std::string check = caseSensitive ? f.field_name : f.field_name;
            if (!caseSensitive) {
                check.resize(f.field_name.size());
                std::transform(f.field_name.begin(), f.field_name.end(), check.begin(), ::tolower);
            }
            
            if (check.find(q_lower) != std::string::npos) {
                results.push_back({
                    {"type", "field"},
                    {"class", f.class_name},
                    {"name", f.field_name},
                    {"fieldType", f.type_name}
                });
                count++;
            }
        }
    }
    
    json result = {
        {"query", q},
        {"searchType", type},
        {"results", results},
        {"count", results.size()}
    };
    
    return string_to_jstring(env, result.dump());
}

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_getClassSmali(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                    jstring className) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string class_name = jstring_to_string(env, className);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    // 设置反汇编器上下文
    dex::SmaliDisassembler disasm;
    disasm.set_strings(parser.strings());
    disasm.set_types(parser.types());
    disasm.set_methods(parser.get_method_signatures());
    disasm.set_fields(parser.get_field_signatures());
    
    // 查找类并反汇编所有方法
    std::stringstream smali;
    bool found = false;
    
    for (const auto& cls : parser.classes()) {
        if (parser.get_class_name(cls.class_idx) == class_name) {
            found = true;
            
            // 输出类声明
            smali << ".class public " << class_name << "\n";
            smali << ".super Ljava/lang/Object;\n\n";
            
            // 获取该类的所有方法
            auto methods = parser.get_methods();
            for (const auto& m : methods) {
                if (m.class_name == class_name) {
                    dex::CodeItem code;
                    if (parser.get_method_code(class_name, m.method_name, code)) {
                        auto insns = disasm.disassemble_method(code.insns.data(), code.insns.size());
                        
                        smali << ".method public " << m.method_name << m.prototype << "\n";
                        smali << "    .registers " << code.registers_size << "\n";
                        smali << disasm.to_smali(insns);
                        smali << ".end method\n\n";
                    }
                }
            }
            break;
        }
    }
    
    if (!found) {
        json error = {{"error", "Class not found: " + class_name}};
        return string_to_jstring(env, error.dump());
    }
    
    json result = {
        {"className", class_name},
        {"smali", smali.str()}
    };
    
    return string_to_jstring(env, result.dump());
}

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_getMethodSmali(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                     jstring className, jstring methodName,
                                                     jstring methodSignature) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string class_name = jstring_to_string(env, className);
    std::string method_name = jstring_to_string(env, methodName);
    std::string method_sig = jstring_to_string(env, methodSignature);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    dex::CodeItem code;
    if (!parser.get_method_code(class_name, method_name, code)) {
        json error = {{"error", "Method not found or has no code"}};
        return string_to_jstring(env, error.dump());
    }
    
    dex::SmaliDisassembler disasm;
    disasm.set_strings(parser.strings());
    disasm.set_types(parser.types());
    disasm.set_methods(parser.get_method_signatures());
    disasm.set_fields(parser.get_field_signatures());
    
    auto insns = disasm.disassemble_method(code.insns.data(), code.insns.size());
    std::string smali_code = disasm.to_smali(insns);
    
    json result = {
        {"className", class_name},
        {"methodName", method_name},
        {"registers", code.registers_size},
        {"smali", smali_code}
    };
    
    return string_to_jstring(env, result.dump());
}

// ==================== Smali 转 Java ====================

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_smaliToJava(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                  jstring className) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string class_name = jstring_to_string(env, className);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    // 先获取类的 Smali 代码
    dex::SmaliDisassembler disasm;
    disasm.set_strings(parser.strings());
    disasm.set_types(parser.types());
    disasm.set_methods(parser.get_method_signatures());
    disasm.set_fields(parser.get_field_signatures());
    
    std::stringstream smali;
    bool found = false;
    
    for (const auto& cls : parser.classes()) {
        if (parser.get_class_name(cls.class_idx) == class_name) {
            found = true;
            smali << ".class public " << class_name << "\n";
            smali << ".super Ljava/lang/Object;\n\n";
            
            auto methods = parser.get_methods();
            for (const auto& m : methods) {
                if (m.class_name == class_name) {
                    dex::CodeItem code;
                    if (parser.get_method_code(class_name, m.method_name, code)) {
                        auto insns = disasm.disassemble_method(code.insns.data(), code.insns.size());
                        smali << ".method public " << m.method_name << m.prototype << "\n";
                        smali << "    .registers " << code.registers_size << "\n";
                        smali << disasm.to_smali(insns);
                        smali << ".end method\n\n";
                    }
                }
            }
            break;
        }
    }
    
    if (!found) {
        json error = {{"error", "Class not found: " + class_name}};
        return string_to_jstring(env, error.dump());
    }
    
    // 转换为 Java 伪代码
    dex::SmaliToJava converter;
    std::string java_code = converter.convert(smali.str());
    
    if (java_code.empty()) {
        json error = {{"error", "Failed to convert class: " + class_name}};
        return string_to_jstring(env, error.dump());
    }
    
    json result = {
        {"className", class_name},
        {"java", java_code}
    };
    
    return string_to_jstring(env, result.dump());
}

// ==================== DEX 修改操作 ====================

JNIEXPORT jbyteArray JNICALL
Java_com_aetherlink_dexeditor_CppDex_modifyClass(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                  jstring className, jstring newSmali) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string class_name = jstring_to_string(env, className);
    std::string smali_code = jstring_to_string(env, newSmali);
    
    dex::DexBuilder builder;
    if (!builder.load(data)) {
        LOGE("Failed to load DEX for modification");
        return nullptr;
    }
    
    // TODO: 实现类修改逻辑
    // 这需要解析 smali_code 并修改 builder
    
    auto result = builder.build();
    if (result.empty()) {
        LOGE("Failed to build modified DEX");
        return nullptr;
    }
    
    return vector_to_jbyteArray(env, result);
}

JNIEXPORT jbyteArray JNICALL
Java_com_aetherlink_dexeditor_CppDex_addClass(JNIEnv* env, jclass, jbyteArray dexBytes,
                                               jstring newSmali) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string smali_code = jstring_to_string(env, newSmali);
    
    dex::DexBuilder builder;
    if (!builder.load(data)) {
        LOGE("Failed to load DEX");
        return nullptr;
    }
    
    // TODO: 解析 smali_code 并添加类
    
    auto result = builder.build();
    if (result.empty()) {
        return nullptr;
    }
    
    return vector_to_jbyteArray(env, result);
}

JNIEXPORT jbyteArray JNICALL
Java_com_aetherlink_dexeditor_CppDex_deleteClass(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                  jstring className) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string class_name = jstring_to_string(env, className);
    
    // TODO: 实现删除类逻辑
    
    return nullptr;
}

// ==================== 方法级操作 ====================

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_listMethods(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                  jstring className) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string class_name = jstring_to_string(env, className);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    json method_list = json::array();
    auto methods = parser.get_methods();
    
    for (const auto& m : methods) {
        if (m.class_name == class_name) {
            method_list.push_back({
                {"name", m.method_name},
                {"prototype", m.prototype},
                {"accessFlags", m.access_flags}
            });
        }
    }
    
    json result = {
        {"className", class_name},
        {"methods", method_list},
        {"count", method_list.size()}
    };
    
    return string_to_jstring(env, result.dump());
}

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_listFields(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                 jstring className) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string class_name = jstring_to_string(env, className);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    json field_list = json::array();
    auto fields = parser.get_fields();
    
    for (const auto& f : fields) {
        if (f.class_name == class_name) {
            field_list.push_back({
                {"name", f.field_name},
                {"type", f.type_name},
                {"accessFlags", f.access_flags}
            });
        }
    }
    
    json result = {
        {"className", class_name},
        {"fields", field_list},
        {"count", field_list.size()}
    };
    
    return string_to_jstring(env, result.dump());
}

// ==================== 字符串操作 ====================

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_listStrings(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                  jstring filter, jint limit) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string filter_str = jstring_to_string(env, filter);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    json string_list = json::array();
    const auto& strings = parser.strings();
    int count = 0;
    int matched = 0;
    
    for (const auto& s : strings) {
        if (!filter_str.empty() && s.find(filter_str) == std::string::npos) {
            continue;
        }
        matched++;
        if (count < limit) {
            string_list.push_back(s);
            count++;
        }
    }
    
    json result = {
        {"strings", string_list},
        {"shown", string_list.size()},
        {"matched", matched},
        {"total", strings.size()}
    };
    
    return string_to_jstring(env, result.dump());
}

// ==================== 交叉引用分析 ====================

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_findMethodXrefs(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                      jstring className, jstring methodName) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string class_name = jstring_to_string(env, className);
    std::string method_name = jstring_to_string(env, methodName);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    auto xrefs = parser.find_method_xrefs(class_name, method_name);
    
    json xref_list = json::array();
    for (const auto& xref : xrefs) {
        xref_list.push_back({
            {"callerClass", xref.caller_class},
            {"callerMethod", xref.caller_method},
            {"offset", xref.offset}
        });
    }
    
    json result = {
        {"className", class_name},
        {"methodName", method_name},
        {"xrefs", xref_list},
        {"count", xref_list.size()}
    };
    
    return string_to_jstring(env, result.dump());
}

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_findFieldXrefs(JNIEnv* env, jclass, jbyteArray dexBytes,
                                                     jstring className, jstring fieldName) {
    auto data = jbyteArray_to_vector(env, dexBytes);
    std::string class_name = jstring_to_string(env, className);
    std::string field_name = jstring_to_string(env, fieldName);
    
    dex::DexParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse DEX"}};
        return string_to_jstring(env, error.dump());
    }
    
    auto xrefs = parser.find_field_xrefs(class_name, field_name);
    
    json xref_list = json::array();
    for (const auto& xref : xrefs) {
        xref_list.push_back({
            {"callerClass", xref.caller_class},
            {"callerMethod", xref.caller_method},
            {"offset", xref.offset}
        });
    }
    
    json result = {
        {"className", class_name},
        {"fieldName", field_name},
        {"xrefs", xref_list},
        {"count", xref_list.size()}
    };
    
    return string_to_jstring(env, result.dump());
}

// ==================== Smali 编译 ====================

JNIEXPORT jbyteArray JNICALL
Java_com_aetherlink_dexeditor_CppDex_smaliToDex(JNIEnv* env, jclass, jstring smaliCode) {
    std::string smali = jstring_to_string(env, smaliCode);
    
    // 使用 DexBuilder 创建 DEX
    dex::DexBuilder builder;
    
    // TODO: 解析 smali 代码并构建 DEX
    // 这需要完整的 Smali 解析器实现
    
    auto result = builder.build();
    if (result.empty()) {
        LOGE("Failed to build DEX from Smali");
        return nullptr;
    }
    
    return vector_to_jbyteArray(env, result);
}

// ==================== AXML 解析 ====================

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_parseAxml(JNIEnv* env, jclass, jbyteArray axmlBytes) {
    auto data = jbyteArray_to_vector(env, axmlBytes);
    
    axml::AxmlParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse AXML"}};
        return string_to_jstring(env, error.dump());
    }
    
    json result = {
        {"packageName", parser.get_package_name()},
        {"versionName", parser.get_version_name()},
        {"versionCode", parser.get_version_code()},
        {"minSdk", parser.get_min_sdk()},
        {"targetSdk", parser.get_target_sdk()},
        {"permissions", parser.get_permissions()},
        {"activities", parser.get_activities()},
        {"services", parser.get_services()},
        {"xml", parser.to_xml()}
    };
    
    return string_to_jstring(env, result.dump());
}

JNIEXPORT jbyteArray JNICALL
Java_com_aetherlink_dexeditor_CppDex_editManifest(JNIEnv* env, jclass, jbyteArray axmlBytes,
                                                   jstring action, jstring value) {
    auto data = jbyteArray_to_vector(env, axmlBytes);
    std::string action_str = jstring_to_string(env, action);
    std::string value_str = jstring_to_string(env, value);
    
    axml::AxmlEditor editor;
    if (!editor.load(data)) {
        LOGE("Failed to load AXML for editing");
        return nullptr;
    }
    
    bool success = false;
    if (action_str == "set_package") {
        success = editor.set_package_name(value_str);
    } else if (action_str == "set_version_name") {
        success = editor.set_version_name(value_str);
    } else if (action_str == "set_version_code") {
        success = editor.set_version_code(std::stoi(value_str));
    } else if (action_str == "set_min_sdk") {
        success = editor.set_min_sdk(std::stoi(value_str));
    } else if (action_str == "set_target_sdk") {
        success = editor.set_target_sdk(std::stoi(value_str));
    } else {
        LOGE("Unknown action: %s", action_str.c_str());
        return nullptr;
    }
    
    if (!success) {
        LOGE("Failed to execute action: %s", action_str.c_str());
        return nullptr;
    }
    
    auto result = editor.save();
    if (result.empty()) {
        LOGE("Failed to save modified AXML");
        return nullptr;
    }
    
    return vector_to_jbyteArray(env, result);
}

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_searchXml(JNIEnv* env, jclass, jbyteArray axmlBytes,
                                                jstring attrName, jstring value, jint limit) {
    auto data = jbyteArray_to_vector(env, axmlBytes);
    std::string attr_name = jstring_to_string(env, attrName);
    std::string value_str = jstring_to_string(env, value);
    
    axml::AxmlEditor editor;
    if (!editor.load(data)) {
        json error = {{"error", "Failed to load AXML"}};
        return string_to_jstring(env, error.dump());
    }
    
    std::vector<axml::SearchResult> results;
    if (!attr_name.empty()) {
        results = editor.search_by_attribute(attr_name, value_str);
    } else if (!value_str.empty()) {
        results = editor.search_by_value(value_str);
    }
    
    json result_list = json::array();
    int count = 0;
    for (const auto& r : results) {
        if (count >= limit) break;
        result_list.push_back({
            {"elementPath", r.element_path},
            {"elementName", r.element_name},
            {"attributeName", r.attribute_name},
            {"attributeValue", r.attribute_value},
            {"elementIndex", r.element_index}
        });
        count++;
    }
    
    json result = {
        {"results", result_list},
        {"count", result_list.size()}
    };
    
    return string_to_jstring(env, result.dump());
}

// ==================== ARSC 解析 ====================

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_parseArsc(JNIEnv* env, jclass, jbyteArray arscBytes) {
    auto data = jbyteArray_to_vector(env, arscBytes);
    
    arsc::ArscParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse ARSC"}};
        return string_to_jstring(env, error.dump());
    }
    
    json result = {
        {"packageName", parser.package_name()},
        {"stringCount", parser.strings().size()},
        {"resourceCount", parser.resources().size()},
        {"info", parser.get_info()}
    };
    
    return string_to_jstring(env, result.dump());
}

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_searchArscStrings(JNIEnv* env, jclass, jbyteArray arscBytes,
                                                        jstring pattern, jint limit) {
    auto data = jbyteArray_to_vector(env, arscBytes);
    std::string pattern_str = jstring_to_string(env, pattern);
    
    arsc::ArscParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse ARSC"}};
        return string_to_jstring(env, error.dump());
    }
    
    auto results = parser.search_strings(pattern_str);
    
    json result_list = json::array();
    int count = 0;
    for (const auto& r : results) {
        if (count >= limit) break;
        result_list.push_back({
            {"index", r.index},
            {"value", r.value}
        });
        count++;
    }
    
    json result = {
        {"pattern", pattern_str},
        {"results", result_list},
        {"count", result_list.size()}
    };
    
    return string_to_jstring(env, result.dump());
}

JNIEXPORT jstring JNICALL
Java_com_aetherlink_dexeditor_CppDex_searchArscResources(JNIEnv* env, jclass, jbyteArray arscBytes,
                                                          jstring pattern, jstring type, jint limit) {
    auto data = jbyteArray_to_vector(env, arscBytes);
    std::string pattern_str = jstring_to_string(env, pattern);
    std::string type_str = jstring_to_string(env, type);
    
    arsc::ArscParser parser;
    if (!parser.parse(data)) {
        json error = {{"error", "Failed to parse ARSC"}};
        return string_to_jstring(env, error.dump());
    }
    
    auto results = parser.search_resources(pattern_str, type_str);
    
    json result_list = json::array();
    int count = 0;
    for (const auto& r : results) {
        if (count >= limit) break;
        result_list.push_back({
            {"id", r.id},
            {"name", r.name},
            {"type", r.type},
            {"value", r.value},
            {"package", r.package}
        });
        count++;
    }
    
    json result = {
        {"pattern", pattern_str},
        {"type", type_str},
        {"results", result_list},
        {"count", result_list.size()}
    };
    
    return string_to_jstring(env, result.dump());
}

} // extern "C"
