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
* -> FunctionDec: main
12: ST 0,-1(5) Store return address
* -> CompoundExp
* Allocating local variable: x
* Allocating local variable: fac
* -> AssignExp
* -> SimpleVar: x
13: LDA 0,-2(5) Load address of x
* <- SimpleVar
14: IN 0,0,0 Built-in input
15: ST 0,-2(5) Store assignment result
* <- AssignExp
* -> AssignExp
* -> SimpleVar: fac
16: LDA 0,-3(5) Load address of fac
* <- SimpleVar
* -> IntExp
17: LDC 0,1(0) Load constant 1
* <- IntExp
18: ST 0,-3(5) Store assignment result
* <- AssignExp
* -> WhileExp
* -> OpExp
* -> SimpleVar: x
19: LD 0,-2(5) Load value of x
* <- SimpleVar
20: ST 0,-4(5) Push left operand
* -> IntExp
21: LDC 0,1(0) Load constant 1
* <- IntExp
22: LD 1,-4(5) Load left operand
23: SUB 0,1,0 Subtract for GT
24: JGT 0,2(7) Jump if greater than
25: LDC 0,0(0) False case
26: LDA 7,1(7) Unconditional jump
27: LDC 0,1(0) True case
* <- OpExp
* -> CompoundExp
* -> AssignExp
* -> SimpleVar: fac
29: LDA 0,-3(5) Load address of fac
* <- SimpleVar
* -> OpExp
* -> SimpleVar: fac
30: LD 0,-3(5) Load value of fac
* <- SimpleVar
31: ST 0,-4(5) Push left operand
* -> SimpleVar: x
32: LD 0,-2(5) Load value of x
* <- SimpleVar
33: LD 1,-4(5) Load left operand
34: MUL 0,1,0 Multiply
* <- OpExp
35: ST 0,-3(5) Store assignment result
* <- AssignExp
* -> AssignExp
* -> SimpleVar: x
36: LDA 0,-2(5) Load address of x
* <- SimpleVar
* -> OpExp
* -> SimpleVar: x
37: LD 0,-2(5) Load value of x
* <- SimpleVar
38: ST 0,-4(5) Push left operand
* -> IntExp
39: LDC 0,1(0) Load constant 1
* <- IntExp
40: LD 1,-4(5) Load left operand
41: SUB 0,1,0 Subtract
* <- OpExp
42: ST 0,-2(5) Store assignment result
* <- AssignExp
* <- CompoundExp
43: LDA 7,-25(7) Jump back to test
28: JEQ 0,15(7) Exit while loop
* <- WhileExp
* -> SimpleVar: fac
44: LD 0,-3(5) Load value of fac
* <- SimpleVar
45: OUT 0,0,0 Built-in output
* <- CompoundExp
46: LD 7,-1(5) Return to caller
11: LDA 7,35(7) Jump around function body
* <- FunctionDec: main
* Finale
47: ST 5,0(5) Push old frame pointer
48: LDA 5,0(5) Establish new frame pointer
49: LDA 0,1(7) Load return pointer
50: LDA 7,-39(7) Jump to main function
51: LD 5,0(5) Pop frame pointer
52: HALT 0,0,0 Halt
