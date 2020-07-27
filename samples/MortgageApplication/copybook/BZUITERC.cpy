      *****************************************************************
      * LICENSED MATERIALS - PROPERTY OF IBM                          *
      * 5655-AC5 5724-T07 5737-J31 COPYRIGHT IBM CORP. 2019, 2019     *
      * ALL RIGHTS RESERVED                                           *
      * US GOVERNMENT USERS RESTRICTED RIGHTS  -  USE, DUPLICATION    *
      * OR DISCLOSURE RESTRICTED BY GSA ADP SCHEDULE CONTRACT WITH    *
      * IBM CORP.                                                     *
      *****************************************************************
      *                                                               *
      * IBM Z/OS DYNAMIC TEST RUNNER                                  *
      *                                                               *
      * COPYBOOK FOR USING THE Z/OS DYNAMIC TEST RUNNER               *
      * USED BY MASTER TESTCASE CALLBACK PROGRAM                      *
      *                                                               *
      *****************************************************************
           05  ITER   PIC 9(9) COMP-4.
           05  O00001 PIC 9(9) COMP-4.
           05  O00002 PIC 9(9) COMP-4.
           05  O00003 PIC 9(9) COMP-4.
           05  O00004 PIC 9(9) COMP-4.
           05  O00005 PIC 9(9) COMP-4.
           05  O00006 PIC 9(9) COMP-4.
           05  O00007 PIC 9(9) COMP-4.
           05  O00008 PIC 9(9) COMP-4.
           05  O00009 PIC 9(9) COMP-4.
           05  O00010 PIC 9(9) COMP-4.
           05  O00011 PIC 9(9) COMP-4.
           05  O00012 PIC 9(9) COMP-4.
           05  O00013 PIC 9(9) COMP-4.
           05  O00014 PIC 9(9) COMP-4.
           05  O00015 PIC 9(9) COMP-4.
           05  O00016 PIC 9(9) COMP-4.
           05  O00017 PIC 9(9) COMP-4.
           05  O00018 PIC 9(9) COMP-4.
           05  O00019 PIC 9(9) COMP-4.
           05  O00020 PIC 9(9) COMP-4.
           05  O00021 PIC 9(9) COMP-4.
           05  O00022 PIC 9(9) COMP-4.
           05  O00023 PIC 9(9) COMP-4.
           05  O00024 PIC 9(9) COMP-4.
           05  O00025 PIC 9(9) COMP-4.
           05  O00026 PIC 9(9) COMP-4.
           05  O00027 PIC 9(9) COMP-4.
           05  O00028 PIC 9(9) COMP-4.
           05  O00029 PIC 9(9) COMP-4.
           05  O00030 PIC 9(9) COMP-4.
           05  O00031 PIC 9(9) COMP-4.
           05  O00032 PIC 9(9) COMP-4.
           05  O00033 PIC 9(9) COMP-4.
           05  O00034 PIC 9(9) COMP-4.
           05  O00035 PIC 9(9) COMP-4.
           05  O00036 PIC 9(9) COMP-4.
           05  O00037 PIC 9(9) COMP-4.
           05  O00038 PIC 9(9) COMP-4.
           05  O00039 PIC 9(9) COMP-4.
           05  O00040 PIC 9(9) COMP-4.
           05  O00041 PIC 9(9) COMP-4.
           05  O00042 PIC 9(9) COMP-4.
           05  O00043 PIC 9(9) COMP-4.
           05  O00044 PIC 9(9) COMP-4.
           05  O00045 PIC 9(9) COMP-4.
           05  O00046 PIC 9(9) COMP-4.
           05  O00047 PIC 9(9) COMP-4.
           05  O00048 PIC 9(9) COMP-4.
           05  O00049 PIC 9(9) COMP-4.
           05  O00050 PIC 9(9) COMP-4.
           05  O00051 PIC 9(9) COMP-4.
           05  O00052 PIC 9(9) COMP-4.
           05  O00053 PIC 9(9) COMP-4.
           05  O00054 PIC 9(9) COMP-4.
           05  O00055 PIC 9(9) COMP-4.
           05  O00056 PIC 9(9) COMP-4.
           05  O00057 PIC 9(9) COMP-4.
           05  O00058 PIC 9(9) COMP-4.
           05  O00059 PIC 9(9) COMP-4.
           05  O00060 PIC 9(9) COMP-4.
           05  O00061 PIC 9(9) COMP-4.
           05  O00062 PIC 9(9) COMP-4.
           05  O00063 PIC 9(9) COMP-4.
           05  O00064 PIC 9(9) COMP-4.
           05  O00065 PIC 9(9) COMP-4.
           05  O00066 PIC 9(9) COMP-4.
           05  O00067 PIC 9(9) COMP-4.
           05  O00068 PIC 9(9) COMP-4.
           05  O00069 PIC 9(9) COMP-4.
           05  O00070 PIC 9(9) COMP-4.
           05  O00071 PIC 9(9) COMP-4.
           05  O00072 PIC 9(9) COMP-4.
           05  O00073 PIC 9(9) COMP-4.
           05  O00074 PIC 9(9) COMP-4.
           05  O00075 PIC 9(9) COMP-4.
           05  O00076 PIC 9(9) COMP-4.
           05  O00077 PIC 9(9) COMP-4.
           05  O00078 PIC 9(9) COMP-4.
           05  O00079 PIC 9(9) COMP-4.
           05  O00080 PIC 9(9) COMP-4.
           05  O00081 PIC 9(9) COMP-4.
           05  O00082 PIC 9(9) COMP-4.
           05  O00083 PIC 9(9) COMP-4.
           05  O00084 PIC 9(9) COMP-4.
           05  O00085 PIC 9(9) COMP-4.
           05  O00086 PIC 9(9) COMP-4.
           05  O00087 PIC 9(9) COMP-4.
           05  O00088 PIC 9(9) COMP-4.
           05  O00089 PIC 9(9) COMP-4.
           05  O00090 PIC 9(9) COMP-4.
           05  O00091 PIC 9(9) COMP-4.
           05  O00092 PIC 9(9) COMP-4.
           05  O00093 PIC 9(9) COMP-4.
           05  O00094 PIC 9(9) COMP-4.
           05  O00095 PIC 9(9) COMP-4.
           05  O00096 PIC 9(9) COMP-4.
           05  O00097 PIC 9(9) COMP-4.
           05  O00098 PIC 9(9) COMP-4.
           05  O00099 PIC 9(9) COMP-4.
           05  O00100 PIC 9(9) COMP-4.