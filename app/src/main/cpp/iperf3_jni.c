#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <netinet/in.h>
#include <stdio.h>
#include "iperf_config.h"
#include "iperf_api.h"
#include "iperf.h"

#define LOG_TAG "Iperf3JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM *jvm = NULL;
static jobject callback_obj = NULL;
static jmethodID on_progress_method = NULL;
static jmethodID on_complete_method = NULL;
static jmethodID on_error_method = NULL;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_com_iperf3client_jni_Iperf3Native_setEnvironmentVariable(JNIEnv *env, jobject thiz, 
                                                              jstring name, jstring value) {
    const char *nameStr = (*env)->GetStringUTFChars(env, name, 0);
    const char *valueStr = (*env)->GetStringUTFChars(env, value, 0);
    
    int result = setenv(nameStr, valueStr, 1);
    LOGI("Setting env %s=%s, result=%d", nameStr, valueStr, result);
    
    (*env)->ReleaseStringUTFChars(env, name, nameStr);
    (*env)->ReleaseStringUTFChars(env, value, valueStr);
    
    return result == 0 ? JNI_TRUE : JNI_FALSE;
}

// Callback function for iperf3 output
static void jni_iperf_reporter_callback(struct iperf_test *test) {
    if (!callback_obj || !jvm) return;
    
    JNIEnv *env;
    int attached = 0;
    
    if ((*jvm)->GetEnv(jvm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        if ((*jvm)->AttachCurrentThread(jvm, &env, NULL) != JNI_OK) {
            return;
        }
        attached = 1;
    }
    
    // Get current interval stats
    struct iperf_stream *sp = SLIST_FIRST(&test->streams);
    if (sp) {
        struct iperf_interval_results *irp = TAILQ_LAST(&sp->result->interval_results, irlisthead);
        if (irp) {
            double mbps = (double)(irp->bytes_transferred * 8) / (irp->interval_duration * 1e6);
            int retransmits = irp->interval_retrans;
            
            // Call Java callback
            (*env)->CallVoidMethod(env, callback_obj, on_progress_method, 
                                  (jdouble)mbps, (jint)retransmits);
        }
    }
    
    if (attached) {
        (*jvm)->DetachCurrentThread(jvm);
    }
}

JNIEXPORT jlong JNICALL
Java_com_iperf3client_jni_Iperf3Native_createTest(JNIEnv *env, jobject thiz) {
    // CRITICAL: Check and set TMPDIR before creating test
    char *cache_dir = getenv("TMPDIR");
    if (!cache_dir || strlen(cache_dir) == 0) {
        // If TMPDIR not set, we must fail early as Android blocks /data/local/tmp
        LOGE("TMPDIR not set! Cannot create test without writable temp directory");
        LOGE("Android apps cannot write to /data/local/tmp");
        return 0;
    }
    
    LOGI("TMPDIR is set to: %s", cache_dir);
    
    // Verify the directory exists and is writable
    if (access(cache_dir, F_OK) != 0) {
        LOGE("TMPDIR %s does not exist", cache_dir);
        // Try to create it
        if (mkdir(cache_dir, 0700) != 0 && errno != EEXIST) {
            LOGE("Failed to create TMPDIR %s: %s", cache_dir, strerror(errno));
            return 0;
        }
    }
    
    if (access(cache_dir, W_OK) != 0) {
        LOGE("TMPDIR %s is not writable: %s", cache_dir, strerror(errno));
        return 0;
    }
    
    struct iperf_test *test = iperf_new_test();
    if (!test) {
        LOGE("Failed to create iperf test");
        return 0;
    }
    
    // Set default parameters
    iperf_defaults(test);
    
    // Set as client mode
    iperf_set_test_role(test, 'c');
    
    // Set bind address to any (important for Android)
    iperf_set_test_bind_address(test, "0.0.0.0");
    
    // Set callback
    test->reporter_callback = jni_iperf_reporter_callback;
    
    // Set temp template for Android - MUST be set to avoid /data/local/tmp
    char template_path[256];
    snprintf(template_path, sizeof(template_path), "%s/iperf3.XXXXXX", cache_dir);
    LOGI("Setting template path: %s", template_path);
    iperf_set_test_template(test, template_path);
    
    return (jlong)(intptr_t)test;
}

JNIEXPORT void JNICALL
Java_com_iperf3client_jni_Iperf3Native_setTestParams(JNIEnv *env, jobject thiz, 
                                                     jlong testPtr, jstring host, 
                                                     jint port, jint duration,
                                                     jint streams, jboolean reverse,
                                                     jboolean udp) {
    struct iperf_test *test = (struct iperf_test *)(intptr_t)testPtr;
    if (!test) return;
    
    const char *hostname = (*env)->GetStringUTFChars(env, host, 0);
    
    LOGI("Setting test params: host=%s, port=%d, duration=%d, streams=%d, reverse=%d, udp=%d",
         hostname, port, duration, streams, reverse, udp);
    
    iperf_set_test_server_hostname(test, hostname);
    iperf_set_test_server_port(test, port);
    iperf_set_test_duration(test, duration);
    iperf_set_test_num_streams(test, streams);
    iperf_set_test_reverse(test, reverse ? 1 : 0);
    
    if (udp) {
        set_protocol(test, Pudp);
    } else {
        set_protocol(test, Ptcp);
    }
    
    // Set JSON output
    iperf_set_test_json_output(test, 1);
    
    (*env)->ReleaseStringUTFChars(env, host, hostname);
}

JNIEXPORT void JNICALL
Java_com_iperf3client_jni_Iperf3Native_setCallback(JNIEnv *env, jobject thiz, jobject callback) {
    // Store callback object globally
    if (callback_obj) {
        (*env)->DeleteGlobalRef(env, callback_obj);
    }
    callback_obj = (*env)->NewGlobalRef(env, callback);
    
    // Get callback methods
    jclass clazz = (*env)->GetObjectClass(env, callback);
    on_progress_method = (*env)->GetMethodID(env, clazz, "onProgress", "(DI)V");
    on_complete_method = (*env)->GetMethodID(env, clazz, "onComplete", "(Ljava/lang/String;)V");
    on_error_method = (*env)->GetMethodID(env, clazz, "onError", "(Ljava/lang/String;)V");
}

JNIEXPORT jint JNICALL
Java_com_iperf3client_jni_Iperf3Native_runClient(JNIEnv *env, jobject thiz, jlong testPtr) {
    struct iperf_test *test = (struct iperf_test *)(intptr_t)testPtr;
    if (!test) {
        LOGE("Invalid test pointer");
        return -1;
    }
    
    LOGI("Starting iperf3 client test");
    LOGI("Test role: %c", test->role);
    LOGI("Server host: %s", test->server_hostname);
    LOGI("Server port: %d", test->server_port);
    LOGI("Duration: %d", test->duration);
    LOGI("Streams: %d", test->num_streams);
    LOGI("Bind address: %s", test->bind_address ? test->bind_address : "NULL");
    LOGI("Template: %s", test->tmp_template ? test->tmp_template : "NULL");
    
    // Test socket creation capability
    int test_sock = socket(AF_INET, SOCK_STREAM, 0);
    if (test_sock < 0) {
        LOGE("Cannot create test socket: %s", strerror(errno));
    } else {
        LOGI("Test socket created successfully: %d", test_sock);
        close(test_sock);
    }
    
    int result = iperf_run_client(test);
    
    if (result < 0) {
        char *error = iperf_strerror(i_errno);
        LOGE("iperf3 error: %s", error);
        if (callback_obj && on_error_method) {
            jstring errorStr = (*env)->NewStringUTF(env, error);
            (*env)->CallVoidMethod(env, callback_obj, on_error_method, errorStr);
            (*env)->DeleteLocalRef(env, errorStr);
        }
    } else {
        LOGI("iperf3 test completed successfully");
        if (callback_obj && on_complete_method) {
            // Get JSON result
            char *json = iperf_get_test_json_output_string(test);
            if (json) {
                jstring jsonStr = (*env)->NewStringUTF(env, json);
                (*env)->CallVoidMethod(env, callback_obj, on_complete_method, jsonStr);
                (*env)->DeleteLocalRef(env, jsonStr);
            }
        }
    }
    
    return result;
}

JNIEXPORT void JNICALL
Java_com_iperf3client_jni_Iperf3Native_stopTest(JNIEnv *env, jobject thiz, jlong testPtr) {
    struct iperf_test *test = (struct iperf_test *)(intptr_t)testPtr;
    if (!test) return;
    
    LOGI("Stopping iperf3 test");
    test->done = 1;
}

JNIEXPORT void JNICALL
Java_com_iperf3client_jni_Iperf3Native_freeTest(JNIEnv *env, jobject thiz, jlong testPtr) {
    struct iperf_test *test = (struct iperf_test *)(intptr_t)testPtr;
    if (!test) return;
    
    LOGI("Freeing iperf3 test");
    iperf_free_test(test);
    
    if (callback_obj) {
        (*env)->DeleteGlobalRef(env, callback_obj);
        callback_obj = NULL;
    }
}