package lexer;

import static control.Control.ConLexer.dump;

import java.io.IOException;
import java.io.InputStream;

import lexer.Token.Kind;
import util.Todo;
import util.Bug;

public class Lexer {
	String fname; // the input file name to be compiled
	InputStream fstream; // input stream for the above file
	public Integer lineNum;

	public Lexer(String fname, InputStream fstream) {
		this.fname = fname;
		this.fstream = fstream;
		this.lineNum = 1;
	}
	public String getLexerFname(){
		return fname;
	}
	public Integer getLexerLineNum(){
		return lineNum;
	}

	// When called, return the next token (refer to the code "Token.java")
	// from the input stream.
	// Return TOKEN_EOF when reaching the end of the input stream.
	private Token nextTokenInternal() throws Exception
	{
		int c = this.fstream.read();
		if (-1 == c)
        // The value for "lineNum" is now "null",
        // you should modify this to an appropriate
        // line number for the "EOF" token.
        return new Token(Kind.TOKEN_EOF, this.lineNum);

        // skip all kinds of "blanks"
		while (' ' == c || '\t' == c || '\n' == c) {
			if('\n' == c){
				this.lineNum++;
			}
			c = this.fstream.read();
		}
		if (-1 == c)
        return new Token(Kind.TOKEN_EOF, this.lineNum);

		switch (c) {
        case '+':
			return new Token(Kind.TOKEN_ADD, this.lineNum);
        case '&':
			if('&' != this.fstream.read()){
				new Bug();
			}else{
				return new Token(Kind.TOKEN_AND, this.lineNum);
			};
			break;
        case '=':
			return new Token(Kind.TOKEN_ASSIGN, this.lineNum);

        case 'b':
			if( ExpecteFollowing("oolean") ){
				return  new Token(Kind.TOKEN_BOOLEAN, this.lineNum);
			}
			break;
        case 'c':
			if( ExpecteFollowing("lass") ){
				return new Token(Kind.TOKEN_CLASS, this.lineNum);
			}
			break;
        case ',':
			return new Token(Kind.TOKEN_COMMER, this.lineNum);

        case '.':
			return new Token(Kind.TOKEN_DOT, this.lineNum);

        case 'e':
			if(ExpecteFollowing("lse")){
				return new Token(Kind.TOKEN_ELSE, this.lineNum);
			}
			if(ExpecteFollowing("xtends")){
				return new Token(Kind.TOKEN_EXTENDS, this.lineNum);
			}
			break;
        case 'f':
			if(ExpecteFollowing("alse")){
				return new Token(Kind.TOKEN_FALSE, this.lineNum);
			}
			break;
        case 'i':
			if(ExpecteFollowing("f")){
				return new Token(Kind.TOKEN_IF, this.lineNum);
			}
			if(ExpecteFollowing("nt")){
				return new Token(Kind.TOKEN_INT, this.lineNum);
			}
			break;
        case '{':
			return new Token(Kind.TOKEN_LBRACE, this.lineNum);

        case '[':
			return new Token(Kind.TOKEN_LBRACK, this.lineNum);

        case 'l':
			if(ExpecteFollowing("ength")){
				return new Token(Kind.TOKEN_LENGTH, this.lineNum);
			}
			break;
        case '(':
            return new Token(Kind.TOKEN_LPAREN, this.lineNum);

        case '<':
            return new Token(Kind.TOKEN_LT, this.lineNum);

        case 'm':
            if(ExpecteFollowing("ain")){
                return new Token(Kind.TOKEN_MAIN, this.lineNum);
            }
            break;
        case 'n':
            if(ExpecteFollowing("ew")){
                return new Token(Kind.TOKEN_NEW, this.lineNum);
            }
            break;
        case '!':
            return new Token(Kind.TOKEN_NOT, this.lineNum);

        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return new Token(Kind.TOKEN_NUM, this.lineNum, buildIntergerLiterals(c));

        case 'o':
            if(ExpecteFollowing("ut")){
                return new Token(Kind.TOKEN_OUT, this.lineNum);
            }
            break;
        case 'p':
            if(ExpecteFollowing("rintln") ){
                return new Token(Kind.TOKEN_PRINTLN, this.lineNum);
            }
            if(ExpecteFollowing("ublic")){
                return new Token(Kind.TOKEN_PUBLIC, this.lineNum);
            }
            break;
        case '}':
            return new Token(Kind.TOKEN_RBRACE, this.lineNum);

        case ']':
            return new Token(Kind.TOKEN_RBRACK, this.lineNum);

        case 'r':
            if(ExpecteFollowing("eturn")){
                return new Token(Kind.TOKEN_RETURN, this.lineNum);
            }
            break;
        case ')':
            return new Token(Kind.TOKEN_RPAREN, this.lineNum);

        case ';':
            return new Token(Kind.TOKEN_SEMI, this.lineNum);

        case 's':
            if(ExpecteFollowing("tatic")){
                return new Token(Kind.TOKEN_STATIC, this.lineNum);
            }
            break;
        case 'S':
            if(ExpecteFollowing("tring")){
                return new Token(Kind.TOKEN_STRING, this.lineNum);
            }
            if(ExpecteFollowing("ystem")){
                return new Token(Kind.TOKEN_SYSTEM, this.lineNum);
            }
            break;
        case '-':
            return new Token(Kind.TOKEN_SUB, this.lineNum);
        case 't':
            if(ExpecteFollowing("his")){
                return new Token(Kind.TOKEN_THIS, this.lineNum);
            }
            if(ExpecteFollowing("rue")){
                return  new Token(Kind.TOKEN_TRUE, this.lineNum);
            }
            break;
        case '*':
            return new Token(Kind.TOKEN_TIMES, this.lineNum);
            
        case 'v':
            if(ExpecteFollowing("oid")){
                return new Token(Kind.TOKEN_VOID, this.lineNum);
            }
            break;
        case 'w':
            if(ExpecteFollowing("hile")){
                return new Token(Kind.TOKEN_WHILE, this.lineNum);
            }
            break;
        case '/':
            if(this.fstream.read() == '/'){
                int c1 = this.fstream.read();
                while ( (-1 != c1) && '\n' != c1) {
                	// loop to eat the commentes 
                    c1 = this.fstream.read();
                }
                return nextTokenInternal();

            }else if(this.fstream.read() == '*'){
            	// there some buges in here, it can not solve the nested comments
                int c1 = this.fstream.read();
                int c2 = this.fstream.read();
                // the blow conditon:(-1 != c2) && ( ( '*'!= c1 ) || ( '/' != c2) )
                // is a good trick
                while(  (-1 != c2) && ( ( '*'!= c1 ) || ( '/' != c2) ) ){
                    c1 = c2;
                    c2 = this.fstream.read();
                }
                if( ('*' == c1) && ('/') == c2 ){

                	return nextTokenInternal();
                }else if( -1 == c2){
                	System.out.print("the commments must be end with '*/' ");
                	new Bug();
                }else{
                	// iglegal end the commments
                	System.out.println("[++ERROE++]the comments error at " + this.lineNum);
                	new Bug();
                }

            }else{
            	new Bug();
            }
            break;
        default:
        	if( ( ('a' <= c)&&( c <= 'z') ) || ( ('A' <= c)&&(c <= 'Z') ) ){
        		break;
        	}else {
        		new Bug();
        		return null;
        	}

            // Lab 1, exercise 2: supply missing code to
            // lex other kinds of tokens.
            // Hint: think carefully about the basic
            // data structure and algorithms. The code
            // is not that much and may be less than 50 lines. If you
            // find you are writing a lot of code, you
            // are on the wrong way.
        }
        return new Token(Kind.TOKEN_ID,this.lineNum, buildID(c));
    }

