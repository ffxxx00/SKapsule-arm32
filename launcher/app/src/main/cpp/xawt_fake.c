#include <jni.h>

// Cacio supplies AWT peers; these hooks only satisfy desktop AWT initialization.
#define INIT_IDS(type) \
    JNIEXPORT void JNICALL Java_##type##_initIDs(JNIEnv *env, jclass cls) {}

INIT_IDS(java_awt_AWTEvent)
INIT_IDS(java_awt_Button)
INIT_IDS(java_awt_Checkbox)
INIT_IDS(java_awt_Component)
INIT_IDS(java_awt_Container)
INIT_IDS(java_awt_Cursor)
INIT_IDS(java_awt_Dialog)
INIT_IDS(java_awt_Event)
INIT_IDS(java_awt_FileDialog)
INIT_IDS(java_awt_Frame)
INIT_IDS(java_awt_Insets)
INIT_IDS(java_awt_KeyboardFocusManager)
INIT_IDS(java_awt_Menu)
INIT_IDS(java_awt_MenuComponent)
INIT_IDS(java_awt_MenuItem)
INIT_IDS(java_awt_Scrollbar)
INIT_IDS(java_awt_ScrollPane)
INIT_IDS(java_awt_TextArea)
INIT_IDS(java_awt_TextField)
INIT_IDS(java_awt_TrayIcon)
INIT_IDS(java_awt_Window)
INIT_IDS(java_awt_event_InputEvent)
INIT_IDS(java_awt_event_KeyEvent)

JNIEXPORT void JNICALL Java_java_awt_Cursor_finalizeImpl(JNIEnv *env, jclass cls, jlong data) {}
JNIEXPORT void JNICALL Java_java_awt_AWTEvent_nativeSetSource(JNIEnv *env, jobject event, jobject source) {}
JNIEXPORT void JNICALL Java_sun_awt_SunToolkit_closeSplashScreen(JNIEnv *env, jclass cls) {}
JNIEXPORT jboolean JNICALL Java_sun_awt_UNIXToolkit_check_1gtk(JNIEnv *env, jclass cls, jint version) { return JNI_FALSE; }
JNIEXPORT jint JNICALL Java_sun_awt_UNIXToolkit_get_1gtk_1version(JNIEnv *env, jclass cls) { return 1; }
JNIEXPORT jboolean JNICALL Java_sun_awt_UNIXToolkit_gtkCheckVersionImpl(JNIEnv *env, jobject self, jint major, jint minor, jint micro) { return JNI_FALSE; }
JNIEXPORT jboolean JNICALL Java_sun_awt_UNIXToolkit_load_1gtk(JNIEnv *env, jclass cls, jint version, jboolean verbose) { return JNI_FALSE; }
JNIEXPORT jboolean JNICALL Java_sun_awt_UNIXToolkit_load_1gtk_1icon(JNIEnv *env, jobject self, jstring filename) { return JNI_FALSE; }
JNIEXPORT jboolean JNICALL Java_sun_awt_UNIXToolkit_load_1stock_1icon(JNIEnv *env, jobject self, jint widget, jstring id, jint size, jint direction, jstring detail) { return JNI_FALSE; }
JNIEXPORT void JNICALL Java_sun_awt_UNIXToolkit_nativeSync(JNIEnv *env, jobject self) {}
JNIEXPORT jboolean JNICALL Java_sun_awt_UNIXToolkit_unload_1gtk(JNIEnv *env, jclass cls) { return JNI_FALSE; }
