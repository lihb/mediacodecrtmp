#include <jni.h>

#include "com_example_mediacodecrtmp_RtmpNative.h"

#include <stdio.h>
#include <librtmp/rtmp_sys.h>
#include <librtmp/log.h>
#include <pthread.h>
#include <stdlib.h>

/*for android logs*/
#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif


#define LOG_TAG "android-mediacodec-rtmp-lihb-test"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);

static JavaVM      *gJavaVM;
static jclass      gJavaClass;
static jmethodID   gMethodID;
//static jbyteArray  gArray;

double duration;
int nRead;
//is live stream ?
int bLiveStream;
int bufsize;
char *buf;
long countbufsize;
RTMP *rtmp;

int InitReceiveSockets(){
    #ifdef WIN32
        WORD version;
        WSADATA wsaData;
        version = MAKEWORD(1, 1);
        return (WSAStartup(version, &wsaData) == 0);
    #endif
  }

void CleanuptReceiveSockets(){
    #ifdef WIN32
        WSACleanup();
    #endif
}
/*
 * Class:     com_lihb_MediaCodecRtmpDemo_MediaCodecUtil
 * Method:    naTest
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_example_mediacodecrtmp_RtmpNative_naTest
  (JNIEnv *pEnv, jobject pObj){

	 //全局化变量
    (*pEnv)->GetJavaVM(pEnv, &gJavaVM);
    jclass clazz = (*pEnv)->GetObjectClass(pEnv,pObj);
    gJavaClass = (jclass)(*pEnv)->NewGlobalRef(pEnv,clazz);

    gMethodID = (*pEnv)->GetStaticMethodID(pEnv,gJavaClass,"offer","([B)Z");


    InitReceiveSockets();
	LOGI("enter naTest method()");

	duration=-1;
	//is live stream ?
	bLiveStream=1;


	bufsize=640*480*3/2;

	buf=(char*)malloc(bufsize);
	memset(buf,0,bufsize);
	countbufsize=0;

//    jbyteArray jarray = (*pEnv)->NewByteArray(pEnv, bufsize);
//	gArray = (*pEnv)->NewGlobalRef(pEnv,jarray);

	/* set log level */
	//RTMP_LogLevel loglvl=RTMP_LOGDEBUG;
	//RTMP_LogSetLevel(loglvl);

	rtmp=RTMP_Alloc();
	RTMP_Init(rtmp);
	//set connection timeout,default 30s
	rtmp->Link.timeout=10;
	// HKS's live URL
//  if(!RTMP_SetupURL(rtmp,"rtmp://183.61.143.98/flvplayback/mp4:panvideo1.mp4"))
	if(!RTMP_SetupURL(rtmp,"rtmp://live.hkstv.hk.lxdns.com/live/hks"))
//	if(!RTMP_SetupURL(rtmp,"rtmp://183.60.140.6/ent/91590716_91590716_10057"))
	{
		RTMP_Log(RTMP_LOGERROR,"SetupURL Err\n");
		RTMP_Free(rtmp);
		CleanuptReceiveSockets();
		LOGI("SetupURL Err\n");
		return ;
	}
	if (bLiveStream){
		rtmp->Link.lFlags|=RTMP_LF_LIVE;
	}

	//1hour
	RTMP_SetBufferMS(rtmp, 3600*1000);

	if(!RTMP_Connect(rtmp,NULL)){
		RTMP_Free(rtmp);
		CleanuptReceiveSockets();
		LOGI("Connect Err\n");
		return ;
	}

	if(!RTMP_ConnectStream(rtmp,0)){
		RTMP_Close(rtmp);
		RTMP_Free(rtmp);
		CleanuptReceiveSockets();
		LOGI("ConnectStream Errorrrrrr\n");
		return ;
	}

  	pthread_t decodeThread;
    LOGI("naPlay() current thread id = %lu", pthread_self());
    return pthread_create(&decodeThread, NULL, parseRtmpData, NULL);
}

static void* parseRtmpData(void *arg){
    JNIEnv    *threadEnv;
    // 注册线程
    int status = (*gJavaVM)->AttachCurrentThread(gJavaVM, &threadEnv, NULL);
    LOGI("decodeVideo() --attach thread, status = %d" , status);


	while(nRead=RTMP_Read(rtmp,buf,bufsize)){
	      if((buf[0] == 8) || (buf[0] == 70 && buf[1] == 76 && buf[2] == 86)|| strlen(buf) == 0){
	          LOGI("aac data or file header data . should ignore.....");
	          continue;
	      }else if((buf[11] == 23 || buf[11]  == 39) && (buf[12] == 1)){
	          LOGI("avc data, procces it.....");
              jbyte *by = (jbyte*)buf;
              jbyteArray jarray = (*threadEnv)->NewByteArray(threadEnv, bufsize);
              (*threadEnv)->SetByteArrayRegion(threadEnv, jarray, 0, bufsize, by);
               //回调java中的方法
    //		  (*threadEnv)->CallVoidMethod(threadEnv, obj, methodID, jarray);
              (*threadEnv)->CallStaticBooleanMethod(threadEnv, gJavaClass, gMethodID, jarray);

              countbufsize+=nRead;
              RTMP_LogPrintf("Receive: %5dByte, Total: %5.2fkB\n",nRead,countbufsize*1.0/1024);
              LOGI("Receive: %5dByte, Total: %5.2fkB\n",nRead,countbufsize*1.0/1024);
              (*threadEnv)->DeleteLocalRef(threadEnv, jarray);
	      }
	      memset(buf,0,bufsize);

	      usleep(68000);

	}

	if(buf){
		free(buf);
	}

	if(rtmp){
		RTMP_Close(rtmp);
		RTMP_Free(rtmp);
		CleanuptReceiveSockets();
		rtmp=NULL;
	}
	 // 销毁全局对象
    (*threadEnv)->DeleteGlobalRef(threadEnv, gJavaClass);
    (*threadEnv)->DeleteGlobalRef(threadEnv, gMethodID);
//    (*threadEnv)->DeleteGlobalRef(threadEnv, gArray);
    //释放当前线程
    (*gJavaVM)->DetachCurrentThread(gJavaVM);
    pthread_exit(0);
    LOGI("thread stopdddd....");

}

#ifdef __cplusplus
}
#endif
