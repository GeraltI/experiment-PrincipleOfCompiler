package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.ir.InstructionKind;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;

import static cn.edu.hitsz.compiler.ir.IRImmediate.of;
import static cn.edu.hitsz.compiler.utils.FileUtils.writeFile;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    //寄存器最多数量
    public static int regMaxNum = 7;

    //返回值寄存器
    public static String returnReg = "a0";

    //需要的栈空间
    private int depth = 0;

    private List<Instruction> instructionList;

    //记录当前变量和寄存器对应关系的双射表
    private final BMap<IRValue,Reg> regMap = new BMap();

    //记录当前变量和内存位置对应关系的双射表
    private final BMap<IRValue,Integer> locationMap = new BMap();

    //记录变量使用次数的哈希表
    private final HashMap<IRValue,Integer> usedTimeMap = new HashMap<>();

    //存放当前寄存器的栈(用队列表示)
    private final Deque<Reg> regDeque = new LinkedList<>();

    List<String> writeList = new ArrayList<>();

    //寄存器类
    class Reg{
        //寄存器号
        private final int num;
        //寄存器值
        private IRValue val;

        @Override
        public String toString() {
            return "t" + num ;
        }

        public Reg(int num){
            this.num = num;
        }


        public void setVal(IRValue val) {
            this.val = val;
        }

        //是否活跃
        public boolean isActive(){
            return val != null && usedTimeMap.get(val) != 0;
        }
        //是否空闲
        public boolean isSpare(){
            return val == null;
        }

        public IRValue getVal() {
            return val;
        }

        public int getNum() {
            return num;
        }
    }



    //双射表
    class BMap<K, V> {
        private final Map<K, V> KVmap = new HashMap<>();
        private final Map<V, K> VKmap = new HashMap<>();

        public void removeByKey(K key) {
            VKmap.remove(KVmap.remove(key));
        }

        public void removeByValue(V value) {
            KVmap.remove(VKmap.remove(value));

        }

        public boolean containsKey(K key) {
            return KVmap.containsKey(key);
        }

        public boolean containsValue(V value) {
            return VKmap.containsKey(value);
        }

        public void replace(K key, V value) {
            // 对于双射关系, 将会删除交叉项
            removeByKey(key);
            removeByValue(value);
            KVmap.put(key, value);
            VKmap.put(value, key);
        }

        public V getByKey(K key) {
            return KVmap.get(key);
        }

        public K getByValue(V value) {
            return VKmap.get(value);
        }
    }

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        instructionList = originInstructions;
    }

    /**
     * 预处理
     * 对于 BinaryOp(两个操作数的指令):
     * 将操作两个立即数的 BinaryOp 直接进行求值得到结果, 然后替换成 MOV 指令
     * 将操作一个立即数的指令 (除了乘法和左立即数减法) 进行调整, 使之满足 a := b op imm 的格式
     * 将操作一个立即数的乘法和左立即数减法调整, 前插一条 MOV a, imm, 用 a 替换原立即数, 将指令调整为无立即数指令
     * 对于 UnaryOp(一个操作数的指令):
     * 根据语言规定, 当遇到 Ret 指令后直接舍弃后续指令.
     */
    public void preTreat(){
        int num = 0;
        for(; num < instructionList.size(); num++){
            Instruction instruction = instructionList.get(num);
            InstructionKind instructionKind = instruction.getKind();
            //返回指令
            if(instructionKind.isReturn()){
                num++;
                break;
            }
            //一个操作数指令 MOV 不做操作
            else if(instructionKind.isUnary()){
            }

            //两个操作数指令
            else if(instructionKind.isBinary()){
                //将操作两个立即数的 BinaryOp 直接进行求值得到结果, 然后替换成 MOV 指令
                if(instruction.getLHS().isImmediate() && instruction.getRHS().isImmediate()){
                    instructionList.remove(num);
                    switch(instructionKind){
                        case ADD:
                            instructionList.add(num,Instruction.createMov(instruction.getResult(),
                                    of(Integer.parseInt(instruction.getLHS().toString()) + Integer.parseInt(instruction.getRHS().toString()))));
                            break;
                        case SUB:
                            instructionList.add(num,Instruction.createMov(instruction.getResult(),
                                    of(Integer.parseInt(instruction.getLHS().toString()) - Integer.parseInt(instruction.getRHS().toString()))));
                            break;
                        case MUL:
                            instructionList.add(num,Instruction.createMov(instruction.getResult(),
                                    of(Integer.parseInt(instruction.getLHS().toString()) * Integer.parseInt(instruction.getRHS().toString()))));
                            break;
                        default:
                            break;
                    }
                }
                //将操作左立即数的 BinaryOp 进行操作
                else if(instruction.getLHS().isImmediate()){
                    instructionList.remove(num);
                    switch(instructionKind){
                        //立即数在左边的ADD
                        case ADD:
                            instructionList.add(num,Instruction.createAdd(instruction.getResult(),instruction.getRHS(),instruction.getLHS()));
                            break;
                        //立即数在左边的SUB
                        case SUB:
                            IRVariable tempLeftSub = IRVariable.temp();
                            instructionList.add(num,Instruction.createMov(tempLeftSub,instruction.getLHS()));
                            num++;
                            instructionList.add(num,Instruction.createSub(instruction.getResult(),tempLeftSub,instruction.getRHS()));
                            break;
                        //立即数在左边的MUL
                        case MUL:
                            IRVariable tempLeftMul = IRVariable.temp();
                            instructionList.add(num,Instruction.createMov(tempLeftMul,instruction.getLHS()));
                            num++;
                            instructionList.add(num,Instruction.createMul(instruction.getResult(),tempLeftMul,instruction.getRHS()));
                            break;
                        default:
                            break;
                    }
                }
                //将操作右立即数的 BinaryOp 进行操作
                else if(instruction.getRHS().isImmediate()){
                    instructionList.remove(num);
                    switch(instructionKind){
                        //立即数在右边的MUL
                        case MUL:
                            IRVariable tempRightMul = IRVariable.temp();
                            instructionList.add(num,Instruction.createMov(tempRightMul,instruction.getRHS()));
                            num++;
                            instructionList.add(num,Instruction.createMul(instruction.getResult(),instruction.getLHS(),tempRightMul));
                            break;
                        default:
                            break;
                    }
                }
            }

        }
        //去除return之后的instruction
        while(instructionList.size() > num){
            instructionList.remove(instructionList.size() - 1);
        }
        //预处理完毕
    }

    /**
     * 计算每个变量的使用情况
    */
    public void countUsedTime(){
        //清理哈希表
        usedTimeMap.clear();
        //计算每个变量的使用情况
        for(Instruction instruction : instructionList){
            InstructionKind instructionKind = instruction.getKind();
            //return不做操作
            //MOV
            if(instructionKind.isUnary()){
                if(instruction.getFrom().isIRVariable()){
                    if(usedTimeMap.containsKey(instruction.getFrom())){
                        usedTimeMap.put(instruction.getFrom(),usedTimeMap.get(instruction.getFrom()) + 1);
                    }
                    else{
                        usedTimeMap.put(instruction.getFrom(),1);
                    }
                }
            }
            //双变量
            else if(instructionKind.isBinary()){
                if(instruction.getLHS().isIRVariable()){
                    if(usedTimeMap.containsKey(instruction.getLHS())){
                        usedTimeMap.put(instruction.getLHS(),usedTimeMap.get(instruction.getLHS()) + 1);
                    }
                    else{
                        usedTimeMap.put(instruction.getLHS(),1);
                    }
                }
                if(instruction.getRHS().isIRVariable()){
                    if(usedTimeMap.containsKey(instruction.getRHS())){
                        usedTimeMap.put(instruction.getRHS(),usedTimeMap.get(instruction.getRHS()) + 1);
                    }
                    else{
                        usedTimeMap.put(instruction.getRHS(),1);
                    }
                }
            }
        }
    }
    /**
     * 寄存器选择算法
     */
    public Reg getReg(IRValue val,int regLeft, int regRight){
        //判断变量是否存在寄存器内
        if(regMap.containsKey(val)){
            Reg reg = regMap.getByKey(val);
            regDeque.remove(reg);
            regDeque.addLast(reg);
            return reg;
        }
        //变量不存在寄存器内则寻找空闲寄存器
        for(Reg reg : regDeque){
            //空闲寄存器
            if(reg.isSpare()){
                reg.setVal(val);
                regMap.replace(val,reg);
                regDeque.remove(reg);
                regDeque.addLast(reg);
                if(locationMap.containsKey(val)){
                    writeList.add("    " + "lw  "+ reg + ", " + locationMap.getByKey(val) + "(sp)");
                }
                return reg;
            }
        }
        //找不到空闲寄存器则寻找不再使用的寄存器
        for(Reg reg : regDeque){
            //不再使用的变量的寄存器
            if(!reg.isActive()){
                reg.setVal(val);
                regMap.replace(val,reg);
                regDeque.remove(reg);
                regDeque.addLast(reg);
                if(locationMap.containsKey(val)){
                    writeList.add("    " + "lw  "+ reg + ", " + locationMap.getByKey(val) + "(sp)");
                }
                return reg;
            }
        }
        Reg reg = null;
        //若再找不到不再使用的寄存器则抢占一个寄存器，队列头即是用的最少的那个寄存器
        for(Reg regFind : regDeque){
            //不再使用的变量的寄存器
            if(regFind.getNum() != regLeft && regFind.getNum() !=  regRight){
                reg = regFind;
                regDeque.remove(regFind);
                break;
            }
        }
        //计算有无空出的内存
        int curDepth;
        for(curDepth = 0; curDepth < depth; curDepth = curDepth + 4){
            if(usedTimeMap.get(locationMap.getByValue(curDepth)) == 0){
                writeList.add("    " + "sw  "+ reg + ", " + curDepth + "(sp)");
                if(locationMap.containsKey(val)){
                    writeList.add("    " + "lw  "+ reg + ", " + locationMap.getByKey(val) + "(sp)");
                }
                locationMap.replace(reg.getVal(),curDepth);
                break;
            }
        }
        if(curDepth == depth){
            depth = depth + 4;
            writeList.add("    " + "sw  "+ reg + ", " + curDepth + "(sp)");
            if(locationMap.containsKey(val)){
                writeList.add("    " + "lw  "+ reg + ", " + locationMap.getByKey(val) + "(sp)");
            }
            locationMap.replace(reg.getVal(),curDepth);
        }
        reg.setVal(val);
        regMap.replace(val,reg);
        regDeque.addLast(reg);
        return reg;
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        //预处理部分
        preTreat();

        //进行使用情况的计算
        countUsedTime();

        //初始化寄存器队列
        regDeque.clear();
        for(int i = 0; i < regMaxNum; i++){
            //寄存器栈队列
            regDeque.add(new Reg(i));
        }
        //生成目标代码
        for(Instruction instruction : instructionList){
            InstructionKind instructionKind = instruction.getKind();
            switch(instructionKind){
                //RET
                case RET:
                    if(instruction.getReturnValue().isImmediate()){
                        writeList.add("    " + "li "+ returnReg + ", " + instruction.getReturnValue() + "        #  " + instruction);
                    }
                    else if(instruction.getReturnValue().isIRVariable()){
                        writeList.add("    " + "mv "+ returnReg + ", " + getReg(instruction.getReturnValue(),regMaxNum,regMaxNum) + "        #  " + instruction);
                    }
                    break;
                //MOV
                case MOV:
                    Reg regMovFrom;
                    if(instruction.getFrom().isImmediate()){
                        writeList.add("    " + "li "+ getReg(instruction.getResult(),regMaxNum,regMaxNum) + ", " + instruction.getFrom() + "        #  " + instruction);
                    }
                    else if(instruction.getFrom().isIRVariable()){
                        regMovFrom = getReg(instruction.getFrom(),regMaxNum,regMaxNum);
                        writeList.add("    " + "mv "+ getReg(instruction.getResult(),regMovFrom.getNum(),regMaxNum) + ", " + regMovFrom + "        #  " + instruction);
                        usedTimeMap.put(instruction.getFrom(),usedTimeMap.get(instruction.getFrom()) - 1);
                    }
                    break;
                //ADD
                case ADD:
                    Reg regAddLhs;
                    Reg regAddRhs;
                    if(instruction.getRHS().isImmediate()){
                        regAddLhs = getReg(instruction.getLHS(),regMaxNum,regMaxNum);
                        writeList.add("    " + "addi "+ getReg(instruction.getResult(),regAddLhs.getNum(),regMaxNum) + ", " + regAddLhs + ", " + instruction.getRHS() + "        #  " + instruction);
                        usedTimeMap.put(instruction.getLHS(),usedTimeMap.get(instruction.getLHS()) - 1);
                    }
                    else if(instruction.getRHS().isIRVariable()){
                        regAddLhs = getReg(instruction.getLHS(),regMaxNum,regMaxNum);
                        regAddRhs = getReg(instruction.getRHS(),regAddLhs.getNum(),regMaxNum);
                        writeList.add("    " + "add "+ getReg(instruction.getResult(),regAddLhs.getNum(),regAddRhs.getNum()) + ", " + regAddLhs + ", " + regAddRhs + "        #  " + instruction);
                        usedTimeMap.put(instruction.getLHS(),usedTimeMap.get(instruction.getLHS()) - 1);
                        usedTimeMap.put(instruction.getRHS(),usedTimeMap.get(instruction.getRHS()) - 1);
                    }
                    break;
                //SUB
                case SUB:
                    Reg regSubLhs;
                    Reg regSubRhs;
                    if(instruction.getRHS().isImmediate()){
                        regSubLhs = getReg(instruction.getLHS(),regMaxNum,regMaxNum);
                        //判断sub右立即数正负，转化为addi
                        if(Integer.parseInt(instruction.getRHS().toString()) >= 0){
                            writeList.add("    " + "addi "+ getReg(instruction.getResult(),regSubLhs.getNum(),regMaxNum) + ", " + regSubLhs + ", -" + instruction.getRHS() + "        #  " + instruction);
                        }
                        else{
                            writeList.add("    " + "addi "+ getReg(instruction.getResult(),regSubLhs.getNum(),regMaxNum) + ", " + regSubLhs + ", " + instruction.getRHS().toString().substring(1) + "        #  " + instruction);
                        }
                        usedTimeMap.put(instruction.getLHS(),usedTimeMap.get(instruction.getLHS()) - 1);
                    }
                    else if(instruction.getRHS().isIRVariable()){
                        regSubLhs = getReg(instruction.getLHS(),regMaxNum,regMaxNum);
                        regSubRhs = getReg(instruction.getRHS(),regSubLhs.getNum(),regMaxNum);
                        writeList.add("    " + "sub "+ getReg(instruction.getResult(),regSubLhs.getNum(),regSubRhs.getNum()) + ", " + regSubLhs + ", " + regSubRhs + "        #  " + instruction);
                        usedTimeMap.put(instruction.getLHS(),usedTimeMap.get(instruction.getLHS()) - 1);
                        usedTimeMap.put(instruction.getRHS(),usedTimeMap.get(instruction.getRHS()) - 1);
                    }
                    break;
                //MUL
                case MUL:
                    Reg regMulLhs;
                    Reg regMulRhs;
                    regMulLhs = getReg(instruction.getLHS(),regMaxNum,regMaxNum);
                    regMulRhs = getReg(instruction.getRHS(),regMulLhs.getNum(),regMaxNum);
                    writeList.add("    " + "mul "+ getReg(instruction.getResult(),regMulLhs.getNum(),regMulRhs.getNum()) + ", " + regMulLhs + ", " + regMulRhs + "        #  " + instruction);
                    usedTimeMap.put(instruction.getLHS(),usedTimeMap.get(instruction.getLHS()) - 1);
                    usedTimeMap.put(instruction.getRHS(),usedTimeMap.get(instruction.getRHS()) - 1);
                    break;
                default:
                    break;
            }
        }
        if(depth != 0){
            writeList.add(0,"    addi sp, sp, -" + depth);
        }
        writeList.add(0,".text");
        if(depth != 0){
            writeList.add(writeList.size() - 1,"    addi sp, sp, " + depth);
        }
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path,writeList);
    }
}

