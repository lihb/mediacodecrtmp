package com.asha.vrlib;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.Surface;

import javax.microedition.khronos.opengles.GL10;

import static com.asha.vrlib.common.GLUtil.glCheck;


/**
 * Created by hzqiujiadi on 16/1/25.
 * hzqiujiadi ashqalcn@gmail.com
 *
 * copied from surfaceTexture
 * Created by nitro888 on 15. 4. 5..
 * https://github.com/Nitro888/NitroAction360
 */
public class MD360Surface {
    public static final int SURFACE_TEXTURE_EMPTY = 0;

    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;
    private int mGlSurfaceTexture;
    private int mWidth;
    private int mHeight;
    private IOnSurfaceReadyListener mOnSurfaceReadyListener;

    public MD360Surface(IOnSurfaceReadyListener onSurfaceReadyListener) {
        this.mOnSurfaceReadyListener = onSurfaceReadyListener;
    }

    public void resize(int width,int height){
        boolean changed = false;
        if (mWidth == width && mHeight == height) changed = true;
        mWidth = width;
        mHeight = height;

        // resize the texture
        if (changed && mSurfaceTexture != null)
            mSurfaceTexture.setDefaultBufferSize(mWidth,mHeight);

    }

    public void createSurface() {
        if (mSurface != null) return;

        releaseSurface();
        mGlSurfaceTexture = createTexture();
        if (mGlSurfaceTexture != SURFACE_TEXTURE_EMPTY) {
            //attach the texture to a surface.
            //It's a clue class for rendering an android view to gl level
            mSurfaceTexture = new SurfaceTexture(mGlSurfaceTexture);
            mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);
            mSurface = new Surface(mSurfaceTexture);
            if (mOnSurfaceReadyListener != null)
                mOnSurfaceReadyListener.onSurfaceReady(mSurface);
        }
    }

    private void releaseSurface() {
        if (mSurface != null) {
            mSurface.release();
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        mSurface = null;
        mSurfaceTexture = null;
    }

    private int createTexture() {
        mGlSurfaceTexture = SURFACE_TEXTURE_EMPTY;
        int[] textures = new int[1];

        // Generate the texture to where android view will be rendered
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        glCheck("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        glCheck("Texture bind");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return textures[0];
    }

    public void onDrawFrame() {
        if(mGlSurfaceTexture == SURFACE_TEXTURE_EMPTY)
            return;

        synchronized (this){
            mSurfaceTexture.updateTexImage();
        }
    }

    public interface IOnSurfaceReadyListener{
        void onSurfaceReady(Surface surface);
    }
}
