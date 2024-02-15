#include "at_syntaxerror_syntaxc_backend_x86_CPUID.h"

static inline void setInt(JNIEnv *env, jobject instance, jclass cls, const char *name, jint value) {
    jfieldID fid = (*env)->GetFieldID(env, cls, name, "I");
    (*env)->SetIntField(env, instance, fid, value);
}

/*
 * Class:     at_syntaxerror_cpuid4j_CPUID
 * Method:    cpuid2
 * Signature: (II)[I
 */
JNIEXPORT void JNICALL Java_at_syntaxerror_syntaxc_backend_x86_CPUID_cpuid2(JNIEnv *env, jobject instance, jint eax,
                                                                            jint ecx) {
    jint buf[4];

    __asm__ __volatile__("cpuid" : "=a"(buf[0]), "=b"(buf[1]), "=c"(buf[2]), "=d"(buf[3]) : "a"(eax), "c"(ecx));

    jclass cls = (*env)->GetObjectClass(env, instance);

    setInt(env, instance, cls, "eax", buf[0]);
    setInt(env, instance, cls, "ebx", buf[1]);
    setInt(env, instance, cls, "ecx", buf[2]);
    setInt(env, instance, cls, "edx", buf[3]);
}
