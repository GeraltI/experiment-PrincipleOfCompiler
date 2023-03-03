package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    private SymbolTable symbolTable;
    private final Stack<TermThis> propertiesStack = new Stack<>();

    class TermThis extends Term {

        private String text;
        private SourceCodeType type;

        public TermThis(String termName) {
            super(termName);
        }

        public void setText(String name) {
            this.text = name;
        }

        public void setType(SourceCodeType type){
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public SourceCodeType getType() {
            return type;
        }

    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        propertiesStack.pop();
        if(!propertiesStack.empty()){
            throw new RuntimeException("Wrong!Accept");
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        switch(production.index()){
            // S -> D id
            case 4:
                TermThis id4 = propertiesStack.pop();
                TermThis D4 = propertiesStack.pop();
                TermThis S4 = new TermThis("S");
                if(symbolTable.has(id4.getText())){
                    symbolTable.get(id4.getText()).setType(D4.getType());
                }
                else{
                    throw new RuntimeException("Wrong!Reduce4");
                }
                propertiesStack.push(S4);
                break;
            // D -> int
            case 5:
                TermThis Int5 = propertiesStack.pop();
                TermThis D5 = new TermThis("D");
                D5.setType(Int5.getType());
                propertiesStack.push(D5);
                break;
            // S -> id = E
            case 6:
                TermThis E6 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis id6 = propertiesStack.pop();
                TermThis S6 = new TermThis("S");
                propertiesStack.push(S6);
                if(symbolTable.get(id6.getText()).getType() != E6.getType()){
                    throw new RuntimeException("Wrong!Reduce6");
                }
                break;
            // E -> E + A
            case 8:
                TermThis A8 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis E8 = propertiesStack.pop();
                if(A8.getType() != E8.getType()){
                    throw new RuntimeException("Wrong!Reduce8");
                }
                propertiesStack.push(E8);
                break;
            // E -> E - A
            case 9:
                TermThis A9 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis E9 = propertiesStack.pop();
                if(A9.getType() != E9.getType()){
                    throw new RuntimeException("Wrong!Reduce9");
                }
                propertiesStack.push(E9);
                break;
            // E -> A
            case 10:
                TermThis A10 = propertiesStack.pop();
                TermThis E10 = new TermThis("E");
                E10.setType(A10.getType());
                propertiesStack.push(E10);
                break;
            // A -> A * B
            case 11:
                TermThis B11 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis A11 = propertiesStack.pop();
                if(A11.getType() != B11.getType()){
                    throw new RuntimeException("Wrong!Reduce11");
                }
                propertiesStack.push(A11);
                break;
            // A -> B
            case 12:
                TermThis B12 = propertiesStack.pop();
                TermThis A12 = new TermThis("A");
                A12.setType(B12.getType());
                propertiesStack.push(A12);
                break;
            // B -> ( E )
            case 13:
                propertiesStack.pop();
                TermThis E13 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis B13 = new TermThis("B");
                B13.setType(E13.getType());
                propertiesStack.push(B13);
                break;
            // B -> id
            case 14:
                TermThis id14 = propertiesStack.pop();
                TermThis B14 = new TermThis("B");
                if(symbolTable.has(id14.getText())){
                    B14.setType(symbolTable.get(id14.getText()).getType());
                }
                else{
                    throw new RuntimeException("Wrong!Reduce14");
                }
                propertiesStack.push(B14);
                break;
            // B -> IntConst
            case 15:
                TermThis IntConst15 = propertiesStack.pop();
                TermThis B15 = new TermThis("B");
                B15.setType(IntConst15.getType());
                propertiesStack.push(B15);
                break;
            default:
                for(int num = 0; num < production.body().size(); num++){
                    propertiesStack.pop();
                }
                propertiesStack.push(new TermThis(production.head().getTermName()));
                break;
        }
    }

    static final int Int = 1;
    static final int id = 51;
    static final int IntConst = 52;

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        TermThis termThis = new TermThis(currentToken.getKind().getTermName());
        int code = currentToken.getKind().getCode();
        if(code == Int){
            termThis.setType(SourceCodeType.Int);
        }
        else if(code == id){
            termThis.setText(currentToken.getText());
        }
        else if(code == IntConst){
            termThis.setType(SourceCodeType.Int);
        }
        propertiesStack.push(termThis);
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        this.symbolTable = table;
    }
}

