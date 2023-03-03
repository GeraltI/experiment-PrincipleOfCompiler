package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {

    private final SymbolTable symbolTable;

    private final LinkedList<String> fileLineList = new LinkedList<>();
    private final LinkedList<Token> tokenList = new LinkedList<>();

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    private boolean isSkip(char c){
        return c == 10 || c == 12 || c == 13 || c == 32;
    }

    private boolean isLetter(char c){
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private boolean isDight(char c){
        return c >= '0' && c <= '9';
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        //BufferedReader是可以按行读取文件
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String str = null;

        while(true) {
            try {
                if ((str = bufferedReader.readLine()) == null) {
                    break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            fileLineList.add(str);
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            bufferedReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        for(int lineNum = 0; lineNum < fileLineList.size(); lineNum++){
            String line = fileLineList.get(lineNum);
            int left = 0;
            int right = 0;
            String str;
            while(left <= line.length() - 1){

                // 检测到回车、空格、换页等字符，则跳过
                if(isSkip(line.charAt(left))){
                    left++;
                    right++;
                }

                // 检测到letter
                else if(isLetter(line.charAt(left))){
                    right++;
                    //检测到letter、dight、_则继续往右检测
                    while(right < line.length() && (isLetter(line.charAt(right)) || line.charAt(right) == '_'|| isDight(line.charAt(right)))){
                        right++;
                    }
                    str = line.substring(left,right);
                    //检测是否为关键字
                    if(TokenKind.isAllowed(str)){
                        tokenList.add(Token.simple(str));
                    }
                    else{
                        tokenList.add(Token.normal("id",str));
                        if(!symbolTable.has(str)){
                            symbolTable.add(str);
                        }
                    }
                    left = right;
                }

                // 检测到dight
                else if(isDight(line.charAt(left))){
                    right++;
                    //检测到dight则继续往右检测
                    while(right < line.length() && isDight(line.charAt(right))){
                        right++;
                    }
                    str = line.substring(left,right);
                    tokenList.add(Token.normal("IntConst",str));
                    left = right;
                }

                // 检测到其他
                else{
                    right++;
                    switch(line.charAt(left)){
                        case '*':
                            if(right < line.length() && line.charAt(right) == '*'){
                                tokenList.add(Token.simple("**"));
                                right++;
                            }
                            else{
                                tokenList.add(Token.simple("*"));
                            }
                            break;
                        case '=':
                            if(right < line.length() && line.charAt(right) == '='){
                                tokenList.add(Token.simple("=="));
                                right++;
                            }
                            else{
                                tokenList.add(Token.simple("="));
                            }
                            break;
                        case'"':
                            while(line.charAt(right) != '"'){
                                right++;
                            }
                            right++;
                            tokenList.add(Token.normal("StrConst",line.substring(left + 1,right - 1)));
                            break;
                        case';':
                            tokenList.add(Token.simple("Semicolon"));
                            break;
                        case'(':
                            tokenList.add(Token.simple("("));
                            break;
                        case')':
                            tokenList.add(Token.simple(")"));
                            break;
                        case'+':
                            tokenList.add(Token.simple("+"));
                            break;
                        case'-':
                            tokenList.add(Token.simple("-"));
                            break;
                        case'/':
                            tokenList.add(Token.simple("/"));
                            break;
                        default:
                            throw new RuntimeException();
                    }
                    left = right;
                }
            }
        }
        tokenList.add(Token.eof());
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        //throw new NotImplementedException();

        return tokenList;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
