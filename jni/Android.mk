LOCAL_PATH := $(call my-dir)


include $(CLEAR_VARS)
LOCAL_MODULE :=  librtmp-0-prebuilt
LOCAL_SRC_FILES := prebuilt/librtmp-0.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := rtmp_jni
LOCAL_SRC_FILES := com_example_mediacodecrtmp_RtmpNative.c

LOCAL_LDLIBS := -llog -ljnigraphics -lz -landroid -lGLESv2
LOCAL_LDLIBS    += -lOpenSLES

LOCAL_SHARED_LIBRARIES := librtmp-0-prebuilt

include $(BUILD_SHARED_LIBRARY)