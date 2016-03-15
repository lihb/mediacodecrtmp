#include <jni.h>

#include "com_example_mediacodecrtmp_RtmpNative.h"

#include <stdio.h>
#include <librtmp/rtmp_sys.h>
#include <librtmp/log.h>
#include <pthread.h>
#include <stdlib.h>
#include <stdint.h>

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
//static jmethodID   gAudioMethodID;
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
 * Class:     Java_com_example_mediacodecrtmp_RtmpNative_naTest
 * Method:    naTest
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_example_mediacodecrtmp_RtmpNative_naTest
  (JNIEnv *pEnv, jobject pObj){

	 //全局化变量
    (*pEnv)->GetJavaVM(pEnv, &gJavaVM);
    jclass clazz = (*pEnv)->GetObjectClass(pEnv,pObj);
    gJavaClass = (jclass)(*pEnv)->NewGlobalRef(pEnv,clazz);

    gMethodID = (*pEnv)->GetStaticMethodID(pEnv,gJavaClass,"offerAvcData","([B)Z");
//    gAudioMethodID = (*pEnv)->GetStaticMethodID(pEnv,gJavaClass,"offerAudioData","([B)Z");


    InitReceiveSockets();
	LOGI("enter naTest method()");

	duration=-1;
	//is live stream ?
	bLiveStream=1;


	bufsize=2000*1000*3/2;

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
//	if(!RTMP_SetupURL(rtmp,"rtmp://live.hkstv.hk.lxdns.com/live/hks"))
	if(!RTMP_SetupURL(rtmp,"rtmp://183.60.140.6/ent/91590716_91590716_10057"))
//	if(!RTMP_SetupURL(rtmp,"rtmp://183.61.143.98/flvplayback/test"))
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
/**
*   8位16进制数据
*/
double hexStr2double(const unsigned char* hex, const unsigned int length) {

    double ret = 0;
    char hexstr[length * 2];
    memset(hexstr, 0, sizeof(hexstr));
	unsigned int i;
    for( i= 0; i < length; i++) {
        sprintf(hexstr + i * 2, "%02x", hex[i]);
    }
    LOGI("hexstr= %s", hexstr);
    sscanf(hexstr, "%llx", (unsigned long long*)&ret);

    return ret;
}

static void* parseRtmpData(void *arg){
    JNIEnv    *threadEnv;
    // 注册线程
    int status = (*gJavaVM)->AttachCurrentThread(gJavaVM, &threadEnv, NULL);
    LOGI("decodeVideo() --attach thread, status = %d" , status);


	while(nRead=RTMP_Read(rtmp,buf,bufsize)){
	       jbyte *by = (jbyte*)buf;
           jbyteArray jarray = (*threadEnv)->NewByteArray(threadEnv, bufsize);
           (*threadEnv)->SetByteArrayRegion(threadEnv, jarray, 0, bufsize, by);
             //回调java中的方法
           /*if(buf[0] == 0x08){ // 音频处理
               (*threadEnv)->CallStaticBooleanMethod(threadEnv, gJavaClass, gAudioMethodID, jarray);
           }else{
               (*threadEnv)->CallStaticBooleanMethod(threadEnv, gJavaClass, gMethodID, jarray);
           }*/
           (*threadEnv)->CallStaticBooleanMethod(threadEnv, gJavaClass, gMethodID, jarray);

           countbufsize+=nRead;
           RTMP_LogPrintf("Receive: %5dByte, Total: %5.2fkB\n",nRead,countbufsize*1.0/1024);
           LOGI("Receive: %5dByte, Total: %5.2fkB\n",nRead,countbufsize*1.0/1024);
           (*threadEnv)->DeleteLocalRef(threadEnv, jarray);
         /* double framerate = 0.0;
	      if(buf[0] == 0x46 && buf[1] == 0x4c && buf[2] == 0x56 && buf[13] == 0x12){
              LOGI("in if buf[13] == 0x12\n");
              int i = 0;
              for(i = 13; i < bufsize; i++){
                  if(buf[i] == 0x66 && buf[i+1] == 0x72 && buf[i+2] == 0x61 && buf[i+3] == 0x6d && buf[i+4] == 0x65 && buf[i+5] == 0x72 && buf[i+6] == 0x61
                      && buf[i+7] == 0x74 && buf[i+8] == 0x65 && buf[i+9] == 0x00){ // framerate
                      LOGI("in if framerate\n");

                      char *length = (char*)malloc(8);
                      memcpy(length, buf+i+10, 8);
                      framerate = hexStr2double(length, 8);
                      LOGI("framerate =  %lf\n", framerate);
                  }
              }

          }else if((buf[0] == 0x09) && (buf[11] == 0x17 || buf[11]  == 0x27) && (buf[12] == 0x01)){
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
	      }*/

	      memset(buf,0,bufsize);
//	      usleep(10 *1000);

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
//    (*threadEnv)->DeleteGlobalRef(threadEnv, gAudioMethodID);
//    (*threadEnv)->DeleteGlobalRef(threadEnv, gArray);
    //释放当前线程
    (*gJavaVM)->DetachCurrentThread(gJavaVM);
    pthread_exit(0);
    LOGI("thread stopdddd....");

}

#ifdef __cplusplus
}
#endif