    /*
     * 判断当前输入的是不是关键字
     */
    private boolean ExpecteFollowing(String ExpectedString) throws IOException {
    	if (this.fstream.markSupported()) {

    		this.fstream.mark(ExpectedString.length() + 1);

    		for (int i = 0; i < ExpectedString.length(); i++) {
    			if (ExpectedString.charAt(i) != this.fstream.read()) {
    				this.fstream.reset();
    				return false;
    			}
    		}
    		this.fstream.mark(1);
    		int c = this.fstream.read();
    		this.fstream.reset();
    		if( IsLegalIdentifier(c) ){
            	// the next char is legal lex or underscores or digits, 
            	// so the array of char is not legal Identifiers
    			return false;
    		}else{
                // 后边跟的不是有效的字符,下划线或者数字,返回true, 是关键字
    			return true;
    		}
    	}else{
            // 当前的输入流不支持 mark方法,直接返回错误
    		System.err.println("this Input Stream can not support fstream.mark!");
    		System.exit(0);
    		return false;
    	}
    }
    
    /** function: build the Identifiers
     *
     *
     **/
    private String buildID(int c) throws IOException{
    	StringBuilder sb = new StringBuilder();
    	sb.append((char)c);
    	for (; ; ) {
    		this.fstream.mark(1);
    		int s = this.fstream.read();
    		if (  ( s == '_') || ( (s >= 'a')&&( s <= 'z') ) || ( (s >= 'A' )&&( s <= 'Z') ) ) {			
    			sb.append((char)s);
    		}else{
    			this.fstream.reset();
    			break;
    		}
    	}
    	return sb.toString();
    }
    
    /** function:build the Integer Listerals
     *
     *
     **/
    private String buildIntergerLiterals(int c) throws IOException{
    	StringBuilder sb = new StringBuilder();
    	sb.append( (char)c );
    	for (; ; ) {
    		this.fstream.mark(1);
    		int s = this.fstream.read();
    		if( ( s < '0') || ( s > '9' ) ){
    			this.fstream.reset();
    			break;
    		}
    		sb.append((char)s);
    	}
    	return sb.toString(); // return the string 
    }
    /* function: 判断 参数s 是不是字符,下划线或者数字
     *
     *
     ****/
    private boolean IsLegalIdentifier(int s)
    {
    	if ( s == '_' || (( s>='a')&&(s<='z')) || ((s>='A')&&(s<='Z')) || ((s >='0')&&(s<='9')) ) {
    		return true;
    	}else
    	return false;
    }
    
    public Token nextToken() {
        Token t = null;
        try {
            t = this.nextTokenInternal();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (dump)
		System.out.println(t.toString());
        return t;
    }
    
}
