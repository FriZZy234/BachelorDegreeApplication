package com.example.gg_livestream;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.pedro.encoder.input.gl.render.filters.BaseFilterRender;
import com.pedro.encoder.utils.gl.GlUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BitmapOverlayFilterRender extends BaseFilterRender {

    private int[] textureId = new int[1];
    private Bitmap bitmap;
    private boolean loadBitmap = false;
    private FloatBuffer textureBuffer;
    private FloatBuffer vertexBuffer;
    private int program;

    public BitmapOverlayFilterRender(Bitmap bitmap) {
        this.bitmap = bitmap;
        textureBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(new float[]{
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
        }).flip();

        // Adjust vertex coordinates to display the bitmap in the top-left corner
        float bitmapWidthRatio = 0.25f; // width of the bitmap as a fraction of screen width
        float bitmapHeightRatio = bitmapWidthRatio * bitmap.getHeight() / bitmap.getWidth(); // maintain aspect ratio

        vertexBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(new float[]{
                -1.0f, 1.0f - 2 * bitmapHeightRatio, // top-left
                -1.0f + 2 * bitmapWidthRatio, 1.0f - 2 * bitmapHeightRatio, // top-right
                -1.0f, 1.0f, // bottom-left
                -1.0f + 2 * bitmapWidthRatio, 1.0f // bottom-right
        }).flip();
    }

    private void loadBitmapToTexture() {
        if (bitmap != null && !bitmap.isRecycled()) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }

    @Override
    protected void initGlFilter(Context context) {
        program = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        loadBitmapToTexture();
    }

    @Override
    protected void drawFilter() {
        if (loadBitmap) {
            loadBitmapToTexture();
            loadBitmap = false;
        }

        GLES20.glUseProgram(program);

        GLES20.glEnableVertexAttribArray(getAttribLocation(program, "aPosition"));
        GLES20.glEnableVertexAttribArray(getAttribLocation(program, "aTextureCoord"));

        GLES20.glVertexAttribPointer(getAttribLocation(program, "aPosition"), 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(getAttribLocation(program, "aTextureCoord"), 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glDisableVertexAttribArray(getAttribLocation(program, "aPosition"));
        GLES20.glDisableVertexAttribArray(getAttribLocation(program, "aTextureCoord"));
    }

    @Override
    public void release() {
        GLES20.glDeleteProgram(program);
        GLES20.glDeleteTextures(1, textureId, 0);
    }

    public void updateBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        loadBitmap = true;
    }

    private int getAttribLocation(int program, String name) {
        return GLES20.glGetAttribLocation(program, name);
    }

    private int getUniformLocation(int program, String name) {
        return GLES20.glGetUniformLocation(program, name);
    }

    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;" +
                    "attribute vec2 aTextureCoord;" +
                    "varying vec2 vTextureCoord;" +
                    "void main() {" +
                    "  gl_Position = aPosition;" +
                    "  vTextureCoord = aTextureCoord;" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform sampler2D uTexture;" +
                    "varying vec2 vTextureCoord;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(uTexture, vTextureCoord);" +
                    "}";
}
