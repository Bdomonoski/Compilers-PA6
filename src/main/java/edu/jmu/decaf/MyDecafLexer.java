package edu.jmu.decaf;

import java.io.*;
import java.util.*;

import edu.jmu.decaf.Token.Type;

/**
 * CS 432 - PA2
 * Concrete Decaf lexer class.
 * 
 * @authors Brandon Domonoski, Bryan Quach
 * 
 *          This work complies with the JMU Honor Code.
 */
class MyDecafLexer extends DecafLexer
{
    String KEYID = "[a-z]+([_]|[0-9]|[a-zA-Z])+";
    String KEY = "def|i(f|nt)|b(reak|ool)|return|void|(continu|tru|fals|whil|els)e[\\s]";
    String ID = "[a-zA-Z][\\w]*[\\s]";
    String DEC = "([0]|[1-9][0-9]*)";
    String HEX = "0x([0]|([1-9]|[A-F])([0-9]|[A-F])*)";
    String STR = "\"[\\p{ASCII}]*\"";
    String SYM = "([( ) \\[ \\] { } , ; = + - / % < > !]|<=|>=|==|!=|&&|[|][|])";
    String COM = "//.*";
  
    /**
     * Perform lexical analysis, converting Decaf source code into a stream of
     * {@link Token} objects.
     *
     * @param input Input text reader
     * @param filename Source file name (for source info)
     * @return Queue of lexed tokens
     * @throws IOException
     * @throws InvalidTokenException
     */
    @Override
    public Queue<Token> lex(BufferedReader input, String filename)
            throws IOException, InvalidTokenException
    {
        Queue<Token> tokens = new ArrayDeque<Token>();
        int line = 1;
        if (filename == null) filename = "<none>";
        
        // Ignore comments and white-space.
        this.addIgnoredPattern(COM);
        this.addIgnoredPattern("[\\s]");
        
        // Add all regex to acceptable pattern list.
        this.addTokenPattern(Type.ID, KEYID);
        this.addTokenPattern(Type.KEY, KEY);
        this.addTokenPattern(Type.ID, ID);
        this.addTokenPattern(Type.HEX, HEX);
        this.addTokenPattern(Type.DEC, DEC);
        this.addTokenPattern(Type.STR, STR);
        this.addTokenPattern(Type.SYM, SYM);
        
        
        // No tokens.
        if (input == null) return tokens;
        else
        { 
          // While there are still lines to be read.
          while(input.ready())
          {
            String curr = input.readLine();
           
            if (curr == null) return tokens;

            // Discard any comments or whitespace.
            StringBuffer sb = new StringBuffer(curr);
            this.discardIgnored(sb);
            
            // If length == 0, then no characters remain in the buffer after discarding.
            if(sb.length() > 0)
            {
              // Get the new string and tokenize.
              String newString = sb.toString();
              StringTokenizer strTok = new StringTokenizer(newString, " ");
              
              // Check for multiple tokens on single line.
              while (strTok.hasMoreTokens())
              {
                // Create buffer for each tokenized string, and pattern match with regex.
                sb = new StringBuffer(strTok.nextToken());
                Token tok = nextToken(sb);
                do
                {
                  // Invalid token found.
                  if (tok == null) throw new InvalidTokenException("Invalid Token: " + curr);
                  else
                  {
                    // Add new token.
                    tok.source = new SourceInfo(filename, line);
                    tokens.add(tok);
                  }
                } while((tok = nextToken(sb)) != null);
              }
            }
            line++;
          }
        }
        return tokens;
    }
    
}

