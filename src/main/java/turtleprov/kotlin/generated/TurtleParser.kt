// Generated from /home/jakub/Documents/Dev/RMLDevTools/provturtle/src/commonMain/grammar/Turtle.g4 by ANTLR 4.13.1
package be.uliege.RMLDevTools.parser.turtle.generated

import com.strumenta.antlrkotlin.runtime.JsName
import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.atn.*
import org.antlr.v4.kotlinruntime.atn.ATN.Companion.INVALID_ALT_NUMBER
import org.antlr.v4.kotlinruntime.dfa.*
import org.antlr.v4.kotlinruntime.misc.*
import org.antlr.v4.kotlinruntime.tree.*
import kotlin.jvm.JvmField

@Suppress(
    // This is required as we are using a custom JsName alias that is not recognized by the IDE.
    // No name clashes will happen tho.
    "JS_NAME_CLASH",
    "UNUSED_VARIABLE",
    "ClassName",
    "FunctionName",
    "LocalVariableName",
    "ConstPropertyName",
    "ConvertSecondaryConstructorToPrimary",
    "CanBeVal",
)
public open class TurtleParser(input: TokenStream) : Parser(input) {
    private companion object {
        init {
            RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.runtimeVersion)
        }

        private const val SERIALIZED_ATN: String =
            "\u0004\u0001\u0035\u00e5\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002\u0008\u0007\u0008\u0002\u0009\u0007\u0009\u0002\u000a\u0007\u000a\u0002\u000b\u0007\u000b\u0002\u000c\u0007\u000c\u0002\u000d\u0007\u000d\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b\u0001\u0000\u0005\u0000\u003a\u0008\u0000\u000a\u0000\u000c\u0000\u003d\u0009\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001\u0045\u0008\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002\u004b\u0008\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u0062\u0008\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u0066\u0008\u0007\u0003\u0007\u0068\u0008\u0007\u0001\u0008\u0001\u0008\u0001\u0008\u0001\u0008\u0001\u0008\u0001\u0008\u0003\u0008\u0070\u0008\u0008\u0005\u0008\u0072\u0008\u0008\u000a\u0008\u000c\u0008\u0075\u0009\u0008\u0001\u0009\u0001\u0009\u0001\u0009\u0001\u0009\u0001\u0009\u0001\u0009\u0005\u0009\u007d\u0008\u0009\u000a\u0009\u000c\u0009\u0080\u0009\u0009\u0001\u000a\u0001\u000a\u0003\u000a\u0084\u0008\u000a\u0001\u000b\u0001\u000b\u0001\u000b\u0003\u000b\u0089\u0008\u000b\u0001\u000c\u0001\u000c\u0001\u000c\u0001\u000c\u0001\u000c\u0001\u000c\u0001\u000c\u0003\u000c\u0092\u0008\u000c\u0001\u000d\u0001\u000d\u0001\u000d\u0003\u000d\u0097\u0008\u000d\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0005\u000f\u009f\u0008\u000f\u000a\u000f\u000c\u000f\u00a2\u0009\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0003\u0010\u00aa\u0008\u0010\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u00b3\u0008\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u00ba\u0008\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u00c1\u0008\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0003\u0016\u00c8\u0008\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0003\u0018\u00d2\u0008\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0003\u0019\u00d8\u0008\u0019\u0001\u001a\u0001\u001a\u0005\u001a\u00dc\u0008\u001a\u000a\u001a\u000c\u001a\u00df\u0009\u001a\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0000\u0000\u001c\u0000\u0002\u0004\u0006\u0008\u000a\u000c\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e\u0020\u0022\u0024\u0026\u0028\u002a\u002c\u002e\u0030\u0032\u0034\u0036\u0000\u0002\u0001\u0000\u0025\u0028\u0002\u0000\u0018\u0018\u001a\u001a\u00f1\u0000\u003b\u0001\u0000\u0000\u0000\u0002\u0044\u0001\u0000\u0000\u0000\u0004\u004a\u0001\u0000\u0000\u0000\u0006\u004c\u0001\u0000\u0000\u0000\u0008\u0051\u0001\u0000\u0000\u0000\u000a\u0055\u0001\u0000\u0000\u0000\u000c\u0059\u0001\u0000\u0000\u0000\u000e\u0067\u0001\u0000\u0000\u0000\u0010\u0069\u0001\u0000\u0000\u0000\u0012\u0076\u0001\u0000\u0000\u0000\u0014\u0083\u0001\u0000\u0000\u0000\u0016\u0088\u0001\u0000\u0000\u0000\u0018\u0091\u0001\u0000\u0000\u0000\u001a\u0096\u0001\u0000\u0000\u0000\u001c\u0098\u0001\u0000\u0000\u0000\u001e\u009c\u0001\u0000\u0000\u0000\u0020\u00a5\u0001\u0000\u0000\u0000\u0022\u00ab\u0001\u0000\u0000\u0000\u0024\u00ad\u0001\u0000\u0000\u0000\u0026\u00af\u0001\u0000\u0000\u0000\u0028\u00b4\u0001\u0000\u0000\u0000\u002a\u00c0\u0001\u0000\u0000\u0000\u002c\u00c7\u0001\u0000\u0000\u0000\u002e\u00c9\u0001\u0000\u0000\u0000\u0030\u00d1\u0001\u0000\u0000\u0000\u0032\u00d7\u0001\u0000\u0000\u0000\u0034\u00dd\u0001\u0000\u0000\u0000\u0036\u00e0\u0001\u0000\u0000\u0000\u0038\u003a\u0003\u0002\u0001\u0000\u0039\u0038\u0001\u0000\u0000\u0000\u003a\u003d\u0001\u0000\u0000\u0000\u003b\u0039\u0001\u0000\u0000\u0000\u003b\u003c\u0001\u0000\u0000\u0000\u003c\u003e\u0001\u0000\u0000\u0000\u003d\u003b\u0001\u0000\u0000\u0000\u003e\u003f\u0005\u0000\u0000\u0001\u003f\u0001\u0001\u0000\u0000\u0000\u0040\u0045\u0003\u0004\u0002\u0000\u0041\u0042\u0003\u000e\u0007\u0000\u0042\u0043\u0005\u0001\u0000\u0000\u0043\u0045\u0001\u0000\u0000\u0000\u0044\u0040\u0001\u0000\u0000\u0000\u0044\u0041\u0001\u0000\u0000\u0000\u0045\u0003\u0001\u0000\u0000\u0000\u0046\u004b\u0003\u0006\u0003\u0000\u0047\u004b\u0003\u0008\u0004\u0000\u0048\u004b\u0003\u000a\u0005\u0000\u0049\u004b\u0003\u000c\u0006\u0000\u004a\u0046\u0001\u0000\u0000\u0000\u004a\u0047\u0001\u0000\u0000\u0000\u004a\u0048\u0001\u0000\u0000\u0000\u004a\u0049\u0001\u0000\u0000\u0000\u004b\u0005\u0001\u0000\u0000\u0000\u004c\u004d\u0005\u0002\u0000\u0000\u004d\u004e\u0005\u0019\u0000\u0000\u004e\u004f\u0005\u0018\u0000\u0000\u004f\u0050\u0005\u0001\u0000\u0000\u0050\u0007\u0001\u0000\u0000\u0000\u0051\u0052\u0005\u0003\u0000\u0000\u0052\u0053\u0005\u0018\u0000\u0000\u0053\u0054\u0005\u0001\u0000\u0000\u0054\u0009\u0001\u0000\u0000\u0000\u0055\u0056\u0005\u0004\u0000\u0000\u0056\u0057\u0005\u0019\u0000\u0000\u0057\u0058\u0005\u0018\u0000\u0000\u0058\u000b\u0001\u0000\u0000\u0000\u0059\u005a\u0005\u0005\u0000\u0000\u005a\u005b\u0005\u0018\u0000\u0000\u005b\u000d\u0001\u0000\u0000\u0000\u005c\u005d\u0003\u0016\u000b\u0000\u005d\u005e\u0003\u0010\u0008\u0000\u005e\u0068\u0001\u0000\u0000\u0000\u005f\u0061\u0003\u001c\u000e\u0000\u0060\u0062\u0003\u0010\u0008\u0000\u0061\u0060\u0001\u0000\u0000\u0000\u0061\u0062\u0001\u0000\u0000\u0000\u0062\u0068\u0001\u0000\u0000\u0000\u0063\u0065\u0003\u0028\u0014\u0000\u0064\u0066\u0003\u0010\u0008\u0000\u0065\u0064\u0001\u0000\u0000\u0000\u0065\u0066\u0001\u0000\u0000\u0000\u0066\u0068\u0001\u0000\u0000\u0000\u0067\u005c\u0001\u0000\u0000\u0000\u0067\u005f\u0001\u0000\u0000\u0000\u0067\u0063\u0001\u0000\u0000\u0000\u0068\u000f\u0001\u0000\u0000\u0000\u0069\u006a\u0003\u0014\u000a\u0000\u006a\u0073\u0003\u0012\u0009\u0000\u006b\u006f\u0005\u0006\u0000\u0000\u006c\u006d\u0003\u0014\u000a\u0000\u006d\u006e\u0003\u0012\u0009\u0000\u006e\u0070\u0001\u0000\u0000\u0000\u006f\u006c\u0001\u0000\u0000\u0000\u006f\u0070\u0001\u0000\u0000\u0000\u0070\u0072\u0001\u0000\u0000\u0000\u0071\u006b\u0001\u0000\u0000\u0000\u0072\u0075\u0001\u0000\u0000\u0000\u0073\u0071\u0001\u0000\u0000\u0000\u0073\u0074\u0001\u0000\u0000\u0000\u0074\u0011\u0001\u0000\u0000\u0000\u0075\u0073\u0001\u0000\u0000\u0000\u0076\u0077\u0003\u0018\u000c\u0000\u0077\u007e\u0003\u0034\u001a\u0000\u0078\u0079\u0005\u0007\u0000\u0000\u0079\u007a\u0003\u0018\u000c\u0000\u007a\u007b\u0003\u0034\u001a\u0000\u007b\u007d\u0001\u0000\u0000\u0000\u007c\u0078\u0001\u0000\u0000\u0000\u007d\u0080\u0001\u0000\u0000\u0000\u007e\u007c\u0001\u0000\u0000\u0000\u007e\u007f\u0001\u0000\u0000\u0000\u007f\u0013\u0001\u0000\u0000\u0000\u0080\u007e\u0001\u0000\u0000\u0000\u0081\u0084\u0003\u0024\u0012\u0000\u0082\u0084\u0005\u0008\u0000\u0000\u0083\u0081\u0001\u0000\u0000\u0000\u0083\u0082\u0001\u0000\u0000\u0000\u0084\u0015\u0001\u0000\u0000\u0000\u0085\u0089\u0003\u0024\u0012\u0000\u0086\u0089\u0005\u0017\u0000\u0000\u0087\u0089\u0003\u001e\u000f\u0000\u0088\u0085\u0001\u0000\u0000\u0000\u0088\u0086\u0001\u0000\u0000\u0000\u0088\u0087\u0001\u0000\u0000\u0000\u0089\u0017\u0001\u0000\u0000\u0000\u008a\u0092\u0003\u0024\u0012\u0000\u008b\u0092\u0005\u0017\u0000\u0000\u008c\u0092\u0003\u001e\u000f\u0000\u008d\u0092\u0003\u001c\u000e\u0000\u008e\u0092\u0003\u001a\u000d\u0000\u008f\u0092\u0003\u002e\u0017\u0000\u0090\u0092\u0003\u0028\u0014\u0000\u0091\u008a\u0001\u0000\u0000\u0000\u0091\u008b\u0001\u0000\u0000\u0000\u0091\u008c\u0001\u0000\u0000\u0000\u0091\u008d\u0001\u0000\u0000\u0000\u0091\u008e\u0001\u0000\u0000\u0000\u0091\u008f\u0001\u0000\u0000\u0000\u0091\u0090\u0001\u0000\u0000\u0000\u0092\u0019\u0001\u0000\u0000\u0000\u0093\u0097\u0003\u0020\u0010\u0000\u0094\u0097\u0005\u0015\u0000\u0000\u0095\u0097\u0005\u0016\u0000\u0000\u0096\u0093\u0001\u0000\u0000\u0000\u0096\u0094\u0001\u0000\u0000\u0000\u0096\u0095\u0001\u0000\u0000\u0000\u0097\u001b\u0001\u0000\u0000\u0000\u0098\u0099\u0005\u0009\u0000\u0000\u0099\u009a\u0003\u0010\u0008\u0000\u009a\u009b\u0005\u000a\u0000\u0000\u009b\u001d\u0001\u0000\u0000\u0000\u009c\u00a0\u0005\u000b\u0000\u0000\u009d\u009f\u0003\u0018\u000c\u0000\u009e\u009d\u0001\u0000\u0000\u0000\u009f\u00a2\u0001\u0000\u0000\u0000\u00a0\u009e\u0001\u0000\u0000\u0000\u00a0\u00a1\u0001\u0000\u0000\u0000\u00a1\u00a3\u0001\u0000\u0000\u0000\u00a2\u00a0\u0001\u0000\u0000\u0000\u00a3\u00a4\u0005\u000c\u0000\u0000\u00a4\u001f\u0001\u0000\u0000\u0000\u00a5\u00a9\u0003\u0022\u0011\u0000\u00a6\u00aa\u0005\u0020\u0000\u0000\u00a7\u00a8\u0005\u000d\u0000\u0000\u00a8\u00aa\u0003\u0024\u0012\u0000\u00a9\u00a6\u0001\u0000\u0000\u0000\u00a9\u00a7\u0001\u0000\u0000\u0000\u00a9\u00aa\u0001\u0000\u0000\u0000\u00aa\u0021\u0001\u0000\u0000\u0000\u00ab\u00ac\u0007\u0000\u0000\u0000\u00ac\u0023\u0001\u0000\u0000\u0000\u00ad\u00ae\u0007\u0001\u0000\u0000\u00ae\u0025\u0001\u0000\u0000\u0000\u00af\u00b2\u0005\u000e\u0000\u0000\u00b0\u00b3\u0003\u0024\u0012\u0000\u00b1\u00b3\u0005\u0017\u0000\u0000\u00b2\u00b0\u0001\u0000\u0000\u0000\u00b2\u00b1\u0001\u0000\u0000\u0000\u00b2\u00b3\u0001\u0000\u0000\u0000\u00b3\u0027\u0001\u0000\u0000\u0000\u00b4\u00b5\u0005\u000f\u0000\u0000\u00b5\u00b6\u0003\u002a\u0015\u0000\u00b6\u00b7\u0003\u0014\u000a\u0000\u00b7\u00b9\u0003\u002c\u0016\u0000\u00b8\u00ba\u0003\u0026\u0013\u0000\u00b9\u00b8\u0001\u0000\u0000\u0000\u00b9\u00ba\u0001\u0000\u0000\u0000\u00ba\u00bb\u0001\u0000\u0000\u0000\u00bb\u00bc\u0005\u0010\u0000\u0000\u00bc\u0029\u0001\u0000\u0000\u0000\u00bd\u00c1\u0003\u0024\u0012\u0000\u00be\u00c1\u0005\u0017\u0000\u0000\u00bf\u00c1\u0003\u0028\u0014\u0000\u00c0\u00bd\u0001\u0000\u0000\u0000\u00c0\u00be\u0001\u0000\u0000\u0000\u00c0\u00bf\u0001\u0000\u0000\u0000\u00c1\u002b\u0001\u0000\u0000\u0000\u00c2\u00c8\u0003\u0024\u0012\u0000\u00c3\u00c8\u0005\u0017\u0000\u0000\u00c4\u00c8\u0003\u001a\u000d\u0000\u00c5\u00c8\u0003\u002e\u0017\u0000\u00c6\u00c8\u0003\u0028\u0014\u0000\u00c7\u00c2\u0001\u0000\u0000\u0000\u00c7\u00c3\u0001\u0000\u0000\u0000\u00c7\u00c4\u0001\u0000\u0000\u0000\u00c7\u00c5\u0001\u0000\u0000\u0000\u00c7\u00c6\u0001\u0000\u0000\u0000\u00c8\u002d\u0001\u0000\u0000\u0000\u00c9\u00ca\u0005\u0011\u0000\u0000\u00ca\u00cb\u0003\u0030\u0018\u0000\u00cb\u00cc\u0003\u0014\u000a\u0000\u00cc\u00cd\u0003\u0032\u0019\u0000\u00cd\u00ce\u0005\u0012\u0000\u0000\u00ce\u002f\u0001\u0000\u0000\u0000\u00cf\u00d2\u0003\u0024\u0012\u0000\u00d0\u00d2\u0005\u0017\u0000\u0000\u00d1\u00cf\u0001\u0000\u0000\u0000\u00d1\u00d0\u0001\u0000\u0000\u0000\u00d2\u0031\u0001\u0000\u0000\u0000\u00d3\u00d8\u0003\u0024\u0012\u0000\u00d4\u00d8\u0005\u0017\u0000\u0000\u00d5\u00d8\u0003\u001a\u000d\u0000\u00d6\u00d8\u0003\u002e\u0017\u0000\u00d7\u00d3\u0001\u0000\u0000\u0000\u00d7\u00d4\u0001\u0000\u0000\u0000\u00d7\u00d5\u0001\u0000\u0000\u0000\u00d7\u00d6\u0001\u0000\u0000\u0000\u00d8\u0033\u0001\u0000\u0000\u0000\u00d9\u00dc\u0003\u0026\u0013\u0000\u00da\u00dc\u0003\u0036\u001b\u0000\u00db\u00d9\u0001\u0000\u0000\u0000\u00db\u00da\u0001\u0000\u0000\u0000\u00dc\u00df\u0001\u0000\u0000\u0000\u00dd\u00db\u0001\u0000\u0000\u0000\u00dd\u00de\u0001\u0000\u0000\u0000\u00de\u0035\u0001\u0000\u0000\u0000\u00df\u00dd\u0001\u0000\u0000\u0000\u00e0\u00e1\u0005\u0013\u0000\u0000\u00e1\u00e2\u0003\u0010\u0008\u0000\u00e2\u00e3\u0005\u0014\u0000\u0000\u00e3\u0037\u0001\u0000\u0000\u0000\u0017\u003b\u0044\u004a\u0061\u0065\u0067\u006f\u0073\u007e\u0083\u0088\u0091\u0096\u00a0\u00a9\u00b2\u00b9\u00c0\u00c7\u00d1\u00d7\u00db\u00dd"

