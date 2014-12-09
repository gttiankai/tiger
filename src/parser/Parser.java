package parser;

import ast.Ast;
import lexer.Lexer;
import lexer.Token;
import lexer.Token.Kind;

import java.util.LinkedList;
import java.util.NoSuchElementException;

public class Parser {
    Lexer lexer;
    Token current;
    LinkedList<Token> savedToken;

    public Parser(String fname, java.io.InputStream fstream) {
        lexer = new Lexer(fname, fstream);
        current = lexer.nextToken();
        savedToken = new LinkedList<>();
    }

    // /////////////////////////////////////////////
    // utility methods to connect the lexer
    // and the parser.

    private void advance() {

        int size = savedToken.size();
        if(size != 0){
            current = savedToken.removeFirst();
        }else{
            current = lexer.nextToken();
        }
    }

    private void saveToken(Token saved){
        // add the element at the header of the savedToken
        this.savedToken.addFirst(saved);
    }
    /**
     * whether the current.kind equals to given kind
     *
     * @param kind
     */
    private void eatToken(Kind kind) {
        if (kind == current.kind)
            // take the next Token
            advance();
        else {
            System.out.println("Expects: " + kind.toString());
            System.out.println("But got: " + current.kind.toString());
            System.out.println("\n");
            error("syntax error");
            System.exit(1);
        }
    }

    private void error() {
        System.out.println("Syntax error:  and compilation aborting...\n");
        System.out.println("the line is " + current.lineNum);
        System.exit(1);
        return;
    }

    private void error(String errorMessage) {
        System.out.println("\n");
        System.out.print("Syntax error: compilation aborting...\n");
        System.out.print(lexer.getLexerFname() + " " + current.lineNum + " :  ");
        System.out.println("error message: " + errorMessage + "\n");
        System.exit(1);
        return;
    }

    // ////////////////////////////////////////////////////////////
    // below are method for parsing.

    // A bunch of parsing methods to parse expressions. The messy
    // parts are to deal with precedence and associativity.(优先级和结合性)

    // ExpList -> Exp ExpRest*
    //         ->
    // ExpRest -> , Exp
    private LinkedList<Ast.Exp.T> parseExpList() {
        if (current.kind == Kind.TOKEN_RPAREN)
            return null;
        LinkedList<Ast.Exp.T> expList = new LinkedList<>();
        expList.add(parseExp());
        while (current.kind == Kind.TOKEN_COMMER) {
            advance();
            expList.add(parseExp());
        }
        return expList;
    }

    // AtomExp -> (Exp)
    // -> INTEGER_LITERAL
    // -> true
    // -> false
    // -> this
    // -> id
    // -> new int [exp]
    // -> new id ()
    private Ast.Exp.T parseAtomExp() {
        switch (current.kind) {
            case TOKEN_LPAREN: {                  // (exp)
                advance();
                Ast.Exp.T exp = parseExp();
                eatToken(Kind.TOKEN_RPAREN);
                return exp;
            }
            case TOKEN_NUM:                     // num
                int num;
                try {
                    num = Integer.parseInt(current.lexeme);
                    advance();

                }catch (Exception e){
                    e.printStackTrace();
                    return null;
                }
                return new Ast.Exp.Num(num);
            case TOKEN_TRUE:                    // true
                advance();
                return new Ast.Exp.True();
            case TOKEN_FALSE:
                advance();
                return new Ast.Exp.False();
            case TOKEN_THIS:                    // this
                advance();
                return new Ast.Exp.This();
            case TOKEN_ID: {                      // id
                // todo
                String id = current.lexeme;
                eatToken(Kind.TOKEN_ID);
                return new Ast.Exp.Id(id);
//                advance();
//
//                Token savedToken = current;
//                if (current.kind == Kind.TOKEN_SEMI) {
//
//                    return new Ast.Exp.Id(id);
//                }else if( (current.kind == Kind.TOKEN_ADD){
//                    advance();
//                    return new Ast.Exp.Add( parseExp() , );
//                    saveToken(current);
//                    current = savedToken;
//
//                }
            }
            case TOKEN_NEW: {                   // new
                advance();
                switch (current.kind) {
                    case TOKEN_INT: {
                        advance();
                        eatToken(Kind.TOKEN_LBRACK);
                        Ast.Exp.T exp = parseExp();
                        eatToken(Kind.TOKEN_RBRACK);
                        return exp;
                    }
                    case TOKEN_ID:
                        String idType = current.lexeme;
                        advance();
                        eatToken(Kind.TOKEN_LPAREN);
                        eatToken(Kind.TOKEN_RPAREN);
                        return new Ast.Exp.Id(idType);
                    default:
                        error();
                        return null;
                }
            }
            default:
                error();
                return null;
        }
    }

