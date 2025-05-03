* Prelude
0: LDC 6,55(0) Load GP with max address
1: LDA 5,0(6) Copy GP to FP
2: ST 0,0(0) Clear location 0
* Input routine
4: ST 0,-1(5) Store return address
5: IN 0,0,0 Input
6: LD 7,-1(5) Return from input
* Output routine
7: ST 0,-1(5) Store return address
8: LD 0,-2(5) Load output value
9: OUT 0,0,0 Output
10: LD 7,-1(5) Return from output
3: LDA 7,7(7) Jump around I/O routines
* End of prelude
* Allocating global variable: y
* -> FunctionDec: gcd
12: ST 0,-1(5) Store return address
* Allocating local variable: u
* Allocating local variable: v
* -> CompoundExp
* -> OpExp
* -> SimpleVar: v
13: LD 0,-3(5) Load value of v
* <- SimpleVar
14: ST 0,-4(5) Push left operand
* -> IntExp
15: LDC 0,0(0) Load constant 0
* <- IntExp
16: LD 1,-4(5) Load left operand
17: SUB 0,1,0 Subtract for EQ
18: JEQ 0,2(7) Jump if equal
19: LDC 0,0(0) False case
20: LDA 7,1(7) Unconditional jump
21: LDC 0,1(0) True case
* <- OpExp
* -> ReturnExp
* -> SimpleVar: u
23: LD 0,-2(5) Load value of u
* <- SimpleVar
24: LD 7,-1(5) Return to caller
* <- ReturnExp
* -> ReturnExp
* -> CallExp: gcd
* -> SimpleVar: v
26: LD 0,-3(5) Load value of v
* <- SimpleVar
27: ST 0,-6(5) Push argument
* -> OpExp
* -> OpExp
* -> OpExp
* -> SimpleVar: u
28: LD 0,-2(5) Load value of u
* <- SimpleVar
29: ST 0,-7(5) Push left operand
* -> SimpleVar: u
30: LD 0,-2(5) Load value of u
* <- SimpleVar
31: LD 1,-7(5) Load left operand
32: SUB 0,1,0 Subtract
* <- OpExp
33: ST 0,-7(5) Push left operand
* -> SimpleVar: v
34: LD 0,-3(5) Load value of v
* <- SimpleVar
35: LD 1,-7(5) Load left operand
36: DIV 0,1,0 Divide
* <- OpExp
37: ST 0,-7(5) Push left operand
* -> SimpleVar: v
38: LD 0,-3(5) Load value of v
* <- SimpleVar
39: LD 1,-7(5) Load left operand
40: MUL 0,1,0 Multiply
* <- OpExp
41: ST 0,-7(5) Push argument
42: ST 5,-4(5) Push old FP
43: LDA 5,-4(5) Set new FP
44: LDA 0,1(7) Load return pointer
45: LDA 7,-34(7) Jump to function: gcd
46: LD 5,0(5) Restore FP
* <- CallExp
47: LD 7,-1(5) Return to caller
* <- ReturnExp
22: JEQ 0,3(7) If AC==0, jump to else
25: LDA 7,22(7) Skip else part
* <- CompoundExp
48: LD 7,-1(5) Return to caller
11: LDA 7,37(7) Jump around function body
* <- FunctionDec: gcd
* -> FunctionDec: main
50: ST 0,-1(5) Store return address
* -> CompoundExp
* Allocating local variable: x
* -> AssignExp
* -> SimpleVar: x
51: LDA 0,-2(5) Load address of x
* <- SimpleVar
52: IN 0,0,0 Built-in input
53: ST 0,-2(5) Store assignment result
* <- AssignExp
* -> AssignExp
* -> SimpleVar: y
54: LDA 0,0(6) Load address of y
* <- SimpleVar
* -> IntExp
55: LDC 0,10(0) Load constant 10
* <- IntExp
56: ST 0,0(6) Store assignment result
* <- AssignExp
* -> CallExp: gcd
* -> SimpleVar: x
57: LD 0,-2(5) Load value of x
* <- SimpleVar
58: ST 0,-5(5) Push argument
* -> SimpleVar: y
59: LD 0,0(6) Load value of y
* <- SimpleVar
60: ST 0,-6(5) Push argument
61: ST 5,-3(5) Push old FP
62: LDA 5,-3(5) Set new FP
63: LDA 0,1(7) Load return pointer
64: LDA 7,-53(7) Jump to function: gcd
65: LD 5,0(5) Restore FP
* <- CallExp
66: OUT 0,0,0 Built-in output
* <- CompoundExp
67: LD 7,-1(5) Return to caller
49: LDA 7,18(7) Jump around function body
* <- FunctionDec: main
* Finale
68: ST 5,0(5) Push old frame pointer
69: LDA 5,0(5) Establish new frame pointer
70: LDA 0,1(7) Load return pointer
71: LDA 7,-22(7) Jump to main function
72: LD 5,0(5) Pop frame pointer
73: HALT 0,0,0 Halt
