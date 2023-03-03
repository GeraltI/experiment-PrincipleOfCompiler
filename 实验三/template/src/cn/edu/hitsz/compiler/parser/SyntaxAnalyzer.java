package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.*;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.*;
import java.util.function.Predicate;

//TODO: 实验二: 实现 LR 语法分析驱动程序

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {

    private final SymbolTable symbolTable;
    private final List<ActionObserver> observers = new ArrayList<>();
    private final Queue<Token> tokensQueue = new LinkedList<>();
    private final Stack<Status> statusStack = new Stack<>();
    private final Stack<Term> termsStack = new Stack<>();
    private LRTable table;

    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    public void loadTokens(Iterable<Token> tokens) {
        // TODO: 加载词法单元
        // 你可以自行选择要如何存储词法单元, 譬如使用迭代器, 或是栈, 或是干脆使用一个 list 全存起来
        // 需要注意的是, 在实现驱动程序的过程中, 你会需要面对只读取一个 token 而不能消耗它的情况,
        // 在自行设计的时候请加以考虑此种情况
        // throw new NotImplementedException();
        for(Token token:tokens){
            tokensQueue.offer(token);
        }
    }

    public void loadLRTable(LRTable table) {
        // TODO: 加载 LR 分析表
        // 你可以自行选择要如何使用该表格:
        // 是直接对 LRTable 调用 getAction/getGoto, 抑或是直接将 initStatus 存起来使用
        // throw new NotImplementedException();
        this.table = table;
    }

    public void run() {
        // TODO: 实现驱动程序
        // 你需要根据上面的输入来实现 LR 语法分析的驱动程序
        // 请分别在遇到 Shift, Reduce, Accept 的时候调用上面的 callWhenInShift, callWhenInReduce, callWhenInAccept
        // 否则用于为实验二打分的产生式输出可能不会正常工作
        // throw new NotImplementedException();
        Status status = table.getInit();
        //将首状态存入状态栈
        statusStack.push(status);
        while(true){
            //判断单词队列和状态栈是否为空，存在为空则抛出错误
            if(!tokensQueue.isEmpty() && !statusStack.isEmpty()){
                //每次读取状态栈栈顶的状态
                status = statusStack.peek();
                //根据token队列头部和状态栈顶判断状态下一步动作
                Token token = tokensQueue.peek();
                Action action = status.getAction(token);
                //如果符号栈非空且符号栈栈顶为非终止符且符号栈栈顶goto状态不是Error,则执行goto
                if(!termsStack.empty() && termsStack.peek() instanceof NonTerminal && !status.getGoto((NonTerminal)termsStack.peek()).isError()){
                    Status statusGoto = status.getGoto((NonTerminal)termsStack.peek());
                    statusStack.push(statusGoto);
                }
                else{
                    switch(action.getKind()){
                        //如果不goto且动作为转移，则执行转移
                        case Shift:
                            callWhenInShift(status,token);
                            statusStack.push(action.getStatus());
                            termsStack.push(tokensQueue.poll().getKind());
                            break;
                        //如果不goto且动作为规约，则执行规约
                        case Reduce:
                            Production production = action.getProduction();
                            callWhenInReduce(status,production);
                            for(int num = 0; num < production.body().size(); num++){
                                //判断规约和产生式是否一一对应
                                if(production.body().get(production.body().size() - num - 1).equals(termsStack.peek())){
                                    termsStack.pop();
                                    statusStack.pop();
                                }
                                else{
                                    throw new RuntimeException("Wrong!");
                                }
                            }
                            termsStack.push(production.head());
                            break;
                        //如果不goto且动作为接受，则执行接受
                        case Accept:
                            callWhenInAccept(status);
                            statusStack.pop();
                            break;
                        //如果不goto且动作为错误，则执行错误
                        case Error:
                            throw new RuntimeException("Wrong!");
                        default:
                            break;
                    }
                    if(action.getKind().equals(Action.ActionKind.Accept)){
                        break;
                    }
                }
            }
            else{
                throw new RuntimeException("Wrong!");
            }
        }
    }
}