    // NotExp -> AtomExp
    //        -> AtomExp .id (expList) // *construct*
    //        -> AtomExp [exp]
    //        -> AtomExp .length
    private Ast.Exp.T parseNotExp() {

        Ast.Exp.T atomExp = parseAtomExp();

        while (current.kind == Kind.TOKEN_DOT
                || current.kind == Kind.TOKEN_LBRACK) {
            if (current.kind == Kind.TOKEN_DOT) {
                advance();
                if (current.kind == Kind.TOKEN_LENGTH) {    // length
                    advance();
                    atomExp =  new Ast.Exp.Length(atomExp);
                }else if(current.kind == Kind.TOKEN_ID){
                    String id = current.lexeme;
                    eatToken(Kind.TOKEN_ID);
                    eatToken(Kind.TOKEN_LPAREN);
                    LinkedList<Ast.Exp.T> args = parseExpList();

                    eatToken(Kind.TOKEN_RPAREN);
                    atomExp = new Ast.Exp.Call(atomExp, id, args);
                }
            } else {
                advance();
                Ast.Exp.T exp = parseExp();
                atomExp = new Ast.Exp.ArraySelect(atomExp, exp);
                eatToken(Kind.TOKEN_RBRACK);
            }
        }
        return atomExp;
    }

    // TimesExp -> ! TimesExp
    //          -> NotExp
    // the special stituation is !!!!Exp -> (! (! (! (! Exp))))
    private Ast.Exp.T parseTimesExp() {
        int nest = 0;
        while (current.kind == Kind.TOKEN_NOT) {
            nest++;
            advance();
        }
        Ast.Exp.T notExp = parseNotExp();
        for (int i = 0 ; i < nest; i++){
            notExp = new Ast.Exp.Not(notExp);
        }
        return notExp;
    }

    // AddSubExp -> TimesExp * TimesExp
    //           -> TimesExp
    private Ast.Exp.T parseAddSubExp() {
        Ast.Exp.T timesExp = parseTimesExp();
        while (current.kind == Kind.TOKEN_TIMES) {
            advance();
            timesExp = new Ast.Exp.Times(timesExp,parseTimesExp());
        }
        return timesExp;
    }

    // LtExp -> AddSubExp + AddSubExp
    // -> AddSubExp - AddSubExp
    // -> AddSubExp
    private Ast.Exp.T parseLtExp() {
        Ast.Exp.T addSubExp = parseAddSubExp();
        while (current.kind == Kind.TOKEN_ADD || current.kind == Kind.TOKEN_SUB) {
            if (current.kind == Kind.TOKEN_ADD){
                eatToken(Kind.TOKEN_ADD);
                addSubExp = new Ast.Exp.Add(addSubExp, parseAddSubExp());
            }else{
                eatToken(Kind.TOKEN_SUB);
                addSubExp = new Ast.Exp.Sub(addSubExp, parseAddSubExp());
            }
        }
        return addSubExp;
    }

    // AndExp -> ( LtExp < LtExp )
    //        -> LtExp
    private Ast.Exp.T parseAndExp() {
        Ast.Exp.T ltExp = parseLtExp();
        while (current.kind == Kind.TOKEN_LT) {
            advance();
            ltExp = new Ast.Exp.Lt(ltExp, parseLtExp());
        }
        return ltExp;
    }

