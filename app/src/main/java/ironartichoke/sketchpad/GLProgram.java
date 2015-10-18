package ironartichoke.sketchpad;

/**
 * A class that holds the OpenGL shader code and the OpenGL program pointer.
 */
final class GLProgram
{
    /** The pointer to the OpenGL program in memory. */
    public static int glProgram;

	/** The GLSL code for the vertex shader. */
    public static final String vertexShaderCode =
            "attribute vec4 vPosition;" +
		    "uniform mat4 uMVPMatrix;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "}";

	/** The GLSL code for the fragment shader. */
    public static final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";
}
