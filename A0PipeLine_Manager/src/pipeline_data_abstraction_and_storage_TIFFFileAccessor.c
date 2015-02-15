#include <jni.h>
#include <stdio.h>
#include "pipeline_data_abstraction_and_storage_TIFFFileAccessor.h"
#include <fcntl.h>
#include <syslog.h>
#include <string.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <errno.h>


/*
 * Class:     pipeline_data_abstraction_and_storage_TIFFFileAccessor
 * Method:    turnOffCaching
 * Signature: (Ljava/io/FileDescriptor;)V
 */
JNIEXPORT jint JNICALL Java_pipeline_data_1abstraction_1and_1storage_TIFFFileAccessor_turnOffCaching
(JNIEnv *env, jobject thisObject, jobject fd){
    jclass clazz;
    jfieldID fid;
    JNIEnv e = *env;
    
    int intID;
    
    if (!(clazz = e->GetObjectClass(env,fd)) ||
        !(fid = e->GetFieldID(env,clazz,"fd","I"))){
        //syslog(0,"ERROR");
        return -1;
    }
    
    
    intID=e->GetIntField(env,fd,fid);
    
    struct stat buffer;
    int         status;
    status = fstat(intID, &buffer);
    
    /*
     char * stringBuffer=(char *) malloc (200);
     sprintf(stringBuffer,"size of file: %li",buffer.st_size);
     syslog(0,stringBuffer);*/
    
    if (fcntl(intID,F_NOCACHE,1)==-1) return errno;
    if (fcntl(intID,F_RDAHEAD,1)==-1) return errno;
    
    return intID;
};

/*
 * Class:     pipeline_data_abstraction_and_storage_TIFFFileAccessor
 * Method:    turnOnCaching
 * Signature: (Ljava/io/FileDescriptor;)V
 */
JNIEXPORT jint JNICALL Java_pipeline_data_1abstraction_1and_1storage_TIFFFileAccessor_turnOnCaching
(JNIEnv * env, jobject thisObject, jobject fd){
    jclass clazz;
    jfieldID fid;
    JNIEnv e = *env;
    
    syslog(0,"Turning on caching");
    
    int intID;
    
    if (!(clazz = e->GetObjectClass(env,fd)) ||
        !(fid = e->GetFieldID(env,clazz,"fd","I"))) return -1;
    
    
    intID=e->GetIntField(env,fd,fid);
    
    if (fcntl(intID,F_NOCACHE,0)==-1) return errno;
    
    return intID;
}

