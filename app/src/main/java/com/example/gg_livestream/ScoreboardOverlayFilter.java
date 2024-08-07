package com.example.gg_livestream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import com.pedro.encoder.input.gl.render.filters.BaseFilterRender;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ScoreboardOverlayFilter extends BaseFilterRender {

    private Bitmap scoreboardBitmap;
    private String team1Name = "Team 1";
    private String team2Name = "Team 2";
    private Bitmap team1Logo;
    private Bitmap team2Logo;
    private int team1Score = 0;
    private int team2Score = 0;

    private Context context;
    private int[] textures = new int[1];
    private FloatBuffer textureBuffer;
    private FloatBuffer vertexBuffer;
    private int program;
    private int positionHandle;
    private int textureHandle;
    private int textureCoordHandle;

    public ScoreboardOverlayFilter(Context context) {
        this.context = context;
        Log.d("ScoreboardOverlayFilter", "Constructor apelat");
    }

    public void updateScoreboard(String team1Name, String team2Name, Bitmap team1Logo, Bitmap team2Logo, int team1Score, int team2Score) {
        this.team1Name = team1Name != null ? team1Name : "Team 1";
        this.team2Name = team2Name != null ? team2Name : "Team 2";
        this.team1Logo = team1Logo;
        this.team2Logo = team2Logo;
        this.team1Score = team1Score;
        this.team2Score = team2Score;
        Log.d("ScoreboardOverlayFilter", "Actualizare tabelă de scor: " +
                "team1Name=" + this.team1Name +
                ", team2Name=" + this.team2Name +
                ", team1Score=" + this.team1Score +
                ", team2Score=" + this.team2Score);
        scoreboardBitmap = createScoreboardBitmap();
        loadTexture();
    }

    private Bitmap createScoreboardBitmap() {
        Log.d("ScoreboardOverlayFilter", "Creare bitmap pentru tabelă de scor");
        int width = 800;
        int height = 200;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        paint.setAntiAlias(true);
        paint.setTypeface(ResourcesCompat.getFont(context, R.font.montserrat_light));

        canvas.drawColor(Color.TRANSPARENT);

        canvas.drawText(team1Name, 50, 50, paint);
        canvas.drawText("VS", width / 2 - 20, 50, paint);
        canvas.drawText(team2Name, width - 200, 50, paint);

        canvas.drawText(String.valueOf(team1Score), 50, 150, paint);
        canvas.drawText(String.valueOf(team2Score), width - 200, 150, paint);

        if (team1Logo != null) {
            canvas.drawBitmap(team1Logo, null, new Rect(50, 100, 150, 200), null);
        }
        if (team2Logo != null) {
            canvas.drawBitmap(team2Logo, null, new Rect(width - 200, 100, width - 100, 200), null);
        }
        Log.d("ScoreboardOverlayFilter", "Bitmap creat cu succes");
        return bitmap;
    }

    private void loadTexture() {
        if (scoreboardBitmap != null) {
            Log.d("ScoreboardOverlayFilter", "Încărcare textură");
            if (textures[0] != 0) {
                GLES20.glDeleteTextures(1, textures, 0);
            }
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, scoreboardBitmap, 0);
            Log.d("ScoreboardOverlayFilter", "Textură încărcată cu succes");
        } else {
            Log.e("ScoreboardOverlayFilter", "Eroare: Bitmap-ul este null");
        }
    }

    @Override
    protected void initGlFilter(Context context) {
        String vertexShaderCode =
                "attribute vec4 aPosition;" +
                        "attribute vec2 aTextureCoord;" +
                        "varying vec2 vTextureCoord;" +
                        "void main() {" +
                        "  gl_Position = aPosition;" +
                        "  vTextureCoord = aTextureCoord;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "varying vec2 vTextureCoord;" +
                        "uniform sampler2D sTexture;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(sTexture, vTextureCoord);" +
                        "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
        textureHandle = GLES20.glGetUniformLocation(program, "sTexture");

        float[] textureVertices = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
        };
        float[] vertices = {
                -0.8f, -0.8f,
                0.8f, -0.8f,
                -0.8f, -0.5f,
                0.8f, -0.5f,
        };

        textureBuffer = ByteBuffer.allocateDirect(textureVertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(textureVertices).position(0);

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        loadTexture();
    }

    @Override
    protected void drawFilter() {
        if (textures[0] != 0) {
            GLES20.glUseProgram(program);

            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            GLES20.glEnableVertexAttribArray(textureCoordHandle);
            GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glUniform1i(textureHandle, 0);

            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glDisable(GLES20.GL_BLEND);

            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);

            Log.d("ScoreboardOverlayFilter", "Draw filter completat cu succes");
        } else {
            Log.e("ScoreboardOverlayFilter", "Eroare: Textura nu este încărcată");
        }
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void release() {
        if (textures[0] != 0) {
            GLES20.glDeleteTextures(1, textures, 0);
        }
    }
}