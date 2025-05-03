import java_cup.runtime.*;

%%

/* ---------------------- Lexer Configuration ---------------------- */

%class Lexer
%cup
%line
%column

%eofval{
    return null;
%eofval};

%{
    /* Helper methods to create Symbol objects */
    private Symbol createSymbol(int tokenType) {
        return new Symbol(tokenType, yyline, yycolumn);
    }
    
    private Symbol createSymbol(int tokenType, Object value) {
        return new Symbol(tokenType, yyline, yycolumn, value);
    }
%}

/* ---------------------- Regular Expressions ---------------------- */

NEWLINE = \r|\n|\r\n
WHITESPACE = {NEWLINE} | [ \t]

DIGIT = [0-9]
INTEGER = {DIGIT}+

IDENTIFIER = [_a-zA-Z][_a-zA-Z0-9]*
TRUTH = "true" | "false"

COMMENT = \/\*[^*]*\*\/

%%

/* ---------------------- Token Definitions ---------------------- */

"bool"         { return createSymbol(sym.BOOL); }
"else"         { return createSymbol(sym.ELSE); }
"if"           { return createSymbol(sym.IF); }
"int"          { return createSymbol(sym.INT); }
"return"       { return createSymbol(sym.RETURN); }
"while"        { return createSymbol(sym.WHILE); }
"void"         { return createSymbol(sym.VOID); }

"+"            { return createSymbol(sym.PLUS); }
"-"            { return createSymbol(sym.MINUS); }
"*"            { return createSymbol(sym.TIMES); }
"/"            { return createSymbol(sym.OVER); }
"<"            { return createSymbol(sym.LT); }
"<="           { return createSymbol(sym.LE); }
">"            { return createSymbol(sym.GT); }
">="           { return createSymbol(sym.GE); }
"=="           { return createSymbol(sym.EQ); }
"!="           { return createSymbol(sym.NE); }
"~"            { return createSymbol(sym.NOT); }
"||"           { return createSymbol(sym.OR); }
"&&"           { return createSymbol(sym.AND); }
"="            { return createSymbol(sym.ASSIGN); }
";"            { return createSymbol(sym.SEMI); }
","            { return createSymbol(sym.COMMA); }
"("            { return createSymbol(sym.LPAREN); }
")"            { return createSymbol(sym.RPAREN); }
"["            { return createSymbol(sym.LBRACKET); }
"]"            { return createSymbol(sym.RBRACKET); }
"{"            { return createSymbol(sym.LBRACE); }
"}"            { return createSymbol(sym.RBRACE); }

{TRUTH}      { return createSymbol(sym.TRUTH, yytext()); }
{IDENTIFIER}   { return createSymbol(sym.ID, yytext()); }
{INTEGER}      { return createSymbol(sym.NUM, yytext()); }

{WHITESPACE}+  { /* Ignore whitespace */ }
{COMMENT}      { /* Ignore comments */ }

.              { System.err.println("ERROR: Unrecognized character '" + yytext() + "' on line " + yyline); return createSymbol(sym.ERROR); }