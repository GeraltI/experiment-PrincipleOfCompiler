package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import static cn.edu.hitsz.compiler.ir.IRImmediate.of;
import static cn.edu.hitsz.compiler.ir.IRVariable.named;
import static cn.edu.hitsz.compiler.ir.IRVariable.temp;
import static cn.edu.hitsz.compiler.ir.Instruction.*;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */

public class IRGenerator implements ActionObserver {
    private SymbolTable symbolTable;
    private final Stack<TermThis> propertiesStack = new Stack<>();
    private final List<Instruction> instructionsList = new LinkedList<>();

    class TermThis extends Term {

        private String text;
        private IRValue val;

        public TermThis(String termName) {
            super(termName);
        }

        public void setText(String name) {
            this.text = name;
        }

        public void setVal(IRValue val){
            this.val = val;
        }

        public String getText() {
            return text;
        }

        public IRValue getVal() {
            return val;
        }

    }

    static final int id = 51;
    static final int IntConst = 52;

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        TermThis termThis = new TermThis(currentToken.getKind().getTermName());
        int code = currentToken.getKind().getCode();
        if(code == id){
            termThis.setText(currentToken.getText());
            termThis.setVal(named(currentToken.getText()));
        }
        else if(code == IntConst){
            termThis.setVal(of(Integer.parseInt(currentToken.getText())));
        }
        propertiesStack.push(termThis);
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        switch(production.index()){
            // S -> id = E
            case 6:
                TermThis E6 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis id6 = propertiesStack.pop();
                IRVariable variable6 = named(id6.getText());
                id6.setVal(variable6);
                TermThis S6 = new TermThis("S");
                propertiesStack.push(S6);
                instructionsList.add(createMov(variable6,E6.getVal()));
                break;
            // S -> return E
            case 7:
                TermThis E7 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis S7 = new TermThis("S");
                propertiesStack.push(S7);
                instructionsList.add(createRet(E7.getVal()));
                break;
            // E -> E + A
            case 8:
                TermThis A8 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis E8 = propertiesStack.pop();
                IRVariable temp8 = temp();
                instructionsList.add(createAdd(temp8,E8.getVal(),A8.getVal()));
                E8.setVal(temp8);
                propertiesStack.push(E8);
                break;
            // E -> E - A
            case 9:
                TermThis A9 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis E9 = propertiesStack.pop();
                IRVariable temp9 = temp();
                instructionsList.add(createSub(temp9,E9.getVal(),A9.getVal()));
                E9.setVal(temp9);
                propertiesStack.push(E9);
                break;
            // E -> A
            case 10:
                TermThis A10 = propertiesStack.pop();
                TermThis E10 = new TermThis("E");
                E10.setVal(A10.getVal());
                propertiesStack.push(E10);
                break;
            // A -> A * B
            case 11:
                TermThis B11 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis A11 = propertiesStack.pop();
                IRVariable temp11 = temp();
                instructionsList.add(createMul(temp11,A11.getVal(),B11.getVal()));
                A11.setVal(temp11);
                propertiesStack.push(A11);
                break;
            // A -> B
            case 12:
                TermThis B12 = propertiesStack.pop();
                TermThis A12 = new TermThis("A");
                A12.setVal(B12.getVal());
                propertiesStack.push(A12);
                break;
            // B -> ( E )
            case 13:
                propertiesStack.pop();
                TermThis E13 = propertiesStack.pop();
                propertiesStack.pop();
                TermThis B13 = new TermThis("B");
                B13.setVal(E13.getVal());
                propertiesStack.push(B13);
                break;
            // B -> id
            case 14:
                TermThis id14 = propertiesStack.pop();
                TermThis B14 = new TermThis("B");
                B14.setVal(id14.getVal());
                propertiesStack.push(B14);
                break;
            // B -> IntConst
            case 15:
                TermThis IntConst15 = propertiesStack.pop();
                TermThis B15 = new TermThis("B");
                B15.setVal(IntConst15.getVal());
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


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        propertiesStack.pop();
        if(!propertiesStack.empty()){
            throw new RuntimeException("Wrong!Accept");
        }
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return instructionsList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