        private val ATN = ATNDeserializer().deserialize(SERIALIZED_ATN.toCharArray())

        private val DECISION_TO_DFA = Array(ATN.numberOfDecisions) {
            DFA(ATN.getDecisionState(it)!!, it)
        }

        private val SHARED_CONTEXT_CACHE = PredictionContextCache()
        private val RULE_NAMES: Array<String> = arrayOf(
            "turtleDoc", "statement", "directive", "prefixID", "base", "sparqlPrefix", 
            "sparqlBase", "triples", "predicateObjectList", "objectList", 
            "verb", "subject", "object_", "literal", "blankNodePropertyList", 
            "collection", "rdfLiteral", "string", "iri", "reifier", "reifiedTriple", 
            "rtSubject", "rtObject", "tripleTerm", "ttSubject", "ttObject", 
            "annotation", "annotationBlock"
        )

        private val LITERAL_NAMES: Array<String?> = arrayOf(
            null, "'.'", "'@prefix'", "'@base'", "'PREFIX'", "'BASE'", "';'", 
            "','", "'a'", "'['", "']'", "'('", "')'", "'^^'", "'~'", "'<<'", 
            "'>>'", "'<<('", "')>>'", "'{|'", "'|}'"
        )

        private val SYMBOLIC_NAMES: Array<String?> = arrayOf(
            null, null, null, null, null, null, null, null, null, null, 
            null, null, null, null, null, null, null, null, null, null, 
            null, "NumericLiteral", "BooleanLiteral", "BlankNode", "IRIREF", 
            "PNAME_NS", "PrefixedName", "PNAME_LN", "WS", "NL", "PN_PREFIX", 
            "BLANK_NODE_LABEL", "LANG_DIR", "INTEGER", "DECIMAL", "DOUBLE", 
            "EXPONENT", "STRING_LITERAL_LONG_SINGLE_QUOTE", "STRING_LITERAL_LONG_QUOTE", 
            "STRING_LITERAL_QUOTE", "STRING_LITERAL_SINGLE_QUOTE", "UCHAR", 
            "ECHAR", "ANON_WS", "ANON", "PN_CHARS_BASE", "PN_CHARS_U", "PN_CHARS", 
            "PN_LOCAL", "PLX", "PERCENT", "HEX", "PN_LOCAL_ESC", "LC"
        )