    // Exp -> AndExp && AndExp // 代表"&&"操作的优先级是最低的,最后进行计算
    //     -> AndExp
    private Ast.Exp.T parseExp() {

        Ast.Exp.T andExp = parseAndExp();
        while (current.kind == Kind.TOKEN_AND) {
            advance();                            // &&
            andExp = parseAndExp();
        }
        return andExp;
    }

    // Statement -> { Statement* }
    // -> if ( Exp ) Statement else Statement
    // -> while ( Exp ) Statement
    // -> System.out.println ( Exp ) ;
    // -> id = Exp ;
    // -> id [ Exp ]= Exp ;
    private Ast.Stm.T parseStatement() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a statement.

        switch (current.kind) {
            case TOKEN_LBRACE: {
                eatToken(Kind.TOKEN_LBRACE);    // {
                LinkedList<Ast.Stm.T> statements = parseStatements();                // Statements*
                eatToken(Kind.TOKEN_RBRACE);    // }
                return new Ast.Stm.Block(statements);
            }
            case TOKEN_IF: {
                eatToken(Kind.TOKEN_IF);            // if
                eatToken(Kind.TOKEN_LPAREN);        // (
                Ast.Exp.T condition = parseExp();   // Exp
                eatToken(Kind.TOKEN_RPAREN);        // )
                Ast.Stm.Block ifBody = new Ast.Stm.Block(parseStatements());
                eatToken(Kind.TOKEN_ELSE);          // else
                Ast.Stm.Block elseBody = new Ast.Stm.Block( parseStatements());// Statements
                return new Ast.Stm.If(condition, ifBody, elseBody);
            }
            case TOKEN_WHILE: {
                eatToken(Kind.TOKEN_WHILE);        // while
                eatToken(Kind.TOKEN_LPAREN);    // (
                Ast.Exp.T condition = parseExp();                     // Exp
                eatToken(Kind.TOKEN_RPAREN);    // )
                Ast.Stm.Block body = new Ast.Stm.Block( parseStatements());                // Statements
                return new Ast.Stm.While(condition, body);
            }
            case TOKEN_SYSTEM: {
                eatToken(Kind.TOKEN_SYSTEM);    // System
                eatToken(Kind.TOKEN_DOT);        // .
                eatToken(Kind.TOKEN_OUT);        // out
                eatToken(Kind.TOKEN_DOT);        // .
                eatToken(Kind.TOKEN_PRINTLN);    // println
                eatToken(Kind.TOKEN_LPAREN);    // (
                Ast.Exp.T exp = parseExp();     // Exp
                eatToken(Kind.TOKEN_RPAREN);    // )
                eatToken(Kind.TOKEN_SEMI);      // ;
                return new Ast.Stm.Print(exp);
            }
            case TOKEN_ID: {
                String id = current.lexeme;
                eatToken(Kind.TOKEN_ID);                    // id
                if (current.kind == Kind.TOKEN_ASSIGN) {
                    eatToken(Kind.TOKEN_ASSIGN);            // =
                    Ast.Exp.T exp = parseExp();             // Exp
                    eatToken(Kind.TOKEN_SEMI);              // ;
                    return new Ast.Stm.Assign(id, exp);
                } else if (current.kind == Kind.TOKEN_LBRACK) {
                    eatToken(Kind.TOKEN_LBRACK);            // [
                    Ast.Exp.T index = parseExp();           // Exp
                    eatToken(Kind.TOKEN_RBRACK);            // ]
                    eatToken(Kind.TOKEN_ASSIGN);            // =
                    Ast.Exp.T exp = parseExp();                            // Exp
                    eatToken(Kind.TOKEN_SEMI);                // ;
                    return new Ast.Stm.AssignArray(id, index, exp);
                }
            }
            default:
                error("in parseStatement, default case");
                return null;
        }
    }

    // Statements -> Statement Statements
    //            ->
    private LinkedList<Ast.Stm.T> parseStatements() {
        LinkedList<Ast.Stm.T> statements = new LinkedList<>();
        while (current.kind == Kind.TOKEN_LBRACE
                || current.kind == Kind.TOKEN_IF
                || current.kind == Kind.TOKEN_WHILE
                || current.kind == Kind.TOKEN_SYSTEM
                || current.kind == Kind.TOKEN_ID) {
            statements.add( parseStatement() );
        }
        return statements;
    }

    // Type -> int []
    // -> boolean
    // -> int
    // -> id
    private Ast.Type.T parseType() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a type.
        switch (current.kind) {
            case TOKEN_INT:
                eatToken(Kind.TOKEN_INT);                // int[]
                if (current.kind == Kind.TOKEN_LBRACK) {
                    eatToken(Kind.TOKEN_LBRACK);        // [
                    eatToken(Kind.TOKEN_RBRACK);        // ]
                    return new Ast.Type.IntArray();
                }else {
                    return new Ast.Type.Int();          // int
                }
            case TOKEN_BOOLEAN:
                eatToken(Kind.TOKEN_BOOLEAN);            // boolean
                return new Ast.Type.Boolean();
            case TOKEN_ID:
                String id = current.lexeme;
                eatToken(Kind.TOKEN_ID);                // id
                return new Ast.Type.ClassType(id);
            default:
                error("in parseType, default case");
                return null;
        }
    }

    // VarDecl -> Type id ;
    private Ast.Dec.T parseVarDecl() {
        // to parse the "Type" nonterminal in this method, instead of writing
        // a fresh one.
        Ast.Type.T type =  parseType();
        String id = current.lexeme;
        eatToken(Kind.TOKEN_ID);
        eatToken(Kind.TOKEN_SEMI);
        Ast.Dec.T varDecl = new Ast.Dec.DecSingle(type, id);
        return varDecl;
    }

    // VarDecls -> VarDecl VarDecls
    // ->
    private LinkedList<Ast.Dec.T> parseVarDecls() {
        LinkedList<Ast.Dec.T> varDecs = new LinkedList<>();
        while (current.kind == Kind.TOKEN_INT
                || current.kind == Kind.TOKEN_BOOLEAN
                || current.kind == Kind.TOKEN_ID) {
            if (current.kind == Kind.TOKEN_ID ){
                Token saved = current;
                advance();
                if (current.kind == Kind.TOKEN_ID){
                    saveToken(current);
                    current = saved;
                    varDecs.add( parseVarDecl() );
                    continue;// jump out this while loop
                }else {
                    saveToken(current);
                    current = saved;
                    return varDecs;
                }
            }
            varDecs.add( parseVarDecl() );
        }
        return varDecs;
    }

    // FormalList -> Type id FormalRest*
    //            ->
    // FormalRest -> , Type id
    private LinkedList<Ast.Dec.T> parseFormalList() {
        LinkedList<Ast.Dec.T> formalList = new LinkedList<>();
        if (current.kind != Kind.TOKEN_RPAREN) {

            Ast.Type.T type = parseType();
            String id = current.lexeme;
            eatToken(Kind.TOKEN_ID);
            formalList.add(new Ast.Dec.DecSingle(type, id));
            while (current.kind == Kind.TOKEN_COMMER) {
                eatToken(Kind.TOKEN_COMMER);
                type = parseType();
                id = current.lexeme;
                eatToken(Kind.TOKEN_ID);
                formalList.add(new Ast.Dec.DecSingle(type, id));
            }
            return formalList;
        }else{
            return formalList;
        }
    }

    // Method -> public Type id ( FormalList )
    // { VarDecl* Statement* return Exp ;}
    private Ast.Method.T parseMethod() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a method.
        eatToken(Kind.TOKEN_PUBLIC);            // public
        Ast.Type.T retType = parseType();       // Type
        String id = current.lexeme;
        eatToken(Kind.TOKEN_ID);                // id
        eatToken(Kind.TOKEN_LPAREN);            // (
        LinkedList<Ast.Dec.T> formals = parseFormalList();// FormalList
        eatToken(Kind.TOKEN_RPAREN);            // )
        eatToken(Kind.TOKEN_LBRACE);            // {
        LinkedList<Ast.Dec.T> locals = parseVarDecls();                        // VarDecl*
        LinkedList<Ast.Stm.T> stms = parseStatements();                        // statements
        eatToken(Kind.TOKEN_RETURN);            // return
        Ast.Exp.T reExp = parseExp();                                // Exp
        eatToken(Kind.TOKEN_SEMI);                // ;
        eatToken(Kind.TOKEN_RBRACE);            // }
        return new Ast.Method.MethodSingle(retType, id, formals, locals,stms, reExp);
    }

    // MethodDecls -> MethodDecl MethodDecls
    //             ->
    private LinkedList<Ast.Method.T> parseMethodDecls() {
        LinkedList<Ast.Method.T> methodDecls = new LinkedList<>();
        while (current.kind == Kind.TOKEN_PUBLIC) {
            methodDecls.add(parseMethod());
        }
        return methodDecls;
    }

    // ClassDecl -> class id { VarDecl* MethodDecl* }
    //           -> class id extends id { VarDecl* MethodDecl* }
    private Ast.Class.T parseClassDecl() {
        eatToken(Kind.TOKEN_CLASS);
        String id = current.lexeme;
        eatToken(Kind.TOKEN_ID);
        String extendss = null;
        if (current.kind == Kind.TOKEN_EXTENDS) {
            eatToken(Kind.TOKEN_EXTENDS);
            extendss = current.lexeme;
            eatToken(Kind.TOKEN_ID);
        }
        eatToken(Kind.TOKEN_LBRACE);
        LinkedList<Ast.Dec.T> decs = parseVarDecls();
        LinkedList<ast.Ast.Method.T> methods = parseMethodDecls();
        eatToken(Kind.TOKEN_RBRACE);
        return new Ast.Class.ClassSingle(id, extendss, decs, methods);
    }

    // ClassDecls -> ClassDecl ClassDecls
    //            ->
    private LinkedList<Ast.Class.T> parseClassDecls() {

        LinkedList<Ast.Class.T> classDecl = new LinkedList<Ast.Class.T>();
        while (current.kind == Kind.TOKEN_CLASS) {
            classDecl.add( parseClassDecl() );
        }
        return classDecl;
    }

    // MainClass -> class id
    // {
    // public static void main ( String [] id )
    // {
    // Statement
    // }
    // }
    private Ast.MainClass.T parseMainClass() {
        // Lab1. Exercise 4: Fill in the missing code
        // to parse a main class as described by the
        // grammar above.
        switch (current.kind) {
            case TOKEN_CLASS: // class
                advance();
                String id = current.lexeme;
                eatToken(Kind.TOKEN_ID); // id
                eatToken(Kind.TOKEN_LBRACE); // {
                eatToken(Kind.TOKEN_PUBLIC); // public
                eatToken(Kind.TOKEN_STATIC); // static
                eatToken(Kind.TOKEN_VOID); // void
                eatToken(Kind.TOKEN_MAIN); // main
                eatToken(Kind.TOKEN_LPAREN); // (
                eatToken(Kind.TOKEN_STRING); // String
                eatToken(Kind.TOKEN_LBRACK); // [
                eatToken(Kind.TOKEN_RBRACK);    // ]
                String argsId = current.lexeme;
                eatToken(Kind.TOKEN_ID);        // id
                eatToken(Kind.TOKEN_RPAREN); // )
                eatToken(Kind.TOKEN_LBRACE); // {
                Ast.Stm.T statement = parseStatement();
                eatToken(Kind.TOKEN_RBRACE); // }
                eatToken(Kind.TOKEN_RBRACE); // }
                return new Ast.MainClass.MainClassSingle(id,argsId, statement);
            default:
                error("in parseMainClass, default case");
                return null;
        }
    }


    // Program -> MainClass ClassDecl*
    private Ast.Program.T parseProgram() {
        Ast.MainClass.T mainClass = parseMainClass();
        LinkedList<Ast.Class.T> classs = parseClassDecls();
        eatToken(Kind.TOKEN_EOF);
        return new Ast.Program.ProgramSingle(mainClass, classs);
    }

    // Program
    public Ast.Program.T parse() {
        return parseProgram();
    }

}
