       PROCESS NODLL,NODYNAM,TEST(NOSEP),NOCICS,NOSQL,PGMN(LU)
      *+---------------------------------------------------------------+
      *| Tepscmor                                                      |
      *| PRODUCT: IBM DEVELOPER FOR Z/OS                               |
      *| COMPONENT: IBM Z/OS AUTOMATED UNIT TESTING FRAMEWORK (ZUNIT)  |
      *|   FOR ENTERPRISE COBOL AND PL/I                               |
      *| PROGRAM: ENTERPRISE COBOL ZUNIT TEST CASE FOR DYNAMIC RUNNER  |
      *| DATE GENERATED: 07/27/2020 14:15                              |
      *| ID: bec0ffa9-6140-4d81-9798-8611ae26c173                      |
      *+---------------------------------------------------------------+
      *+---------------------------------------------------------------+
      *| TEST_TEST3                                                    |
      *|     THIS PROGRAM IS FOR TEST TEST3  - regi
      *+---------------------------------------------------------------+
       IDENTIFICATION DIVISION.
       PROGRAM-ID. 'TEST_TEST3'.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 PROGRAM-NAME   PIC X(8)  VALUE 'EPSCMORT'.
       01 BZ-ASSERT.
         03 MESSAGE-LEN PIC S9(4) COMP-4 VALUE 24.
         03 MESSAGE-TXT PIC X(254) VALUE 'HELLO FROM TEST CALLBACK'.
       01  BZ-P1 PIC S9(9) COMP-4 VALUE 4.
       01  BZ-P2 PIC S9(9) COMP-4 VALUE 2001.
       01  BZ-P3 PIC X(3) VALUE 'AZU'.
       01 BZ-TRACE.
         03 TRACE-LEN       PIC S9(4) COMP-4 VALUE 5.
         03 TRACE-TXT       PIC X(254) VALUE 'TRACE'.
       01 BZUASSRT          PIC X(8) VALUE 'BZUASSRT'.
       01 BZUTRACE          PIC X(8) VALUE 'BZUTRACE'.
       01 ASSERT-ST.
         03 ASSERT-RC PIC 9(9) BINARY VALUE 4.
         03 ASSERT-TEXT PIC 9(4) BINARY VALUE 0.
       01 AZ-TEST-NAME-LEN       PIC S9(9) COMP-5.
       1 AZ-TEST-EXPECTED-DATA-VALUE.
          3 ZUT00000000.
            5 PIC X(1) DISPLAY VALUE '3'.
          3 ZUT00000001.
            5 PIC X(4) VALUE X'00000000'.
       01 AZ-COMPARE.
         03 AZ-COMPARE-ITEM-NAME-PTR  POINTER.
         03 AZ-COMPARE-ITEM-NAME-LEN  PIC S9(9) COMP-5.
         03 AZ-COMPARE-ITEM-VALUE-PTR POINTER.
         03 AZ-COMPARE-ITEM-VALUE-LEN PIC S9(9) COMP-5.
         03 AZ-COMPARE-ITEM-EXP-VALUE-PTR POINTER.
         03 AZ-COMPARE-ITEM-EXP-VALUE-LEN PIC S9(9) COMP-5.
       LOCAL-STORAGE SECTION.
       1 AZ-COMPARE-ITEM-NAMES.
         3 ZUT00000002.
            5 PIC X(20) DISPLAY VALUE 'PROCESS-INDICATOR OF'.
            5 PIC X(12) DISPLAY VALUE ' DFHCOMMAREA'.
         3 ZUT00000004.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-PRINCIPLE-DA'.
            5 PIC X(17) DISPLAY VALUE 'TA OF DFHCOMMAREA'.
         3 ZUT00000006.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-NUMBER-OF-YE'.
            5 PIC X(18) DISPLAY VALUE 'ARS OF DFHCOMMAREA'.
         3 ZUT00000008.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-NUMBER-OF-MO'.
            5 PIC X(19) DISPLAY VALUE 'NTHS OF DFHCOMMAREA'.
         3 ZUT0000000A.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-QUOTED-INTER'.
            5 PIC X(20) DISPLAY VALUE 'EST-RATE OF DFHCOMMA'.
            5 PIC X(3) DISPLAY VALUE 'REA'.
         3 ZUT0000000C.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-RETURN-MONTH'.
            5 PIC X(20) DISPLAY VALUE '-PAYMENT OF DFHCOMMA'.
            5 PIC X(3) DISPLAY VALUE 'REA'.
         3 ZUT0000000E.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-PROGRAM-RETC'.
            5 PIC X(18) DISPLAY VALUE 'ODE OF DFHCOMMAREA'.
       1 AZ-COMPARE-WORK-ITEMS.
          3 ZUT00000003 PIC X(1) OCCURS 2.
          3 ZUT00000005 PIC -9(9).9(2) OCCURS 2.
          3 ZUT00000007 PIC -9(4) OCCURS 2.
          3 ZUT00000009 PIC -9(4) OCCURS 2.
          3 ZUT0000000B PIC -9(2).9(3) OCCURS 2.
          3 ZUT0000000D PIC -9(7).9(2) OCCURS 2.
          3 ZUT0000000F PIC X(8) OCCURS 2.
       01 AZ-CONVERT.
         03 AZ-CONVERT-HEXIN  PIC X(1).
         03 AZ-CONVERT-HEXVAL PIC X(2).
         03 AZ-HEXSTR PIC X(16) VALUE "0123456789ABCDEF".
         03 AZ-DEC  PIC S9(4) COMP VALUE 0.
         03 FILLER REDEFINES AZ-DEC.
           05 FILLER PIC X.
           05 AZ-DECBYTE PIC X.
         03 AZ-I PIC S9(8) COMP.
         03 AZ-J PIC S9(8) COMP.
         03 AZ-Q PIC S9(8) COMP.
         03 AZ-R PIC S9(8) COMP.
         03 AZ-Q1 PIC S9(8) COMP.
         03 AZ-R1 PIC S9(8) COMP.
       LINKAGE SECTION.
       01 AZ-TEST                   PIC X(80).
       01 AZ-ARG-LIST.
         03 ARG-LENGTH PIC 9(4) COMP-4.
         03 ARG-DATA PIC X(256).
       01 AZ-COMPARE-ITEM-NAME      PIC X(254).
       01 AZ-COMPARE-ITEM-VALUE     PIC X(254).
       01 AZ-COMPARE-ITEM-EXP-VALUE PIC X(254).
       1 DFHEIBLK.
         2 EIBTIME PICTURE S9(7) USAGE COMPUTATIONAL-3.
         2 EIBDATE PICTURE S9(7) USAGE COMPUTATIONAL-3.
         2 EIBTRNID PICTURE X(4).
         2 EIBTASKN PICTURE S9(7) USAGE COMPUTATIONAL-3.
         2 EIBTRMID PICTURE X(4).
         2 DFHEIGDI PICTURE S9(4) USAGE COMPUTATIONAL-5.
         2 EIBCPOSN PICTURE S9(4) USAGE COMPUTATIONAL-5.
         2 EIBCALEN PICTURE S9(4) USAGE COMPUTATIONAL-5.
         2 EIBAID PICTURE X(1).
         2 EIBFN PICTURE X(2).
         2 EIBRCODE PICTURE X(6).
         2 EIBDS PICTURE X(8).
         2 EIBREQID PICTURE X(8).
         2 EIBRSRCE PICTURE X(8).
         2 EIBSYNC PICTURE X.
         2 EIBFREE PICTURE X.
         2 EIBRECV PICTURE X.
         2 EIBSEND PICTURE X.
         2 EIBATT PICTURE X.
         2 EIBEOC PICTURE X.
         2 EIBFMH PICTURE X.
         2 EIBCOMPL PICTURE X(1).
         2 EIBSIG PICTURE X(1).
         2 EIBCONF PICTURE X(1).
         2 EIBERR PICTURE X(1).
         2 EIBERRCD PICTURE X(4).
         2 EIBSYNRB PICTURE X.
         2 EIBNODAT PICTURE X.
         2 EIBRESP PICTURE S9(8) USAGE COMPUTATIONAL.
         2 EIBRESP2 PICTURE S9(8) USAGE COMPUTATIONAL.
         2 EIBRLDBK PICTURE X(1).
       1 DFHCOMMAREA.
         10 PROCESS-INDICATOR PIC X.
         10 EPSPCOM-PRINCIPLE-DATA PIC S9(9)V99 COMP.
         10 EPSPCOM-NUMBER-OF-YEARS PIC S9(4) COMP.
         10 EPSPCOM-NUMBER-OF-MONTHS PIC S9(4) COMP.
         10 EPSPCOM-QUOTED-INTEREST-RATE PIC S9(2)v9(3) COMP.
         10 EPSPCOM-YEAR-MONTH-IND PIC X.
         10 EPSPCOM-RETURN-MONTH-PAYMENT PIC S9(7)V99 COMP.
         10 EPSPCOM-ERRMSG PIC X(80).
         10 EPSPCOM-PROGRAM-RETCODE PIC 9(4).
         88 EPS02-REQUEST-SUCCESS VALUE 0.
          10 EPSPCOM-PROGRAM-RETCODE-AZ REDEFINES
           EPSPCOM-PROGRAM-RETCODE.
          11 PIC X(4) DISPLAY.
         10 EPSPCOM-PROGRAM-RETCODE-RDF REDEFINES
           EPSPCOM-PROGRAM-RETCODE PIC X(4).
       PROCEDURE DIVISION USING AZ-TEST
           DFHEIBLK DFHCOMMAREA.
      * START
           DISPLAY 'TEST_TEST3 STARTED...'
           MOVE 0 TO AZ-TEST-NAME-LEN.
           INSPECT AZ-TEST TALLYING AZ-TEST-NAME-LEN FOR
           CHARACTERS BEFORE INITIAL SPACE.
      * INITIALIZE PARAMETER
           PERFORM INITIALIZE-PARM
      * SET AREA ADDRESS TO POINTER
      * SET INPUT VALUE
           MOVE 0 TO RETURN-CODE.
      * CALL TEST PROGRAM
           DISPLAY 'CALL EPSCMORT'
           CALL PROGRAM-NAME
           USING DFHEIBLK DFHCOMMAREA
           .
      * EVALUATE OUTPUT VALUE
           MOVE 0 TO RETURN-CODE
           IF PROCESS-INDICATOR OF DFHCOMMAREA = ZUT00000000 THEN
             CONTINUE
           ELSE
             MOVE PROCESS-INDICATOR OF DFHCOMMAREA TO ZUT00000003(1)
             MOVE ZUT00000000 TO ZUT00000003(2)
             SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000002
             MOVE LENGTH OF ZUT00000002 TO AZ-COMPARE-ITEM-NAME-LEN
             SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF ZUT00000003(1)
             MOVE 1 TO AZ-COMPARE-ITEM-VALUE-LEN
             SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000003(2)
             MOVE 1 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
             PERFORM THROW-ASSERTION
           END-IF
           IF EPSPCOM-PRINCIPLE-DATA OF DFHCOMMAREA = 0 THEN
             CONTINUE
           ELSE
             MOVE EPSPCOM-PRINCIPLE-DATA OF DFHCOMMAREA TO
           ZUT00000005(1)
             MOVE 0 TO ZUT00000005(2)
             SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000004
             MOVE LENGTH OF ZUT00000004 TO AZ-COMPARE-ITEM-NAME-LEN
             SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF ZUT00000005(1)
             MOVE 13 TO AZ-COMPARE-ITEM-VALUE-LEN
             SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000005(2)
             MOVE 13 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
             PERFORM THROW-ASSERTION
           END-IF
           IF EPSPCOM-NUMBER-OF-YEARS OF DFHCOMMAREA = 0 THEN
             CONTINUE
           ELSE
             MOVE EPSPCOM-NUMBER-OF-YEARS OF DFHCOMMAREA TO
           ZUT00000007(1)
             MOVE 0 TO ZUT00000007(2)
             SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000006
             MOVE LENGTH OF ZUT00000006 TO AZ-COMPARE-ITEM-NAME-LEN
             SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF ZUT00000007(1)
             MOVE 5 TO AZ-COMPARE-ITEM-VALUE-LEN
             SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000007(2)
             MOVE 5 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
             PERFORM THROW-ASSERTION
           END-IF
           IF EPSPCOM-NUMBER-OF-MONTHS OF DFHCOMMAREA = 0 THEN
             CONTINUE
           ELSE
             MOVE EPSPCOM-NUMBER-OF-MONTHS OF DFHCOMMAREA TO
           ZUT00000009(1)
             MOVE 0 TO ZUT00000009(2)
             SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000008
             MOVE LENGTH OF ZUT00000008 TO AZ-COMPARE-ITEM-NAME-LEN
             SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF ZUT00000009(1)
             MOVE 5 TO AZ-COMPARE-ITEM-VALUE-LEN
             SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000009(2)
             MOVE 5 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
             PERFORM THROW-ASSERTION
           END-IF
           IF EPSPCOM-QUOTED-INTEREST-RATE OF DFHCOMMAREA = 0 THEN
             CONTINUE
           ELSE
             MOVE EPSPCOM-QUOTED-INTEREST-RATE OF DFHCOMMAREA TO
           ZUT0000000B(1)
             MOVE 0 TO ZUT0000000B(2)
             SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT0000000A
             MOVE LENGTH OF ZUT0000000A TO AZ-COMPARE-ITEM-NAME-LEN
             SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF ZUT0000000B(1)
             MOVE 7 TO AZ-COMPARE-ITEM-VALUE-LEN
             SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT0000000B(2)
             MOVE 7 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
             PERFORM THROW-ASSERTION
           END-IF
           IF EPSPCOM-RETURN-MONTH-PAYMENT OF DFHCOMMAREA = 0 THEN
             CONTINUE
           ELSE
             MOVE EPSPCOM-RETURN-MONTH-PAYMENT OF DFHCOMMAREA TO
           ZUT0000000D(1)
             MOVE 0 TO ZUT0000000D(2)
             SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT0000000C
             MOVE LENGTH OF ZUT0000000C TO AZ-COMPARE-ITEM-NAME-LEN
             SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF ZUT0000000D(1)
             MOVE 11 TO AZ-COMPARE-ITEM-VALUE-LEN
             SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT0000000D(2)
             MOVE 11 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
             PERFORM THROW-ASSERTION
           END-IF
           IF EPSPCOM-PROGRAM-RETCODE-AZ OF DFHCOMMAREA = ZUT00000001
           THEN
             CONTINUE
           ELSE
             PERFORM VARYING AZ-I FROM 1 BY 1 UNTIL AZ-I > 4
               MOVE EPSPCOM-PROGRAM-RETCODE-AZ OF DFHCOMMAREA(AZ-I:1)
           TO AZ-CONVERT-HEXIN
               PERFORM CONVERT
               COMPUTE AZ-J = AZ-I * 2 - 1
               MOVE AZ-CONVERT-HEXVAL TO ZUT0000000F(1)(AZ-J:2)
             END-PERFORM
             PERFORM VARYING AZ-I FROM 1 BY 1 UNTIL AZ-I > 4
               MOVE ZUT00000001(AZ-I:1) TO AZ-CONVERT-HEXIN
               PERFORM CONVERT
               COMPUTE AZ-J = AZ-I * 2 - 1
               MOVE AZ-CONVERT-HEXVAL TO ZUT0000000F(2)(AZ-J:2)
             END-PERFORM
             SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT0000000E
             MOVE LENGTH OF ZUT0000000E TO AZ-COMPARE-ITEM-NAME-LEN
             SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF ZUT0000000F(1)
             MOVE 8 TO AZ-COMPARE-ITEM-VALUE-LEN
             SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT0000000F(2)
             MOVE 8 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
             PERFORM THROW-ASSERTION
           END-IF
      * END
           DISPLAY 'TEST_TEST3 SUCCESSFUL.'
           GOBACK.
       INITIALIZE-PARM.
           EXIT.
       CONVERT.
           MOVE AZ-CONVERT-HEXIN TO AZ-DECBYTE
           DIVIDE AZ-DEC BY 16 GIVING AZ-Q REMAINDER AZ-R
           COMPUTE AZ-Q1 = AZ-Q + 1
           COMPUTE AZ-R1 = AZ-R + 1
           MOVE AZ-HEXSTR(AZ-Q1:1) TO AZ-CONVERT-HEXVAL(1:1)
           MOVE AZ-HEXSTR(AZ-R1:1) TO AZ-CONVERT-HEXVAL(2:1)
           EXIT.
       THROW-ASSERTION.
           MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
           STRING 'COMPARE FAILED IN PROCEDURE DIVISION.'
             DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
             WITH POINTER MESSAGE-LEN OF BZ-ASSERT
           END-STRING
           SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
           SET ADDRESS OF AZ-COMPARE-ITEM-NAME TO
           AZ-COMPARE-ITEM-NAME-PTR.
           SET ADDRESS OF AZ-COMPARE-ITEM-VALUE TO
           AZ-COMPARE-ITEM-VALUE-PTR.
           SET ADDRESS OF AZ-COMPARE-ITEM-EXP-VALUE TO
           AZ-COMPARE-ITEM-EXP-VALUE-PTR.
           DISPLAY '****************************************************
      -    '****************************'
           DISPLAY 'AZU2001W THE TEST "' AZ-TEST(1:AZ-TEST-NAME-LEN) '"
      -    'FAILED DUE TO AN ASSERTION.'
           DISPLAY 'AZU1101I ' MESSAGE-TXT OF BZ-ASSERT(1:MESSAGE-LEN
           OF BZ-ASSERT)
           DISPLAY ' DATA ITEM NAME : '
           AZ-COMPARE-ITEM-NAME(1:AZ-COMPARE-ITEM-NAME-LEN)
           DISPLAY '  VALUE         : '
           AZ-COMPARE-ITEM-VALUE(1:AZ-COMPARE-ITEM-VALUE-LEN)
           DISPLAY '  EXPECTED VALUE: '
           AZ-COMPARE-ITEM-EXP-VALUE(1:AZ-COMPARE-ITEM-EXP-VALUE-LEN)
           DISPLAY '****************************************************
      -    '****************************'
           CALL BZUASSRT USING BZ-P1 BZ-P2 BZ-P3 BZ-ASSERT
           MOVE 1 TO TRACE-LEN OF BZ-TRACE
           STRING 'ITEM NAME='
           AZ-COMPARE-ITEM-NAME(1:AZ-COMPARE-ITEM-NAME-LEN)
               DELIMITED BY SIZE INTO TRACE-TXT OF BZ-TRACE
               WITH POINTER TRACE-LEN OF BZ-TRACE
             END-STRING
           SUBTRACT 1 FROM TRACE-LEN OF BZ-TRACE
           CALL BZUTRACE USING BZ-TRACE
           MOVE 1 TO TRACE-LEN OF BZ-TRACE
           STRING 'VALUE='
           AZ-COMPARE-ITEM-VALUE(1:AZ-COMPARE-ITEM-VALUE-LEN)
               DELIMITED BY SIZE INTO TRACE-TXT OF BZ-TRACE
               WITH POINTER TRACE-LEN OF BZ-TRACE
             END-STRING
           SUBTRACT 1 FROM TRACE-LEN OF BZ-TRACE
           CALL BZUTRACE USING BZ-TRACE
           MOVE 1 TO TRACE-LEN OF BZ-TRACE
           STRING 'EXPECTED VALUE='
           AZ-COMPARE-ITEM-EXP-VALUE(1:AZ-COMPARE-ITEM-EXP-VALUE-LEN)
               DELIMITED BY SIZE INTO TRACE-TXT OF BZ-TRACE
               WITH POINTER TRACE-LEN OF BZ-TRACE
             END-STRING
           SUBTRACT 1 FROM TRACE-LEN OF BZ-TRACE
           CALL BZUTRACE USING BZ-TRACE
           EXIT.
       END PROGRAM TEST_TEST3.
      *+---------------------------------------------------------------+
      *| BZU_INIT                                                      |
      *|     INITIAL PROCEDURE                                         |
      *+---------------------------------------------------------------+
       IDENTIFICATION DIVISION.
       PROGRAM-ID. 'BZU_INIT'.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 AZ-TEST-NAME-LEN      PIC S9(9) COMP-5.
       01 AZ-TESTCASE-ID        PIC X(36)
           VALUE 'bec0ffa9-6140-4d81-9798-8611ae26c173'.
       LINKAGE SECTION.
       01 AZ-TEST               PIC X(80).
       01 AZ-TEST-ID            PIC X(80).
       PROCEDURE DIVISION USING AZ-TEST AZ-TEST-ID.
           MOVE 0 TO AZ-TEST-NAME-LEN.
           INSPECT AZ-TEST TALLYING AZ-TEST-NAME-LEN FOR
           CHARACTERS BEFORE INITIAL SPACE.
           DISPLAY 'BZU_INIT : ' AZ-TEST(1:AZ-TEST-NAME-LEN)
           MOVE AZ-TESTCASE-ID TO AZ-TEST-ID
           GOBACK.
       END PROGRAM BZU_INIT.
      *+---------------------------------------------------------------+
      *| BZU_TERM                                                      |
      *|     TERMINATION PROCEDURE                                     |
      *+---------------------------------------------------------------+
       IDENTIFICATION DIVISION.
       PROGRAM-ID. 'BZU_TERM'.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 AZ-TEST-NAME-LEN      PIC S9(9) COMP-5.
       LINKAGE SECTION.
       01 AZ-TEST               PIC X(80).
       PROCEDURE DIVISION USING AZ-TEST.
           MOVE 0 TO AZ-TEST-NAME-LEN.
           INSPECT AZ-TEST TALLYING AZ-TEST-NAME-LEN FOR
           CHARACTERS BEFORE INITIAL SPACE.
           DISPLAY 'BZU_TERM : ' AZ-TEST(1:AZ-TEST-NAME-LEN)
           GOBACK.
       END PROGRAM BZU_TERM.
      *+---------------------------------------------------------------+
      *| EPSNBRVL                                                      |
      *|                                                               |
      *+---------------------------------------------------------------+
       IDENTIFICATION DIVISION.
       PROGRAM-ID. 'PGM_EPSNBRVL'.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 BZ-ASSERT.
         03 MESSAGE-LEN PIC S9(4) COMP-4 VALUE 24.
         03 MESSAGE-TXT PIC X(254) VALUE 'HELLO FROM STUB CALLBACK'.
       01  BZ-P1 PIC S9(9) COMP-4 VALUE 4.
       01  BZ-P2 PIC S9(9) COMP-4 VALUE 2001.
       01  BZ-P3 PIC X(3) VALUE 'AZU'.
       01 BZ-TRACE.
         03 TRACE-LEN       PIC S9(4) COMP-4 VALUE 5.
         03 TRACE-TXT       PIC X(254) VALUE 'TRACE'.
       01 BZUASSRT          PIC X(8) VALUE 'BZUASSRT'.
       01 BZUTRACE          PIC X(8) VALUE 'BZUTRACE'.
       01 AZ-TEST-LEN       PIC S9(8) COMP.
       01 AZ-RECORD.
         03 AZ-RECORD-COUNT-OT PIC 9(5) COMP-5 VALUE 0.
         03 AZ-RECORD-COUNT-IN PIC 9(5) COMP-5 VALUE 0.
       LOCAL-STORAGE SECTION.
       LINKAGE SECTION.
       01 AZ-TEST                   PIC X(80).
       01 AZ-INFO-BLOCK.
          COPY BZUITERC.
       1 EPS-NUMBER-VALIDATION.
         3 EPSPARM-VALIDATE-DATA PIC X(13).
         3 EPSPARM-MAX-LENGTH PIC 99.
         3 EPSPARM-NUMBER PIC 9(13).
         3 EPSPARM-DECIMAL PIC V9(13).
         3 EPSPARM-BINARY-NUMBER PIC 9(9)V99 COMP.
         3 EPSPARM-RETURN-ERROR PIC X(80).
      *
       PROCEDURE DIVISION.
      * CHECK OUTPUT VALUE
           ENTRY "PGM_INPT_EPSCMORT_EPSNBRVL" USING
              AZ-TEST AZ-INFO-BLOCK
           EPS-NUMBER-VALIDATION.
           DISPLAY 'PGM_INPT_EPSCMORT_EPSNBRVL CHECK VALUES...'.
           MOVE 4 TO RETURN-CODE.
           MOVE 0 TO AZ-TEST-LEN.
           INSPECT AZ-TEST TALLYING AZ-TEST-LEN FOR
           CHARACTERS BEFORE INITIAL SPACE.
      * SET AREA ADDRESS TO POINTER
           MOVE ITER OF AZ-INFO-BLOCK TO AZ-RECORD-COUNT-OT
           EVALUATE AZ-TEST(1:AZ-TEST-LEN)
           WHEN SPACE
             CONTINUE
           WHEN 'TEST3'
             PERFORM P-OUTPUT-TEST3
           WHEN OTHER
             CONTINUE
           END-EVALUATE.
           PERFORM TEARDOWN.
      * SET INPUT VALUE
           ENTRY "PGM_OUTP_EPSCMORT_EPSNBRVL" USING
              AZ-TEST AZ-INFO-BLOCK
           EPS-NUMBER-VALIDATION.
           DISPLAY 'PGM_OUTP_EPSCMORT_EPSNBRVL INPUT VALUES...'.
           MOVE 0 TO RETURN-CODE.
           MOVE 0 TO AZ-TEST-LEN.
           INSPECT AZ-TEST TALLYING AZ-TEST-LEN FOR
           CHARACTERS BEFORE INITIAL SPACE.
      * SET AREA ADDRESS TO POINTER
           MOVE ITER OF AZ-INFO-BLOCK TO AZ-RECORD-COUNT-IN
           EVALUATE AZ-TEST(1:AZ-TEST-LEN)
           WHEN SPACE
             CONTINUE
           WHEN 'TEST3'
             PERFORM P-INPUT-TEST3
           WHEN OTHER
             CONTINUE
           END-EVALUATE.
           PERFORM TEARDOWN.
       TEARDOWN.
           DISPLAY 'PGM_EPSNBRVL END.'
           GOBACK.
       P-OUTPUT-TEST3.
           IF AZ-RECORD-COUNT-OT = 0 THEN
             CONTINUE
           ELSE
             CONTINUE
           END-IF.
           EXIT.
       P-INPUT-TEST3.
           IF AZ-RECORD-COUNT-IN = 0 THEN
             CONTINUE
           ELSE
             CONTINUE
           END-IF.
           EXIT.
       END PROGRAM 'PGM_EPSNBRVL'.
      *+---------------------------------------------------------------+
      *| EVALOPT                                                       |
      *|   FUNCTION TO EVALUATE THAT THE BIT OF OPTION DATA            |
      *|   (1) TAKE AND OF GROUP COMMON MASK AND OPTION IN ARG0        |
      *|   (2) CHECK IF THE GROUP MASK IS EQUAL TO (1)                 |
      *|       IF EQUAL,    RTN01 IS 0                                 |
      *|       IF NO EQUAL, RTN01 IS 1                                 |
      *+---------------------------------------------------------------+
       ID DIVISION.
       PROGRAM-ID. EVALOPT.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01  OUT1-REC.
         05 OUT1-DATA                PIC X(1) OCCURS 8.
       01 OUT1-DATA-R REDEFINES OUT1-REC.
         05 OUT1-DATA-UP             PIC X(4).
         05 OUT1-DATA-DOWN           PIC X(4).
       01  OUT2-REC.
         05  OUT2-DATA               PIC X(1) OCCURS 8.
       01  OUT2-DATA-R REDEFINES OUT2-REC.
         05 OUT2-DATA-UP             PIC X(4).
         05 OUT2-DATA-DOWN           PIC X(4).
       01  WORK1-REC.
         05  WORK1-DATA              PIC X(1) OCCURS 8.
       01  WORK1-DATA-R REDEFINES WORK1-REC.
         05 WORK1-DATA-UP            PIC X(4).
         05 WORK1-DATA-DOWN          PIC X(4).
       01  WORK-AREA.
         05  WORK-HEX-UP             PIC 9(4)  COMP.
         05  WORK-HEX-DOWN           PIC 9(4)  COMP.
       01  HEX-CHG-BEF.
         05  HEX-CHANGE-LV           PIC X(1) VALUE LOW-VALUE.
         05  HEX-CHANGE-BEFORE       PIC X(1).
       01  HEX-CHG-AFT      REDEFINES  HEX-CHG-BEF.
         05  HEX-CHANGE-AFTER        PIC 9(4)  COMP.
       01  TBL-CHANGE-DATA.
          05  FILLER                 PIC  X(004) VALUE '0000'.
          05  FILLER                 PIC  X(001) VALUE '0'.
          05  FILLER                 PIC  X(004) VALUE '0001'.
          05  FILLER                 PIC  X(001) VALUE '1'.
          05  FILLER                 PIC  X(004) VALUE '0010'.
          05  FILLER                 PIC  X(001) VALUE '2'.
          05  FILLER                 PIC  X(004) VALUE '0011'.
          05  FILLER                 PIC  X(001) VALUE '3'.
          05  FILLER                 PIC  X(004) VALUE '0100'.
          05  FILLER                 PIC  X(001) VALUE '4'.
          05  FILLER                 PIC  X(004) VALUE '0101'.
          05  FILLER                 PIC  X(001) VALUE '5'.
          05  FILLER                 PIC  X(004) VALUE '0110'.
          05  FILLER                 PIC  X(001) VALUE '6'.
          05  FILLER                 PIC  X(004) VALUE '0111'.
          05  FILLER                 PIC  X(001) VALUE '7'.
          05  FILLER                 PIC  X(004) VALUE '1000'.
          05  FILLER                 PIC  X(001) VALUE '8'.
          05  FILLER                 PIC  X(004) VALUE '1001'.
          05  FILLER                 PIC  X(001) VALUE '9'.
          05  FILLER                 PIC  X(004) VALUE '1010'.
          05  FILLER                 PIC  X(001) VALUE 'A'.
          05  FILLER                 PIC  X(004) VALUE '1011'.
          05  FILLER                 PIC  X(001) VALUE 'B'.
          05  FILLER                 PIC  X(004) VALUE '1100'.
          05  FILLER                 PIC  X(001) VALUE 'C'.
          05  FILLER                 PIC  X(004) VALUE '1101'.
          05  FILLER                 PIC  X(001) VALUE 'D'.
          05  FILLER                 PIC  X(004) VALUE '1110'.
          05  FILLER                 PIC  X(001) VALUE 'E'.
          05  FILLER                 PIC  X(004) VALUE '1111'.
          05  FILLER                 PIC  X(001) VALUE 'F'.
          01  TBL-DATA REDEFINES TBL-CHANGE-DATA.
           05  TBL-CHG  OCCURS  16 TIMES.
             10  TBL-BIT-CHAR        PIC  X(004).
             10  TBL-HEX-CHAR        PIC  X(001).
       01 BIT-COUNT                  PIC 9(1).
       01 I                          PIC S9(8) COMP.
       LINKAGE SECTION.
       01 G-MASK.
         03 D-G-MASK                 PIC X(1) OCCURS 19.
       01 COM-MASK.
         03 D-COM-MASK               PIC X(1) OCCURS 19.
       01 O-ARG0.
         03 D-O-ARG0                 PIC X(1) OCCURS 19.
       01 BYTE-COUNT                 PIC S9(8) COMP.
       01 RTN01                      PIC 9(1).
       PROCEDURE DIVISION USING G-MASK COM-MASK O-ARG0 BYTE-COUNT
            RTN01.
            MOVE 0 TO RTN01
            PERFORM VARYING I FROM 1 BY 1 UNTIL I > BYTE-COUNT
              PERFORM ANDCOMMASK
              IF RTN01 = 1 THEN
                GOBACK
              END-IF
            END-PERFORM.
            EXIT PROGRAM.
       ANDCOMMASK.
      * CONVERT GROUP COMMON MASK TO BIT
            MOVE D-COM-MASK(I) TO HEX-CHANGE-BEFORE.
            DIVIDE 16 INTO HEX-CHANGE-AFTER GIVING WORK-HEX-UP
                                         REMAINDER WORK-HEX-DOWN.
            MOVE TBL-BIT-CHAR(WORK-HEX-UP + 1)   TO OUT1-DATA-UP.
            MOVE TBL-BIT-CHAR(WORK-HEX-DOWN + 1) TO OUT1-DATA-DOWN.
      * CONVERT OPTION IN ARG0 TO BIT
            MOVE D-O-ARG0(I) TO HEX-CHANGE-BEFORE.
            DIVIDE 16 INTO HEX-CHANGE-AFTER GIVING WORK-HEX-UP
                                         REMAINDER WORK-HEX-DOWN.
            MOVE TBL-BIT-CHAR(WORK-HEX-UP + 1)   TO OUT2-DATA-UP.
            MOVE TBL-BIT-CHAR(WORK-HEX-DOWN + 1) TO OUT2-DATA-DOWN.
      * CREATE EVAL BIT FROM GROUP COMMON MASK BIT AND ARG0 BIT
            PERFORM VARYING BIT-COUNT FROM 1 BY 1 UNTIL BIT-COUNT > 8
              IF OUT1-DATA(BIT-COUNT) = '1' AND
                 OUT2-DATA(BIT-COUNT) = '1' THEN
                MOVE '1' TO WORK1-DATA(BIT-COUNT)
              ELSE
                MOVE '0' TO WORK1-DATA(BIT-COUNT)
              END-IF
            END-PERFORM.
      * CONVERT GROUP MASK TO BIT DATA
            MOVE D-G-MASK(I) TO HEX-CHANGE-BEFORE.
            DIVIDE 16 INTO HEX-CHANGE-AFTER GIVING WORK-HEX-UP
                                         REMAINDER WORK-HEX-DOWN.
            MOVE TBL-BIT-CHAR(WORK-HEX-UP + 1)   TO OUT1-DATA-UP.
            MOVE TBL-BIT-CHAR(WORK-HEX-DOWN + 1) TO OUT1-DATA-DOWN.
      * CHECK IF EQUAL BETWEEN EVAL BIT AND GROUP MASK BIT
            IF WORK1-DATA-UP = OUT1-DATA-UP AND
               WORK1-DATA-DOWN = OUT1-DATA-DOWN THEN
              CONTINUE
            ELSE
              MOVE 1 TO RTN01
            END-IF
            EXIT.
       END PROGRAM 'EVALOPT'.
      *+---------------------------------------------------------------+
      *| AZU_GENERIC_CICS                                              |
      *|   GENERIC CICS CALLBACK EXIT POINT                            |
      *+---------------------------------------------------------------+
       IDENTIFICATION DIVISION.
       PROGRAM-ID. 'AZU_GENERIC_CICS'.
       PROCEDURE DIVISION.
      * CHECK OUTPUT VALUE
      * CICS_INPT.
           ENTRY 'CICS_INPT'.
           DISPLAY 'CICS_INPT ...'
           MOVE 4 TO RETURN-CODE.
           GOBACK.
      * CICS_OUTP.
           ENTRY 'CICS_OUTP'.
           DISPLAY 'CICS_OUTP ...'
           MOVE 4 TO RETURN-CODE.
           GOBACK.
      * CICS_INPT_0E08 FOR RETURN.
           ENTRY 'CICS_INPT_0E08'.
           DISPLAY 'CICS_INPT_0E08 ...'
           MOVE 0 TO RETURN-CODE.
           GOBACK.
       END PROGRAM 'AZU_GENERIC_CICS'.
      *+---------------------------------------------------------------+
      *| AZU_GENERIC_DB2                                               |
      *|   GENERIC DB2 CALLBACK EXIT POINT                             |
      *+---------------------------------------------------------------+
       IDENTIFICATION DIVISION.
       PROGRAM-ID. 'AZU_GENERIC_DB2'.
       PROCEDURE DIVISION.
      * CHECK OUTPUT VALUE
      * DB2_INPT.
           ENTRY 'DB2_INPT'.
           DISPLAY 'DB2_INPT ...'
           MOVE 4 TO RETURN-CODE.
           GOBACK.
      * DB2_OUTP.
           ENTRY 'DB2_OUTP'.
           DISPLAY 'DB2_OUTP ...'
           MOVE 4 TO RETURN-CODE.
           GOBACK.
       END PROGRAM 'AZU_GENERIC_DB2'.
      *+---------------------------------------------------------------+
      *| PROGRAM FOR EXEC CICS RETURN                                  |
      *|    FUNCTION CODE: 0E08                                        |
      *|                                                               |
      *+---------------------------------------------------------------+
       IDENTIFICATION DIVISION.
       PROGRAM-ID. 'CICS_0E08_EPSCMORT'.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 BZ-ASSERT.
         03 MESSAGE-LEN PIC S9(4) COMP-4 VALUE 24.
         03 MESSAGE-TXT PIC X(254) VALUE 'HELLO FROM CICS CALLBACK'.
       01  BZ-P1 PIC S9(9) COMP-4 VALUE 4.
       01  BZ-P2 PIC S9(9) COMP-4 VALUE 2001.
       01  BZ-P3 PIC X(3) VALUE 'AZU'.
       01 BZ-TRACE.
         03 TRACE-LEN       PIC S9(4) COMP-4 VALUE 5.
         03 TRACE-TXT       PIC X(254) VALUE 'TRACE'.
       01 BZUASSRT          PIC X(8) VALUE 'BZUASSRT'.
       01 BZUTRACE          PIC X(8) VALUE 'BZUTRACE'.
       01 AZ-TEST-LEN       PIC S9(8) COMP.
       01 AZ-RECORD.
         03 AZ-RECORD-COUNT-OT OCCURS 2 PIC 9(5) COMP-5 VALUE 0.
         03 AZ-RECORD-COUNT-IN OCCURS 2 PIC 9(5) COMP-5 VALUE 0.
       01 AZ-RECORD-PTR POINTER.
       01 AZ-RECORD-PTR-VALUE REDEFINES
            AZ-RECORD-PTR PIC S9(9) COMP-5.
       01 AZ-OPT-MASK-DATA2  PIC X(2).
       01 AZ-OPT-MASK-DATA9  PIC X(9).
       01 AZ-OPT-MASK-DATA11 PIC X(11).
       01 AZ-OPT-MASK-DATA19 PIC X(19).
       01 AZ-OPT-BYTECOUNT   PIC S9(8) COMP.
       01 AZ-OPT-RC          PIC 9(1) VALUE 0.
       01 AZ-OPT-COMMASK.
         03  AZ-OPT-COMMASK-DATA2  PIC X(2) OCCURS 1.
         03  AZ-OPT-COMMASK-DATA9  PIC X(9) OCCURS 1.
         03  AZ-OPT-COMMASK-DATA11 PIC X(11) OCCURS 1.
         03  AZ-OPT-COMMASK-DATA19 PIC X(19) OCCURS 1.
       01 AZ-LINE-BYTE      PIC S9(2) COMP.
       01 AZ-LINE-NUM       PIC 9(5).
       01 AZ-LINE-NUM-R   REDEFINES AZ-LINE-NUM.
         03 AZ-LINE-NUM-RD  PIC 9(1) OCCURS 5.
       01 AZ-LINE-I         PIC S9(8) COMP.
       01 AZ-LINE-J         PIC S9(8) COMP.
       01 AZ-COMPARE.
         03 AZ-COMPARE-ITEM-NAME-PTR POINTER.
         03 AZ-COMPARE-ITEM-NAME-LEN PIC S9(9) COMP-5.
         03 AZ-COMPARE-ITEM-VALUE-PTR POINTER.
         03 AZ-COMPARE-ITEM-VALUE-LEN PIC S9(9) COMP-5.
         03 AZ-COMPARE-ITEM-EXP-VALUE-PTR POINTER.
         03 AZ-COMPARE-ITEM-EXP-VALUE-LEN PIC S9(9) COMP-5.
       1 AZ-TEST-EXPECTED-DATA-VALUE.
          3 ZUT00000000.
            5 PIC X(1) DISPLAY VALUE '0'.
          3 ZUT00000001.
            5 PIC X(1) DISPLAY VALUE 'Y'.
          3 ZUT00000002.
            5 PIC X(80) DISPLAY VALUE SPACES.
          3 ZUT00000003.
            5 PIC X(4) DISPLAY VALUE '0000'.
       LOCAL-STORAGE SECTION.
       1 AZ-COMPARE-ITEM-NAMES.
         3 ZUT00000004.
            5 PIC X(16) DISPLAY VALUE 'W-COMAREA-LENGTH'.
         3 ZUT00000006.
            5 PIC X(20) DISPLAY VALUE 'PROCESS-INDICATOR OF'.
            5 PIC X(20) DISPLAY VALUE ' W-COMMUNICATION-ARE'.
            5 PIC X(1) DISPLAY VALUE 'A'.
         3 ZUT00000008.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-PRINCIPLE-DA'.
            5 PIC X(20) DISPLAY VALUE 'TA OF W-COMMUNICATIO'.
            5 PIC X(6) DISPLAY VALUE 'N-AREA'.
         3 ZUT0000000A.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-NUMBER-OF-YE'.
            5 PIC X(20) DISPLAY VALUE 'ARS OF W-COMMUNICATI'.
            5 PIC X(7) DISPLAY VALUE 'ON-AREA'.
         3 ZUT0000000C.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-NUMBER-OF-MO'.
            5 PIC X(20) DISPLAY VALUE 'NTHS OF W-COMMUNICAT'.
            5 PIC X(8) DISPLAY VALUE 'ION-AREA'.
         3 ZUT0000000E.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-QUOTED-INTER'.
            5 PIC X(20) DISPLAY VALUE 'EST-RATE OF W-COMMUN'.
            5 PIC X(12) DISPLAY VALUE 'ICATION-AREA'.
         3 ZUT00000010.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-YEAR-MONTH-I'.
            5 PIC X(20) DISPLAY VALUE 'ND OF W-COMMUNICATIO'.
            5 PIC X(6) DISPLAY VALUE 'N-AREA'.
         3 ZUT00000012.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-RETURN-MONTH'.
            5 PIC X(20) DISPLAY VALUE '-PAYMENT OF W-COMMUN'.
            5 PIC X(12) DISPLAY VALUE 'ICATION-AREA'.
         3 ZUT00000014.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-ERRMSG OF W-'.
            5 PIC X(18) DISPLAY VALUE 'COMMUNICATION-AREA'.
         3 ZUT00000016.
            5 PIC X(20) DISPLAY VALUE 'EPSPCOM-PROGRAM-RETC'.
            5 PIC X(20) DISPLAY VALUE 'ODE-RDF OF W-COMMUNI'.
            5 PIC X(11) DISPLAY VALUE 'CATION-AREA'.
       1 AZ-COMPARE-WORK-ITEMS.
          3 ZUT00000005 PIC 9(4) OCCURS 2.
          3 ZUT00000007 PIC X(1) OCCURS 2.
          3 ZUT00000009 PIC -9(9).9(2) OCCURS 2.
          3 ZUT0000000B PIC -9(4) OCCURS 2.
          3 ZUT0000000D PIC -9(4) OCCURS 2.
          3 ZUT0000000F PIC -9(2).9(3) OCCURS 2.
          3 ZUT00000011 PIC X(1) OCCURS 2.
          3 ZUT00000013 PIC -9(7).9(2) OCCURS 2.
          3 ZUT00000015 PIC X(80) OCCURS 2.
          3 ZUT00000017 PIC X(4) OCCURS 2.
       LINKAGE SECTION.
       01 AZ-TEST                   PIC X(80).
       01 AZ-INFO-BLOCK.
          COPY BZUITERC.
       01 AZ-WK-RECORD-COUNT PIC 9(5) COMP-5 VALUE 0.
       01 AZ-MODX                   PIC X(4).
       01 AZ-DFHEIBLK.
         03 FILLER                  PIC X(85).
       01 AZ-DFHCOMMAREA.
         03 FILLER                  PIC X(1).
       01 AZ-COMPARE-ITEM-NAME      PIC X(254).
       01 AZ-COMPARE-ITEM-VALUE     PIC X(254).
       01 AZ-COMPARE-ITEM-EXP-VALUE PIC X(254).
       01 ARG0.
         03 ARG0-1        PIC X(2).
         03 ARG0-A        PIC X(28).
         03 ARG0-B REDEFINES ARG0-A.
           05 ARG0-2      PIC X(9).
           05 FILLER      PIC X(19).
         03 ARG0-C REDEFINES ARG0-A.
           05 FILLER      PIC X(6).
           05 ARG0-D.
             07 ARG0-3    PIC 9(1) OCCURS 22.
       01 ARG1            POINTER.
       01 ARG2            POINTER.
       01 ARG3            POINTER.
       01 ARG4            POINTER.
       01 ARG5            POINTER.
       01 ARG6            POINTER.
       01 ARG7            POINTER.
       01 ARG8            POINTER.
       01 ARG9            POINTER.
       01 ARG10           POINTER.
       01 AZ-CICS-TARGET-NAME-DEF4 PIC X(4).
       01 AZ-CICS-TARGET-NAME-DEF7 PIC X(7).
       01 AZ-CICS-TARGET-NAME-DEF8 PIC X(8).
       1 W-COMMUNICATION-AREA.
         10 PROCESS-INDICATOR PIC X.
         10 EPSPCOM-PRINCIPLE-DATA PIC S9(9)V99 COMP.
         10 EPSPCOM-NUMBER-OF-YEARS PIC S9(4) COMP.
         10 EPSPCOM-NUMBER-OF-MONTHS PIC S9(4) COMP.
         10 EPSPCOM-QUOTED-INTEREST-RATE PIC S9(2)v9(3) COMP.
         10 EPSPCOM-YEAR-MONTH-IND PIC X.
         10 EPSPCOM-RETURN-MONTH-PAYMENT PIC S9(7)V99 COMP.
         10 EPSPCOM-ERRMSG PIC X(80).
         10 EPSPCOM-PROGRAM-RETCODE PIC 9(4).
         88 EPS02-REQUEST-SUCCESS VALUE 0.
         10 EPSPCOM-PROGRAM-RETCODE-RDF REDEFINES
           EPSPCOM-PROGRAM-RETCODE PIC X(4).
       1 W-COMAREA-LENGTH PIC 9(4) COMP.
       PROCEDURE DIVISION.
      * CHECK OUTPUT VALUE
      * CICS_INPT_0E08_EPSCMORT.
           ENTRY 'CICS_INPT_0E08_EPSCMORT' USING AZ-TEST
           AZ-INFO-BLOCK AZ-DFHEIBLK AZ-DFHCOMMAREA ARG0 ARG1 ARG2
           ARG3 ARG4 ARG5 ARG6 ARG7 ARG8 ARG9 ARG10.
           DISPLAY 'CICS_0E08_EPSCMORT CHECK VALUES...'
           MOVE 4 TO RETURN-CODE.
           MOVE 0 TO AZ-TEST-LEN.
           INSPECT AZ-TEST TALLYING AZ-TEST-LEN FOR
           CHARACTERS BEFORE INITIAL SPACE.
      * SET GROUP OPTION COMMON MASK IN CICS GROUP
           MOVE X'E000' TO AZ-OPT-COMMASK-DATA2(1).
      * EXEC CICS RETURN X'0000'
           IF ARG0-1 = X'0E08'
             MOVE X'0000' TO AZ-OPT-MASK-DATA2
             MOVE 2 TO AZ-OPT-BYTECOUNT
             CALL 'EVALOPT' USING AZ-OPT-MASK-DATA2
               AZ-OPT-COMMASK-DATA2(1) ARG0-2
               AZ-OPT-BYTECOUNT AZ-OPT-RC
             IF AZ-OPT-RC = 0 THEN
               MOVE 3 TO AZ-LINE-BYTE
               PERFORM GETLINENUM
               DISPLAY 'EXEC CICS RETURN X''0000'''
                ' L=' AZ-LINE-NUM
               MOVE ITER OF AZ-INFO-BLOCK TO AZ-RECORD-COUNT-OT(1)
               EVALUATE AZ-TEST(1:AZ-TEST-LEN)
                 WHEN SPACE
                   CONTINUE
                 WHEN 'TEST3'
                   PERFORM O0E080-TEST3
                   CONTINUE
                 WHEN OTHER
                   CONTINUE
                 END-EVALUATE
             END-IF
           END-IF.
      * EXEC CICS RETURN X'E000'
           IF ARG0-1 = X'0E08'
             MOVE X'E000' TO AZ-OPT-MASK-DATA2
             MOVE 2 TO AZ-OPT-BYTECOUNT
             CALL 'EVALOPT' USING AZ-OPT-MASK-DATA2
               AZ-OPT-COMMASK-DATA2(1) ARG0-2
               AZ-OPT-BYTECOUNT AZ-OPT-RC
             IF AZ-OPT-RC = 0 THEN
               MOVE 3 TO AZ-LINE-BYTE
               PERFORM GETLINENUM
               DISPLAY 'EXEC CICS RETURN X''E000'''
                ' L=' AZ-LINE-NUM
               SET ADDRESS OF W-COMMUNICATION-AREA TO ADDRESS OF ARG2
               SET ADDRESS OF W-COMAREA-LENGTH TO ADDRESS OF ARG3
               MOVE ITER OF AZ-INFO-BLOCK TO AZ-RECORD-COUNT-OT(2)
               EVALUATE AZ-TEST(1:AZ-TEST-LEN)
                 WHEN SPACE
                   CONTINUE
                 WHEN 'TEST3'
                   PERFORM O0E081-TEST3
                   CONTINUE
                 WHEN OTHER
                   CONTINUE
                 END-EVALUATE
             END-IF
           END-IF.
           PERFORM TEARDOWN.
      * SET INPUT VALUE
      * CICS_OUTP_0E08_EPSCMORT.
           ENTRY 'CICS_OUTP_0E08_EPSCMORT' USING AZ-TEST
           AZ-INFO-BLOCK AZ-DFHEIBLK AZ-DFHCOMMAREA ARG0 ARG1 ARG2
           ARG3 ARG4 ARG5 ARG6 ARG7 ARG8 ARG9 ARG10.
           DISPLAY 'CICS_0E08_EPSCMORT INPUT VALUES...'
           MOVE 0 TO RETURN-CODE.
           MOVE 0 TO AZ-TEST-LEN.
           INSPECT AZ-TEST TALLYING AZ-TEST-LEN FOR
           CHARACTERS BEFORE INITIAL SPACE.
      * SET GROUP OPTION COMMON MASK IN CICS GROUP
           MOVE X'E000' TO AZ-OPT-COMMASK-DATA2(1).
      * EXEC CICS RETURN X'0000'
           IF ARG0-1 = X'0E08'
             MOVE X'0000' TO AZ-OPT-MASK-DATA2
             MOVE 2 TO AZ-OPT-BYTECOUNT
             CALL 'EVALOPT' USING AZ-OPT-MASK-DATA2
               AZ-OPT-COMMASK-DATA2(1) ARG0-2
               AZ-OPT-BYTECOUNT AZ-OPT-RC
             IF AZ-OPT-RC = 0 THEN
               MOVE 3 TO AZ-LINE-BYTE
               PERFORM GETLINENUM
               DISPLAY 'EXEC CICS RETURN X''0000'''
                ' L=' AZ-LINE-NUM
               MOVE ITER OF AZ-INFO-BLOCK TO AZ-RECORD-COUNT-IN(1)
               EVALUATE AZ-TEST(1:AZ-TEST-LEN)
                 WHEN SPACE
                   CONTINUE
                 WHEN 'TEST3'
                   CONTINUE
                 WHEN OTHER
                   CONTINUE
                 END-EVALUATE
             END-IF
           END-IF.
      * EXEC CICS RETURN X'E000'
           IF ARG0-1 = X'0E08'
             MOVE X'E000' TO AZ-OPT-MASK-DATA2
             MOVE 2 TO AZ-OPT-BYTECOUNT
             CALL 'EVALOPT' USING AZ-OPT-MASK-DATA2
               AZ-OPT-COMMASK-DATA2(1) ARG0-2
               AZ-OPT-BYTECOUNT AZ-OPT-RC
             IF AZ-OPT-RC = 0 THEN
               MOVE 3 TO AZ-LINE-BYTE
               PERFORM GETLINENUM
               DISPLAY 'EXEC CICS RETURN X''E000'''
                ' L=' AZ-LINE-NUM
               SET ADDRESS OF W-COMMUNICATION-AREA TO ADDRESS OF ARG2
               SET ADDRESS OF W-COMAREA-LENGTH TO ADDRESS OF ARG3
               MOVE ITER OF AZ-INFO-BLOCK TO AZ-RECORD-COUNT-IN(2)
               EVALUATE AZ-TEST(1:AZ-TEST-LEN)
                 WHEN SPACE
                   CONTINUE
                 WHEN 'TEST3'
                   CONTINUE
                 WHEN OTHER
                   CONTINUE
                 END-EVALUATE
             END-IF
           END-IF.
           PERFORM TEARDOWN.
       O0E080-TEST3.
           IF AZ-RECORD-COUNT-OT(1) = 0 THEN
             CONTINUE
           ELSE
             CONTINUE
           END-IF.
       O0E081-TEST3.
           INITIALIZE AZ-COMPARE-WORK-ITEMS
           IF AZ-RECORD-COUNT-OT(2) = 0 THEN
             CONTINUE
           ELSE IF AZ-RECORD-COUNT-OT(2) = 1
             MOVE 0 TO RETURN-CODE
             IF PROCESS-INDICATOR OF W-COMMUNICATION-AREA =
           ZUT00000000 THEN
               CONTINUE
             ELSE
               MOVE PROCESS-INDICATOR OF W-COMMUNICATION-AREA TO
           ZUT00000007(1)
               MOVE ZUT00000000 TO ZUT00000007(2)
               SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000006
               MOVE LENGTH OF ZUT00000006 TO AZ-COMPARE-ITEM-NAME-LEN
               SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF
           ZUT00000007(1)
               MOVE 1 TO AZ-COMPARE-ITEM-VALUE-LEN
               SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000007(2)
               MOVE 1 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
               MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
               STRING
                 'COMPARE FAILED AT RECORD 1 '
                 '''COMMAREA'' '
                 'IN EXEC CICS RETURN '
                 '(LISTINGLINE=' AZ-LINE-NUM ').'
                 DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
                 WITH POINTER MESSAGE-LEN OF BZ-ASSERT
               END-STRING
               SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
               PERFORM THROW-ASSERTION
             END-IF
             IF EPSPCOM-PRINCIPLE-DATA OF W-COMMUNICATION-AREA =
           200000.99 THEN
               CONTINUE
             ELSE
                MOVE EPSPCOM-PRINCIPLE-DATA OF W-COMMUNICATION-AREA TO
           ZUT00000009(1)
               MOVE 200000.99 TO ZUT00000009(2)
               SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000008
               MOVE LENGTH OF ZUT00000008 TO AZ-COMPARE-ITEM-NAME-LEN
               SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF
           ZUT00000009(1)
               MOVE 13 TO AZ-COMPARE-ITEM-VALUE-LEN
               SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000009(2)
               MOVE 13 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
               MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
               STRING
                 'COMPARE FAILED AT RECORD 1 '
                 '''COMMAREA'' '
                 'IN EXEC CICS RETURN '
                 '(LISTINGLINE=' AZ-LINE-NUM ').'
                 DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
                 WITH POINTER MESSAGE-LEN OF BZ-ASSERT
               END-STRING
               SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
               PERFORM THROW-ASSERTION
             END-IF
             IF EPSPCOM-NUMBER-OF-YEARS OF W-COMMUNICATION-AREA = 15
           THEN
               CONTINUE
             ELSE
                MOVE EPSPCOM-NUMBER-OF-YEARS OF W-COMMUNICATION-AREA
           TO ZUT0000000B(1)
               MOVE 15 TO ZUT0000000B(2)
               SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT0000000A
               MOVE LENGTH OF ZUT0000000A TO AZ-COMPARE-ITEM-NAME-LEN
               SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF
           ZUT0000000B(1)
               MOVE 5 TO AZ-COMPARE-ITEM-VALUE-LEN
               SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT0000000B(2)
               MOVE 5 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
               MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
               STRING
                 'COMPARE FAILED AT RECORD 1 '
                 '''COMMAREA'' '
                 'IN EXEC CICS RETURN '
                 '(LISTINGLINE=' AZ-LINE-NUM ').'
                 DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
                 WITH POINTER MESSAGE-LEN OF BZ-ASSERT
               END-STRING
               SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
               PERFORM THROW-ASSERTION
             END-IF
             IF EPSPCOM-NUMBER-OF-MONTHS OF W-COMMUNICATION-AREA = 0
           THEN
               CONTINUE
             ELSE
                MOVE EPSPCOM-NUMBER-OF-MONTHS OF W-COMMUNICATION-AREA
           TO ZUT0000000D(1)
               MOVE 0 TO ZUT0000000D(2)
               SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT0000000C
               MOVE LENGTH OF ZUT0000000C TO AZ-COMPARE-ITEM-NAME-LEN
               SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF
           ZUT0000000D(1)
               MOVE 5 TO AZ-COMPARE-ITEM-VALUE-LEN
               SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT0000000D(2)
               MOVE 5 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
               MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
               STRING
                 'COMPARE FAILED AT RECORD 1 '
                 '''COMMAREA'' '
                 'IN EXEC CICS RETURN '
                 '(LISTINGLINE=' AZ-LINE-NUM ').'
                 DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
                 WITH POINTER MESSAGE-LEN OF BZ-ASSERT
               END-STRING
               SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
               PERFORM THROW-ASSERTION
             END-IF
             IF EPSPCOM-QUOTED-INTEREST-RATE OF W-COMMUNICATION-AREA =
           6.990 THEN
               CONTINUE
             ELSE
                MOVE EPSPCOM-QUOTED-INTEREST-RATE OF
           W-COMMUNICATION-AREA TO ZUT0000000F(1)
               MOVE 6.990 TO ZUT0000000F(2)
               SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT0000000E
               MOVE LENGTH OF ZUT0000000E TO AZ-COMPARE-ITEM-NAME-LEN
               SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF
           ZUT0000000F(1)
               MOVE 7 TO AZ-COMPARE-ITEM-VALUE-LEN
               SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT0000000F(2)
               MOVE 7 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
               MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
               STRING
                 'COMPARE FAILED AT RECORD 1 '
                 '''COMMAREA'' '
                 'IN EXEC CICS RETURN '
                 '(LISTINGLINE=' AZ-LINE-NUM ').'
                 DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
                 WITH POINTER MESSAGE-LEN OF BZ-ASSERT
               END-STRING
               SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
               PERFORM THROW-ASSERTION
             END-IF
             IF EPSPCOM-YEAR-MONTH-IND OF W-COMMUNICATION-AREA =
           ZUT00000001 THEN
               CONTINUE
             ELSE
               MOVE EPSPCOM-YEAR-MONTH-IND OF W-COMMUNICATION-AREA TO
           ZUT00000011(1)
               MOVE ZUT00000001 TO ZUT00000011(2)
               SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000010
               MOVE LENGTH OF ZUT00000010 TO AZ-COMPARE-ITEM-NAME-LEN
               SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF
           ZUT00000011(1)
               MOVE 1 TO AZ-COMPARE-ITEM-VALUE-LEN
               SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000011(2)
               MOVE 1 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
               MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
               STRING
                 'COMPARE FAILED AT RECORD 1 '
                 '''COMMAREA'' '
                 'IN EXEC CICS RETURN '
                 '(LISTINGLINE=' AZ-LINE-NUM ').'
                 DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
                 WITH POINTER MESSAGE-LEN OF BZ-ASSERT
               END-STRING
               SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
               PERFORM THROW-ASSERTION
             END-IF
             IF EPSPCOM-RETURN-MONTH-PAYMENT OF W-COMMUNICATION-AREA =
           1796.55 THEN
               CONTINUE
             ELSE
                MOVE EPSPCOM-RETURN-MONTH-PAYMENT OF
           W-COMMUNICATION-AREA TO ZUT00000013(1)
               MOVE 1796.55 TO ZUT00000013(2)
               SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000012
               MOVE LENGTH OF ZUT00000012 TO AZ-COMPARE-ITEM-NAME-LEN
               SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF
           ZUT00000013(1)
               MOVE 11 TO AZ-COMPARE-ITEM-VALUE-LEN
               SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000013(2)
               MOVE 11 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
               MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
               STRING
                 'COMPARE FAILED AT RECORD 1 '
                 '''COMMAREA'' '
                 'IN EXEC CICS RETURN '
                 '(LISTINGLINE=' AZ-LINE-NUM ').'
                 DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
                 WITH POINTER MESSAGE-LEN OF BZ-ASSERT
               END-STRING
               SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
               PERFORM THROW-ASSERTION
             END-IF
             IF EPSPCOM-ERRMSG OF W-COMMUNICATION-AREA = ZUT00000002
           THEN
               CONTINUE
             ELSE
               MOVE EPSPCOM-ERRMSG OF W-COMMUNICATION-AREA TO
           ZUT00000015(1)
               MOVE ZUT00000002 TO ZUT00000015(2)
               SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000014
               MOVE LENGTH OF ZUT00000014 TO AZ-COMPARE-ITEM-NAME-LEN
               SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF
           ZUT00000015(1)
               MOVE 80 TO AZ-COMPARE-ITEM-VALUE-LEN
               SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000015(2)
               MOVE 80 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
               MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
               STRING
                 'COMPARE FAILED AT RECORD 1 '
                 '''COMMAREA'' '
                 'IN EXEC CICS RETURN '
                 '(LISTINGLINE=' AZ-LINE-NUM ').'
                 DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
                 WITH POINTER MESSAGE-LEN OF BZ-ASSERT
               END-STRING
               SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
               PERFORM THROW-ASSERTION
             END-IF
             IF EPSPCOM-PROGRAM-RETCODE-RDF OF W-COMMUNICATION-AREA =
           ZUT00000003 THEN
               CONTINUE
             ELSE
               MOVE EPSPCOM-PROGRAM-RETCODE-RDF OF
           W-COMMUNICATION-AREA TO ZUT00000017(1)
               MOVE ZUT00000003 TO ZUT00000017(2)
               SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000016
               MOVE LENGTH OF ZUT00000016 TO AZ-COMPARE-ITEM-NAME-LEN
               SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF
           ZUT00000017(1)
               MOVE 4 TO AZ-COMPARE-ITEM-VALUE-LEN
               SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000017(2)
               MOVE 4 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
               MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
               STRING
                 'COMPARE FAILED AT RECORD 1 '
                 '''COMMAREA'' '
                 'IN EXEC CICS RETURN '
                 '(LISTINGLINE=' AZ-LINE-NUM ').'
                 DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
                 WITH POINTER MESSAGE-LEN OF BZ-ASSERT
               END-STRING
               SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
               PERFORM THROW-ASSERTION
             END-IF
             IF W-COMAREA-LENGTH = 106 THEN
               CONTINUE
             ELSE
                MOVE W-COMAREA-LENGTH TO ZUT00000005(1)
               MOVE 106 TO ZUT00000005(2)
               SET AZ-COMPARE-ITEM-NAME-PTR TO ADDRESS OF ZUT00000004
               MOVE LENGTH OF ZUT00000004 TO AZ-COMPARE-ITEM-NAME-LEN
               SET AZ-COMPARE-ITEM-VALUE-PTR TO ADDRESS OF
           ZUT00000005(1)
               MOVE 4 TO AZ-COMPARE-ITEM-VALUE-LEN
               SET AZ-COMPARE-ITEM-EXP-VALUE-PTR TO ADDRESS OF
           ZUT00000005(2)
               MOVE 4 TO AZ-COMPARE-ITEM-EXP-VALUE-LEN
               MOVE 1 TO MESSAGE-LEN OF BZ-ASSERT
               STRING
                 'COMPARE FAILED AT RECORD 1 '
                 '''LENGTH'' '
                 'IN EXEC CICS RETURN '
                 '(LISTINGLINE=' AZ-LINE-NUM ').'
                 DELIMITED BY SIZE INTO MESSAGE-TXT OF BZ-ASSERT
                 WITH POINTER MESSAGE-LEN OF BZ-ASSERT
               END-STRING
               SUBTRACT 1 FROM MESSAGE-LEN OF BZ-ASSERT
               PERFORM THROW-ASSERTION
             END-IF
           ELSE
             CONTINUE
           END-IF
           END-IF.
       THROW-ASSERTION.
           SET ADDRESS OF AZ-COMPARE-ITEM-NAME TO
           AZ-COMPARE-ITEM-NAME-PTR.
           SET ADDRESS OF AZ-COMPARE-ITEM-VALUE TO
           AZ-COMPARE-ITEM-VALUE-PTR.
           SET ADDRESS OF AZ-COMPARE-ITEM-EXP-VALUE TO
           AZ-COMPARE-ITEM-EXP-VALUE-PTR.
      *    DISPLAY ERROR MESSAGE AND ENDS TEST
           DISPLAY '****************************************************
      -    '****************************'
           DISPLAY 'AZU2001W THE TEST "' AZ-TEST(1:AZ-TEST-LEN) '" FAILE
      -    'D DUE TO AN ASSERTION.'
           DISPLAY 'AZU1101I ' MESSAGE-TXT OF BZ-ASSERT(1:MESSAGE-LEN
           OF BZ-ASSERT)
           DISPLAY ' DATA ITEM NAME : '
           AZ-COMPARE-ITEM-NAME(1:AZ-COMPARE-ITEM-NAME-LEN)
           DISPLAY '  VALUE         : '
           AZ-COMPARE-ITEM-VALUE(1:AZ-COMPARE-ITEM-VALUE-LEN)
           DISPLAY '  EXPECTED VALUE: '
           AZ-COMPARE-ITEM-EXP-VALUE(1:AZ-COMPARE-ITEM-EXP-VALUE-LEN)
           DISPLAY '****************************************************
      -    '****************************'
           CALL BZUASSRT USING BZ-P1 BZ-P2 BZ-P3 BZ-ASSERT
           MOVE 1 TO TRACE-LEN OF BZ-TRACE
           STRING 'ITEM NAME='
           AZ-COMPARE-ITEM-NAME(1:AZ-COMPARE-ITEM-NAME-LEN)
               DELIMITED BY SIZE INTO TRACE-TXT OF BZ-TRACE
               WITH POINTER TRACE-LEN OF BZ-TRACE
             END-STRING
           SUBTRACT 1 FROM TRACE-LEN OF BZ-TRACE
           CALL BZUTRACE USING BZ-TRACE
           MOVE 1 TO TRACE-LEN OF BZ-TRACE
           STRING 'VALUE='
           AZ-COMPARE-ITEM-VALUE(1:AZ-COMPARE-ITEM-VALUE-LEN)
               DELIMITED BY SIZE INTO TRACE-TXT OF BZ-TRACE
               WITH POINTER TRACE-LEN OF BZ-TRACE
             END-STRING
           SUBTRACT 1 FROM TRACE-LEN OF BZ-TRACE
           CALL BZUTRACE USING BZ-TRACE
           MOVE 1 TO TRACE-LEN OF BZ-TRACE
           STRING 'EXPECTED VALUE='
           AZ-COMPARE-ITEM-EXP-VALUE(1:AZ-COMPARE-ITEM-EXP-VALUE-LEN)
               DELIMITED BY SIZE INTO TRACE-TXT OF BZ-TRACE
               WITH POINTER TRACE-LEN OF BZ-TRACE
             END-STRING
           SUBTRACT 1 FROM TRACE-LEN OF BZ-TRACE
           CALL BZUTRACE USING BZ-TRACE
           EXIT.
       GETLINENUM.
           MOVE 1 TO AZ-LINE-J
           PERFORM VARYING AZ-LINE-I FROM AZ-LINE-BYTE BY 1
             UNTIL AZ-LINE-I > AZ-LINE-BYTE + 5
             MOVE ARG0-3(AZ-LINE-I) TO AZ-LINE-NUM-RD(AZ-LINE-J)
             ADD 1 TO AZ-LINE-J
           END-PERFORM
           EXIT.
       TEARDOWN.
           DISPLAY 'CICS_0E08_EPSCMORT SUCCESSFUL.'
           GOBACK.
       END PROGRAM 'CICS_0E08_EPSCMORT'.
