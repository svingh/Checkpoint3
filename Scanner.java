import java.io.InputStreamReader;
import java.io.Reader;
import java_cup.runtime.Symbol;

public class Scanner {
    private Lexer scanner;

    public Scanner(Lexer lexer) {
        this.scanner = lexer;
    }

    public Symbol getNextToken() throws java.io.IOException {
        Symbol token = scanner.next_token();
        if (token.sym == sym.ERROR) {
            System.err.println("Lexical Error: Invalid token detected on line " + token.left + ", column " + token.right);
        }
        return token;
    }

    public static void main(String[] argv) {
        try (Reader reader = new InputStreamReader(System.in)) {
            Scanner scanner = new Scanner(new Lexer(reader));
            Symbol tok;
            while ((tok = scanner.getNextToken()) != null && tok.sym != sym.EOF) {
                System.out.print(sym.terminalNames[tok.sym]);
                if (tok.value != null)
                    System.out.print("(" + tok.value + ")");
                System.out.println();
            }
        } catch (Exception e) {
            System.err.println("Unexpected exception during lexical analysis:");
            e.printStackTrace();
        }
    }
}