        private val VOCABULARY = VocabularyImpl(LITERAL_NAMES, SYMBOLIC_NAMES)

        private val TOKEN_NAMES: Array<String> = Array(SYMBOLIC_NAMES.size) {
            VOCABULARY.getLiteralName(it)
                ?: VOCABULARY.getSymbolicName(it)
                ?: "<INVALID>"
        }
    }

    public object Tokens {
        public const val EOF: Int = -1
        public const val T__0: Int = 1
        public const val T__1: Int = 2
        public const val T__2: Int = 3
        public const val T__3: Int = 4
        public const val T__4: Int = 5
        public const val T__5: Int = 6
        public const val T__6: Int = 7
        public const val T__7: Int = 8
        public const val T__8: Int = 9
        public const val T__9: Int = 10
        public const val T__10: Int = 11
        public const val T__11: Int = 12
        public const val T__12: Int = 13
        public const val T__13: Int = 14
        public const val T__14: Int = 15
        public const val T__15: Int = 16
        public const val T__16: Int = 17
        public const val T__17: Int = 18
        public const val T__18: Int = 19
        public const val T__19: Int = 20
        public const val NumericLiteral: Int = 21
        public const val BooleanLiteral: Int = 22
        public const val BlankNode: Int = 23
        public const val IRIREF: Int = 24
        public const val PNAME_NS: Int = 25
        public const val PrefixedName: Int = 26
        public const val PNAME_LN: Int = 27
        public const val WS: Int = 28
        public const val NL: Int = 29
        public const val PN_PREFIX: Int = 30
        public const val BLANK_NODE_LABEL: Int = 31
        public const val LANG_DIR: Int = 32
        public const val INTEGER: Int = 33
        public const val DECIMAL: Int = 34
        public const val DOUBLE: Int = 35
        public const val EXPONENT: Int = 36
        public const val STRING_LITERAL_LONG_SINGLE_QUOTE: Int = 37
        public const val STRING_LITERAL_LONG_QUOTE: Int = 38
        public const val STRING_LITERAL_QUOTE: Int = 39
        public const val STRING_LITERAL_SINGLE_QUOTE: Int = 40
        public const val UCHAR: Int = 41
        public const val ECHAR: Int = 42
        public const val ANON_WS: Int = 43
        public const val ANON: Int = 44
        public const val PN_CHARS_BASE: Int = 45
        public const val PN_CHARS_U: Int = 46
        public const val PN_CHARS: Int = 47
        public const val PN_LOCAL: Int = 48
        public const val PLX: Int = 49
        public const val PERCENT: Int = 50
        public const val HEX: Int = 51
        public const val PN_LOCAL_ESC: Int = 52
        public const val LC: Int = 53
    }

    public object Rules {
        public const val TurtleDoc: Int = 0
        public const val Statement: Int = 1
        public const val Directive: Int = 2
        public const val PrefixID: Int = 3
        public const val Base: Int = 4
        public const val SparqlPrefix: Int = 5
        public const val SparqlBase: Int = 6
        public const val Triples: Int = 7
        public const val PredicateObjectList: Int = 8
        public const val ObjectList: Int = 9
        public const val Verb: Int = 10
        public const val Subject: Int = 11
        public const val Object_: Int = 12
        public const val Literal: Int = 13
        public const val BlankNodePropertyList: Int = 14
        public const val Collection: Int = 15
        public const val RdfLiteral: Int = 16
        public const val String: Int = 17
        public const val Iri: Int = 18
        public const val Reifier: Int = 19
        public const val ReifiedTriple: Int = 20
        public const val RtSubject: Int = 21
        public const val RtObject: Int = 22
        public const val TripleTerm: Int = 23
        public const val TtSubject: Int = 24
        public const val TtObject: Int = 25
        public const val Annotation: Int = 26
        public const val AnnotationBlock: Int = 27
    }

    override var interpreter: ParserATNSimulator =
        @Suppress("LeakingThis")
        ParserATNSimulator(this, ATN, DECISION_TO_DFA, SHARED_CONTEXT_CACHE)

    override val grammarFileName: String =
        "Turtle.g4"

    @Deprecated("Use vocabulary instead", replaceWith = ReplaceWith("vocabulary"))
    override val tokenNames: Array<String> =
        TOKEN_NAMES

    override val ruleNames: Array<String> =
        RULE_NAMES

    override val atn: ATN =
        ATN

    override val vocabulary: Vocabulary =
        VOCABULARY

    override val serializedATN: String =
        SERIALIZED_ATN

    /* Named actions */

    /* Funcs */
    public open class TurtleDocContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.TurtleDoc

        public fun EOF(): TerminalNode = getToken(Tokens.EOF, 0)!!
        public fun statement(): List<StatementContext> = getRuleContexts(StatementContext::class)
        public fun statement(i: Int): StatementContext? = getRuleContext(StatementContext::class, i)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterTurtleDoc(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitTurtleDoc(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitTurtleDoc(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun turtleDoc(): TurtleDocContext {
        var _localctx = TurtleDocContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 0, Rules.TurtleDoc)
        var _la: Int

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 59
            errorHandler.sync(this)
            _la = _input.LA(1)

            while ((((_la) and 0x3f.inv()) == 0 && ((1L shl _la) and 92310076L) != 0L)) {
                this.state = 56
                statement()

                this.state = 61
                errorHandler.sync(this)
                _la = _input.LA(1)
            }
            this.state = 62
            match(Tokens.EOF)

        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class StatementContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Statement

        public fun directive(): DirectiveContext? = getRuleContext(DirectiveContext::class, 0)
        public fun triples(): TriplesContext? = getRuleContext(TriplesContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterStatement(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitStatement(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitStatement(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun statement(): StatementContext {
        var _localctx = StatementContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 2, Rules.Statement)

        try {
            this.state = 68
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.T__1, Tokens.T__2, Tokens.T__3, Tokens.T__4 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 64
                    directive()

                }Tokens.T__8, Tokens.T__10, Tokens.T__14, Tokens.BlankNode, Tokens.IRIREF, Tokens.PrefixedName -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 65
                    triples()

                    this.state = 66
                    match(Tokens.T__0)

                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class DirectiveContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Directive

        public fun prefixID(): PrefixIDContext? = getRuleContext(PrefixIDContext::class, 0)
        public fun base(): BaseContext? = getRuleContext(BaseContext::class, 0)
        public fun sparqlPrefix(): SparqlPrefixContext? = getRuleContext(SparqlPrefixContext::class, 0)
        public fun sparqlBase(): SparqlBaseContext? = getRuleContext(SparqlBaseContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterDirective(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitDirective(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitDirective(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun directive(): DirectiveContext {
        var _localctx = DirectiveContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 4, Rules.Directive)

        try {
            this.state = 74
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.T__1 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 70
                    prefixID()

                }Tokens.T__2 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 71
                    base()

                }Tokens.T__3 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 3)
                    this.state = 72
                    sparqlPrefix()

                }Tokens.T__4 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 4)
                    this.state = 73
                    sparqlBase()

                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class PrefixIDContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.PrefixID

        public fun PNAME_NS(): TerminalNode = getToken(Tokens.PNAME_NS, 0)!!
        public fun IRIREF(): TerminalNode = getToken(Tokens.IRIREF, 0)!!

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterPrefixID(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitPrefixID(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitPrefixID(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun prefixID(): PrefixIDContext {
        var _localctx = PrefixIDContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 6, Rules.PrefixID)

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 76
            match(Tokens.T__1)

            this.state = 77
            match(Tokens.PNAME_NS)

            this.state = 78
            match(Tokens.IRIREF)

            this.state = 79
            match(Tokens.T__0)

        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class BaseContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Base

        public fun IRIREF(): TerminalNode = getToken(Tokens.IRIREF, 0)!!

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterBase(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitBase(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitBase(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun base(): BaseContext {
        var _localctx = BaseContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 8, Rules.Base)

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 81
            match(Tokens.T__2)

            this.state = 82
            match(Tokens.IRIREF)

            this.state = 83
            match(Tokens.T__0)

        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class SparqlPrefixContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.SparqlPrefix

        public fun PNAME_NS(): TerminalNode = getToken(Tokens.PNAME_NS, 0)!!
        public fun IRIREF(): TerminalNode = getToken(Tokens.IRIREF, 0)!!

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterSparqlPrefix(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitSparqlPrefix(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitSparqlPrefix(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun sparqlPrefix(): SparqlPrefixContext {
        var _localctx = SparqlPrefixContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 10, Rules.SparqlPrefix)

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 85
            match(Tokens.T__3)

            this.state = 86
            match(Tokens.PNAME_NS)

            this.state = 87
            match(Tokens.IRIREF)

        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class SparqlBaseContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.SparqlBase

        public fun IRIREF(): TerminalNode = getToken(Tokens.IRIREF, 0)!!

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterSparqlBase(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitSparqlBase(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitSparqlBase(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun sparqlBase(): SparqlBaseContext {
        var _localctx = SparqlBaseContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 12, Rules.SparqlBase)

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 89
            match(Tokens.T__4)

            this.state = 90
            match(Tokens.IRIREF)

        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class TriplesContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Triples

        public fun subject(): SubjectContext? = getRuleContext(SubjectContext::class, 0)
        public fun predicateObjectList(): PredicateObjectListContext? = getRuleContext(PredicateObjectListContext::class, 0)
        public fun blankNodePropertyList(): BlankNodePropertyListContext? = getRuleContext(BlankNodePropertyListContext::class, 0)
        public fun reifiedTriple(): ReifiedTripleContext? = getRuleContext(ReifiedTripleContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterTriples(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitTriples(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitTriples(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun triples(): TriplesContext {
        var _localctx = TriplesContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 14, Rules.Triples)
        var _la: Int

        try {
            this.state = 103
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.T__10, Tokens.BlankNode, Tokens.IRIREF, Tokens.PrefixedName -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 92
                    subject()

                    this.state = 93
                    predicateObjectList()

                }Tokens.T__8 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 95
                    blankNodePropertyList()

                    this.state = 97
                    errorHandler.sync(this)
                    _la = _input.LA(1)

                    if ((((_la) and 0x3f.inv()) == 0 && ((1L shl _la) and 83886336L) != 0L)) {
                        this.state = 96
                        predicateObjectList()

                    }
                }Tokens.T__14 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 3)
                    this.state = 99
                    reifiedTriple()

                    this.state = 101
                    errorHandler.sync(this)
                    _la = _input.LA(1)

                    if ((((_la) and 0x3f.inv()) == 0 && ((1L shl _la) and 83886336L) != 0L)) {
                        this.state = 100
                        predicateObjectList()

                    }
                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class PredicateObjectListContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.PredicateObjectList

        public fun verb(): List<VerbContext> = getRuleContexts(VerbContext::class)
        public fun verb(i: Int): VerbContext? = getRuleContext(VerbContext::class, i)
        public fun objectList(): List<ObjectListContext> = getRuleContexts(ObjectListContext::class)
        public fun objectList(i: Int): ObjectListContext? = getRuleContext(ObjectListContext::class, i)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterPredicateObjectList(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitPredicateObjectList(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitPredicateObjectList(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun predicateObjectList(): PredicateObjectListContext {
        var _localctx = PredicateObjectListContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 16, Rules.PredicateObjectList)
        var _la: Int

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 105
            verb()

            this.state = 106
            objectList()

            this.state = 115
            errorHandler.sync(this)
            _la = _input.LA(1)

            while (_la == Tokens.T__5) {
                this.state = 107
                match(Tokens.T__5)

                this.state = 111
                errorHandler.sync(this)
                _la = _input.LA(1)

                if ((((_la) and 0x3f.inv()) == 0 && ((1L shl _la) and 83886336L) != 0L)) {
                    this.state = 108
                    verb()

                    this.state = 109
                    objectList()

                }
                this.state = 117
                errorHandler.sync(this)
                _la = _input.LA(1)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class ObjectListContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.ObjectList

        public fun object_(): List<Object_Context> = getRuleContexts(Object_Context::class)
        public fun object_(i: Int): Object_Context? = getRuleContext(Object_Context::class, i)
        public fun annotation(): List<AnnotationContext> = getRuleContexts(AnnotationContext::class)
        public fun annotation(i: Int): AnnotationContext? = getRuleContext(AnnotationContext::class, i)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterObjectList(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitObjectList(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitObjectList(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun objectList(): ObjectListContext {
        var _localctx = ObjectListContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 18, Rules.ObjectList)
        var _la: Int

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 118
            object_()

            this.state = 119
            annotation()

            this.state = 126
            errorHandler.sync(this)
            _la = _input.LA(1)

            while (_la == Tokens.T__6) {
                this.state = 120
                match(Tokens.T__6)

                this.state = 121
                object_()

                this.state = 122
                annotation()

                this.state = 128
                errorHandler.sync(this)
                _la = _input.LA(1)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class VerbContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Verb

        public fun iri(): IriContext? = getRuleContext(IriContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterVerb(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitVerb(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitVerb(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun verb(): VerbContext {
        var _localctx = VerbContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 20, Rules.Verb)

        try {
            this.state = 131
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.IRIREF, Tokens.PrefixedName -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 129
                    iri()

                }Tokens.T__7 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 130
                    match(Tokens.T__7)

                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class SubjectContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Subject

        public fun iri(): IriContext? = getRuleContext(IriContext::class, 0)
        public fun BlankNode(): TerminalNode? = getToken(Tokens.BlankNode, 0)
        public fun collection(): CollectionContext? = getRuleContext(CollectionContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterSubject(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitSubject(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitSubject(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun subject(): SubjectContext {
        var _localctx = SubjectContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 22, Rules.Subject)

        try {
            this.state = 136
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.IRIREF, Tokens.PrefixedName -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 133
                    iri()

                }Tokens.BlankNode -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 134
                    match(Tokens.BlankNode)

                }Tokens.T__10 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 3)
                    this.state = 135
                    collection()

                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class Object_Context : ParserRuleContext {
        override val ruleIndex: Int = Rules.Object_

        public fun iri(): IriContext? = getRuleContext(IriContext::class, 0)
        public fun BlankNode(): TerminalNode? = getToken(Tokens.BlankNode, 0)
        public fun collection(): CollectionContext? = getRuleContext(CollectionContext::class, 0)
        public fun blankNodePropertyList(): BlankNodePropertyListContext? = getRuleContext(BlankNodePropertyListContext::class, 0)
        public fun literal(): LiteralContext? = getRuleContext(LiteralContext::class, 0)
        public fun tripleTerm(): TripleTermContext? = getRuleContext(TripleTermContext::class, 0)
        public fun reifiedTriple(): ReifiedTripleContext? = getRuleContext(ReifiedTripleContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterObject_(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitObject_(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitObject_(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun object_(): Object_Context {
        var _localctx = Object_Context(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 24, Rules.Object_)

        try {
            this.state = 145
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.IRIREF, Tokens.PrefixedName -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 138
                    iri()

                }Tokens.BlankNode -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 139
                    match(Tokens.BlankNode)

                }Tokens.T__10 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 3)
                    this.state = 140
                    collection()

                }Tokens.T__8 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 4)
                    this.state = 141
                    blankNodePropertyList()

                }Tokens.NumericLiteral, Tokens.BooleanLiteral, Tokens.STRING_LITERAL_LONG_SINGLE_QUOTE, Tokens.STRING_LITERAL_LONG_QUOTE, Tokens.STRING_LITERAL_QUOTE, Tokens.STRING_LITERAL_SINGLE_QUOTE -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 5)
                    this.state = 142
                    literal()

                }Tokens.T__16 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 6)
                    this.state = 143
                    tripleTerm()

                }Tokens.T__14 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 7)
                    this.state = 144
                    reifiedTriple()

                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class LiteralContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Literal

        public fun rdfLiteral(): RdfLiteralContext? = getRuleContext(RdfLiteralContext::class, 0)
        public fun NumericLiteral(): TerminalNode? = getToken(Tokens.NumericLiteral, 0)
        public fun BooleanLiteral(): TerminalNode? = getToken(Tokens.BooleanLiteral, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterLiteral(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitLiteral(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitLiteral(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun literal(): LiteralContext {
        var _localctx = LiteralContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 26, Rules.Literal)

        try {
            this.state = 150
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.STRING_LITERAL_LONG_SINGLE_QUOTE, Tokens.STRING_LITERAL_LONG_QUOTE, Tokens.STRING_LITERAL_QUOTE, Tokens.STRING_LITERAL_SINGLE_QUOTE -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 147
                    rdfLiteral()

                }Tokens.NumericLiteral -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 148
                    match(Tokens.NumericLiteral)

                }Tokens.BooleanLiteral -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 3)
                    this.state = 149
                    match(Tokens.BooleanLiteral)

                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class BlankNodePropertyListContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.BlankNodePropertyList

        public fun predicateObjectList(): PredicateObjectListContext = getRuleContext(PredicateObjectListContext::class, 0)!!

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterBlankNodePropertyList(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitBlankNodePropertyList(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitBlankNodePropertyList(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun blankNodePropertyList(): BlankNodePropertyListContext {
        var _localctx = BlankNodePropertyListContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 28, Rules.BlankNodePropertyList)

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 152
            match(Tokens.T__8)

            this.state = 153
            predicateObjectList()

            this.state = 154
            match(Tokens.T__9)

        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class CollectionContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Collection

        public fun object_(): List<Object_Context> = getRuleContexts(Object_Context::class)
        public fun object_(i: Int): Object_Context? = getRuleContext(Object_Context::class, i)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterCollection(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitCollection(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitCollection(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun collection(): CollectionContext {
        var _localctx = CollectionContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 30, Rules.Collection)
        var _la: Int

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 156
            match(Tokens.T__10)

            this.state = 160
            errorHandler.sync(this)
            _la = _input.LA(1)

            while ((((_la) and 0x3f.inv()) == 0 && ((1L shl _la) and 2061683034624L) != 0L)) {
                this.state = 157
                object_()

                this.state = 162
                errorHandler.sync(this)
                _la = _input.LA(1)
            }
            this.state = 163
            match(Tokens.T__11)

        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class RdfLiteralContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.RdfLiteral

        public fun string(): StringContext = getRuleContext(StringContext::class, 0)!!
        public fun LANG_DIR(): TerminalNode? = getToken(Tokens.LANG_DIR, 0)
        public fun iri(): IriContext? = getRuleContext(IriContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterRdfLiteral(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitRdfLiteral(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitRdfLiteral(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun rdfLiteral(): RdfLiteralContext {
        var _localctx = RdfLiteralContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 32, Rules.RdfLiteral)

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 165
            string()

            this.state = 169
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.LANG_DIR -> {
                    this.state = 166
                    match(Tokens.LANG_DIR)

                }Tokens.T__12 -> {
                    this.state = 167
                    match(Tokens.T__12)

                    this.state = 168
                    iri()

                }Tokens.T__0, Tokens.T__5, Tokens.T__6, Tokens.T__8, Tokens.T__9, Tokens.T__10, Tokens.T__11, Tokens.T__13, Tokens.T__14, Tokens.T__15, Tokens.T__16, Tokens.T__17, Tokens.T__18, Tokens.T__19, Tokens.NumericLiteral, Tokens.BooleanLiteral, Tokens.BlankNode, Tokens.IRIREF, Tokens.PrefixedName, Tokens.STRING_LITERAL_LONG_SINGLE_QUOTE, Tokens.STRING_LITERAL_LONG_QUOTE, Tokens.STRING_LITERAL_QUOTE, Tokens.STRING_LITERAL_SINGLE_QUOTE -> {
                    Unit
                }
                else -> Unit
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class StringContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.String

        public fun STRING_LITERAL_QUOTE(): TerminalNode? = getToken(Tokens.STRING_LITERAL_QUOTE, 0)
        public fun STRING_LITERAL_SINGLE_QUOTE(): TerminalNode? = getToken(Tokens.STRING_LITERAL_SINGLE_QUOTE, 0)
        public fun STRING_LITERAL_LONG_SINGLE_QUOTE(): TerminalNode? = getToken(Tokens.STRING_LITERAL_LONG_SINGLE_QUOTE, 0)
        public fun STRING_LITERAL_LONG_QUOTE(): TerminalNode? = getToken(Tokens.STRING_LITERAL_LONG_QUOTE, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterString(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitString(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitString(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun string(): StringContext {
        var _localctx = StringContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 34, Rules.String)
        var _la: Int

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 171
            _la = _input.LA(1)

            if (!((((_la) and 0x3f.inv()) == 0 && ((1L shl _la) and 2061584302080L) != 0L))) {
                errorHandler.recoverInline(this)
            }
            else {
                if (_input.LA(1) == Tokens.EOF) {
                    isMatchedEOF = true
                }

                errorHandler.reportMatch(this)
                consume()
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class IriContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Iri

        public fun IRIREF(): TerminalNode? = getToken(Tokens.IRIREF, 0)
        public fun PrefixedName(): TerminalNode? = getToken(Tokens.PrefixedName, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterIri(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitIri(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitIri(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun iri(): IriContext {
        var _localctx = IriContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 36, Rules.Iri)
        var _la: Int

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 173
            _la = _input.LA(1)

            if (!(_la == Tokens.IRIREF || _la == Tokens.PrefixedName)) {
                errorHandler.recoverInline(this)
            }
            else {
                if (_input.LA(1) == Tokens.EOF) {
                    isMatchedEOF = true
                }

                errorHandler.reportMatch(this)
                consume()
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class ReifierContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Reifier

        public fun iri(): IriContext? = getRuleContext(IriContext::class, 0)
        public fun BlankNode(): TerminalNode? = getToken(Tokens.BlankNode, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterReifier(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitReifier(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitReifier(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun reifier(): ReifierContext {
        var _localctx = ReifierContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 38, Rules.Reifier)

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 175
            match(Tokens.T__13)

            this.state = 178
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.IRIREF, Tokens.PrefixedName -> {
                    this.state = 176
                    iri()

                }Tokens.BlankNode -> {
                    this.state = 177
                    match(Tokens.BlankNode)

                }Tokens.T__0, Tokens.T__5, Tokens.T__6, Tokens.T__9, Tokens.T__13, Tokens.T__15, Tokens.T__18, Tokens.T__19 -> {
                    Unit
                }
                else -> Unit
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class ReifiedTripleContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.ReifiedTriple

        public fun rtSubject(): RtSubjectContext = getRuleContext(RtSubjectContext::class, 0)!!
        public fun verb(): VerbContext = getRuleContext(VerbContext::class, 0)!!
        public fun rtObject(): RtObjectContext = getRuleContext(RtObjectContext::class, 0)!!
        public fun reifier(): ReifierContext? = getRuleContext(ReifierContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterReifiedTriple(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitReifiedTriple(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitReifiedTriple(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun reifiedTriple(): ReifiedTripleContext {
        var _localctx = ReifiedTripleContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 40, Rules.ReifiedTriple)
        var _la: Int

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 180
            match(Tokens.T__14)

            this.state = 181
            rtSubject()

            this.state = 182
            verb()

            this.state = 183
            rtObject()

            this.state = 185
            errorHandler.sync(this)
            _la = _input.LA(1)

            if (_la == Tokens.T__13) {
                this.state = 184
                reifier()

            }
            this.state = 187
            match(Tokens.T__15)

        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class RtSubjectContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.RtSubject

        public fun iri(): IriContext? = getRuleContext(IriContext::class, 0)
        public fun BlankNode(): TerminalNode? = getToken(Tokens.BlankNode, 0)
        public fun reifiedTriple(): ReifiedTripleContext? = getRuleContext(ReifiedTripleContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterRtSubject(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitRtSubject(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitRtSubject(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun rtSubject(): RtSubjectContext {
        var _localctx = RtSubjectContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 42, Rules.RtSubject)

        try {
            this.state = 192
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.IRIREF, Tokens.PrefixedName -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 189
                    iri()

                }Tokens.BlankNode -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 190
                    match(Tokens.BlankNode)

                }Tokens.T__14 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 3)
                    this.state = 191
                    reifiedTriple()

                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class RtObjectContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.RtObject

        public fun iri(): IriContext? = getRuleContext(IriContext::class, 0)
        public fun BlankNode(): TerminalNode? = getToken(Tokens.BlankNode, 0)
        public fun literal(): LiteralContext? = getRuleContext(LiteralContext::class, 0)
        public fun tripleTerm(): TripleTermContext? = getRuleContext(TripleTermContext::class, 0)
        public fun reifiedTriple(): ReifiedTripleContext? = getRuleContext(ReifiedTripleContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterRtObject(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitRtObject(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitRtObject(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun rtObject(): RtObjectContext {
        var _localctx = RtObjectContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 44, Rules.RtObject)

        try {
            this.state = 199
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.IRIREF, Tokens.PrefixedName -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 194
                    iri()

                }Tokens.BlankNode -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 195
                    match(Tokens.BlankNode)

                }Tokens.NumericLiteral, Tokens.BooleanLiteral, Tokens.STRING_LITERAL_LONG_SINGLE_QUOTE, Tokens.STRING_LITERAL_LONG_QUOTE, Tokens.STRING_LITERAL_QUOTE, Tokens.STRING_LITERAL_SINGLE_QUOTE -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 3)
                    this.state = 196
                    literal()

                }Tokens.T__16 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 4)
                    this.state = 197
                    tripleTerm()

                }Tokens.T__14 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 5)
                    this.state = 198
                    reifiedTriple()

                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class TripleTermContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.TripleTerm

        public fun ttSubject(): TtSubjectContext = getRuleContext(TtSubjectContext::class, 0)!!
        public fun verb(): VerbContext = getRuleContext(VerbContext::class, 0)!!
        public fun ttObject(): TtObjectContext = getRuleContext(TtObjectContext::class, 0)!!

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterTripleTerm(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitTripleTerm(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitTripleTerm(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun tripleTerm(): TripleTermContext {
        var _localctx = TripleTermContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 46, Rules.TripleTerm)

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 201
            match(Tokens.T__16)

            this.state = 202
            ttSubject()

            this.state = 203
            verb()

            this.state = 204
            ttObject()

            this.state = 205
            match(Tokens.T__17)

        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class TtSubjectContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.TtSubject

        public fun iri(): IriContext? = getRuleContext(IriContext::class, 0)
        public fun BlankNode(): TerminalNode? = getToken(Tokens.BlankNode, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterTtSubject(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitTtSubject(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitTtSubject(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun ttSubject(): TtSubjectContext {
        var _localctx = TtSubjectContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 48, Rules.TtSubject)

        try {
            this.state = 209
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.IRIREF, Tokens.PrefixedName -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 207
                    iri()

                }Tokens.BlankNode -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 208
                    match(Tokens.BlankNode)

                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class TtObjectContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.TtObject

        public fun iri(): IriContext? = getRuleContext(IriContext::class, 0)
        public fun BlankNode(): TerminalNode? = getToken(Tokens.BlankNode, 0)
        public fun literal(): LiteralContext? = getRuleContext(LiteralContext::class, 0)
        public fun tripleTerm(): TripleTermContext? = getRuleContext(TripleTermContext::class, 0)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterTtObject(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitTtObject(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitTtObject(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun ttObject(): TtObjectContext {
        var _localctx = TtObjectContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 50, Rules.TtObject)

        try {
            this.state = 215
            errorHandler.sync(this)

            when (_input.LA(1)) {
                Tokens.IRIREF, Tokens.PrefixedName -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 1)
                    this.state = 211
                    iri()

                }Tokens.BlankNode -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 2)
                    this.state = 212
                    match(Tokens.BlankNode)

                }Tokens.NumericLiteral, Tokens.BooleanLiteral, Tokens.STRING_LITERAL_LONG_SINGLE_QUOTE, Tokens.STRING_LITERAL_LONG_QUOTE, Tokens.STRING_LITERAL_QUOTE, Tokens.STRING_LITERAL_SINGLE_QUOTE -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 3)
                    this.state = 213
                    literal()

                }Tokens.T__16 -> /*LL1AltBlock*/ {
                    enterOuterAlt(_localctx, 4)
                    this.state = 214
                    tripleTerm()

                }
                else -> throw NoViableAltException(this)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class AnnotationContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.Annotation

        public fun reifier(): List<ReifierContext> = getRuleContexts(ReifierContext::class)
        public fun reifier(i: Int): ReifierContext? = getRuleContext(ReifierContext::class, i)
        public fun annotationBlock(): List<AnnotationBlockContext> = getRuleContexts(AnnotationBlockContext::class)
        public fun annotationBlock(i: Int): AnnotationBlockContext? = getRuleContext(AnnotationBlockContext::class, i)

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterAnnotation(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitAnnotation(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitAnnotation(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun annotation(): AnnotationContext {
        var _localctx = AnnotationContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 52, Rules.Annotation)
        var _la: Int

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 221
            errorHandler.sync(this)
            _la = _input.LA(1)

            while (_la == Tokens.T__13 || _la == Tokens.T__18) {
                this.state = 219
                errorHandler.sync(this)

                when (_input.LA(1)) {
                    Tokens.T__13 -> /*LL1AltBlock*/ {
                        this.state = 217
                        reifier()

                    }Tokens.T__18 -> /*LL1AltBlock*/ {
                        this.state = 218
                        annotationBlock()

                    }
                    else -> throw NoViableAltException(this)
                }
                this.state = 223
                errorHandler.sync(this)
                _la = _input.LA(1)
            }
        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }

    public open class AnnotationBlockContext : ParserRuleContext {
        override val ruleIndex: Int = Rules.AnnotationBlock

        public fun predicateObjectList(): PredicateObjectListContext = getRuleContext(PredicateObjectListContext::class, 0)!!

        public constructor(parent: ParserRuleContext?, invokingState: Int) : super(parent, invokingState) {
        }

        override fun enterRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.enterAnnotationBlock(this)
            }
        }

        override fun exitRule(listener: ParseTreeListener) {
            if (listener is TurtleListener) {
                listener.exitAnnotationBlock(this)
            }
        }

        override fun <T> accept(visitor: ParseTreeVisitor<out T>): T {
            return if (visitor is TurtleVisitor) {
                visitor.visitAnnotationBlock(this)
            } else {
                visitor.visitChildren(this)
            }
        }
    }


    public fun annotationBlock(): AnnotationBlockContext {
        var _localctx = AnnotationBlockContext(context, state)
        var _token: Token?
        var _ctx: RuleContext?

        enterRule(_localctx, 54, Rules.AnnotationBlock)

        try {
            enterOuterAlt(_localctx, 1)
            this.state = 224
            match(Tokens.T__18)

            this.state = 225
            predicateObjectList()

            this.state = 226
            match(Tokens.T__19)

        }
        catch (re: RecognitionException) {
            _localctx.exception = re
            errorHandler.reportError(this, re)
            errorHandler.recover(this, re)
        }
        finally {
            exitRule()
        }

        return _localctx
    }
}
